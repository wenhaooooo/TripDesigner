import request from '@/utils/request'
import type { ConversationVO, ConversationMessageVO, CreateConversationRequest } from '@/types/api'
// 对话相关 API 调用封装。
// 包含对话和消息的 CRUD 操作。


export const conversationApi = {
  list: () => request.get<ConversationVO[]>('/conversations'),
  create: (data?: CreateConversationRequest) => request.post<ConversationVO>('/conversations', data),
  get: (id: number) => request.get<ConversationVO>(`/conversations/${id}`),
  updateTitle: (id: number, title: string) => request.put(`/conversations/${id}`, { title }),
  delete: (id: number) => request.delete(`/conversations/${id}`),
  listMessages: (id: number) => request.get<ConversationMessageVO[]>(`/conversations/${id}/messages`),
  addMessage: (id: number, role: string, content: string) => request.post(`/conversations/${id}/messages`, { role, content }),
}
