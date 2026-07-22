import request from '@/utils/request'
import type { PreferenceRequest, PreferenceVO, TripMemoryRequest, TripMemoryVO, MemorySummaryVO } from '@/types/api'
// 偏好/记忆相关 API 调用封装。
// 包含用户偏好和旅行记忆的 CRUD 及摘要查询。


export const memoryApi = {
  // Preferences
  listPreferences: () => request.get<PreferenceVO[]>('/memory/preferences'),
  getPreference: (id: number) => request.get<PreferenceVO>(`/memory/preferences/${id}`),
  savePreference: (data: PreferenceRequest) => request.post<PreferenceVO>('/memory/preferences', data),
  deletePreference: (id: number) => request.delete(`/memory/preferences/${id}`),

  // Trip Memories
  listMemories: () => request.get<TripMemoryVO[]>('/memory/trip-memories'),
  listMemoriesByType: (type: string) => request.get<TripMemoryVO[]>(`/memory/trip-memories/type/${type}`),
  saveMemory: (data: TripMemoryRequest) => request.post<TripMemoryVO>('/memory/trip-memories', data),

  // Summary
  getSummary: () => request.get<MemorySummaryVO>('/memory/summary'),
}
