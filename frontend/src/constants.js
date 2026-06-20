// 竞赛类型
export const COMPETITION_TYPES = [
  { value: 'PROGRAM', label: '程序设计' },
  { value: 'MODELING', label: '数学建模' },
  { value: 'INNOVATION', label: '创新创业' },
  { value: 'COURSE', label: '课程设计' }
]

export const COMPETITION_TYPE_MAP = Object.fromEntries(
  COMPETITION_TYPES.map((t) => [t.value, t.label])
)

export const COMPETITION_TYPE_TAG = {
  PROGRAM: 'primary',
  MODELING: 'success',
  INNOVATION: 'warning',
  COURSE: 'info'
}

// 队伍状态
export const TEAM_STATUS = [
  { value: 'RECRUITING', label: '招募中' },
  { value: 'FULL', label: '已满员' },
  { value: 'CLOSED', label: '已关闭' }
]

export const TEAM_STATUS_MAP = Object.fromEntries(
  TEAM_STATUS.map((t) => [t.value, t.label])
)

export const TEAM_STATUS_TAG = {
  RECRUITING: 'success',
  FULL: 'warning',
  CLOSED: 'info'
}
