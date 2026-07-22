import request from '@/utils/request'
import type {
  CreateTeamRequest,
  TravelTeamVO,
  TeamApplicationVO,
  PageResult,
} from '@/types/api'

interface ApplyTeamRequestBody {
  message?: string
}

export const teamApi = {
  create: (data: CreateTeamRequest) => request.post<TravelTeamVO>('/teams', data),
  listOpen: (page = 0, size = 10) =>
    request.get<PageResult<TravelTeamVO>>('/teams', { params: { page, size } }),
  myCreated: () => request.get<TravelTeamVO[]>('/teams/mine/created'),
  myJoined: () => request.get<TravelTeamVO[]>('/teams/mine/joined'),
  get: (id: number) => request.get<TravelTeamVO>(`/teams/${id}`),
  close: (id: number) => request.put(`/teams/${id}/close`),
  delete: (id: number) => request.delete(`/teams/${id}`),
  apply: (id: number, data?: ApplyTeamRequestBody) =>
    request.post<TeamApplicationVO>(`/teams/${id}/applications`, data || {}),
  approve: (applicationId: number) =>
    request.put<TeamApplicationVO>(`/teams/applications/${applicationId}/approve`),
  reject: (applicationId: number) =>
    request.put<TeamApplicationVO>(`/teams/applications/${applicationId}/reject`),
  cancel: (applicationId: number) =>
    request.put(`/teams/applications/${applicationId}/cancel`),
  listApplications: (teamId: number) =>
    request.get<TeamApplicationVO[]>(`/teams/${teamId}/applications`),
  myApplications: () => request.get<TeamApplicationVO[]>('/teams/applications/mine'),
  leave: (teamId: number) => request.delete(`/teams/${teamId}/members`),
  findMatches: (destination: string, startDate: string, endDate: string) =>
    request.get<TravelTeamVO[]>('/teams/matches', { params: { destination, startDate, endDate } }),
}
