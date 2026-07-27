// Mock backend server for 又见炊烟 餐饮管理系统
const express = require('express');
const cors = require('cors');

const app = express();
const PORT = 3001;

app.use(cors());
app.use(express.json());

// In-memory database
let bookings = [];
let bookingIdCounter = 1;

// Helper function to generate booking ID
function generateBookingId() {
  const now = new Date();
  const date = now.getFullYear().toString() +
    String(now.getMonth() + 1).padStart(2, '0') +
    String(now.getDate()).padStart(2, '0');
  const time = String(now.getHours()).padStart(2, '0') +
    String(now.getMinutes()).padStart(2, '0') +
    String(now.getSeconds()).padStart(2, '0');
  const random = String(Math.floor(Math.random() * 10000)).padStart(4, '0');
  return `BK${date}${time}${random}`;
}

// Initialize some mock data
function initMockData() {
  const today = new Date().toISOString().split('T')[0];
  bookings = [
    {
      bookingId: 'BK202607271100010001',
      bookingDate: today,
      bookingTime: '11:00:00',
      timeLabel: '午餐',
      customerName: '张先生',
      customerPhone: '13800138001',
      guestCount: 10,
      tableCount: 1,
      spareTables: 0,
      tableNames: 'A01',
      tableArea: '大厅',
      occasionType: 'wedding',
      dishCount: 8,
      totalAmount: 1880,
      dishNames: '红烧肉×1,清蒸鱼×1,白切鸡×1,酱牛肉×1,糖醋排骨×1,蒜蓉西兰花×1,米饭×1,水果拼盘×1',
      staffName: '王经理',
      createdAt: today + ' 09:30:00',
      bookingStatus: 'confirmed',
      remark: '请安排靠窗位置'
    },
    {
      bookingId: 'BK202607271200010002',
      bookingDate: today,
      bookingTime: '12:00:00',
      timeLabel: '午餐',
      customerName: '李女士',
      customerPhone: '13800138002',
      guestCount: 6,
      tableCount: 1,
      spareTables: 0,
      tableNames: 'B02',
      tableArea: '包厢',
      occasionType: 'birthday',
      dishCount: 6,
      totalAmount: 1280,
      dishNames: '长寿面×1,蛋糕×1,红烧肉×1,清蒸鲈鱼×1,蒜蓉时蔬×1,米饭×1',
      staffName: '张领班',
      createdAt: today + ' 10:15:00',
      bookingStatus: 'confirmed',
      remark: '生日宴，准备蛋糕'
    },
    {
      bookingId: 'BK202607271800010003',
      bookingDate: today,
      bookingTime: '18:00:00',
      timeLabel: '晚餐',
      customerName: '陈总',
      customerPhone: '13800138003',
      guestCount: 20,
      tableCount: 2,
      spareTables: 0,
      tableNames: 'VIP1,VIP2',
      tableArea: 'VIP区',
      occasionType: 'business',
      dishCount: 12,
      totalAmount: 4800,
      dishNames: '冷菜拼盘×1,红烧鲍鱼×1,清蒸大龙虾×1,黑椒牛柳×1,松鼠桂鱼×1,佛跳墙×1,海参×1,蒜蓉菜心×1,炒饭×1,甜品×1,水果×1,茶水×1',
      staffName: '王经理',
      createdAt: today + ' 14:00:00',
      bookingStatus: 'pending',
      remark: '商务宴请，准备红酒'
    },
    {
      bookingId: 'BK202607271900010004',
      bookingDate: today,
      bookingTime: '19:00:00',
      timeLabel: '晚餐',
      customerName: '赵小姐',
      customerPhone: '13800138004',
      guestCount: 4,
      tableCount: 1,
      spareTables: 0,
      tableNames: 'C03',
      tableArea: '大厅',
      occasionType: 'a_la_carte',
      dishCount: 4,
      totalAmount: 680,
      dishNames: '红烧肉×1,宫保鸡丁×1,麻婆豆腐×1,米饭×1',
      staffName: '李服务员',
      createdAt: today + ' 16:30:00',
      bookingStatus: 'confirmed',
      remark: ''
    }
  ];
}

initMockData();

// Response wrapper
function success(data, message = 'success') {
  return { code: 200, message, data };
}

function error(message, code = 500) {
  return { code, message };
}

// 用户数据（模拟数据库）
const USERS = [
  {
    staffId: 1,
    staffName: '张婧',
    username: 'rino',
    password: '002323',
    role: 'super_admin',
    roleName: '超级管理员',
    department: '总经办',
    deptName: '总经办',
    storeId: 1,
    storeName: '宁国店',
    phone: '13800138000',
    position: '总经理'
  },
  {
    staffId: 2,
    staffName: '王经理',
    username: 'admin',
    password: '123456',
    role: 'admin',
    roleName: '管理员',
    department: '运营部',
    deptName: '运营部',
    storeId: 1,
    storeName: '宁国店',
    phone: '13800138001',
    position: '前厅经理'
  }
]

// Auth routes
app.post('/api/auth/login', (req, res) => {
  const { username, password } = req.body;
  const user = USERS.find(u => u.username === username && u.password === password);
  if (user) {
    const { password: _, ...userInfo } = user;
    res.json(success({
      token: 'mock-token-' + Date.now(),
      user: userInfo,
      storeId: user.storeId,
      storeName: user.storeName
    }));
  } else {
    res.status(401).json(error('账号或密码错误', 401));
  }
});

app.get('/api/auth/me', (req, res) => {
  const authHeader = req.headers.authorization || '';
  // mock 模式下默认返回 rino 用户
  const user = USERS.find(u => u.username === 'rino');
  const { password: _, ...userInfo } = user;
  res.json(success(userInfo));
});

// Booking routes
app.get('/api/bookings/list', (req, res) => {
  try {
    const { date, endDate, time, keyword, status, occasionType, page = 1, pageSize = 20 } = req.query;
    
    let filtered = [...bookings];
    
    // Date filter
    if (date) {
      if (endDate) {
        filtered = filtered.filter(b => b.bookingDate >= date && b.bookingDate <= endDate);
      } else {
