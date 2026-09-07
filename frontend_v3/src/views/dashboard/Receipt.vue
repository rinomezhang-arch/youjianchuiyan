<template>
  <section class="receipt-page">
    <header><div><h2>入库验收</h2><p>核对实收明细，确认后更新库存及菜肴成本。成本按最近一次有效入库价计算。</p></div><el-button type="primary" data-testid="new-receipt" @click="openCreate">新增入库单</el-button></header>
    <div class="filters">
      <el-input v-model="keyword" placeholder="搜索单号、供应商" clearable />
      <el-select v-model="status" placeholder="全部状态" clearable><el-option label="待验收" value="PENDING"/><el-option label="已入库" value="ACCEPTED"/></el-select>
      <el-button :loading="loading" @click="loadReceipts">刷新</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-table :data="filteredRows" v-loading="loading" row-key="receiptId" data-testid="receipt-list">
      <el-table-column prop="receiptNo" label="入库单号" min-width="210" />
      <el-table-column prop="receiptDate" label="入库日期" width="120" />
      <el-table-column prop="supplierName" label="供应商" min-width="140" />
      <el-table-column label="实收金额" width="130"><template #default="{row}">¥{{ money(row.totalAmount) }}</template></el-table-column>
      <el-table-column label="状态" width="110"><template #default="{row}"><el-tag :type="row.status === 'ACCEPTED' ? 'success' : 'warning'">{{ stateName(row.status) }}</el-tag></template></el-table-column>
      <el-table-column prop="warehouseKeeperName" label="验收人" width="120" />
      <el-table-column label="操作" width="170"><template #default="{row}"><el-button link @click="viewDetails(row)">明细</el-button><el-button v-if="row.status === 'PENDING'" link type="primary" :loading="accepting === row.receiptId" @click="accept(row)">验收入库</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="createVisible" title="新增入库单" width="900px" destroy-on-close>
      <el-alert title="先核实实收数量、采购单位和单价。保存待验收单不会增加可用库存。" type="info" :closable="false" />
      <el-form label-position="top" class="receipt-form">
        <div class="header-fields">
          <el-form-item label="供应商"><el-select v-model="form.supplierId" filterable clearable placeholder="选择本店供应商"><el-option v-for="s in suppliers" :key="s.supplierId" :label="s.supplierName" :value="Number(s.supplierId)"/></el-select></el-form-item>
          <el-form-item label="入库日期"><el-date-picker v-model="form.receiptDate" value-format="YYYY-MM-DD" :disabled-date="date => date > new Date()" /></el-form-item>
          <el-form-item label="验收人"><el-input v-model="form.warehouseKeeperName" maxlength="50" /></el-form-item>
        </div>
        <el-table :data="items">
          <el-table-column label="原料" min-width="180"><template #default="{row}"><el-select v-model="row.ingredientId" filterable placeholder="选择原料" @change="pickIngredient(row)"><el-option v-for="i in ingredients" :key="i.ingredientId" :label="i.ingredientName" :value="i.ingredientId" /></el-select></template></el-table-column>
          <el-table-column label="采购单位" width="90" prop="unit" />
          <el-table-column label="实收数量" width="160"><template #default="{row}"><el-input-number v-model="row.actualQuantity" :min="0.01" :precision="2" controls-position="right" /></template></el-table-column>
          <el-table-column label="采购单价（元，最多8位小数）" width="210"><template #default="{row}"><el-input-number v-model="row.unitPrice" :min="0" :precision="8" controls-position="right" /></template></el-table-column>
          <el-table-column label="预计金额" width="100"><template #default="{row}">¥{{ money(row.actualQuantity * row.unitPrice) }}</template></el-table-column>
          <el-table-column width="70"><template #default="{$index}"><el-button link :disabled="items.length === 1" @click="items.splice($index,1)">移除</el-button></template></el-table-column>
        </el-table>
        <el-button class="add-line" @click="items.push(newLine())">添加原料</el-button>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" maxlength="500" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存待验收单</el-button></template>
    </el-dialog>
    <el-dialog v-model="detailsVisible" :title="`入库明细 · ${selected?.receiptNo || ''}`" width="760px">
      <el-table :data="detailRows" v-loading="detailsLoading"><el-table-column prop="ingredientName" label="原料"/><el-table-column prop="actualQuantity" label="实收数量"/><el-table-column prop="unit" label="单位"/><el-table-column label="当次单价"><template #default="{row}">¥{{ price(row.unitPrice) }}</template></el-table-column><el-table-column label="当次金额"><template #default="{row}">¥{{ money(row.amount) }}</template></el-table-column></el-table>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
const user = useUserStore()
const storeId = computed(() => Number(user.storeId || localStorage.getItem('currentStoreId') || localStorage.getItem('storeId') || 0))
const rows=ref([]), keyword=ref(''), status=ref(''), loading=ref(false), error=ref('')
const ingredients=ref([]), suppliers=ref([]), items=ref([]), form=ref({})
const createVisible=ref(false), saving=ref(false), accepting=ref(null)
const detailsVisible=ref(false), detailsLoading=ref(false), detailRows=ref([]), selected=ref(null)
const money = value => Number(value || 0).toFixed(2)
const price = value => Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 8, useGrouping: false })
const stateName = value => ({ PENDING:'待验收', ACCEPTED:'已入库', REJECTED:'已拒收' }[value] || value)
const filteredRows = computed(() => rows.value.filter(r => (!status.value || r.status===status.value) && (!keyword.value || `${r.receiptNo} ${r.supplierName || ''}`.includes(keyword.value))))
const newLine=()=>({ingredientId:'',ingredientName:'',unit:'',actualQuantity:1,unitPrice:0,qualityStatus:'QUALIFIED'})
function requireStore(){if(storeId.value<=0)throw new Error('请先选择具体门店')}
async function loadReceipts(){
  loading.value=true; error.value=''
  try{requireStore();const res=await request.get('/kitchen-supply/goods-receipts',{params:{storeId:storeId.value}});rows.value=res.data || []}
  catch(e){error.value=e.response?.data?.message || e.message || '加载入库单失败';rows.value=[]}
  finally{loading.value=false}
}
async function openCreate(){
  try{
    requireStore()
    const [a,b]=await Promise.all([request.get('/ingredients',{params:{storeId:storeId.value}}),request.get('/suppliers',{params:{storeId:storeId.value}})])
    ingredients.value=a.data || [];suppliers.value=b.data || []
    const now=new Date();const date=`${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`
    form.value={storeId:storeId.value,receiptNo:'GR'+crypto.randomUUID().replaceAll('-',''),receiptDate:date,status:'PENDING',warehouseKeeperName:user.userInfo?.name || user.userInfo?.username || '',remark:''}
    items.value=[newLine()];createVisible.value=true
  }catch(e){ElMessage.error(e.response?.data?.message || e.message || '加载原料和供应商失败')}
}
function pickIngredient(row){const i=ingredients.value.find(x=>x.ingredientId===row.ingredientId);if(i){row.ingredientName=i.ingredientName;row.unit=i.purchaseUnit || i.unit || '';row.unitPrice=Number(i.unitPrice || 0)}}
async function save(){
  if(saving.value)return
  if(items.value.some(i=>!i.ingredientId || !i.unit || !(i.actualQuantity>0) || i.unitPrice<0)){ElMessage.warning('请补全每行原料、采购单位、实收数量和单价');return}
  saving.value=true
  try{await request.post('/kitchen-supply/goods-receipts',{receipt:form.value,items:items.value});ElMessage.success('已保存待验收单');createVisible.value=false;await loadReceipts()}
  catch(e){ElMessage.error(e.response?.data?.message || e.message || '保存失败')}
  finally{saving.value=false}
}
async function accept(row){
  if(accepting.value!==null)return
  try{await ElMessageBox.confirm(`确认已核对本单实收明细？入库金额 ¥${money(row.totalAmount)}，确认后更新库存及相关菜肴成本。`,'验收入库',{confirmButtonText:'确认入库',cancelButtonText:'返回核对'})}catch{return}
  accepting.value=row.receiptId
  try{await request.put(`/kitchen-supply/goods-receipts/${row.receiptId}/accept`,{warehouseKeeperName:row.warehouseKeeperName});ElMessage.success('验收入库成功');await loadReceipts()}
  catch(e){ElMessage.error(e.response?.data?.message || e.message || '验收入库失败')}
  finally{accepting.value=null}
}
async function viewDetails(row){selected.value=row;detailRows.value=[];detailsVisible.value=true;detailsLoading.value=true;try{const res=await request.get(`/kitchen-supply/goods-receipts/${row.receiptId}/items`);detailRows.value=res.data || []}catch(e){detailsVisible.value=false;ElMessage.error(e.response?.data?.message || '读取明细失败')}finally{detailsLoading.value=false}}
watch(storeId,()=>{createVisible.value=false;detailsVisible.value=false;loadReceipts()})
onMounted(loadReceipts)
</script>

<style scoped>
.receipt-page { max-width:1440px; margin:auto; }
header { display:flex; justify-content:space-between; align-items:center; gap:20px; margin-bottom:24px; }
h2 { margin:0 0 8px; } p { color:#64748b; margin:0; line-height:1.7; }
.filters { display:flex; gap:12px; margin-bottom:18px; }.filters .el-input { max-width:320px; }.filters .el-select { width:180px; }
.receipt-form { margin-top:18px; }.header-fields { display:grid; grid-template-columns:repeat(3,1fr); gap:16px; }
.add-line { margin:16px 0; }.el-input-number { width:100%; }
@media(max-width:700px){header{align-items:flex-start;flex-direction:column}.header-fields{grid-template-columns:1fr}.filters{flex-wrap:wrap}}
</style>
