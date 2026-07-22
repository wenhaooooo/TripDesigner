import request from '@/utils/request'
import type { CreateShareRequest, TripShareVO, TripDetailVO } from '@/types/api'

export const shareApi = {
  createShare: (tripId: number, data: CreateShareRequest) =>
    request.post<TripShareVO>(`/trips/${tripId}/shares`, data),
  listShares: (tripId: number) =>
    request.get<TripShareVO[]>(`/trips/${tripId}/shares`),
  revokeShare: (tripId: number, shareId: number) =>
    request.delete(`/trips/${tripId}/shares/${shareId}`),
  getSharedTrip: (token: string) =>
    request.get<TripDetailVO>(`/shared/${token}`),
}
