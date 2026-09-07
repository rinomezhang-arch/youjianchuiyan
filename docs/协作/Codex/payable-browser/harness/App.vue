<template><main><h2>隔离应付验收 · 合成数据</h2><form v-if="!logged" @submit.prevent="login"><label>合成账号<input aria-label="合成账号" v-model="username"></label><label>密码<input aria-label="密码" type="password" v-model="password"></label><button type="submit">登录</button><p>{{error}}</p></form><PayableLedger v-else /></main></template>
<script setup>
import {ref} from 'vue'
import request from '@/utils/request.js'
import PayableLedger from '@/components/PayableLedger.vue'
const logged=ref(!!localStorage.getItem('token')),username=ref(''),password=ref(''),error=ref('')
async function login(){try{const res=await request.post('/auth/login',{username:username.value,password:password.value});localStorage.setItem('token',res.data.token);password.value='';logged.value=true}catch{error.value='登录未通过'}}
</script>
<style>body{margin:30px;background:#faf9f6;color:#243429;font-family:Arial,sans-serif}main{max-width:1200px;margin:auto}label{margin-right:16px}input{padding:8px}</style>
