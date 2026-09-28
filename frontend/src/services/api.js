import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

export const playerApi = {
  getAll: (params) => api.get('/players', { params }),
  refreshFromSite: () => api.post('/players/refresh'),
  uploadExcel: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/players/upload/excel', fd)
  },
  uploadCsv: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/players/upload/csv', fd)
  },
  updateAnalysis: (id, data) => api.patch(`/players/${id}/analysis`, data)
}

export const teamApi = {
  getAll: () => api.get('/teams'),
  create: (data) => api.post('/teams', data),
  update: (id, data) => api.put(`/teams/${id}`, data),
  delete: (id) => api.delete(`/teams/${id}`)
}

export const participantApi = {
  getAll: () => api.get('/participants'),
  create: (data) => api.post('/participants', data),
  update: (id, data) => api.put(`/participants/${id}`, data),
  delete: (id) => api.delete(`/participants/${id}`)
}

export const auctionApi = {
  getAll: () => api.get('/auction'),
  assign: (data) => api.post('/auction', data),
  remove: (id) => api.delete(`/auction/${id}`)
}

export const statsApi = {
  upload: (formData) => api.post('/stats/upload', formData),
  getSeasons: () => api.get('/stats/seasons'),
  getBySeason: (season) => api.get('/stats', { params: { season } }),
  getByPlayer: (playerId) => api.get(`/stats/player/${playerId}`)
}
