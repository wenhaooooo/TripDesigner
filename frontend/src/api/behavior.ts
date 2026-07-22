import request from '@/utils/request'
import type {
  TrackBehaviorRequest,
  UserBehaviorVO,
  PreferenceProfileVO,
} from '@/types/api'

export const behaviorApi = {
  track: (data: TrackBehaviorRequest) => request.post<void>('/behaviors/track', data),
  trackSync: (data: TrackBehaviorRequest) => request.post<UserBehaviorVO>('/behaviors/track-sync', data),
  list: (limit = 50) => request.get<UserBehaviorVO[]>('/behaviors', { params: { limit } }),
  profile: () => request.get<PreferenceProfileVoAlias>('/behaviors/profile'),
  syncPreferences: () => request.post<{ syncedCount: number }>('/behaviors/sync-preferences'),
}

// 类型别名避免循环依赖
type PreferenceProfileVoAlias = PreferenceProfileVO
