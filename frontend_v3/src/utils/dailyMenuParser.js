/**
 * 每日菜单文本解析器
 * 将用户粘贴的纯文本解析为分段数据结构
 *
 * 输入格式示例：
 *   特别介绍:
 *   红烧溪石班
 *   广西玉林带皮狗锅
 *
 *   急推:
 *   红酒牛尾锅
 *   老虎班，清蒸，葱油蒸
 *
 *   沽清:
 *   酸汤牛舌锅
 *   地皮菜炒鸡蛋
 *
 * 规则：
 * 1. 以冒号（中/英）结尾的非空行 = 段落标题
 * 2. 标题之后到下一个标题之前的非空行 = 该段落的菜品
 * 3. 自动去除菜品行的序号前缀（1.  ①  -  • 等）
 * 4. 菜品名中的逗号/顿号保留（如"老虎班，清蒸，葱油蒸"视为一个菜品）
 * 5. 忽略空行
 */

// 预设的段落标题（按优先级排序）
const PRESET_SECTIONS = [
  '特別介绍', '特别介绍', '推荐', '招牌', '特色',
  '急推', '特价', '促销', '热销',
  '沽清', '售罄', '估清', '已沽清',
  '新品', '上新',
  '活动', '优惠'
]

// 段落标题对应的样式类型
const SECTION_STYLE = {
  '特別介绍': 'feature',
  '特别介绍': 'feature',
  '推荐': 'feature',
  '招牌': 'feature',
  '特色': 'feature',
  '急推': 'urgent',
  '特价': 'urgent',
  '促销': 'urgent',
  '热销': 'urgent',
  '沽清': 'soldout',
  '售罄': 'soldout',
  '估清': 'soldout',
  '已沽清': 'soldout',
  '新品': 'new',
  '上新': 'new',
  '活动': 'promo',
  '优惠': 'promo'
}

const SECTION_LABEL_EN = {
  '特別介绍': "Today's Feature",
  '特别介绍': "Today's Feature",
  '推荐': 'Recommendation',
  '招牌': 'Signature',
  '特色': 'Specialty',
  '急推': 'Urgent',
  '特价': 'Special',
  '促销': 'Promotion',
  '热销': 'Hot',
  '沽清': 'Sold Out',
  '售罄': 'Sold Out',
  '估清': 'Sold Out',
  '已沽清': 'Sold Out',
  '新品': 'New',
  '上新': 'New',
  '活动': 'Event',
  '优惠': 'Offer'
}

/**
 * 解析文本为结构化数据
 * @param {string} text - 原始文本
 * @returns {Array<{section: string, style: string, items: string[], labelEn: string}>}
 */
export function parseDailyMenu(text) {
  if (!text || !text.trim()) return []

  const lines = text.split(/\r?\n/)
  const sections = []
  let currentSection = null

  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (!line) continue // 忽略空行

    // 检测是否为段落标题（以冒号结尾）
    const titleMatch = line.match(/^(.+?)[：:]$/)
    if (titleMatch) {
      const title = titleMatch[1].trim()
      // 检查是否为预设标题（或自动识别）
      const matchedTitle = PRESET_SECTIONS.find(s => title.includes(s)) || title
      const style = SECTION_STYLE[matchedTitle] || 'default'
      const labelEn = SECTION_LABEL_EN[matchedTitle] || ''

      currentSection = {
        section: matchedTitle,
        style,
        labelEn,
        items: []
      }
      sections.push(currentSection)
    } else if (currentSection) {
      // 菜品行：去除序号前缀
      const cleaned = line
        .replace(/^[\s]*[①②③④⑤⑥⑦⑧⑨⑩\d]+[\.\)\s、]+/, '')
        .replace(/^[\s]*[-•·●★☆◆◇■□]+[\s]*/, '')
        .trim()
      if (cleaned) {
        currentSection.items.push(cleaned)
      }
    }
  }

  return sections.filter(s => s.items.length > 0)
}

/**
 * 生成下载用的独立 H5 HTML
 * @param {Array} sections - 解析后的段落数据
 * @param {Object} meta - 元信息 { date, storeName }
 * @returns {string} 完整 HTML 字符串
 */
export function generateH5Html(sections, meta = {}) {
  const dateStr = meta.date || new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
  const storeName = meta.storeName || '又见炊烟'

  const sectionsHtml = sections.map(s => {
    const itemsHtml = s.items.map(item => {
      // 沽清类显示删除线
      if (s.style === 'soldout') {
        return `<li class="dish soldout">${escapeHtml(item)}</li>`
      }
      return `<li class="dish">${escapeHtml(item)}</li>`
    }).join('')

    const styleClass = `section-${s.style}`
    const badgeHtml = s.labelEn ? `<span class="badge">${escapeHtml(s.labelEn)}</span>` : ''

    return `
    <section class="section ${styleClass}">
      <div class="section-header">
        <h2 class="section-title">${escapeHtml(s.section)}</h2>
        ${badgeHtml}
      </div>
      <ul class="dish-list">${itemsHtml}</ul>
    </section>`
  }).join('')

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<meta name="theme-color" content="#1a3a2a">
<title>每日菜单 · ${escapeHtml(storeName)}</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
  html { font-size: 16px; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "Segoe UI", sans-serif;
    background: #F5F0E8;
    color: #2A2A28;
    min-height: 100vh;
    padding-bottom: 40px;
  }
  .header {
    background: linear-gradient(135deg, #1a3a2a 0%, #2d5a3d 100%);
    color: #F5F0E8;
    padding: 32px 20px 28px;
    text-align: center;
    position: relative;
  }
  .header::after {
    content: '';
    position: absolute;
    bottom: 0; left: 0; right: 0;
    height: 3px;
    background: linear-gradient(90deg, transparent, #C4A35A 50%, transparent);
  }
  .header .brand {
    font-size: 13px;
    letter-spacing: 4px;
    color: #C4A35A;
    margin-bottom: 8px;
  }
  .header .title {
    font-size: 22px;
    font-weight: 700;
    letter-spacing: 2px;
  }
  .header .date {
    font-size: 13px;
    color: rgba(245, 240, 232, 0.75);
    margin-top: 10px;
  }
  .container {
    max-width: 480px;
    margin: 0 auto;
    padding: 20px 16px;
  }
  .section {
    background: #fff;
    border-radius: 12px;
    margin-bottom: 16px;
    overflow: hidden;
    box-shadow: 0 2px 12px rgba(26, 58, 42, 0.06);
  }
  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 18px;
    border-bottom: 1px solid #F0EAD8;
  }
  .section-title {
    font-size: 16px;
    font-weight: 700;
    letter-spacing: 1px;
  }
  .badge {
    font-size: 11px;
    padding: 2px 10px;
    border-radius: 10px;
    letter-spacing: 0.5px;
  }
  /* 特别介绍 — 金色 */
  .section-feature { border-top: 3px solid #C4A35A; }
  .section-feature .section-title { color: #C4A35A; }
  .section-feature .badge { background: #C4A35A; color: #fff; }
  /* 急推 — 红色 */
  .section-urgent { border-top: 3px solid #8B2020; }
  .section-urgent .section-title { color: #8B2020; }
  .section-urgent .badge { background: #8B2020; color: #fff; }
  /* 沽清 — 灰色 */
  .section-soldout { border-top: 3px solid #999; opacity: 0.85; }
  .section-soldout .section-title { color: #666; }
  .section-soldout .badge { background: #999; color: #fff; }
  .section-soldout .dish-list { color: #888; }
  /* 新品 — 墨绿 */
  .section-new { border-top: 3px solid #1a3a2a; }
  .section-new .section-title { color: #1a3a2a; }
  .section-new .badge { background: #1a3a2a; color: #fff; }
  /* 活动 — 暖色 */
  .section-promo { border-top: 3px solid #C4A35A; }
  .section-promo .section-title { color: #B87333; }
  .section-promo .badge { background: #B87333; color: #fff; }
  /* 默认 */
  .section-default { border-top: 3px solid #1a3a2a; }
  .section-default .section-title { color: #1a3a2a; }

  .dish-list {
    list-style: none;
    padding: 8px 0;
  }
  .dish {
    padding: 10px 18px;
    font-size: 15px;
    line-height: 1.6;
    border-bottom: 1px solid #F7F3E8;
    display: flex;
    align-items: flex-start;
  }
  .dish:last-child { border-bottom: none; }
  .dish::before {
    content: '·';
    color: #C4A35A;
    font-size: 20px;
    margin-right: 10px;
    line-height: 1;
  }
  .section-soldout .dish::before { color: #999; content: '×'; }
  .section-urgent .dish::before { color: #8B2020; content: '★'; }
  .dish.soldout {
    text-decoration: line-through;
    text-decoration-color: #999;
  }
  .dish.soldout::before { text-decoration: none; }

  .footer {
    text-align: center;
    padding: 24px 20px;
    font-size: 12px;
    color: #999;
    line-height: 1.8;
  }
  .footer .store {
    font-size: 13px;
    color: #1a3a2a;
    font-weight: 600;
    letter-spacing: 1px;
    margin-bottom: 4px;
  }
  @media (max-width: 360px) {
    html { font-size: 15px; }
    .header .title { font-size: 20px; }
  }
</style>
</head>
<body>
  <header class="header">
    <div class="brand">YOUJIANCHUIYAN · 每日菜单</div>
    <h1 class="title">又见炊烟</h1>
    <div class="date">${dateStr}</div>
  </header>
  <main class="container">
    ${sectionsHtml}
  </main>
  <footer class="footer">
    <div class="store">${escapeHtml(storeName)}</div>
    <div>匠心之作 · 时令为先</div>
  </footer>
</body>
</html>`
}

function escapeHtml(str) {
  if (typeof document !== 'undefined') {
    const div = document.createElement('div')
    div.textContent = str
    return div.innerHTML
  }
  // Node.js 环境回退
  return String(str).replace(/[&<>"']/g, c => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[c]))
}

/**
 * 下载 H5 文件
 * @param {string} html - HTML 内容
 * @param {string} filename - 文件名
 */
export function downloadH5(html, filename) {
  const blob = new Blob([html], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename || `daily-menu-${Date.now()}.html`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

/**
 * 获取默认示例文本
 */
export function getDefaultSample() {
  return `特別介绍:
红烧溪石班
广西玉林带皮狗锅
红烧牛排锅
古法红烧肉
金牌猪肚鸡
红烧臭桂鱼
绩溪牛肉锅
水阳三宝锅

急推:
红酒牛尾锅
老虎班，清蒸，葱油蒸
长脚蟹清蒸，蒜蓉蒸
面包蟹姜葱炒，避风塘
生啫黄蟮

沽清:
酸汤牛舌锅
地皮菜炒鸡蛋
子然羊排`
}
