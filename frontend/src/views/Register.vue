<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const emailError = ref('')
const passwordError = ref('')
const formError = ref('')
const isSubmitting = ref(false)

async function handleSubmit() {
  emailError.value = ''
  passwordError.value = ''
  formError.value = ''
  isSubmitting.value = true

  try {
    const response = await api.post('/auth/register', {
      email: email.value,
      password: password.value,
    })

    authStore.setToken(response.data.token)

    const redirectTo = route.query.redirectTo
    if (typeof redirectTo === 'string' && redirectTo.startsWith('/')) {
      router.push(redirectTo)
    } else {
      router.push({ name: 'home' })
    }
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      const status = error.response.status
      const data = error.response.data

      if (status === 400 && data?.fieldErrors) {
        emailError.value = data.fieldErrors.email ?? ''
        passwordError.value = data.fieldErrors.password ?? ''
      } else if (status === 409) {
        emailError.value = data?.message ?? 'This email is already in use.'
      } else {
        formError.value = 'Unable to register. Please check your details and try again.'
      }
    } else {
      formError.value = 'Something went wrong. Please try again.'
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <form class="register-form" @submit.prevent="handleSubmit">
      <h1>Register</h1>

      <div class="form-field">
        <label for="email">Email</label>
        <input
          id="email"
          v-model="email"
          type="email"
          name="email"
          autocomplete="email"
          required
        />
        <p v-if="emailError" class="field-error" role="alert">{{ emailError }}</p>
      </div>

      <div class="form-field">
        <label for="password">Password</label>
        <input
          id="password"
          v-model="password"
          type="password"
          name="password"
          autocomplete="new-password"
          required
        />
        <p v-if="passwordError" class="field-error" role="alert">{{ passwordError }}</p>
      </div>

      <p v-if="formError" class="error-message" role="alert">
        {{ formError }}
      </p>

      <button type="submit" :disabled="isSubmitting">
        {{ isSubmitting ? 'Registering...' : 'Register' }}
      </button>

      <p class="form-footer">
        Already have an account? <RouterLink to="/login">Log in</RouterLink>
      </p>
    </form>
  </div>
</template>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 1rem;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  width: 100%;
  max-width: 320px;
  padding: 2rem;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.register-form h1 {
  margin: 0 0 0.5rem;
  text-align: center;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-field label {
  font-weight: 600;
  font-size: 0.9rem;
}

.form-field input {
  padding: 0.5rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 1rem;
}

.field-error {
  margin: 0;
  color: #b00020;
  font-size: 0.85rem;
}

.error-message {
  margin: 0;
  color: #b00020;
  font-size: 0.9rem;
}

button[type='submit'] {
  padding: 0.6rem;
  border: none;
  border-radius: 4px;
  background-color: #2563eb;
  color: #fff;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}

button[type='submit']:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-footer {
  margin: 0;
  text-align: center;
  font-size: 0.9rem;
}
</style>
