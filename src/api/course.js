import request from './request'

export function getCourseListApi(params) {
  return request.get('/course/list', { params })
}

export function getCourseDetailApi(id) {
  return request.get(`/course/${id}`, { showLoading: false })
}

export function createCourseApi(data) {
  return request.post('/course', data)
}

export function updateCourseApi(id, data) {
  return request.put(`/course/${id}`, data)
}

export function deleteCourseApi(id) {
  return request.delete(`/course/${id}`)
}
