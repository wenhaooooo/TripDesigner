import { defineStore } from 'pinia'
import { ref } from 'vue'
import { tripApi } from '@/api/trip'
import type { TripVO, TripDetailVO } from '@/types/api'
// Pinia 行程状态管理。
// 管理当前查看的行程数据和每日活动列表。


export const useTripStore = defineStore('trip', () => {
  const trips = ref<TripVO[]>([])
  const currentTrip = ref<TripDetailVO | null>(null)
  const loading = ref(false)

  async function fetchTrips() {
    loading.value = true
    try {
      const res = await tripApi.list()
      trips.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchTrip(id: number) {
    loading.value = true
    try {
      const res = await tripApi.detail(id)
      currentTrip.value = res.data
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function createTrip(data: {
    title: string
    destinationName: string
    startDate: string
    endDate: string
    description?: string
    budget?: number
  }) {
    loading.value = true
    try {
      const res = await tripApi.create(data)
      trips.value.unshift(res.data)
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function deleteTrip(id: number) {
    await tripApi.delete(id)
    trips.value = trips.value.filter(t => t.id !== id)
    if (currentTrip.value?.id === id) {
      currentTrip.value = null
    }
  }

  return { trips, currentTrip, loading, fetchTrips, fetchTrip, createTrip, deleteTrip }
})
