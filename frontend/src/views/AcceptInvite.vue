<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { getInvitation, acceptInvitation } from '@/services/sharing'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const token = route.params.token as string

const loading = ref(true)
const errorMessage = ref('')
const showLoginPrompt = ref(false)
const accepting = ref(false)
const acceptError = ref('')

async function init() {
  loading.value = true
  errorMessage.value = ''
  showLoginPrompt.value = false

  try {
    const invitation = await getInvitation(token)

    if (invitation.expired) {
      errorMessage.value = 'This invitation link has expired'
      return
    }

    if (authStore.isAuthenticated) {
      await acceptCurrentInvitation()
    } else {
      showLoginPrompt.value = true
    }
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      errorMessage.value = 'This invitation link is invalid'
    } else {
      errorMessage.value = 'This invitation link is invalid'
    }
  } finally {
    loading.value = false
  }
}

async function acceptCurrentInvitation() {
  accepting.value = true
  acceptError.value = ''
  try {
    await acceptInvitation(token)
    router.push({ name: 'home' })
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 410) {
      errorMessage.value = 'This invitation link has expired'
    } else {
      acceptError.value = 'Unable to accept this invitation. Please try again later.'
    }
  } finally {
    accepting.value = false
  }
}
</script>

<template>
  <div class="accept-invite-page">
    <div class="accept-invite-card">
      <h1>Shared list invitation</h1>

      <p v-if="loading">Checking invitation...</p>

      <p v-else-if="errorMessage" class="error-message" role="alert">{{ errorMessage }}</p>

      <template v-else-if="showLoginPrompt">
        <p>You've been invited to a shared list — log in or register to continue.</p>
        <div class="invite-actions">
          <RouterLink :to="{ path: '/register', query: { redirectTo: `/invite/${token}` } }">
            Register
          </RouterLink>
          <RouterLink :to="{ path: '/login', query: { redirectTo: `/invite/${token}` } }">
            Log in instead
          </RouterLink>
        </div>
      </template>

      <template v-else>
        <p v-if="accepting">Joining shared list...</p>
        <p v-if="acceptError" class="error-message" role="alert">{{ acceptError }}</p>
      </template>
    </div>
  </div>
</template>

<style scoped>
.accept-invite-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 1rem;
}

.accept-invite-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  width: 100%;
  max-width: 360px;
  padding: 2rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  text-align: center;
}

.accept-invite-card h1 {
  margin: 0 0 0.5rem;
}

.error-message {
  margin: 0;
  color: #b00020;
  font-size: 0.95rem;
}

.invite-actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.invite-actions a {
  padding: 0.6rem 1.2rem;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: 600;
  text-decoration: none;
}

.invite-actions a:first-child {
  background-color: #2563eb;
  color: #fff;
}

.invite-actions a:last-child {
  border: 1px solid #2563eb;
  color: #2563eb;
}
</style>
