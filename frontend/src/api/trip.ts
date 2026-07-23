import request from '@/utils/request'
import type {
// 行程相关 API 调用封装。
// 包含行程、行程日、活动的 CRUD 操作。

  CreateTripRequest, UpdateTripRequest, TripVO, TripDetailVO,
  CreateTripDayRequest, UpdateTripDayRequest, TripDayVO,
  CreateTripActivityRequest, UpdateTripActivityRequest, TripActivityVO,
  DestinationVO,
} from '@/types/api'

export const tripApi = {
  // Trip
  list: () => request.get<TripVO[]>('/trips'),
  detail: (id: number) => request.get<TripDetailVO>(`/trips/${id}`),
  create: (data: CreateTripRequest) => request.post<TripVO>('/trips', data),
  update: (id: number, data: UpdateTripRequest) => request.put<TripVO>(`/trips/${id}`, data),
  delete: (id: number) => request.delete(`/trips/${id}`),
  updateStatus: (id: number, status: string) => request.put(`/trips/${id}/status`, { status }),

  // TripDay
  listDays: (tripId: number) => request.get<TripDayVO[]>(`/trips/${tripId}/days`),
  createDay: (tripId: number, data: CreateTripDayRequest) => request.post<TripDayVO>(`/trips/${tripId}/days`, data),
  updateDay: (tripId: number, dayId: number, data: UpdateTripDayRequest) => request.put<TripDayVO>(`/trips/${tripId}/days/${dayId}`, data),
  deleteDay: (tripId: number, dayId: number) => request.delete(`/trips/${tripId}/days/${dayId}`),

  // TripActivity
  listActivities: (tripId: number, dayId: number) => request.get<TripActivityVO[]>(`/trips/${tripId}/days/${dayId}/activities`),
  createActivity: (tripId: number, dayId: number, data: CreateTripActivityRequest) => request.post<TripActivityVO>(`/trips/${tripId}/days/${dayId}/activities`, data),
  updateActivity: (tripId: number, dayId: number, activityId: number, data: UpdateTripActivityRequest) => request.put<TripActivityVO>(`/trips/${tripId}/days/${dayId}/activities/${activityId}`, data),
  deleteActivity: (tripId: number, dayId: number, activityId: number) => request.delete(`/trips/${tripId}/days/${dayId}/activities/${activityId}`),

  // Destination
  listDestinations: () => request.get<DestinationVO[]>('/destinations'),
  createDestination: (data: Partial<DestinationVO>) => request.post<DestinationVO>('/destinations', data),
  updateDestination: (id: number, data: Partial<DestinationVO>) => request.put<DestinationVO>(`/destinations/${id}`, data),
  deleteDestination: (id: number) => request.delete(`/destinations/${id}`),

  // Export
  exportExcel: (id: number) => request.get(`/trips/${id}/export/excel`, { responseType: 'blob' }),
  exportCsv: (id: number) => request.get(`/trips/${id}/export/csv`, { responseType: 'blob' }),
  exportIcs: (id: number) => request.get(`/trips/${id}/export/ics`, { responseType: 'blob' }),
  exportJson: (id: number) => request.get(`/trips/${id}/export/json`, { responseType: 'blob' }),
}
