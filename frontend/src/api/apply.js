import request from './request'

export const submitApply = (data) => request.post('/apply', data)

export const listTeamApplies = (teamId) => request.get(`/apply/team/${teamId}`)

export const myApplies = () => request.get('/apply/mine')

export const auditApply = (id, data) => request.put(`/apply/${id}/audit`, data)
