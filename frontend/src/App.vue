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
    .replace(/^#{1,3}\s+(.+)$/gm, '<p class="font-semibold text-slate-700 mt-4 mb-1">$1</p>')
    .replace(/^[-•]\s+(.+)$/gm, '<li>$1</li>')
    .replace(/(<li>.*<\/li>\n?)+/gs, '<ul>$&</ul>')
    .replace(/\n\n+/g, '</p><p>')
    .replace(/\n/g, '<br>')
    .replace(/^(?!<[pul])(.)/gm, '<p>$1')
    .replace(/([^>])$/gm, '$1</p>')
}
</script>
