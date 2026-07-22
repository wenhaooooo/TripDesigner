import request from '@/utils/request'
import type { WeatherInfo } from '@/types/api'

export const weatherApi = {
  getWeather: (destination: string, startDate: string, endDate: string) =>
    request.get<WeatherInfo>('/weather', { params: { destination, startDate, endDate } }),
  getWeatherByCoords: (lat: number, lon: number, startDate: string, endDate: string) =>
    request.get<WeatherInfo>('/weather/coords', { params: { lat, lon, startDate, endDate } }),
}
