<template>
  <div class="app">

    <!-- Header -->
    <header class="hdr">
      <div class="wrap">
        <div class="hdr-inner">
          <div class="brand">
            <div class="brand-icon">
              <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M3 13.5h3a1 1 0 001-1V7a1 1 0 011-1h2a1 1 0 011 1v8.5h1V10a1 1 0 011-1h2a1 1 0 011 1v3.5h1"/>
                <path stroke-linecap="round" stroke-linejoin="round" d="M3 17h18"/>
              </svg>
            </div>
            <div>
              <h1 class="brand-name">账单分析助手</h1>
              <p class="brand-sub">AI Financial Intelligence</p>
            </div>
          </div>
          <div class="hdr-status">
            <span class="dot"></span>
            <span>DeepSeek AI</span>
          </div>
        </div>
      </div>
    </header>

    <!-- Main -->
    <main class="main">
      <div class="wrap">

        <!-- Input row: CSV left, Question right -->
        <div class="input-row">

          <!-- 01 CSV Card -->
          <section class="card">
            <div class="card-hd">
              <h2 class="card-ttl"><em class="n">01</em>账单数据</h2>
              <div class="card-acts">
                <button class="act-btn" @click="csvContent = SAMPLE_CSV">示例数据</button>
                <label class="act-btn act-btn--gold">
                  上传 CSV
                  <input type="file" accept=".csv" class="sr-only" @change="handleFileUpload" />
                </label>
              </div>
            </div>
            <div class="drop-zone" :class="{ 'is-dragging': isDragging }"
              @dragover.prevent="isDragging = true"
              @dragleave="isDragging = false"
              @drop.prevent="handleDrop">
              <textarea
                v-model="csvContent"
                class="csv-ta"
                rows="10"
                placeholder="粘贴 CSV 格式账单数据，或拖拽文件至此..."
              ></textarea>
              <div v-if="isDragging" class="drop-hint">松开以导入 CSV</div>
            </div>
            <div class="card-ft">
              <span class="ft-meta">date · type · amount · category · description</span>
              <span v-if="csvLineCount > 0" class="ft-count">{{ csvLineCount }} 条</span>
            </div>
          </section>

          <!-- 02 Question Card -->
          <section class="card">
            <div class="card-hd">
              <h2 class="card-ttl"><em class="n">02</em>分析问题</h2>
            </div>
            <div class="chips">
              <button
                v-for="s in suggestions" :key="s"
                class="chip" :class="{ 'chip--active': question === s }"
                @click="question = s"
              >{{ s }}</button>
            </div>
            <textarea
              v-model="question"
              class="q-ta"
              rows="4"
              placeholder="输入分析问题，例如：近三个月收支如何？有没有异常消费？"
            ></textarea>
            <button
              class="analyze-btn"
              :disabled="loading || !csvContent.trim() || !question.trim()"
              @click="analyze"
            >
              <template v-if="loading">
                <span class="spin"></span>
                AI 分析中...
              </template>
              <template v-else>
                <svg class="btn-ico" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd"
                    d="M11.3 1.046A1 1 0 0112 2v5h4a1 1 0 01.82 1.573l-7 10A1 1 0 018 18v-5H4a1 1 0 01-.82-1.573l7-10a1 1 0 011.12-.38z"
                    clip-rule="evenodd"/>
                </svg>
                开始分析
              </template>
            </button>
          </section>

        </div><!-- /input-row -->

        <!-- 03 Result — full width, four states -->

        <!-- Loading -->
        <section v-if="loading" class="card">
          <div class="card-hd">
            <h2 class="card-ttl"><em class="n">03</em>分析结果</h2>
          </div>
          <div class="skel-body">
            <div v-for="(w, i) in [90,76,100,63,88,70,100,54,82,68]" :key="i"
              class="skel-line" :style="{ width: w + '%' }"/>
          </div>
        </section>

        <!-- Error -->
        <section v-else-if="error" class="card card--err">
          <div class="card-hd">
            <h2 class="card-ttl"><em class="n err-n">!</em>请求失败</h2>
          </div>
          <div class="err-body">
            <p class="err-msg">{{ error }}</p>
            <p class="err-hint">请确认后端服务已在 8080 端口启动</p>
          </div>
        </section>

        <!-- Result -->
        <section v-else-if="result" class="card">
          <div class="card-hd">
            <h2 class="card-ttl"><em class="n">03</em>分析结果</h2>
            <span class="badge" :class="result.groundTruthValid ? 'badge--ok' : 'badge--warn'">
              {{ result.groundTruthValid ? '数据已验证' : '存在疑问' }}
            </span>
          </div>
          <div class="result-scroll">
            <div class="prose" v-html="formatAnswer(result.answer)" />
          </div>
          <div class="card-ft">
            <span class="ft-meta">{{ result.validationMessage }}</span>
            <span class="ft-count">{{ result.processingTimeMs }}ms</span>
          </div>
        </section>

        <!-- Empty -->
        <section v-else class="card card--empty">
          <div class="empty">
            <div class="empty-glyph">
              <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="0.8">
                <rect x="10" y="8" width="44" height="48" rx="2"/>
                <line x1="18" y1="22" x2="46" y2="22"/>
                <line x1="18" y1="30" x2="46" y2="30"/>
                <line x1="18" y1="38" x2="34" y2="38"/>
              </svg>
            </div>
            <h2 class="empty-h">等待分析</h2>
            <p class="empty-p">载入账单数据，提出问题<br>获取 AI 财务分析报告</p>
            <div class="feat-row">
              <div class="feat"><span class="feat-sym">Σ</span><span>收支统计</span></div>
              <div class="feat-sep"></div>
              <div class="feat"><span class="feat-sym">↗</span><span>趋势分析</span></div>
              <div class="feat-sep"></div>
              <div class="feat"><span class="feat-sym">⚑</span><span>异常检测</span></div>
            </div>
          </div>
        </section>

      </div>
    </main>

    <!-- Footer -->
    <footer class="ftr">
      <div class="wrap">
        <p class="ftr-txt">Spring AI · DeepSeek · Ground Truth 校验</p>
      </div>
    </footer>

  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const SAMPLE_CSV = `date,type,amount,category,description
2024-01-05,INCOME,15000.00,Salary,January salary
2024-01-10,EXPENSE,3500.00,Rent,Monthly rent
2024-01-12,EXPENSE,580.00,Food,Grocery shopping
2024-01-15,EXPENSE,299.00,Entertainment,Streaming subscriptions
2024-01-18,EXPENSE,120.00,Transport,Monthly bus pass
2024-01-22,EXPENSE,85.00,Food,Restaurant dinner
2024-01-28,EXPENSE,350.00,Shopping,Winter clothes
2024-02-05,INCOME,15000.00,Salary,February salary
2024-02-08,EXPENSE,3500.00,Rent,Monthly rent
2024-02-10,EXPENSE,420.00,Food,Grocery shopping
2024-02-14,EXPENSE,680.00,Shopping,Valentine's day gifts
2024-02-20,EXPENSE,150.00,Transport,Taxi and rideshare
2024-02-25,EXPENSE,4800.00,Shopping,New laptop purchase
2024-03-05,INCOME,15000.00,Salary,March salary
2024-03-08,INCOME,2000.00,Freelance,Web design project
2024-03-10,EXPENSE,3500.00,Rent,Monthly rent
2024-03-12,EXPENSE,520.00,Food,Grocery shopping
2024-03-18,EXPENSE,200.00,Health,Doctor visit
2024-03-22,EXPENSE,90.00,Transport,Monthly bus pass
2024-03-28,EXPENSE,460.00,Food,Team dinner`

const csvContent = ref(SAMPLE_CSV)
const question = ref('')
const loading = ref(false)
const result = ref(null)
const error = ref(null)
const isDragging = ref(false)

const suggestions = [
  '请分析总收支情况',
  '有哪些异常支出？',
  '各类别支出如何？',
  '月度趋势分析',
]

const csvLineCount = computed(() => {
  const lines = csvContent.value.trim().split('\n').filter(l => l.trim())
  return Math.max(0, lines.length - 1)
})

async function analyze() {
  loading.value = true
  error.value = null
  result.value = null

  try {
    const response = await fetch('/api/bills/analyze', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        question: question.value,
        csvContent: csvContent.value,
      }),
    })

    if (!response.ok) {
      const err = await response.json().catch(() => ({}))
      throw new Error(err.message || `请求失败 (${response.status})`)
    }

    result.value = await response.json()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function handleFileUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => { csvContent.value = ev.target.result }
  reader.readAsText(file)
}

function handleDrop(e) {
  isDragging.value = false
  const file = e.dataTransfer.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => { csvContent.value = ev.target.result }
  reader.readAsText(file)
}

function formatAnswer(text) {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/^#{1,3}\s+(.+)$/gm, '<h4>$1</h4>')
    .replace(/^[-•]\s+(.+)$/gm, '<li>$1</li>')
    .replace(/(<li>.*<\/li>\n?)+/gs, '<ul>$&</ul>')
    .replace(/\n\n+/g, '</p><p>')
    .replace(/\n/g, '<br>')
    .replace(/^(?!<[pul])(.)/gm, '<p>$1')
    .replace(/([^>])$/gm, '$1</p>')
}
</script>

<style scoped>
/* ── CSS Tokens ──────────────────────────────────── */
.app {
  --bg:     #0c0b09;
  --surf:   #111008;
  --card:   #161412;
  --card-h: #1e1c18;
  --brd:    #282420;
  --brd-h:  #383430;
  --gold:   #c9a028;
  --gold-d: #5a4810;
  --gold-g: rgba(201, 160, 40, 0.07);
  --txt:    #e2dbd0;
  --txt-m:  #8a8278;
  --txt-d:  #504840;
  --txt-dd: #3a3228;
  --grn:    #5a9a6a;
  --gold-dark: #8a6810;
  --btn-txt:   #1a1200;
  --err-rgb:   154, 80, 80;

  min-height: 100vh;
  background: var(--bg);
  color: var(--txt);
  font-family: 'DM Sans', -apple-system, sans-serif;
  font-size: 14px;
  line-height: 1.6;
  display: flex;
  flex-direction: column;
}

/* ── Layout ──────────────────────────────────────── */
.wrap {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ── Header ──────────────────────────────────────── */
.hdr {
  border-bottom: 1px solid var(--brd);
  padding: 18px 0;
  flex-shrink: 0;
}
.hdr-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}
.brand-icon {
  width: 32px;
  height: 32px;
  border: 1px solid var(--brd);
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gold);
  flex-shrink: 0;
}
.brand-name {
  font-family: 'Cormorant Garamond', Georgia, serif;
  font-size: 21px;
  font-weight: 500;
  color: var(--txt);
  letter-spacing: 0.01em;
  line-height: 1.1;
}
.brand-sub {
  font-size: 9px;
  letter-spacing: 0.13em;
  text-transform: uppercase;
  color: var(--txt-dd);
  margin-top: 3px;
}
.hdr-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  color: var(--txt-d);
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--grn);
  animation: pulse 2.4s ease-in-out infinite;
  flex-shrink: 0;
}
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(90, 154, 106, 0.5); }
  50%       { box-shadow: 0 0 0 5px rgba(90, 154, 106, 0); }
}

/* ── Main ────────────────────────────────────────── */
.main {
  flex: 1;
  padding: 28px 0 40px;
}

/* ── Input row ───────────────────────────────────── */
.input-row {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 14px;
  margin-bottom: 14px;
}
@media (max-width: 768px) {
  .input-row { grid-template-columns: 1fr; }
}

/* ── Cards ───────────────────────────────────────── */
.card {
  background: var(--card);
  border: 1px solid var(--brd);
  border-radius: 9px;
  overflow: hidden;
  transition: border-color 0.2s;
}
.card:hover {
  border-color: var(--brd-h);
}
.card-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 16px;
  border-bottom: 1px solid var(--brd);
}
.card-ttl {
  display: flex;
  align-items: baseline;
  gap: 9px;
  font-size: 12px;
  font-weight: 500;
  color: var(--txt-m);
  letter-spacing: 0.02em;
}
.n {
  font-family: 'JetBrains Mono', monospace;
  font-size: 9px;
  font-style: normal;
  font-weight: 400;
  color: var(--gold);
  letter-spacing: 0.06em;
}
.err-n { color: rgba(var(--err-rgb), 0.75); }
.card-acts {
  display: flex;
  gap: 6px;
}
.act-btn {
  font-size: 10px;
  padding: 4px 10px;
  border-radius: 5px;
  border: 1px solid var(--brd);
  background: transparent;
  color: var(--txt-d);
  cursor: pointer;
  font-family: 'DM Sans', sans-serif;
  transition: all 0.15s;
  white-space: nowrap;
}
.act-btn:hover {
  border-color: var(--brd-h);
  color: var(--txt-m);
  background: var(--card-h);
}
.act-btn--gold {
  border-color: var(--gold-d);
  color: var(--gold);
  background: var(--gold-g);
}
.act-btn--gold:hover {
  background: rgba(201, 160, 40, 0.12);
}
.card-ft {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-top: 1px solid var(--brd);
  background: var(--surf);
}
.ft-meta {
  font-size: 9px;
  letter-spacing: 0.04em;
  color: var(--txt-dd);
}
.ft-count {
  font-family: 'JetBrains Mono', monospace;
  font-size: 10px;
  color: var(--gold);
}

/* sr-only (file input hidden) */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

/* ── Drop Zone + CSV ─────────────────────────────── */
.drop-zone {
  position: relative;
}
.drop-zone.is-dragging::after {
  content: '';
  position: absolute;
  inset: 0;
  border: 2px dashed var(--gold-d);
  pointer-events: none;
  background: rgba(201, 160, 40, 0.04);
}
.drop-hint {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(12, 11, 9, 0.75);
  color: var(--gold);
  font-size: 12px;
  letter-spacing: 0.05em;
  backdrop-filter: blur(4px);
}
.csv-ta {
  display: block;
  width: 100%;
  padding: 14px 16px;
  background: var(--surf);
  color: var(--txt-d);
  font-family: 'JetBrains Mono', monospace;
  font-size: 10.5px;
  line-height: 1.75;
  border: none;
  outline: none;
  resize: none;
  scrollbar-width: thin;
  scrollbar-color: var(--brd-h) transparent;
}
.csv-ta::placeholder { color: var(--txt-dd); }

/* ── Suggestion Chips ────────────────────────────── */
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 12px 16px 0;
}
.chip {
  font-size: 10px;
  padding: 4px 11px;
  border-radius: 16px;
  border: 1px solid var(--brd);
  background: transparent;
  color: var(--txt-d);
  cursor: pointer;
  font-family: 'DM Sans', sans-serif;
  transition: all 0.15s;
  letter-spacing: 0.01em;
}
.chip:hover {
  border-color: var(--gold-d);
  color: var(--txt-m);
}
.chip--active {
  border-color: var(--gold-d);
  color: var(--gold);
  background: var(--gold-g);
}

/* ── Question Textarea ───────────────────────────── */
.q-ta {
  display: block;
  width: 100%;
  padding: 12px 16px;
  background: transparent;
  color: var(--txt);
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  line-height: 1.7;
  border: none;
  outline: none;
  resize: none;
}
.q-ta::placeholder { color: var(--txt-dd); }

/* ── Analyze Button ──────────────────────────────── */
.analyze-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: calc(100% - 32px);
  margin: 0 16px 16px;
  padding: 11px 24px;
  border-radius: 7px;
  border: none;
  background: linear-gradient(135deg, var(--gold) 0%, var(--gold-dark) 100%);
  color: var(--btn-txt);
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: all 0.2s;
}
.analyze-btn:hover:not(:disabled) {
  filter: brightness(1.1);
  transform: translateY(-1px);
  box-shadow: 0 4px 18px rgba(201, 160, 40, 0.28);
}
.analyze-btn:active:not(:disabled) {
  transform: translateY(0);
}
.analyze-btn:disabled {
  background: var(--card-h);
  color: var(--txt-dd);
  cursor: not-allowed;
}
.btn-ico {
  width: 13px;
  height: 13px;
  flex-shrink: 0;
}
.spin {
  width: 13px;
  height: 13px;
  border: 2px solid rgba(26, 18, 0, 0.3);
  border-top-color: var(--btn-txt);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  flex-shrink: 0;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Skeleton ────────────────────────────────────── */
.skel-body {
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.skel-line {
  height: 11px;
  border-radius: 3px;
  background: linear-gradient(90deg, var(--card-h) 25%, var(--brd) 50%, var(--card-h) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.6s infinite;
}
@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ── Error ───────────────────────────────────────── */
.card--err {
  border-color: rgba(var(--err-rgb), 0.35);
}
.err-body { padding: 16px; }
.err-msg {
  font-size: 12px;
  color: rgba(var(--err-rgb), 0.78);
  background: rgba(var(--err-rgb), 0.1);
  border: 1px solid rgba(var(--err-rgb), 0.2);
  border-radius: 7px;
  padding: 11px 14px;
  line-height: 1.6;
  margin-bottom: 8px;
}
.err-hint {
  font-size: 10px;
  color: var(--txt-dd);
}

/* ── Validation Badge ────────────────────────────── */
.badge {
  font-family: 'JetBrains Mono', monospace;
  font-size: 9px;
  padding: 3px 9px;
  border-radius: 12px;
  letter-spacing: 0.04em;
  border: 1px solid;
}
.badge--ok {
  background: rgba(90, 154, 106, 0.1);
  border-color: rgba(90, 154, 106, 0.3);
  color: var(--grn);
}
.badge--warn {
  background: rgba(180, 130, 40, 0.1);
  border-color: rgba(180, 130, 40, 0.3);
  color: #b08040;
}

/* ── Result ──────────────────────────────────────── */
.result-scroll {
  max-height: 60vh;
  overflow-y: auto;
  padding: 20px;
  scrollbar-width: thin;
  scrollbar-color: var(--brd-h) transparent;
}
.prose {
  font-size: 13px;
  color: var(--txt-m);
  line-height: 1.85;
}
.prose :deep(p) {
  margin-bottom: 0.85rem;
}
.prose :deep(strong), .prose :deep(b) {
  font-weight: 500;
  color: var(--txt);
}
.prose :deep(em) {
  font-style: italic;
}
.prose :deep(h4) {
  font-family: 'Cormorant Garamond', Georgia, serif;
  font-size: 15px;
  font-weight: 500;
  color: var(--txt-m);
  margin: 1.2rem 0 0.4rem;
  letter-spacing: 0.01em;
}
.prose :deep(ul) {
  margin-left: 1.25rem;
  margin-bottom: 0.85rem;
  list-style: disc;
}
.prose :deep(ol) {
  margin-left: 1.25rem;
  margin-bottom: 0.85rem;
  list-style: decimal;
}
.prose :deep(li) {
  margin-bottom: 0.25rem;
  line-height: 1.75;
}

/* ── Empty State ─────────────────────────────────── */
.card--empty {
  min-height: 320px;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 44px 32px;
  text-align: center;
}
.empty-glyph {
  width: 72px;
  height: 72px;
  color: var(--brd-h);
  margin-bottom: 22px;
}
.empty-h {
  font-family: 'Cormorant Garamond', Georgia, serif;
  font-size: 26px;
  font-weight: 400;
  color: var(--txt-m);
  margin-bottom: 10px;
  letter-spacing: 0.01em;
}
.empty-p {
  font-size: 12px;
  color: var(--txt-d);
  line-height: 1.75;
  margin-bottom: 28px;
}
.feat-row {
  display: flex;
  align-items: center;
  gap: 18px;
}
.feat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  color: var(--txt-d);
  letter-spacing: 0.03em;
}
.feat-sym {
  font-family: 'Cormorant Garamond', Georgia, serif;
  font-size: 20px;
  color: var(--gold);
  line-height: 1;
}
.feat-sep {
  width: 1px;
  height: 30px;
  background: var(--brd);
}

/* ── Footer ──────────────────────────────────────── */
.ftr {
  border-top: 1px solid var(--brd);
  padding: 14px 0;
  flex-shrink: 0;
}
.ftr-txt {
  text-align: center;
  font-size: 9px;
  letter-spacing: 0.09em;
  text-transform: uppercase;
  color: var(--txt-dd);
}
</style>
