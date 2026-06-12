import api from '@/services/api'

export interface Collaborator {
  userId: number
  email: string
  owner: boolean
}

export interface InvitationDetails {
  listId: number
  listName: string
  invitedEmail: string
  expired: boolean
  accepted: boolean
}

export interface AcceptInvitationResult {
  listId: number
  listName: string
}

export async function inviteCollaborator(email: string): Promise<void> {
  await api.post('/lists/invitations', { email })
}

export async function listCollaborators(): Promise<Collaborator[]> {
  const response = await api.get<Collaborator[]>('/lists/collaborators')
  return response.data
}

export async function removeCollaborator(userId: number): Promise<void> {
  await api.delete(`/lists/collaborators/${userId}`)
}

export async function getInvitation(token: string): Promise<InvitationDetails> {
  const response = await api.get<InvitationDetails>(`/invitations/${token}`)
  return response.data
}

export async function acceptInvitation(token: string): Promise<AcceptInvitationResult> {
  const response = await api.post<AcceptInvitationResult>(`/invitations/${token}/accept`)
  return response.data
}
