// Generic API response wrapper
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// Pagination
export interface PageResult<T> {
  content: T[]
  total: number
  page: number
  size: number
  pages: number
}

// Auth
export interface LoginRequest {
  email: string
  password: string
}
export interface RegisterRequest {
  email: string
  password: string
}
export interface TokenResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
}
export interface UserVO {
  id: number
  email: string
  createdAt: string
}

// Trip
export interface CreateTripRequest {
  title: string
  description?: string
  destinationName: string
  startDate: string
  endDate: string
  budget?: number
}
export interface UpdateTripRequest {
  title?: string
  description?: string
  destinationName?: string
  startDate?: string
  endDate?: string
  budget?: number
}
export interface TripVO {
  id: number
  title: string
  description: string
  destinationName: string
  status: string
  startDate: string
  endDate: string
  budget: number
  createdAt: string
}
export interface TripDetailVO extends TripVO {
  userId: number
  updatedAt: string
  days: TripDayVO[]
}
export interface CreateTripDayRequest {
  dayNumber: number
  date: string
  title?: string
  description?: string
}
export interface UpdateTripDayRequest {
  date?: string
  title?: string
  description?: string
}
export interface TripDayVO {
  id: number
  tripId: number
  dayNumber: number
  date: string
  title: string
  description: string
  activities: TripActivityVO[]
}
export interface CreateTripActivityRequest {
  name: string
  description?: string
  startTime?: string
  endTime?: string
  category?: string
  place?: string
  notes?: string
}
export interface UpdateTripActivityRequest {
  name?: string
  description?: string
  startTime?: string
  endTime?: string
  category?: string
  place?: string
  notes?: string
}
export interface TripActivityVO {
  id: number
  tripDayId: number
  name: string
  description: string
  startTime: string
  endTime: string
  category: string
  place: string
  notes: string
  sortOrder: number
}

// Destination
export interface DestinationVO {
  id: number
  userId: number
  name: string
  country: string
  description?: string
  tags: string[]
}

// Conversation
export interface CreateConversationRequest {
  title?: string
}
export interface ConversationVO {
  id: number
  userId: number
  title: string
  status: string
  createdAt: string
  updatedAt: string
}
export interface ConversationMessageVO {
  id: number
  conversationId: number
  role: string
  content: string
  createdAt: string
}

// Experience
export interface CreateExperienceRequest {
  tripId: number
  tripDayId?: number
  tripActivityId?: number
  title: string
  content?: string
  rating?: number
  tags?: string[]
  mediaUrls?: string[]
}
export interface UpdateExperienceRequest {
  title?: string
  content?: string
  rating?: number
  tags?: string[]
  mediaUrls?: string[]
}
export interface ExperienceVO {
  id: number
  tripId: number
  tripDayId: number | null
  tripActivityId: number | null
  title: string
  content: string
  rating: number | null
  tags: string[]
  mediaUrls: string[]
  status: string
  createdAt: string
  updatedAt: string
}

// Memory
export interface PreferenceRequest {
  category: string
  preference: Record<string, unknown>
  source?: string
}
export interface PreferenceVO {
  id: number
  category: string
  data: Record<string, unknown>
  source: string
  createdAt: string
  updatedAt: string
}
export interface TripMemoryRequest {
  tripId: number
  memoryType: string
  content: string
  tags?: string[]
}
export interface TripMemoryVO {
  id: number
  tripId: number
  memoryType: string
  content: string
  tags: string[]
  createdAt: string
}
export interface MemorySummaryVO {
  preferenceSummary: string
  tripMemorySummary: string
}

// AI
export interface TripGenerationResult {
  sessionId: number
  trip: TripDetailVO | null
}

export type TripGenerationStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'

export interface TripGenerationTask {
  id: number
  userId: number
  prompt: string
  status: TripGenerationStatus
  progress: number
  progressMessage: string
  conversationId: number | null
  tripId: number | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

// Weather
export interface WeatherInfo {
  destination: string
  dailyForecasts: DailyForecast[]
}
export interface DailyForecast {
  date: string
  maxTemp: number
  minTemp: number
  precipitation: number
  weatherCode: number
  weatherDescription: string
  windSpeed: number
}

// Trip Share
export interface CreateShareRequest {
  shareType?: string
  maxViews?: number
  expireDays?: number
}
export interface TripShareVO {
  id: number
  tripId: number
  shareToken: string
  shareType: string
  maxViews: number | null
  currentViews: number
  expiresAt: string | null
  status: string
  createdAt: string
}

// Price Monitor
export interface CreateMonitorRequest {
  destination: string
  departure?: string
  ticketClass?: string
  departureTime?: string
  arrivalTime?: string
  monitorType?: string
  targetPrice?: number
  tripId?: number
}
export interface PricePoint {
  price: number
  recordedAt: string
  source: string
}
export interface PriceMonitorVO {
  id: number
  userId: number
  tripId: number | null
  destination: string
  departure: string | null
  ticketClass: string | null
  departureTime: string | null
  arrivalTime: string | null
  monitorType: string
  targetPrice: number | null
  currentPrice: number | null
  lowestPrice: number | null
  priceHistory: PricePoint[]
  notificationSent: boolean
  status: string
  createdAt: string
}

// Advisor
export interface AdvisorRequest {
  question: string
  conversationId?: number
}
export interface AdvisorResponse {
  answer: string
  conversationId: number
}

// ========== P1.1 Multimodal ==========
export interface MultimodalUploadVO {
  id: number
  userId: number
  originalFilename: string
  contentType: string
  fileSize: number
  recognitionResult: Record<string, unknown>
  generatedTripId: number | null
  status: string
  createdAt: string
  updatedAt: string
}

// ========== P1.2 Community ==========
export interface CreatePostRequest {
  title: string
  content: string
  destination?: string
  tags?: string[]
  mediaUrls?: string[]
}
export interface UpdatePostRequest {
  title?: string
  content?: string
  destination?: string
  tags?: string[]
  mediaUrls?: string[]
}
export interface CreateCommentRequest {
  content: string
  parentId?: number
}
export interface CommunityPostVO {
  id: number
  userId: number
  authorEmail: string
  title: string
  content: string
  destination: string
  tags: string[]
  mediaUrls: string[]
  viewCount: number
  likeCount: number
  commentCount: number
  favoriteCount: number
  likedByMe: boolean
  favoritedByMe: boolean
  status: string
  createdAt: string
  updatedAt: string
}
export interface CommunityCommentVO {
  id: number
  postId: number
  userId: number
  authorEmail: string
  parentId: number | null
  content: string
  likeCount: number
  likedByMe: boolean
  status: string
  createdAt: string
  replies: CommunityCommentVO[]
}

// ========== P1.4 Behavior ==========
export interface TrackBehaviorRequest {
  behaviorType: string
  targetType: string
  targetId?: number
  context?: Record<string, unknown>
}
export interface UserBehaviorVO {
  id: number
  userId: number
  behaviorType: string
  targetType: string
  targetId: number | null
  context: Record<string, unknown>
  weight: number
  createdAt: string
}
export interface PreferenceProfileVO {
  userId: number
  totalBehaviors: number
  topDestinations: Array<Record<string, unknown>>
  topCategories: Array<Record<string, unknown>>
  topKeywords: string[]
  preferenceSummary: Record<string, unknown>
  recommendationHint: string
}

// ========== P2.1 Team ==========
export interface CreateTeamRequest {
  title: string
  description?: string
  destination: string
  startDate: string
  endDate: string
  teamType?: string
  interests?: string[]
  maxMembers?: number
  genderRequirement?: string
  minAge?: number
  maxAge?: number
  contact?: string
}
export interface TravelTeamVO {
  id: number
  creatorId: number
  creatorEmail: string
  title: string
  description: string
  destination: string
  startDate: string
  endDate: string
  teamType: string
  interests: string[]
  maxMembers: number
  currentMembers: number
  genderRequirement: string
  minAge: number | null
  maxAge: number | null
  status: string
  createdAt: string
  isCreator: boolean
  isMember: boolean
  hasApplied: boolean
}
export interface TeamApplicationVO {
  id: number
  teamId: number
  applicantId: number
  applicantEmail: string
  message: string
  status: string
  processedAt: string | null
  createdAt: string
}

// ========== P2.2 Checkin ==========
export interface CreateCheckinRequest {
  tripId: number
  tripDayId?: number
  activityId?: number
  placeName?: string
  latitude?: number
  longitude?: number
  notes?: string
  photoUrls?: string[]
}
export interface TripCheckinVO {
  id: number
  userId: number
  tripId: number
  tripDayId: number | null
  activityId: number | null
  placeName: string
  latitude: number | null
  longitude: number | null
  notes: string
  photoUrls: string[]
  status: string
  checkedInAt: string
  createdAt: string
}
export interface CheckinStatsVO {
  userId: number
  totalCheckins: number
  completedCount: number
  skippedCount: number
  recentCheckins: Array<Record<string, unknown>>
  visitedPlaces: string[]
}

// ========== P2.3 Booking ==========
export interface BookingLinkVO {
  platform: string
  platformName: string
  bookingUrl: string
  description: string
}
export interface BookingSuggestion {
  activityName: string
  category: string
  links: BookingLinkVO[]
}

// ========== P3.2 Statistics ==========
export interface TripStatisticsVO {
  userId: number
  totalTrips: number
  completedTrips: number
  planningTrips: number
  draftTrips: number
  totalBudget: number
  averageBudget: number
  topDestinations: Array<Record<string, unknown>>
  activityCategoryDistribution: Array<Record<string, unknown>>
  tripDurationDistribution: Array<Record<string, unknown>>
  monthlyStats: Array<Record<string, unknown>>
  achievements: string[]
}
