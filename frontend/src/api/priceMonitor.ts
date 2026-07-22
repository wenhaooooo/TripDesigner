import request from '@/utils/request'
import type { CreateMonitorRequest, PriceMonitorVO } from '@/types/api'

export interface TrainTicketInfo {
  id: number
  departure: string
  destination: string
  trainNumber: string
  ticketClass: string
  price: number
  departureTime: string
  arrivalTime: string
  durationMinutes: number
  trainType: string
}

export const priceMonitorApi = {
  list: () => request.get<PriceMonitorVO[]>('/price-monitors'),
  get: (id: number) => request.get<PriceMonitorVO>(`/price-monitors/${id}`),
  create: (data: CreateMonitorRequest) => request.post<PriceMonitorVO>('/price-monitors', data),
  cancel: (id: number) => request.put(`/price-monitors/${id}/cancel`),
  delete: (id: number) => request.delete(`/price-monitors/${id}`),
  listTrains: (params: { departure: string; destination: string; ticketClass?: string }) =>
    request.get<TrainTicketInfo[]>('/price-monitors/trains', { params }),
}
