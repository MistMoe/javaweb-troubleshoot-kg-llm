function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function asList(items) {
  const values = Array.isArray(items) ? items.filter(Boolean) : []
  if (!values.length) return '<p class="muted">暂无</p>'
  return `<ul>${values.map(item => `<li>${escapeHtml(item)}</li>`).join('')}</ul>`
}

function safeFilename(value) {
  return String(value || 'report')
    .replace(/[\\/:*?"<>|]+/g, '-')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 90) || 'report'
}

function graphSummary(graph) {
  const nodeCount = graph?.nodes?.length || 0
  const linkCount = graph?.links?.length || 0
  return `${nodeCount} 个节点 / ${linkCount} 条关系`
}

function buildReportHtml({ title, subtitle, sections }) {
  const body = sections
    .map(section => `
      <section>
        <h2>${escapeHtml(section.title)}</h2>
        ${section.html}
      </section>
    `)
    .join('')

  return `
<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <title>${escapeHtml(title)}</title>
  <style>
    body {
      margin: 0;
      padding: 42px;
      color: #172235;
      font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif;
      line-height: 1.72;
      background: #fff;
    }
    h1 {
      margin: 0;
      font-size: 30px;
      line-height: 1.25;
    }
    .subtitle {
      margin: 10px 0 28px;
      color: #6f7f94;
      font-size: 14px;
    }
    section {
      padding: 18px 0;
      border-top: 1px solid #d8e4f0;
    }
    h2 {
      margin: 0 0 10px;
      font-size: 18px;
    }
    p {
      margin: 0;
      white-space: pre-wrap;
    }
    ul {
      margin: 0;
      padding-left: 20px;
    }
    li {
      margin: 4px 0;
    }
    .meta {
      display: grid;
      grid-template-columns: 120px 1fr;
      gap: 8px 14px;
      margin-top: 8px;
    }
    .meta span {
      color: #7b8b9f;
      font-weight: 700;
    }
    code {
      display: block;
      padding: 10px 12px;
      margin: 8px 0;
      color: #334155;
      background: #f3f7fb;
      border: 1px solid #dbe8f5;
      border-radius: 6px;
      white-space: pre-wrap;
    }
    .muted {
      color: #8a98aa;
    }
  </style>
</head>
<body>
  <h1>${escapeHtml(title)}</h1>
  <p class="subtitle">${escapeHtml(subtitle)}</p>
  ${body}
</body>
</html>`
}

export function buildAnalyzeReport(result, form = {}, user = {}) {
  const matchedName = result?.matched ? result.errorName : '未命中明确故障案例'
  return buildReportHtml({
    title: `智能排障报告 - ${matchedName}`,
    subtitle: `技术栈：${form.techStack || result?.component || '未填写'} · 导出人：${user.nickname || user.username || '系统用户'}`,
    sections: [
      {
        title: '原始报错输入',
        html: `<p>${escapeHtml(form.inputText || '暂无')}</p>`
      },
      {
        title: '匹配结果',
        html: `
          <div class="meta">
            <span>故障名称</span><strong>${escapeHtml(matchedName)}</strong>
            <span>发生环节</span><strong>${escapeHtml(result?.stage || '未填写')}</strong>
            <span>技术组件</span><strong>${escapeHtml(result?.component || form.techStack || '未填写')}</strong>
            <span>图谱证据</span><strong>${escapeHtml(graphSummary(result?.graph))}</strong>
          </div>`
      },
      { title: '可能原因', html: asList(result?.causes) },
      { title: '解决方案', html: asList(result?.solutions) },
      { title: '相关文件', html: asList(result?.files) },
      { title: '建议命令', html: asList(result?.commands?.map(item => `命令：${item}`)) },
      {
        title: 'AI 排障建议',
        html: `<p>${escapeHtml(result?.aiAdvice || '暂无 AI 建议')}</p>`
      }
    ]
  })
}

export function buildHistoryReport(detail) {
  return buildReportHtml({
    title: `历史排障报告 - ${detail?.matchedErrorName || '未命中案例'}`,
    subtitle: `记录编号：#${detail?.id || '--'} · 技术栈：${detail?.techStack || '未填写'} · 提交用户：${detail?.nickname || detail?.username || '系统用户'}`,
    sections: [
      {
        title: '原始报错输入',
        html: `<p>${escapeHtml(detail?.inputText || '暂无')}</p>`
      },
      {
        title: '复盘概览',
        html: `
          <div class="meta">
            <span>命中状态</span><strong>${detail?.matched ? '已命中知识库' : '未命中，建议补充案例'}</strong>
            <span>命中案例</span><strong>${escapeHtml(detail?.matchedErrorName || '暂无')}</strong>
            <span>图谱快照</span><strong>${escapeHtml(graphSummary(detail?.graph))}</strong>
            <span>提交时间</span><strong>${escapeHtml(String(detail?.createTime || '--').replace('T', ' ').slice(0, 16))}</strong>
          </div>`
      },
      {
        title: 'AI 建议',
        html: `<p>${escapeHtml(detail?.aiAdvice || '暂无 AI 建议')}</p>`
      },
      {
        title: '图谱节点快照',
        html: asList(detail?.graph?.nodes?.map(node => `${node.category || 'Node'}：${node.name}`))
      }
    ]
  })
}

export function downloadWordReport(html, filename) {
  const blob = new Blob(['\ufeff', html], { type: 'application/msword;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${safeFilename(filename)}.doc`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

export function printReportAsPdf(html) {
  const printWindow = window.open('', '_blank', 'width=980,height=720')
  if (!printWindow) {
    throw new Error('浏览器拦截了打印窗口，请允许弹窗后重试')
  }
  printWindow.document.open()
  printWindow.document.write(html)
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
  }, 250)
}
