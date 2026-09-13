#!/usr/bin/env node
/** playwright 冒烟：确认可用浏览器驱动 */
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const pwPath = process.env.PLAYWRIGHT_MODULE || 'C:/Users/rinom/.openclaw/npm/projects/tencent-weixin-openclaw-weixin-7783ac86ba__openclaw-generation__g-419ee2a92569ec32/node_modules/playwright-core';
const { chromium } = require(pwPath);
let browser;
try {
  browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  await page.setContent('<title>mx</title><h1>ok</h1>');
  console.log('CHROMIUM_OK title=' + await page.title());
} catch (e) {
  console.log('CHROMIUM_FAIL ' + e.message);
  try {
    browser = await chromium.launch({ channel: 'msedge', headless: true });
    const page = await browser.newPage();
    await page.setContent('<title>mx</title><h1>ok</h1>');
    console.log('MSEDGE_OK title=' + await page.title());
  } catch (e2) { console.log('MSEDGE_FAIL ' + e2.message); }
} finally { if (browser) await browser.close(); }
