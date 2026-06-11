import request from './request'

export function sendAiChatApi(data) {
  return request.post('/ai/chat', data, { showLoading: false, timeout: 60000 })
}

export function getQuickQuestionsApi() {
  return request.get('/ai/quick-questions', { showLoading: false })
}
