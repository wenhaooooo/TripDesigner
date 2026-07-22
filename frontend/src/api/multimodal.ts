import request from '@/utils/request'
import type { MultimodalUploadVO } from '@/types/api'

export const multimodalApi = {
  upload: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<MultimodalUploadVO>('/multimodal/upload', formData, {
      headers: { 'Content-Type': undefined },
    })
  },
  list: () => request.get<MultimodalUploadVO[]>('/multimodal'),
  get: (id: number) => request.get<MultimodalUploadVO>(`/multimodal/${id}`),
  generate: (id: number) => request.post<MultimodalUploadVO>(`/multimodal/${id}/generate`),
  delete: (id: number) => request.delete(`/multimodal/${id}`),
}
