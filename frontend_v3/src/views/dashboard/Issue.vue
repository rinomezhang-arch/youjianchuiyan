<template>
  <section class="issue-page">
    <header><div><h2>厨房领料与加工</h2><p>按领料明细追踪毛料、净料和出成率；确认出库扣减库存，加工登记不重复扣减。</p></div><el-button type="primary" @click="openCreate">新增领料</el-button></header>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-tabs v-model="tab">
      <el-tab-pane label="领料单" name="requisitions">
        <el-table :data="headers" v-loading="loading" data-testid="requisition-list">
          <el-table-column prop="requisitionNo" label="领料单号" min-width="240" />
          <el-table-column prop="requisitionDate" label="领料日期" width="120" />
          <el-table-column prop="requestedBy" label="领料人" width="120" />
          <el-table-column label="金额" width="110"><template #default="{row}">¥{{ money(row.totalAmount) }}</template></el-table-column>
          <el-table-column label="状态" width="110"><template #default="{row}"><el-tag :type="row.status==='APPROVED'?'success':'warning'">{{ row.status==='APPROVED'?'已领出':'待确认' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="260"><template #default="{row}"><el-button link @click="showItems(row)">明细</el-button><el-button v-if="row.status==='PENDING'" link type="primary" :loading="busy===row.requisitionId" @click="approve(row)">确认出库</el-button><el-button v-if="row.status==='APPROVED'" link type="primary" @click="openProcessing(row)">登记加工</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="加工记录" name="processing">
        <el-table :data="records" data-testid="processing-list"><el-table-column prop="recordDate" label="日期"/><el-table-column prop="ingredientName" label="原料"/><el-table-column prop="requisitionItemId" label="来源明细号"/><el-table-column prop="rawQty" label="毛料数量"/><el-table-column prop="processedQty" label="净料数量"/><el-table-column prop="unit" label="单位"/><el-table-column label="出成率"><template #default="{row}">{{ money(row.yieldRate) }}%</template></el-table-column><el-table-column prop="operator" label="加工人"/></el-table>
      </el-tab-pane>
    </el-tabs>
    <el-button :loading="loading" @click="load">刷新</el-button>
    <el-dialog v-model="creating" title="新增领料" width="800px">
      <p>用原料采购单位填写领料数量。提交申请不扣库存，确认出库时按当时有效入库价记录领料成本。</p>
      <el-table :data="lines"><el-table-column label="原料" min-width="180"><template #default="{row}"><el-select v-model="row.ingredientId" filterable @change="pick(row)"><el-option v-for="i in ingredients" :key="i.ingredientId" :label="i.ingredientName" :value="i.ingredientId"/></el-select></template></el-table-column><el-table-column label="数量" width="180"><template #default="{row}"><el-input-number v-model="row.quantity" :min="0.001" :precision="3" controls-position="right"/></template></el-table-column><el-table-column prop="unit" label="单位"/><el-table-column label="当前单价"><template #default="{row}">¥{{ price(row.unitPrice) }}</template></el-table-column><el-table-column width="80"><template #default="{$index}"><el-button link :disabled="lines.length===1" @click="lines.splice($index,1)">移除</el-button></template></el-table-column></el-table>
      <el-button class="add-line" @click="lines.push(newLine())">添加原料</el-button>
      <el-input v-model="notes" type="textarea" placeholder="领料用途或备注"/>
      <template #footer><el-button @click="creating=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveRequisition">提交领料单</el-button></template>
    </el-dialog>
    <el-dialog v-model="processing" title="登记加工" width="620px">
      <el-form label-position="top">
        <el-form-item label="来源领料明细"><el-select v-model="processForm.requisitionItemId" style="width:100%" @change="pickSource"><el-option v-for="i in sources" :key="i.itemId" :label="`${i.ingredientName} · 可加工 ${remaining(i)} ${i.unit}`" :value="i.itemId" :disabled="remaining(i)<=0"/></el-select></el-form-item>
        <div class="weights"><el-form-item :label="`毛料数量（${processForm.unit || '领料单位'}）`"><el-input-number v-model="processForm.rawQty" :precision="3" :min="0.001" controls-position="right"/></el-form-item><el-form-item :label="`净料数量（${processForm.unit || '领料单位'}）`"><el-input-number v-model="processForm.processedQty" :precision="3" :min="0" controls-position="right"/></el-form-item></div>
        <el-form-item label="加工方式"><el-select v-model="processForm.preprocessingType"><el-option label="净菜整理" value="净菜整理"/><el-option label="分割去骨" value="分割去骨"/><el-option label="泡发复水" value="泡发复水"/></el-select></el-form-item>
        <el-alert :title="`预计出成率 ${processForm.rawQty>0?money(processForm.processedQty/processForm.rawQty*100):'待填写'}%。同一计量单位下，净料 ÷ 毛料 × 100%。`" type="info" :closable="false"/>
        <el-form-item label="加工备注"><el-input v-model="processForm.notes" type="textarea"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="processing=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveProcessing">保存加工记录</el-button></template>
    </el-dialog>
    <el-dialog v-model="detailsVisible" title="领料明细" width="750px"><el-table :data="detailRows"><el-table-column prop="itemId" label="明细号"/><el-table-column prop="ingredientName" label="原料"/><el-table-column prop="quantity" label="数量"/><el-table-column prop="unit" label="单位"/><el-table-column label="当次单价"><template #default="{row}">¥{{ price(row.unitPrice) }}</template></el-table-column><el-table-column label="当次金额"><template #default="{row}">¥{{ money(row.amount) }}</template></el-table-column></el-table></el-dialog>
  </section>
</template>
<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
const user=useUserStore(), storeId=computed(()=>Number(user.storeId || 0))
const operator=computed(()=>user.userInfo?.staffName || user.userInfo?.name || user.userInfo?.username || '')
const headers=ref([]),records=ref([]),ingredients=ref([]),lines=ref([]),sources=ref([]),detailRows=ref([])
const tab=ref('requisitions'),loading=ref(false),error=ref(''),creating=ref(false),saving=ref(false),busy=ref(null),processing=ref(false),detailsVisible=ref(false),notes=ref(''),processForm=ref({})
const money=v=>Number(v || 0).toFixed(2)
const price=v=>Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 8, useGrouping: false })
const newLine=()=>({ingredientId:'',quantity:1,unit:'',unitPrice:0})
function requireStore(){if(storeId.value<=0)throw new Error('请先选择具体门店')}
async function load(){loading.value=true;error.value='';try{requireStore();const [a,b]=await Promise.all([request.get('/kitchen-supply/requisitions',{params:{storeId:storeId.value}}),request.get('/kitchen-supply/preprocessing',{params:{storeId:storeId.value}})]);headers.value=a.data || [];records.value=b.data || []}catch(e){error.value=e.response?.data?.message || e.message || '加载失败'}finally{loading.value=false}}
async function openCreate(){try{requireStore();const res=await request.get('/ingredients',{params:{storeId:storeId.value}});ingredients.value=res.data || [];lines.value=[newLine()];notes.value='';creating.value=true}catch(e){ElMessage.error(e.response?.data?.message || e.message || '加载原料失败')}}
function pick(row){const i=ingredients.value.find(x=>x.ingredientId===row.ingredientId);if(i){row.unit=i.purchaseUnit || i.unit || '';row.unitPrice=i.unitPrice}}
async function saveRequisition(){if(saving.value)return;if(lines.value.some(i=>!i.ingredientId || !i.unit || !(i.quantity>0))){ElMessage.warning('请补全原料、采购单位和领料数量');return}saving.value=true;try{await request.post('/kitchen-supply/requisitions',{requisition:{storeId:storeId.value,requestedBy:operator.value,notes:notes.value},items:lines.value});ElMessage.success('领料单已提交');creating.value=false;await load()}catch(e){ElMessage.error(e.response?.data?.message || e.message || '提交失败')}finally{saving.value=false}}
async function approve(row){if(busy.value!==null)return;try{await ElMessageBox.confirm('确认原料已核对并领出？确认后扣减库存并保留领料流水。','确认出库',{confirmButtonText:'确认领出',cancelButtonText:'返回核对'})}catch{return}busy.value=row.requisitionId;try{await request.put(`/kitchen-supply/requisitions/${row.requisitionId}/approve`,{approver:operator.value});ElMessage.success('已确认出库');await load()}catch(e){ElMessage.error(e.response?.data?.message || e.message || '出库失败')}finally{busy.value=null}}
async function showItems(row){try{const res=await request.get(`/kitchen-supply/requisitions/${row.requisitionId}/items`);detailRows.value=res.data || [];detailsVisible.value=true}catch(e){ElMessage.error(e.response?.data?.message || '读取明细失败')}}
function remaining(item){return Number((Number(item.quantity)-records.value.filter(r=>r.requisitionItemId===item.itemId).reduce((s,r)=>s+Number(r.rawQty),0)).toFixed(3))}
function pickSource(){const item=sources.value.find(i=>i.itemId===processForm.value.requisitionItemId);if(item){processForm.value.ingredientId=item.ingredientId;processForm.value.unit=item.unit;processForm.value.rawQty=remaining(item);processForm.value.processedQty=remaining(item)}}
async function openProcessing(row){try{await load();const res=await request.get(`/kitchen-supply/requisitions/${row.requisitionId}/items`);sources.value=res.data || [];const item=sources.value.find(i=>remaining(i)>0);if(!item){ElMessage.info('本单已领毛料已全部登记加工');return}processForm.value={storeId:storeId.value,requisitionItemId:item.itemId,operator:operator.value,preprocessingType:'净菜整理',notes:''};pickSource();processing.value=true}catch(e){ElMessage.error(e.response?.data?.message || e.message || '加载加工来源失败')}}
async function saveProcessing(){if(saving.value)return;if(!processForm.value.requisitionItemId || !(processForm.value.rawQty>0) || processForm.value.processedQty<0){ElMessage.warning('请填写有效加工来源及毛料、净料数量');return}saving.value=true;try{await request.post('/kitchen-supply/preprocessing',processForm.value);ElMessage.success('加工记录已保存，出成率和相关成本已更新');processing.value=false;tab.value='processing';await load()}catch(e){ElMessage.error(e.response?.data?.message || e.message || '加工登记失败')}finally{saving.value=false}}
watch(storeId,()=>{creating.value=false;processing.value=false;detailsVisible.value=false;headers.value=[];records.value=[];load()})
onMounted(load)
</script>
<style scoped>
.issue-page{max-width:1440px;margin:auto}header{display:flex;align-items:center;justify-content:space-between;gap:20px;margin-bottom:20px}h2{margin:0 0 8px}p{color:#64748b;line-height:1.7;margin:0 0 16px}.weights{display:grid;grid-template-columns:1fr 1fr;gap:20px}.el-input-number{width:100%}.add-line{margin:15px 0}@media(max-width:700px){header{flex-direction:column;align-items:flex-start}.weights{grid-template-columns:1fr}}
</style>