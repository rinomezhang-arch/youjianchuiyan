import assert from 'node:assert/strict'
import { previewRecipeLine } from '../src/utils/dishCostPreview.js'
const ingredient={unitPrice:20,purchaseUnit:'斤',usageUnit:'克',conversionRate:500,yieldRate:80}
assert.equal(previewRecipeLine({quantity:100,unit:'克'},ingredient).totalCost,5)
assert.equal(previewRecipeLine({quantity:100,unit:'克'},{...ingredient,unitPrice:24}).totalCost,6)
assert.equal(previewRecipeLine({quantity:0.2,unit:'斤'},ingredient).totalCost,5)
assert.equal(previewRecipeLine({quantity:100,unit:'克',yieldRate:50},ingredient).totalCost,8)
assert.ok(previewRecipeLine({quantity:100,unit:'克'},{...ingredient,conversionRate:null}).error)
assert.ok(previewRecipeLine({quantity:100,unit:'克',yieldRate:0},ingredient).error)
assert.ok(previewRecipeLine({quantity:100,unit:'袋'},ingredient).error)
assert.ok(previewRecipeLine({quantity:-1,unit:'克'},ingredient).error)
console.log('COST_PREVIEW_VECTORS=8 PASS')
