import request from './request'

export function getCourseListApi(params) {
  return request.get('/course/list', { params })
}

export function getCourseDetailApi(id) {
  return request.get(`/course/${id}`, { showLoading: false })
}
