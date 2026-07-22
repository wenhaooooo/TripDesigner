import request from '@/utils/request'
import type { TripGenerationResult, TripGenerationTask } from '@/types/api'
// AI 相关 API 调用封装。
// 包含单 Agent 生成、多 Agent 工作流生成和工作流详情查询。


export const aiApi = {
  smoke: (prompt: string) => request.get('/ai/smoke', { params: { prompt } }),
  toolSmoke: (prompt: string) => request.get('/ai/tool-smoke', { params: { prompt } }),

  // Single agent generation
  generateTrip: (prompt: string) => request.post<TripGenerationResult>('/ai/trip/generate', { prompt }),
  generateTripAsync: (prompt: string) => request.post<TripGenerationTask>('/ai/trip/generate/async', { prompt }),
  getTaskStatus: (taskId: number) => request.get<TripGenerationTask>(`/ai/trip/tasks/${taskId}`),
  listTasks: () => request.get<TripGenerationTask[]>('/ai/trip/tasks'),
  chatWithTrip: (prompt: string, tripId?: number) => request.post('/ai/trip/chat', { prompt, tripId }),

  // Multi-agent workflow - synchronous (blocking)
  generateWorkflow: (prompt: string) => request.post<TripGenerationResult>('/ai/workflow/generate', { prompt }),
  // Multi-agent workflow - RabbitMQ async (recommended)
  generateWorkflowMq: (prompt: string) => request.post('/ai/workflow/generate/mq', { prompt }),
  workflowDetails: (sessionId: number) => request.get(`/ai/workflow/${sessionId}`),
  cancelWorkflow: (sessionId: number) => request.post(`/ai/workflow/${sessionId}/cancel`),
}
