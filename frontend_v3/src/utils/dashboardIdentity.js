// Display adaptation only. Never use this projection for authorization.
export function dashboardIdentity(value) {
  const root = value && typeof value === 'object' ? value : {}
  const user = root.user && typeof root.user === 'object' && !Array.isArray(root.user) ? root.user : root
  const text = input => typeof input === 'string' ? input.trim() : ''
  return {
    staffName: text(user.staffName) || '未提供姓名',
    staffPosition: text(user.staffPosition) || text(user.position) || text(user.role) || '未提供岗位'
  }
}

// Names mirror the existing header menu; IDs come from the selected-store context.
export function dashboardStoreLabel(selectedId, persistedId, identityStoreName) {
  const positive = value => /^\d+$/.test(String(value)) && Number.isSafeInteger(Number(value)) && Number(value) > 0
  if (positive(selectedId) && positive(persistedId)) {
    return ({ 1: '宁国店', 2: '宣城店' })[Number(selectedId)] || `门店 ${Number(selectedId)}`
  }
  return typeof identityStoreName === 'string' && identityStoreName.trim() ? identityStoreName.trim() : '请选择门店'
}
