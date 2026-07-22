import request from '@/utils/request'
import type { CreateExperienceRequest, UpdateExperienceRequest, ExperienceVO } from '@/types/api'
// 体验相关 API 调用封装。
// 包含体验的 CRUD 和按行程查询。


export const experienceApi = {
  list: () => request.get<ExperienceVO[]>('/experiences'),
  listByTrip: (tripId: number) => request.get<ExperienceVO[]>(`/experiences/trip/${tripId}`),
  get: (id: number) => request.get<ExperienceVO>(`/experiences/${id}`),
  create: (data: CreateExperienceRequest) => request.post<ExperienceVO>('/experiences', data),
  update: (id: number, data: UpdateExperienceRequest) => request.put<ExperienceVO>(`/experiences/${id}`, data),
  delete: (id: number) => request.delete(`/experiences/${id}`),
}
