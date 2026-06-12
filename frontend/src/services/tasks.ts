import api from '@/services/api'

export interface Task {
  id: number
  title: string
  completed: boolean
  dueDate: string | null
  estimatedMinutes: number | null
}

export interface TaskPayload {
  title: string
  dueDate?: string | null
  estimatedMinutes?: number | null
}

export async function listTasks(): Promise<Task[]> {
  const response = await api.get<Task[]>('/tasks')
  return response.data
}

export async function createTask(payload: TaskPayload): Promise<Task> {
  const response = await api.post<Task>('/tasks', payload)
  return response.data
}

export async function updateTask(id: number, payload: TaskPayload): Promise<Task> {
  const response = await api.put<Task>(`/tasks/${id}`, payload)
  return response.data
}

export async function setTaskCompleted(id: number, completed: boolean): Promise<Task> {
  const response = await api.patch<Task>(`/tasks/${id}/complete`, { completed })
  return response.data
}

export async function deleteTask(id: number): Promise<void> {
  await api.delete(`/tasks/${id}`)
}
