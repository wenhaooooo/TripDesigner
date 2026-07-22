import request from '@/utils/request'

export interface XiaohongshuNote {
  id: string
  title: string
  content: string
  coverImage: string
  images: string[]
  authorName: string
  authorAvatar: string
  likes: number
  comments: number
  shares: number
  noteUrl: string
  tags: string
}

export interface XiaohongshuSearchResponse {
  notes: XiaohongshuNote[]
  total: number
}

export const xiaohongshuApi = {
  search: (keyword: string, limit?: number) => 
    request.get<XiaohongshuSearchResponse>('/api/xiaohongshu/search', { 
      params: { keyword, limit: limit || 5 } 
    }),
}