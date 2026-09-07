// Preview only; the backend persists authoritative decimal costs using the same unit/yield formula.
export function previewRecipeLine(row, ingredient) {
  if (!ingredient) return { error: '请选择有效原料' }
  const quantity = Number(row.quantity)
  const price = ingredient.unitPrice == null ? NaN : Number(ingredient.unitPrice)
  if (!Number.isFinite(quantity) || quantity <= 0) return { error: '配方用量必须大于0' }
  if (!Number.isFinite(price) || price < 0) return { error: '原料缺少有效入库单价' }
  const purchaseUnit = ingredient.purchaseUnit || ingredient.unit
  const usageUnit = ingredient.usageUnit || purchaseUnit
  const unit = row.unit || usageUnit
  if (!purchaseUnit || !unit) return { error: '请补全采购单位和使用单位' }
  let conversion
  if (unit === purchaseUnit) conversion = 1
  else if (unit === usageUnit) conversion = Number(ingredient.conversionRate)
  else return { error: '配方单位与原料档案不一致' }
  if (!Number.isFinite(conversion) || conversion <= 0) return { error: '请填写有效单位换算率' }
  const yieldRate = Number(row.yieldRate ?? (row.wastageRate == null ? ingredient.yieldRate : 100 - Number(row.wastageRate)))
  if (!Number.isFinite(yieldRate) || yieldRate <= 0 || yieldRate > 999.99) return { error: '请填写有效出成率' }
  const netUnitPrice = Number((price * 100 / (conversion * yieldRate)).toFixed(8))
  return { netUnitPrice, totalCost: Number((quantity * netUnitPrice).toFixed(4)), yieldRate }
}
