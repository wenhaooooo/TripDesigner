import request from '@/utils/request'
import type { AdvisorRequest, AdvisorResponse } from '@/types/api'

export const advisorApi = {
  ask: (data: AdvisorRequest) => request.post<AdvisorResponse>('/advisor/ask', data),
  askStream: (data: AdvisorRequest) =>
    fetch('/api/advisor/ask/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('trip_designer_token')}`,
      },
      body: JSON.stringify(data),
    }),
}
