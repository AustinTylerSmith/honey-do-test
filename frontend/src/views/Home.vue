<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useTasksStore } from '@/stores/tasks'
import TaskModal from '@/components/TaskModal.vue'
import ShareModal from '@/components/ShareModal.vue'
import type { Task, TaskPayload } from '@/services/tasks'

const tasksStore = useTasksStore()

const showModal = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const editingTask = ref<Task | null>(null)
const showShareModal = ref(false)

function formatDueDate(dueDate: string | null): string {
  return dueDate ? `Due: ${dueDate}` : 'No due date'
}

function formatEstimatedTime(estimatedMinutes: number | null): string {
  return estimatedMinutes != null ? `Est: ${estimatedMinutes} min` : 'No estimate'
}

function openAddModal() {
  modalMode.value = 'create'
  editingTask.value = null
  showModal.value = true
}

function openEditModal(task: Task) {
  modalMode.value = 'edit'
  editingTask.value = task
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editingTask.value = null
}

function openShareModal() {
  showShareModal.value = true
}

function closeShareModal() {
  showShareModal.value = false
}

async function handleModalSubmit(payload: TaskPayload) {
  if (modalMode.value === 'create') {
    await tasksStore.addTask(payload)
  } else if (editingTask.value) {
    await tasksStore.editTask(editingTask.value.id, payload)
  }
  closeModal()
}

async function handleToggleCompleted(task: Task) {
  await tasksStore.toggleCompleted(task.id, !task.completed)
}

async function handleDelete(task: Task) {
  await tasksStore.removeTask(task.id)
}

onMounted(() => {
  tasksStore.fetchTasks()
})
</script>

<template>
  <div class="home">
    <div class="home-header">
      <h1>My Tasks</h1>
      <div class="header-actions">
        <button type="button" class="share-button" @click="openShareModal">Share</button>
        <button type="button" class="add-button" @click="openAddModal">Add a task</button>
      </div>
    </div>

    <p v-if="tasksStore.loading">Loading tasks...</p>

    <p v-else-if="tasksStore.error" class="error">{{ tasksStore.error }}</p>

    <div v-else-if="tasksStore.tasks.length === 0" class="empty-state">
      <p>You don't have any tasks yet.</p>
      <p>Add your first task to get started!</p>
    </div>

    <ul v-else class="task-list">
      <li
        v-for="task in tasksStore.tasks"
        :key="task.id"
        class="task-row"
        :class="{ completed: task.completed }"
      >
        <input
          type="checkbox"
          :checked="task.completed"
          class="task-checkbox"
          @change="handleToggleCompleted(task)"
        />
        <div class="task-details" @click="openEditModal(task)">
          <span class="task-title">{{ task.title }}</span>
          <span class="task-meta">{{ formatDueDate(task.dueDate) }}</span>
          <span class="task-meta">{{ formatEstimatedTime(task.estimatedMinutes) }}</span>
        </div>
        <div class="task-actions">
          <button
            type="button"
            class="icon-button"
            aria-label="Edit task"
            title="Edit task"
            @click="openEditModal(task)"
          >
            ✏️
          </button>
          <button
            type="button"
            class="icon-button"
            aria-label="Delete task"
            title="Delete task"
            @click="handleDelete(task)"
          >
            🗑️
          </button>
        </div>
      </li>
    </ul>

    <TaskModal
      v-if="showModal"
      :mode="modalMode"
      :task="editingTask"
      @submit="handleModalSubmit"
      @close="closeModal"
    />

    <ShareModal v-if="showShareModal" @close="closeShareModal" />
  </div>
</template>

<style scoped>
.home {
  max-width: 600px;
  margin: 0 auto;
  padding: 1rem;
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.add-button {
  padding: 0.6rem 1.2rem;
  border: none;
  border-radius: 4px;
  background-color: #2563eb;
  color: #fff;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}

.share-button {
  padding: 0.6rem 1.2rem;
  border: 1px solid #2563eb;
  border-radius: 4px;
  background-color: #fff;
  color: #2563eb;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}

.error {
  color: #b00020;
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: #555;
}

.task-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.task-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border-bottom: 1px solid #e0e0e0;
}

.task-row.completed .task-title {
  text-decoration: line-through;
  color: #999;
}

.task-checkbox {
  width: 1.1rem;
  height: 1.1rem;
  flex-shrink: 0;
}

.task-details {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  flex: 1;
  cursor: pointer;
}

.task-title {
  font-weight: 600;
}

.task-meta {
  font-size: 0.85rem;
  color: #666;
}

.task-actions {
  display: flex;
  gap: 0.25rem;
  flex-shrink: 0;
}

.icon-button {
  background: none;
  border: none;
  font-size: 1.1rem;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 4px;
  line-height: 1;
}

.icon-button:hover {
  background-color: #f0f0f0;
}
</style>
