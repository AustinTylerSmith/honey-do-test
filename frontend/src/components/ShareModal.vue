<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import {
  inviteCollaborator,
  listCollaborators,
  removeCollaborator,
  type Collaborator,
} from '@/services/sharing'

const emit = defineEmits<{
  (e: 'close'): void
}>()

const email = ref('')
const inviteError = ref('')
const inviteSuccess = ref('')
const isSubmitting = ref(false)

const collaborators = ref<Collaborator[]>([])
const loadingCollaborators = ref(false)
const collaboratorsError = ref('')
const removingUserId = ref<number | null>(null)

async function fetchCollaborators() {
  loadingCollaborators.value = true
  collaboratorsError.value = ''
  try {
    collaborators.value = await listCollaborators()
  } catch (error) {
    collaboratorsError.value = 'Failed to load collaborators. Please try again later.'
  } finally {
    loadingCollaborators.value = false
  }
}

async function handleSubmit() {
  inviteError.value = ''
  inviteSuccess.value = ''

  const trimmedEmail = email.value.trim()
  if (!trimmedEmail) return

  isSubmitting.value = true
  try {
    await inviteCollaborator(trimmedEmail)
    inviteSuccess.value = `Invitation sent to ${trimmedEmail}.`
    email.value = ''
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      const status = error.response.status
      const data = error.response.data
      if (status === 409) {
        inviteError.value = data?.message ?? 'This user already has access to this list'
      } else if (status === 400) {
        inviteError.value = data?.message ?? 'Please enter a valid email address.'
      } else {
        inviteError.value = 'Unable to send invitation. Please try again.'
      }
    } else {
      inviteError.value = 'Something went wrong. Please try again.'
    }
  } finally {
    isSubmitting.value = false
  }
}

async function handleRemove(userId: number) {
  removingUserId.value = userId
  collaboratorsError.value = ''
  try {
    await removeCollaborator(userId)
    await fetchCollaborators()
  } catch (error) {
    collaboratorsError.value = 'Failed to remove collaborator. Please try again later.'
  } finally {
    removingUserId.value = null
  }
}

function handleClose() {
  emit('close')
}

onMounted(() => {
  fetchCollaborators()
})
</script>

<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal" role="dialog" aria-modal="true">
      <h2>Share list</h2>

      <form @submit.prevent="handleSubmit">
        <div class="form-field">
          <label for="invite-email">Invite by email</label>
          <input
            id="invite-email"
            v-model="email"
            type="email"
            name="email"
            placeholder="someone@example.com"
            autofocus
            required
          />
        </div>

        <p v-if="inviteError" class="error-message" role="alert">{{ inviteError }}</p>
        <p v-if="inviteSuccess" class="success-message">{{ inviteSuccess }}</p>

        <div class="modal-actions">
          <button type="button" class="secondary-button" @click="handleClose">Close</button>
          <button type="submit" :disabled="isSubmitting || !email.trim()">
            {{ isSubmitting ? 'Sending...' : 'Send Invite' }}
          </button>
        </div>
      </form>

      <div class="collaborators-section">
        <h3>Collaborators</h3>

        <p v-if="loadingCollaborators">Loading collaborators...</p>
        <p v-else-if="collaboratorsError" class="error-message">{{ collaboratorsError }}</p>

        <ul v-else class="collaborator-list">
          <li v-for="collaborator in collaborators" :key="collaborator.userId" class="collaborator-row">
            <span class="collaborator-email">
              {{ collaborator.email }}
              <span v-if="collaborator.owner" class="owner-badge">(Owner)</span>
            </span>
            <button
              v-if="!collaborator.owner"
              type="button"
              class="remove-button"
              :disabled="removingUserId === collaborator.userId"
              @click="handleRemove(collaborator.userId)"
            >
              {{ removingUserId === collaborator.userId ? 'Removing...' : 'Remove' }}
            </button>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  z-index: 100;
}

.modal {
  background: #fff;
  border-radius: 8px;
  padding: 1.5rem;
  width: 100%;
  max-width: 400px;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.modal h2 {
  margin: 0;
}

.modal form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
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

.error-message {
  margin: 0;
  color: #b00020;
  font-size: 0.9rem;
}

.success-message {
  margin: 0;
  color: #1a7f37;
  font-size: 0.9rem;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.modal-actions button {
  padding: 0.6rem 1.2rem;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}

.modal-actions button[type='submit'] {
  background-color: #2563eb;
  color: #fff;
}

.modal-actions button[type='submit']:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.secondary-button {
  background-color: #eee;
  color: #333;
}

.collaborators-section {
  border-top: 1px solid #e0e0e0;
  padding-top: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.collaborators-section h3 {
  margin: 0;
  font-size: 1rem;
}

.collaborator-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
}

.collaborator-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid #f0f0f0;
}

.collaborator-row:last-child {
  border-bottom: none;
}

.collaborator-email {
  font-size: 0.95rem;
  word-break: break-all;
}

.owner-badge {
  color: #666;
  font-size: 0.85rem;
  font-weight: 600;
}

.remove-button {
  padding: 0.4rem 0.8rem;
  border: none;
  border-radius: 4px;
  background-color: #f3f3f3;
  color: #b00020;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
}

.remove-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.remove-button:hover:not(:disabled) {
  background-color: #fbe9e9;
}
</style>
