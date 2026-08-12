/**
 * iPad 点餐子系统路由
 * 路由前缀 /ipad，横屏布局，触控优化
 */
const iPadRoutes = [
  {
    path: '/ipad',
    component: () => import('@/views/ipad/IpadLayout.vue'),
    redirect: '/ipad/store',
    children: [
      // 层级1：登录层
      { path: 'store', name: 'IpadStore', component: () => import('@/views/ipad/login/StoreSelect.vue'), meta: { title: '选择门店' } },
      { path: 'login', name: 'IpadLogin', component: () => import('@/views/ipad/login/Login.vue'), meta: { title: '员工登录' } },
      
      // 层级2：桌台首页
      { path: 'home', name: 'IpadHome', component: () => import('@/views/ipad/home/TableMap.vue'), meta: { requiresAuth: true, title: '桌台' } },
      { path: 'bookings', name: 'IpadBookings', component: () => import('@/views/ipad/home/BookingList.vue'), meta: { requiresAuth: true, title: '预定' } },
      { path: 'wait', name: 'IpadWait', component: () => import('@/views/ipad/home/WaitQueue.vue'), meta: { requiresAuth: true, title: '等位' } },
      
      // 层级3：点餐核心
      { path: 'order', name: 'IpadOrderNew', component: () => import('@/views/ipad/order/OrderMain.vue'), meta: { requiresAuth: true, title: '点餐' } },
      { path: 'order/:bookingId', name: 'IpadOrder', component: () => import('@/views/ipad/order/OrderMain.vue'), meta: { requiresAuth: true, title: '点餐' } },
      { path: 'guest-order/:bookingId', name: 'IpadGuestOrder', component: () => import('@/views/ipad/order/GuestOrder.vue'), meta: { title: '客人点菜' } },
      { path: 'dishes', name: 'IpadDishes', component: () => import('@/views/ipad/order/DishCategory.vue'), meta: { requiresAuth: true, title: '菜品' } },
      { path: 'dish/:dishId', name: 'IpadDishDetail', component: () => import('@/views/ipad/order/DishDetail.vue'), meta: { requiresAuth: true, title: '菜品详情' } },
      { path: 'packages', name: 'IpadPackages', component: () => import('@/views/ipad/order/PackageList.vue'), meta: { requiresAuth: true, title: '套餐' } },
      { path: 'cart', name: 'IpadCart', component: () => import('@/views/ipad/order/DishList.vue'), meta: { requiresAuth: true, title: '已点' } },
      
      // 层级4：结算
      { path: 'bill/:bookingId', name: 'IpadBill', component: () => import('@/views/ipad/settlement/BillMain.vue'), meta: { requiresAuth: true, title: '账单' } },
      { path: 'pay/:bookingId', name: 'IpadPay', component: () => import('@/views/ipad/settlement/PaySelect.vue'), meta: { requiresAuth: true, title: '支付' } },
      { path: 'history', name: 'IpadHistory', component: () => import('@/views/ipad/settlement/History.vue'), meta: { requiresAuth: true, title: '历史' } },
      
      // 层级5：会员辅助
      { path: 'member', name: 'IpadMember', component: () => import('@/views/ipad/mine/MemberCenter.vue'), meta: { requiresAuth: true, title: '会员' } },
      { path: 'settings', name: 'IpadSettings', component: () => import('@/views/ipad/mine/DeviceSetting.vue'), meta: { requiresAuth: true, title: '设置' } },
    ]
  }
]

export default iPadRoutes
