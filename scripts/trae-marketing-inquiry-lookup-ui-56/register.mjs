// 注册入口：node --import ./register.mjs <test>
import { register } from 'node:module'
register('./loader.mjs', import.meta.url)
