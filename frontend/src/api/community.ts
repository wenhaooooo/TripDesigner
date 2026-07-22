import request from '@/utils/request'
import type {
  CommunityPostVO,
  CommunityCommentVO,
  CreatePostRequest,
  UpdatePostRequest,
  CreateCommentRequest,
  PageResult,
} from '@/types/api'

export const communityApi = {
  // 帖子
  listPosts: (page = 0, size = 10) =>
    request.get<PageResult<CommunityPostVO>>('/community/posts', { params: { page, size } }),
  listHot: (limit = 5) =>
    request.get<CommunityPostVO[]>('/community/posts/hot', { params: { limit } }),
  listByDestination: (destination: string, page = 0, size = 10) =>
    request.get<CommunityPostVO[]>('/community/posts/destination', { params: { destination, page, size } }),
  listMine: () => request.get<CommunityPostVO[]>('/community/posts/mine'),
  listFavorites: () => request.get<CommunityPostVO[]>('/community/posts/favorites'),
  getPost: (id: number) => request.get<CommunityPostVO>(`/community/posts/${id}`),
  createPost: (data: CreatePostRequest) => request.post<CommunityPostVO>('/community/posts', data),
  updatePost: (id: number, data: UpdatePostRequest) => request.put<CommunityPostVO>(`/community/posts/${id}`, data),
  deletePost: (id: number) => request.delete(`/community/posts/${id}`),
  toggleLike: (id: number) => request.post<{ liked: boolean }>(`/community/posts/${id}/like`),
  toggleFavorite: (id: number) => request.post<{ favorited: boolean }>(`/community/posts/${id}/favorite`),

  // 评论
  listComments: (postId: number) =>
    request.get<CommunityCommentVO[]>(`/community/posts/${postId}/comments`),
  createComment: (postId: number, data: CreateCommentRequest) =>
    request.post<CommunityCommentVO>(`/community/posts/${postId}/comments`, data),
  deleteComment: (id: number) => request.delete(`/community/comments/${id}`),
  toggleCommentLike: (id: number) =>
    request.post<{ liked: boolean }>(`/community/comments/${id}/like`),
}
