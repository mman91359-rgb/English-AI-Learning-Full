import request from './request'

export function login(data) {
  return request.post('/user/login', data)
}

export function register(data) {
  return request.post('/user/register', data)
}

export function getRadarData(userId) {
  return request.get(`/user/radar/${userId}`)
}

export function updateElo(data) {
  return request.post('/user/radar/update-elo', data)
}

export function submitAssess(data) {
  return request.post('/user/assess/submit', data)
}
