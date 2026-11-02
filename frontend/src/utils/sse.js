// POST SSE supports request bodies and AbortController; EventSource only supports GET.
export async function readSse(response, onEvent) {
  if (!response.ok) throw new Error(`分析请求失败 (${response.status})`)
  if (!response.headers.get('content-type')?.includes('text/event-stream')) throw new Error('服务未返回事件流')
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  function dispatch() {
    let end
    while ((end = buffer.indexOf('\n\n')) >= 0) {
      const frame = buffer.slice(0, end)
      buffer = buffer.slice(end + 2)
      const lines = frame.split('\n')
      const event = lines.find(line => line.startsWith('event:'))?.slice(6).trim() || 'message'
      const data = lines.filter(line => line.startsWith('data:')).map(line => line.slice(5).trimStart()).join('\n')
      if (data) onEvent(event, JSON.parse(data).value)
    }
  }
  try {
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      // Normalize line endings after concatenation, including CR/LF split across network chunks.
      buffer = buffer.replace(/\r\n/g, '\n')
      dispatch()
    }
    buffer += decoder.decode()
    dispatch()
    if (buffer.trim()) throw new Error('事件流意外中断')
  } finally { await reader.cancel(); reader.releaseLock() }
}
