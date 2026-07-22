import request from '@/utils/request'
import type {
  CreateCheckinRequest,
  TripCheckinVO,
  CheckinStatsVO,
} from '@/types/api'

export const checkinApi = {
  create: (data: CreateCheckinRequest) => request.post<TripCheckinVO>('/checkins', data),
  listMine: () => request.get<TripCheckinVO[]>('/checkins'),
  listByTrip: (tripId: number) => request.get<TripCheckinVO[]>(`/checkins/trip/${tripId}`),
  get: (id: number) => request.get<TripCheckinVO>(`/checkins/${id}`),
  updateStatus: (id: number, status: string) =>
    request.put<TripCheckinVO>(`/checkins/${id}/status`, null, { params: { status } }),
  delete: (id: number) => request.delete(`/checkins/${id}`),
  stats: () => request.get<CheckinStatsVO>('/checkins/stats'),
}
