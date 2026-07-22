import request from '@/utils/request'

interface TtsResultVO {
  text: string
  lang: string
  rate: number
  pitch: number
  suggestion: string
}

export const voiceApi = {
  prepareTts: (text: string, lang = 'zh-CN') =>
    request.post<TtsResultVO>('/voice/tts', { text, lang }),
}
