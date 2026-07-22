import request from '@/utils/request'
import type { TripStatisticsVO } from '@/types/api'

export const statisticsApi = {
  get: () => request.get<TripStatisticsVO>('/statistics'),
}
