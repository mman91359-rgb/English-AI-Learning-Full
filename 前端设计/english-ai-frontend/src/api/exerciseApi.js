import request from './request'

export function getAssessQuestions() {
  return request.get('/exercise/assess-questions')
}

export function getQuestions(params) {
  return request.get('/exercise/questions', { params })
}

export function getQuestionDetail(id, params) {
  return request.get(`/exercise/question/${id}`, { params })
}

export function submitAnswer(data) {
  return request.post('/exercise/submit', data)
}

export function getErrorBook(userId) {
  return request.get(`/exercise/error-book/${userId}`)
}

export function getKnowledgePoints() {
  return request.get('/exercise/knowledge-points')
}
