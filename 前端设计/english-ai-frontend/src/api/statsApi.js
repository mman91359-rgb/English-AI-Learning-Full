import request from './request'

export function getReviewTasks(userId) {
  return request.get(`/learning/review-tasks/${userId}`)
}

export function getTodayStats(userId) {
  return request.get(`/learning/today-stats/${userId}`)
}
