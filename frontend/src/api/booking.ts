import request from '@/utils/request'
import type { BookingLinkVO, BookingSuggestion } from '@/types/api'

export const bookingApi = {
  getLinks: (tripId: number, activityId: number) =>
    request.get<BookingLinkVO[]>(`/booking/trips/${tripId}/activities/${activityId}`),
  getSuggestions: (tripId: number) =>
    request.get<Record<string, BookingSuggestion[]>>(`/booking/trips/${tripId}/suggestions`),
}
