import { defineStore } from 'pinia'
import {
  listTasks,
  createTask,
  updateTask,
  setTaskCompleted,
  deleteTask,
  type Task,
  type TaskPayload,
} from '@/services/tasks'

export const useTasksStore = defineStore('tasks', {
  state: () => ({
    tasks: [] as Task[],
    loading: false,
    error: '',
  }),
  actions: {
    async fetchTasks() {
      this.loading = true
      this.error = ''
      try {
        this.tasks = await listTasks()
      } catch (err) {
        this.error = 'Failed to load tasks. Please try again later.'
      } finally {
        this.loading = false
      }
    },

    async addTask(payload: TaskPayload) {
      await createTask(payload)
      await this.fetchTasks()
    },

    async editTask(id: number, payload: TaskPayload) {
      await updateTask(id, payload)
      await this.fetchTasks()
    },

    async toggleCompleted(id: number, completed: boolean) {
      await setTaskCompleted(id, completed)
      await this.fetchTasks()
    },

    async removeTask(id: number) {
      await deleteTask(id)
      await this.fetchTasks()
    },
  },
})
