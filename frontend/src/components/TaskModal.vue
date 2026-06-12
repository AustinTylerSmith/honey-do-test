<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { Task, TaskPayload } from '@/services/tasks'

const props = defineProps<{
  mode: 'create' | 'edit'
  task?: Task | null
}>()

const emit = defineEmits<{
  (e: 'submit', payload: TaskPayload): void
  (e: 'close'): void
}>()

const title = ref('')
const dueDate = ref('')
const estimatedMinutes = ref<number | null>(null)

function resetFromTask() {
  if (props.mode === 'edit' && props.task) {
    title.value = props.task.title
    dueDate.value = props.task.dueDate ?? ''
    estimatedMinutes.value = props.task.estimatedMinutes ?? null
  } else {
    title.value = ''
    dueDate.value = ''
    estimatedMinutes.value = null
  }
}

resetFromTask()

watch(() => props.task, resetFromTask)

const canSubmit = computed(() => title.value.trim().length > 0)

function handleSubmit() {
  if (!canSubmit.value) return

  const payload: TaskPayload = {
    title: title.value.trim(),
    dueDate: dueDate.value ? dueDate.value : null,
    estimatedMinutes:
      estimatedMinutes.value === null || estimatedMinutes.value === undefined
        ? null
        : Number(estimatedMinutes.value),
  }

  emit('submit', payload)
}

function handleClose() {
  emit('close')
}
</script>

<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal" role="dialog" aria-modal="true">
      <h2>{{ mode === 'create' ? 'Add a task' : 'Edit task' }}</h2>

      <form @submit.prevent="handleSubmit">
        <div class="form-field">
          <label for="task-title">Title</label>
          <input
            id="task-title"
            v-model="title"
            type="text"
            name="title"
            placeholder="What needs to be done?"
            autofocus
            required
          />
        </div>

        <div class="form-field">
          <label for="task-due-date">Due date (optional)</label>
          <input id="task-due-date" v-model="dueDate" type="date" name="dueDate" />
        </div>

        <div class="form-field">
          <label for="task-estimate">Estimated time (minutes, optional)</label>
          <input
            id="task-estimate"
            v-model.number="estimatedMinutes"
            type="number"
            name="estimatedMinutes"
            min="0"
            step="1"
          />
        </div>

        <div class="modal-actions">
          <button type="button" class="secondary-button" @click="handleClose">Cancel</button>
          <button type="submit" :disabled="!canSubmit">
            {{ mode === 'create' ? 'Add' : 'Save' }}
          </button>
        </div>
      </form>
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
  max-width: 360px;
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
</style>
