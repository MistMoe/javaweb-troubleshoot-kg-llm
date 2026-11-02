import assert from 'node:assert/strict'
import { readSse } from '../src/utils/sse.js'

const wire = 'event: token\r\ndata: {"value":"排障建议"}\r\n\r\nevent: result\r\ndata: {"value":{"matched":true}}\r\n\r\n'
const bytes = new TextEncoder().encode(wire)
const events = []
const response = new Response(new ReadableStream({
  start(controller) {
    // One-byte chunks exercise UTF-8 and CRLF split boundaries.
    for (const byte of bytes) controller.enqueue(Uint8Array.of(byte))
    controller.close()
  }
}), { headers: { 'content-type': 'text/event-stream' } })
await readSse(response, (event, data) => events.push([event, data]))
assert.deepEqual(events, [['token', '排障建议'], ['result', { matched: true }]])
await assert.rejects(readSse(new Response('bad', { status: 503 }), () => {}), /503/)
await assert.rejects(readSse(new Response('event: token\ndata: {', {
  headers: { 'content-type': 'text/event-stream' }
}), () => {}), /中断/)
console.log('SSE parser tests passed')
