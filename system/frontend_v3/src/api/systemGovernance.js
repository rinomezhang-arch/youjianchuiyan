import request from '@/utils/request'

export function getDatabaseGovernanceAudit() {
  return request.get('/system/database-governance/audit')
}
