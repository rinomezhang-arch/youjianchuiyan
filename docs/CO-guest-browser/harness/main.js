import {createApp} from 'vue'
import {createPinia} from 'pinia'
import {createRouter,createWebHistory,RouterView} from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import {useIpadStore} from '@/store/ipad'
import GuestOrder from '@/views/ipad/order/GuestOrder.vue'
const pinia=createPinia(),app=createApp(RouterView)
app.use(pinia)
// Synthetic device setup only: no employee login/auth bypass.
const ipad=useIpadStore();ipad.deviceSn='SYN-A';localStorage.setItem('ipad_device_sn','SYN-A');ipad.selectStore({id:1,name:'Synthetic1'})
app.use(createRouter({history:createWebHistory(),routes:[{path:'/guest/:bookingId',component:GuestOrder}]})).use(ElementPlus).mount('#app')
