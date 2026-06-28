import request from './request'

// 提交互评（body: teamId,toUserId,responsibility,tech,communication,anonymous）
export const submit = (data) => request.post('/evaluation', data)

// 某用户收到的评价列表（匿名评价后端已脱敏：fromUserName=匿名用户）
export const listByUser = (id) => request.get(`/evaluation/user/${id}`)

// 我发出的评价列表（含 toUserName 被评价人、anonymous、各维度分）
export const listSent = () => request.get('/evaluation/sent')

// 评价回复
export const reply = (id, data) => request.post(`/evaluation/${id}/reply`, data)
export const listReplies = (id) => request.get(`/evaluation/${id}/replies`)

// 评价举报（body: reason）
export const reportEval = (id, data) => request.post(`/evaluation/${id}/report`, data)

// 切换某条评价的匿名状态（仅评价人本人，body: anonymous 0/1）
export const toggleAnonymous = (id, anonymous) =>
  request.put(`/evaluation/${id}/anonymous`, { anonymous })

// 信誉分变动记录（时间倒序）
export const reputationLog = (id) => request.get(`/user/${id}/reputation-log`)
