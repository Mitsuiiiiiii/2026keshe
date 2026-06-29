import request from './request'

// v2 赛事报名
export const registerCompetition = (id, data) => request.post(`/competition/${id}/register`, data)
export const auditRegister = (registerId, data) => request.put(`/competition/register/${registerId}/audit`, data)
export const listRegisters = (id, params) => request.get(`/competition/${id}/register`, { params })

// v2 赛事附件
export const listAttachments = (id) => request.get(`/competition/${id}/attachments`)
export const addAttachment = (id, data) => request.post(`/competition/${id}/attachments`, data)
export const deleteAttachment = (id) => request.delete(`/competition/attachment/${id}`)

// v2 赛事资讯
export const listNews = (id) => request.get(`/competition/${id}/news`)
export const publishNews = (id, data) => request.post(`/competition/${id}/news`, data)

// v2 赛事排行榜
export const competitionRanking = () => request.get('/competition/ranking')

// v3 赛事日程
export const listSchedule = (id) => request.get(`/competition/${id}/schedule`)
export const addSchedule = (id, data) => request.post(`/competition/${id}/schedule`, data)
export const deleteSchedule = (sid) => request.delete(`/competition/schedule/${sid}`)

// v3 获奖公示
export const listAwards = (id) => request.get(`/competition/${id}/awards`)
export const publishAward = (id, data) => request.post(`/competition/${id}/awards`, data)

// v3 获奖榜单管理（管理员）
export const listAllAwards = () => request.get('/competition/awards/all')
export const updateAward = (awardId, data) => request.put(`/competition/awards/${awardId}`, data)
export const deleteAward = (awardId) => request.delete(`/competition/awards/${awardId}`)

// v3 获奖排行榜 / 统计
export const awardRanking = () => request.get('/competition/award/ranking')
export const awardStats = () => request.get('/competition/award/stats')

// v3 我的报名
export const myRegisters = () => request.get('/competition/my-registers')
