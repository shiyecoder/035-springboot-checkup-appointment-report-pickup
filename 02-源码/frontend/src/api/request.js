import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

/** 统一请求实例：自动带 token、统一拆包、401 回登录页 */
const request = axios.create({ baseURL: '/api', timeout: 20000 })

request.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code !== 'undefined' && body.code !== 200) {
      ElMessage.error(body.msg || '请求失败')
      if (body.code === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      return Promise.reject(new Error(body.msg))
    }
    return body.data
  },
  (err) => {
    ElMessage.error(err.message || '网络异常')
    return Promise.reject(err)
  }
)

export default request
