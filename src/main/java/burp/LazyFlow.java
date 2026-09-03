package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.hotkey.HotKey;
import burp.api.montoya.ui.hotkey.HotKeyHandler;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Collections;

public class LazyFlow implements BurpExtension {

    private MontoyaApi api;

    // ── embedded HTML ─────────────────────────────────────────────────────────

    private static final String HTML_TEMPLATE =
        "<!DOCTYPE html>\n" +
        "<html lang=\"en\">\n" +
        "<head>\n" +
        "<meta charset=\"UTF-8\"/>\n" +
        "<link rel=\"icon\" type=\"image/png\" href=\"data:image/png;base64,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\">\n" +
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n" +
        "<title>LazyFlow</title>\n" +
        "<style>\n" +
        "  @import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600;700&family=Syne:wght@400;700;800&display=swap');\n" +
        "  :root {\n" +
        "    --bg:#0a0c10;--surface:#111318;--border:#1e2128;--border2:#2a2f3a;\n" +
        "    --text:#c9d1d9;--muted:#6e7681;--accent:#58a6ff;\n" +
        "    --req-head:#1a2744;--res-head:#1a2a1a;--req-col:#58a6ff;--res-col:#3fb950;\n" +
        "    --left-w:220px;--right-w:360px;\n" +
        "  }\n" +
        "  *{box-sizing:border-box;margin:0;padding:0;}\n" +
        "  body{background:var(--bg);color:var(--text);font-family:'JetBrains Mono',monospace;font-size:13px;min-height:100vh;overflow-x:hidden;}\n" +
        "  header{position:sticky;top:0;z-index:200;background:var(--surface);border-bottom:1px solid var(--border);padding:10px 16px;display:flex;align-items:center;gap:14px;}\n" +
        "  header h1{font-family:'Syne',sans-serif;font-size:14px;font-weight:800;letter-spacing:.12em;text-transform:uppercase;color:var(--accent);white-space:nowrap;}\n" +
        "  .upload-zone{display:flex;align-items:center;gap:10px;flex:1;}\n" +
        "  .upload-label{display:flex;align-items:center;gap:8px;padding:5px 12px;border:1px dashed var(--border2);border-radius:6px;cursor:pointer;color:var(--muted);font-size:11px;transition:border-color .2s,background .2s;}\n" +
        "  .upload-label:hover,.upload-label.dragover{border-color:var(--accent);color:var(--accent);background:#58a6ff11;}\n" +
        "  #file-input{display:none;}\n" +
        "  #filename{font-size:11px;color:var(--muted);max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}\n" +
        "  .stats{margin-left:auto;display:flex;gap:14px;font-size:11px;color:var(--muted);}\n" +
        "  .stats span{white-space:nowrap;}.stats b{color:var(--text);}\n" +
        "  .panel-toggle-btn{display:flex;align-items:center;gap:6px;padding:5px 10px;border:1px solid var(--border2);border-radius:5px;background:transparent;color:var(--muted);font-family:'JetBrains Mono',monospace;font-size:11px;cursor:pointer;transition:all .15s;white-space:nowrap;user-select:none;}\n" +
        "  .panel-toggle-btn:hover{border-color:var(--accent);color:var(--accent);background:#58a6ff0a;}\n" +
        "  .panel-toggle-btn.active{border-color:var(--accent);color:var(--accent);background:#58a6ff18;}\n" +
        "  .layout{display:flex;height:calc(100vh - 49px);overflow:hidden;}\n" +
        "  .left-panel{width:var(--left-w);flex-shrink:0;border-right:1px solid var(--border);background:var(--surface);display:flex;flex-direction:column;overflow:hidden;transition:width .25s ease,opacity .2s ease;}\n" +
        "  .left-panel.collapsed{width:0;opacity:0;pointer-events:none;}\n" +
        "  .left-panel-header{padding:10px 12px;border-bottom:1px solid var(--border);font-family:'Syne',sans-serif;font-size:10px;font-weight:800;text-transform:uppercase;letter-spacing:.12em;color:var(--muted);display:flex;align-items:center;justify-content:space-between;flex-shrink:0;}\n" +
        "  .left-panel-actions{display:flex;gap:6px;}\n" +
        "  .tiny-btn{font-family:'JetBrains Mono',monospace;font-size:9px;padding:2px 6px;border:1px solid var(--border2);border-radius:3px;background:transparent;color:var(--muted);cursor:pointer;transition:all .15s;}\n" +
        "  .tiny-btn:hover{border-color:var(--accent);color:var(--accent);}\n" +
        "  .left-panel-content{flex:1;overflow-y:auto;padding:8px;}\n" +
        "  .vis-row{display:flex;align-items:center;gap:8px;padding:5px 6px;border-radius:4px;cursor:pointer;transition:background .1s;margin-bottom:2px;}\n" +
        "  .vis-row:hover{background:#ffffff08;}\n" +
        "  .vis-check{width:14px;height:14px;border-radius:3px;border:2px solid;flex-shrink:0;display:flex;align-items:center;justify-content:center;transition:background .15s;font-size:9px;color:#000;font-weight:800;}\n" +
        "  .vis-check.unchecked{background:transparent!important;}\n" +
        "  .vis-swatch{width:8px;height:8px;border-radius:50%;flex-shrink:0;}\n" +
        "  .vis-label{font-size:10px;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--text);}\n" +
        "  .vis-row.hidden-val .vis-label{color:var(--muted);text-decoration:line-through;}\n" +
        "  .flow-area{flex:1;overflow-y:auto;padding:16px 16px 40px;display:flex;flex-direction:column;gap:14px;min-width:0;}\n" +
        "  .pair-card{border:1px solid var(--border);border-radius:8px;overflow:hidden;transition:border-color .2s;}\n" +
        "  .pair-card:hover{border-color:var(--border2);}\n" +
        "  .pair-card.highlighted{border-color:var(--accent)!important;}\n" +
        "  .pair-header{display:flex;align-items:center;padding:7px 12px;background:var(--surface);border-bottom:1px solid var(--border);gap:10px;font-size:11px;}\n" +
        "  .pair-index{font-family:'Syne',sans-serif;font-weight:800;color:var(--muted);font-size:10px;letter-spacing:.1em;}\n" +
        "  .pair-summary-req{color:var(--req-col);flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}\n" +
        "  .status-badge{padding:2px 7px;border-radius:3px;font-size:10px;font-weight:600;}\n" +
        "  .s2xx{background:#1a3a1a;color:#3fb950;}.s3xx{background:#1a2f44;color:#58a6ff;}\n" +
        "  .s4xx{background:#3a1a1a;color:#f85149;}.s5xx{background:#3a2a1a;color:#e3b341;}\n" +
        "  .pair-body{display:grid;grid-template-columns:1fr 1fr;}\n" +
        "  .http-block{overflow:hidden;display:flex;flex-direction:column;max-height:75vh;}\n" +
        "  .http-block+.http-block{border-left:1px solid var(--border);}\n" +
        "  .http-block-header{padding:5px 10px;font-size:10px;font-weight:700;letter-spacing:.1em;text-transform:uppercase;display:flex;align-items:center;gap:6px;flex-shrink:0;}\n" +
        "  .req-header-bar{background:var(--req-head);color:var(--req-col);}\n" +
        "  .res-header-bar{background:var(--res-head);color:var(--res-col);}\n" +
        "  .http-raw{padding:10px 12px;white-space:pre-wrap;word-break:break-all;font-size:11px;line-height:1.7;background:var(--bg);min-height:50px;tab-size:2;overflow-y:auto;flex:1 1 auto;}\n" +
        "  mark.corr-mark{cursor:pointer;font-family:inherit;font-size:inherit;transition:filter .15s,opacity .15s;}\n" +
        "  mark.corr-mark:hover{filter:brightness(1.5);}\n" +
        "  mark.corr-mark.dimmed{opacity:0.2;}\n" +
        "  mark.corr-mark.val-hidden{background:transparent!important;border-color:transparent!important;color:inherit!important;cursor:default;pointer-events:none;}\n" +
        "  .right-panel{width:var(--right-w);flex-shrink:0;border-left:1px solid var(--border);background:var(--surface);display:flex;flex-direction:column;overflow:hidden;transition:width .25s ease,opacity .2s ease;}\n" +
        "  .right-panel.collapsed{width:0;opacity:0;pointer-events:none;}\n" +
        "  .panel-header{padding:10px 14px;border-bottom:1px solid var(--border);font-family:'Syne',sans-serif;font-size:10px;font-weight:800;text-transform:uppercase;letter-spacing:.12em;color:var(--muted);display:flex;align-items:center;justify-content:space-between;flex-shrink:0;}\n" +
        "  .filter-bar{padding:7px 10px;border-bottom:1px solid var(--border);flex-shrink:0;}\n" +
        "  .filter-input{width:100%;background:var(--bg);border:1px solid var(--border);border-radius:4px;color:var(--text);font-family:'JetBrains Mono',monospace;font-size:11px;padding:4px 8px;outline:none;transition:border-color .15s;}\n" +
        "  .filter-input:focus{border-color:var(--accent);}\n" +
        "  .filter-input::placeholder{color:var(--muted);}\n" +
        "  .panel-content{flex:1;overflow-y:auto;padding:8px;}\n" +
        "  .corr-entry{border:1px solid var(--border);border-radius:5px;margin-bottom:7px;overflow:hidden;cursor:pointer;transition:border-color .15s;}\n" +
        "  .corr-entry:hover{border-color:var(--border2);}\n" +
        "  .corr-entry.active-entry{border-color:var(--accent);}\n" +
        "  .corr-value-bar{padding:5px 9px;font-size:11px;display:flex;align-items:center;gap:8px;border-bottom:1px solid var(--border);}\n" +
        "  .corr-dot{width:9px;height:9px;border-radius:50%;flex-shrink:0;}\n" +
        "  .corr-value-text{flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:11px;}\n" +
        "  .corr-flow{padding:5px 9px;display:flex;flex-direction:column;gap:4px;font-size:10px;}\n" +
        "  .corr-flow-row{display:flex;align-items:flex-start;gap:6px;color:var(--muted);}\n" +
        "  .corr-flow-label{font-weight:700;font-size:9px;text-transform:uppercase;letter-spacing:.08em;padding:2px 5px;border-radius:3px;white-space:nowrap;flex-shrink:0;}\n" +
        "  .lbl-from{background:#1a2744;color:var(--req-col);}.lbl-to{background:#1a3a1a;color:var(--res-col);}\n" +
        "  .corr-flow-loc{color:var(--text);word-break:break-all;}\n" +
        "  .empty-state{display:flex;flex-direction:column;align-items:center;justify-content:center;height:100%;gap:12px;color:var(--muted);}\n" +
        "  .empty-icon{font-size:40px;opacity:.25;}\n" +
        "  .empty-text{font-family:'Syne',sans-serif;font-size:12px;font-weight:700;text-transform:uppercase;letter-spacing:.1em;}\n" +
        "  .empty-sub{font-size:11px;text-align:center;max-width:200px;line-height:1.6;}\n" +
        "  .loading{display:none;align-items:center;justify-content:center;height:100%;flex-direction:column;gap:12px;color:var(--muted);}\n" +
        "  .spinner{width:26px;height:26px;border:2px solid var(--border2);border-top-color:var(--accent);border-radius:50%;animation:spin .7s linear infinite;}\n" +
        "  @keyframes spin{to{transform:rotate(360deg);}}\n" +
        "  .error-banner{display:none;margin:10px;padding:9px 12px;background:#3a1a1a;border:1px solid #f8514966;border-radius:5px;color:#f85149;font-size:11px;}\n" +
        "  .export-btns{display:none;flex-direction:row;gap:6px;margin:8px;flex-shrink:0;}\n" +
        "  .export-btn{flex:1;padding:7px 6px;background:transparent;border:1px solid #3fb95066;border-radius:5px;color:#3fb950;font-family:'JetBrains Mono',monospace;font-size:10px;cursor:pointer;transition:all .15s;text-align:center;}\n" +
        "  .export-btn:hover{background:#3fb95018;border-color:#3fb950;}\n" +
        "  ::-webkit-scrollbar-track{background:transparent;}\n" +
        "  ::-webkit-scrollbar-thumb{background:var(--border2);border-radius:3px;}\n" +
        "  ::-webkit-scrollbar-thumb:hover{background:#3a4050;}\n" +
        "  .arrow{color:var(--muted);margin:0 4px;}\n" +
        "</style>\n" +
        "</head>\n" +
        "<body>\n" +
        "\n" +
        "<header>\n" +
        "  <h1>⬡ LazyFlow</h1>\n" +
        "  <button class=\"panel-toggle-btn\" id=\"btn-left\" onclick=\"togglePanel('left')\">☰ Filters</button>\n" +
        "  <div class=\"upload-zone\">\n" +
        "    <label class=\"upload-label\" id=\"drop-zone\" for=\"file-input\"><span>▲</span> Upload flow.txt</label>\n" +
        "    <input type=\"file\" id=\"file-input\" accept=\".txt,text/plain\"/>\n" +
        "    <span id=\"filename\">no file selected</span>\n" +
        "  </div>\n" +
        "  <div class=\"stats\" id=\"stats\" style=\"display:none\">\n" +
        "    <span>Pairs: <b id=\"stat-pairs\">0</b></span>\n" +
        "    <span>Corrs: <b id=\"stat-corrs\">0</b></span>\n" +
        "    <span>Values: <b id=\"stat-vals\">0</b></span>\n" +
        "  </div>\n" +
        "  <button class=\"panel-toggle-btn\" id=\"btn-right\" onclick=\"togglePanel('right')\">\n" +
        "    Correlations <b id=\"corr-badge\" style=\"color:var(--accent)\">—</b>\n" +
        "  </button>\n" +
        "</header>\n" +
        "\n" +
        "<div class=\"layout\">\n" +
        "  <div class=\"left-panel\" id=\"left-panel\">\n" +
        "    <div class=\"left-panel-header\">\n" +
        "      <span>Visibility</span>\n" +
        "      <div class=\"left-panel-actions\">\n" +
        "        <button class=\"tiny-btn\" onclick=\"setAllVisibility(true)\">all</button>\n" +
        "        <button class=\"tiny-btn\" onclick=\"setAllVisibility(false)\">none</button>\n" +
        "      </div>\n" +
        "    </div>\n" +
        "    <div class=\"left-panel-content\" id=\"vis-list\">\n" +
        "      <div class=\"empty-state\" id=\"vis-empty\" style=\"height:120px\">\n" +
        "        <div class=\"empty-text\" style=\"font-size:10px\">No values</div>\n" +
        "      </div>\n" +
        "    </div>\n" +
        "    <div class=\"export-btns\" id=\"export-btns\">\n" +
        "      <button class=\"export-btn\" onclick=\"exportCoookies()\">⬇ .coookies</button>\n" +
        "      <button class=\"export-btn\" onclick=\"exportFlow()\">⬇ .flow</button>\n" +
        "    </div>\n" +
        "  </div>\n" +
        "\n" +
        "  <div class=\"flow-area\" id=\"flow-area\">\n" +
        "    <div class=\"empty-state\" id=\"empty-state\">\n" +
        "      <div class=\"empty-icon\">⬡</div>\n" +
        "      <div class=\"empty-text\">No flow loaded</div>\n" +
        "      <div class=\"empty-sub\">Upload a flow.txt or open via the LazyFlow Burp extension</div>\n" +
        "    </div>\n" +
        "    <div class=\"loading\" id=\"loading\"><div class=\"spinner\"></div><span style=\"font-size:11px\">Analyzing…</span></div>\n" +
        "    <div class=\"error-banner\" id=\"error-banner\"></div>\n" +
        "    <div id=\"pairs-container\"></div>\n" +
        "  </div>\n" +
        "\n" +
        "  <div class=\"right-panel collapsed\" id=\"right-panel\">\n" +
        "    <div class=\"panel-header\"><span>Correlations</span></div>\n" +
        "    <div class=\"filter-bar\">\n" +
        "      <input class=\"filter-input\" id=\"filter-input\" placeholder=\"Filter by value or location…\"/>\n" +
        "    </div>\n" +
        "    <div class=\"panel-content\">\n" +
        "      <div class=\"empty-state\" id=\"corrs-empty\" style=\"height:160px\">\n" +
        "        <div class=\"empty-text\" style=\"font-size:10px\">No correlations yet</div>\n" +
        "      </div>\n" +
        "      <div id=\"corrs-list\"></div>\n" +
        "    </div>\n" +
        "  </div>\n" +
        "</div>\n" +
        "\n" +
        "<script>\n" +
        "'use strict';\n" +
        "\n" +
        "// ═══════════════════════════════════════════════════════════════════════════════\n" +
        "// PARSING & CORRELATION ENGINE  (ported from flow_analyzer.py)\n" +
        "// Zero server. Zero network. Runs entirely in the browser.\n" +
        "// ═══════════════════════════════════════════════════════════════════════════════\n" +
        "\n" +
        "const COLORS = [\n" +
        "  \"#FF6B6B\",\"#FFD93D\",\"#6BCB77\",\"#4D96FF\",\"#FF922B\",\"#CC5DE8\",\"#20C997\",\n" +
        "  \"#F06595\",\"#74C0FC\",\"#A9E34B\",\"#FF8787\",\"#FFA94D\",\"#69DB7C\",\"#4DABF7\",\n" +
        "  \"#E599F7\",\"#63E6BE\",\"#F783AC\",\"#91A7FF\",\"#C0EB75\",\"#FFD43B\",\n" +
        "];\n" +
        "\n" +
        "const MIN_LEN = 4;\n" +
        "const BORING  = new Set([\n" +
        "  \"text/html\",\"application/json\",\"keep-alive\",\"close\",\"chunked\",\"gzip\",\n" +
        "  \"no-cache\",\"no-store\",\"200 ok\",\"true\",\"false\",\"text/html; charset=utf-8\",\n" +
        "  \"application/json; charset=utf-8\",\"application/x-www-form-urlencoded\",\"0\",\"1\",\"utf-8\",\n" +
        "]);\n" +
        "const SKIP_REQ_HEADERS = new Set([\n" +
        "  \"host\",\"user-agent\",\"accept\",\"accept-language\",\"accept-encoding\",\"connection\",\n" +
        "  \"upgrade-insecure-requests\",\"priority\",\"origin\",\"content-type\",\"content-length\",\n" +
        "  \"cache-control\",\"pragma\",\"x-pwnfox-color\",\"referer\",\"sec-fetch-site\",\n" +
        "  \"sec-fetch-mode\",\"sec-fetch-dest\",\"sec-fetch-user\",\"te\",\n" +
        "]);\n" +
        "\n" +
        "// ── utilities ─────────────────────────────────────────────────────────────────\n" +
        "\n" +
        "function isInteresting(v) {\n" +
        "  v = v.trim();\n" +
        "  if (v.length < MIN_LEN) return false;\n" +
        "  if (BORING.has(v.toLowerCase())) return false;\n" +
        "  if (/^\\d+$/.test(v) && v.length < 5) return false;\n" +
        "  return true;\n" +
        "}\n" +
        "\n" +
        "function parseQS(qs) {\n" +
        "  const out = new Map();\n" +
        "  if (!qs) return out;\n" +
        "  for (const part of qs.split('&')) {\n" +
        "    const eq  = part.indexOf('=');\n" +
        "    if (eq === -1) continue;\n" +
        "    const key = safeDecodeURI(part.slice(0, eq).replace(/\\+/g,' '));\n" +
        "    const val = safeDecodeURI(part.slice(eq+1).replace(/\\+/g,' '));\n" +
        "    if (!out.has(key)) out.set(key, []);\n" +
        "    out.get(key).push(val);\n" +
        "  }\n" +
        "  return out;\n" +
        "}\n" +
        "\n" +
        "function parseQSRaw(qs) {\n" +
        "  const out = new Map();\n" +
        "  if (!qs) return out;\n" +
        "  for (const part of qs.split('&')) {\n" +
        "    const eq  = part.indexOf('=');\n" +
        "    if (eq === -1) continue;\n" +
        "    const key = safeDecodeURI(part.slice(0, eq).replace(/\\+/g,' '));\n" +
        "    const val = part.slice(eq+1);\n" +
        "    if (!out.has(key)) out.set(key, []);\n" +
        "    out.get(key).push(val);\n" +
        "  }\n" +
        "  return out;\n" +
        "}\n" +
        "\n" +
        "function pathSegments(raw) {\n" +
        "  if (!raw) return [];\n" +
        "  let p = raw;\n" +
        "  const schemeIdx = p.indexOf('://');\n" +
        "  if (schemeIdx !== -1) {\n" +
        "    const afterHost = p.indexOf('/', schemeIdx + 3);\n" +
        "    p = afterHost === -1 ? '' : p.slice(afterHost);\n" +
        "  }\n" +
        "  p = p.split('?')[0].split('#')[0];\n" +
        "  return p.split('/').filter(Boolean).map(safeDecodeURI);\n" +
        "}\n" +
        "\n" +
        "function safeDecodeURI(s) { try { return decodeURIComponent(s); } catch { return s; } }\n" +
        "\n" +
        "function flattenJSON(obj, prefix) {\n" +
        "  prefix = prefix || '';\n" +
        "  if (obj === null || typeof obj !== 'object') return [[prefix, String(obj)]];\n" +
        "  const out = [];\n" +
        "  if (Array.isArray(obj)) {\n" +
        "    obj.forEach((v,i) => out.push(...flattenJSON(v, prefix ? `${prefix}[${i}]` : `[${i}]`)));\n" +
        "  } else {\n" +
        "    for (const [k,v] of Object.entries(obj))\n" +
        "      out.push(...flattenJSON(v, prefix ? `${prefix}.${k}` : k));\n" +
        "  }\n" +
        "  return out;\n" +
        "}\n" +
        "\n" +
        "// ── HTTP parsing ──────────────────────────────────────────────────────────────\n" +
        "\n" +
        "function parseRawHTTP(raw, index, kind) {\n" +
        "  const msg = { index, kind, raw, method:null, path:null, status:null, headers: new Map(), body:'' };\n" +
        "  const lines = raw.replace(/\\r\\n/g,'\\n').replace(/\\r/g,'\\n').split('\\n');\n" +
        "  if (!lines.length) return msg;\n" +
        "\n" +
        "  const parts0 = lines[0].split(' ');\n" +
        "  if (kind === 'request') {\n" +
        "    msg.method = parts0[0] || null;\n" +
        "    msg.path   = parts0[1] || null;\n" +
        "  } else {\n" +
        "    const code = parseInt(parts0[1]);\n" +
        "    msg.status = isNaN(code) ? null : code;\n" +
        "  }\n" +
        "\n" +
        "  let i = 1;\n" +
        "  while (i < lines.length && lines[i].trim() !== '') {\n" +
        "    const colon = lines[i].indexOf(':');\n" +
        "    if (colon !== -1) {\n" +
        "      const k = lines[i].slice(0, colon).trim().toLowerCase();\n" +
        "      const v = lines[i].slice(colon+1).trim();\n" +
        "      if (msg.headers.has(k)) {\n" +
        "        const ex = msg.headers.get(k);\n" +
        "        Array.isArray(ex) ? ex.push(v) : msg.headers.set(k, [ex, v]);\n" +
        "      } else msg.headers.set(k, v);\n" +
        "    }\n" +
        "    i++;\n" +
        "  }\n" +
        "  msg.body = lines.slice(i+1).join('\\n').trim();\n" +
        "  return msg;\n" +
        "}\n" +
        "\n" +
        "function loadPairsFromText(content) {\n" +
        "  content = content.replace(/\\r\\n/g,'\\n').replace(/\\r/g,'\\n');\n" +
        "  const parts = content.split(/^===(REQUEST|RESPONSE)===$/m).slice(1);\n" +
        "  if (parts.length % 4 !== 0)\n" +
        "    throw new Error(`Malformed input: expected REQUEST+RESPONSE pairs, got ${parts.length} sections`);\n" +
        "  const pairs = [];\n" +
        "  for (let i = 0; i < parts.length; i += 4) {\n" +
        "    if (parts[i] !== 'REQUEST' || parts[i+2] !== 'RESPONSE')\n" +
        "      throw new Error(`Expected REQUEST then RESPONSE at pair ${i/4}`);\n" +
        "    pairs.push({ request: parts[i+1].trim(), response: parts[i+3].trim() });\n" +
        "  }\n" +
        "  return pairs;\n" +
        "}\n" +
        "\n" +
        "// ── extraction from response ──────────────────────────────────────────────────\n" +
        "\n" +
        "function extractFromResponse(res) {\n" +
        "  const extracted = [], seen = new Set();\n" +
        "  function add(value, location) {\n" +
        "    for (const v of new Set([value, safeDecodeURI(value)])) {\n" +
        "      const vt = v.trim();\n" +
        "      if (isInteresting(vt) && !seen.has(vt)) {\n" +
        "        seen.add(vt);\n" +
        "        extracted.push({ value:vt, source_index:res.index, source_location:location });\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "\n" +
        "  for (const [k, v] of res.headers) {\n" +
        "    const vals = Array.isArray(v) ? v : [v];\n" +
        "    for (const hval of vals) {\n" +
        "      if (k === 'set-cookie') {\n" +
        "        for (const part of hval.split(';')) {\n" +
        "          const eq = part.indexOf('=');\n" +
        "          if (eq !== -1) add(part.slice(eq+1).trim(), `header:set-cookie:${part.slice(0,eq).trim()}`);\n" +
        "        }\n" +
        "        add(hval, 'header:set-cookie');\n" +
        "      } else {\n" +
        "        add(hval, `header:${k}`);\n" +
        "        if (hval.includes('?')) {\n" +
        "          try {\n" +
        "            const qs = hval.slice(hval.indexOf('?')+1);\n" +
        "            for (const [p, pv] of parseQS(qs)) for (const pval of pv) add(pval, `header:${k}:queryparam:${p}`);\n" +
        "            for (const [p, pv] of parseQSRaw(qs)) for (const pval of pv) add(pval, `header:${k}:queryparam:${p}`);\n" +
        "          } catch {}\n" +
        "        }\n" +
        "        if (hval.startsWith('/') || hval.includes('://')) {\n" +
        "          pathSegments(hval).forEach((seg, i) => add(seg, `header:${k}:pathsegment:${i}`));\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "\n" +
        "  if (!res.body) return extracted;\n" +
        "\n" +
        "  try {\n" +
        "    const data = JSON.parse(res.body);\n" +
        "    for (const [loc, val] of flattenJSON(data)) add(val, `body:json:${loc}`);\n" +
        "    return extracted;\n" +
        "  } catch {}\n" +
        "\n" +
        "  try {\n" +
        "    const doc = new DOMParser().parseFromString(res.body, 'text/html');\n" +
        "    for (const el of doc.querySelectorAll('input[type=\"hidden\"]'))\n" +
        "      add(el.getAttribute('value')||'', `body:html:hidden_input:${el.getAttribute('name')||'?'}`);\n" +
        "    for (const el of doc.querySelectorAll('meta'))\n" +
        "      add(el.getAttribute('content')||'', `body:html:meta:${el.getAttribute('name')||el.getAttribute('property')||'?'}`);\n" +
        "    for (const el of doc.querySelectorAll('script')) {\n" +
        "      const src = el.textContent||'';\n" +
        "      for (const m of src.matchAll(/(\\w+)\\s*=\\s*[\"']([^\"']{4,})[\"']/g))\n" +
        "        add(m[2], `body:html:script:${m[1]}`);\n" +
        "    }\n" +
        "  } catch {}\n" +
        "\n" +
        "  try { for (const [k, pv] of parseQS(res.body)) for (const v of pv) add(v, `body:form:${k}`); } catch {}\n" +
        "  try { for (const [k, pv] of parseQSRaw(res.body)) for (const v of pv) add(v, `body:form:${k}`); } catch {}\n" +
        "\n" +
        "  for (const line of res.body.split('\\n')) {\n" +
        "    const lt = line.trim();\n" +
        "    if (isInteresting(lt) && /^[\\w\\-\\.=+/]+$/.test(lt)) add(lt, 'body:raw_line');\n" +
        "  }\n" +
        "  return extracted;\n" +
        "}\n" +
        "\n" +
        "// ── search in request ─────────────────────────────────────────────────────────\n" +
        "\n" +
        "function searchInRequest(req, value) {\n" +
        "  const v = value.trim(), locs = [];\n" +
        "\n" +
        "  for (const [k, hv] of req.headers) {\n" +
        "    const vals = Array.isArray(hv) ? hv : [hv];\n" +
        "    if (vals.some(h => h.includes(v))) locs.push(`header:${k}`);\n" +
        "  }\n" +
        "\n" +
        "  if (req.path) {\n" +
        "    if (req.path.includes('?')) {\n" +
        "      const qs = req.path.slice(req.path.indexOf('?')+1);\n" +
        "      if (qs.includes(v)) {\n" +
        "        let matched = false;\n" +
        "        for (const [p, pv] of parseQS(qs)) {\n" +
        "          if (pv.some(pval => pval.includes(v))) { locs.push(`url_param:${p}`); matched = true; }\n" +
        "        }\n" +
        "        if (!matched) locs.push('url_query_raw');\n" +
        "      }\n" +
        "    }\n" +
        "    const pathOnly = req.path.split('?')[0];\n" +
        "    if (pathOnly.includes(v)) {\n" +
        "      const segs = pathSegments(pathOnly);\n" +
        "      const segIdx = segs.findIndex(s => s.includes(v));\n" +
        "      locs.push(segIdx !== -1 ? `url_path_segment:${segIdx}` : 'url_path_raw');\n" +
        "    }\n" +
        "  }\n" +
        "\n" +
        "  if (req.body && req.body.includes(v)) {\n" +
        "    try { for (const [loc, val] of flattenJSON(JSON.parse(req.body))) if (String(val).includes(v)) locs.push(`body:json:${loc}`); } catch {}\n" +
        "    try { for (const [p, pv] of parseQS(req.body)) if (pv.some(pval => pval.includes(v))) locs.push(`body:form:${p}`); } catch {}\n" +
        "    if (!locs.some(l => l.startsWith('body:'))) locs.push('body:raw');\n" +
        "  }\n" +
        "  return locs;\n" +
        "}\n" +
        "\n" +
        "// ── extract from request (reverse pass) ──────────────────────────────────────\n" +
        "\n" +
        "function extractFromRequest(req) {\n" +
        "  const results = [];\n" +
        "  for (const [k, hv] of req.headers) {\n" +
        "    if (SKIP_REQ_HEADERS.has(k)) continue;\n" +
        "    const vals = Array.isArray(hv) ? hv : [hv];\n" +
        "    for (const hval of vals) {\n" +
        "      if (k === 'cookie') {\n" +
        "        for (const part of hval.split(';')) {\n" +
        "          const eq = part.indexOf('=');\n" +
        "          if (eq !== -1) { const val = part.slice(eq+1).trim(); if (isInteresting(val)) results.push([val, `header:cookie:${part.slice(0,eq).trim()}`]); }\n" +
        "        }\n" +
        "      } else if (isInteresting(hval)) results.push([hval, `header:${k}`]);\n" +
        "    }\n" +
        "  }\n" +
        "  if (req.path && req.path.includes('?')) {\n" +
        "    const qs = req.path.slice(req.path.indexOf('?')+1);\n" +
        "    for (const [p, pv] of parseQS(qs)) for (const v of pv) if (isInteresting(v)) results.push([v, `url_param:${p}`]);\n" +
        "    for (const [p, pv] of parseQSRaw(qs)) for (const v of pv) if (isInteresting(v)) results.push([v, `url_param:${p}`]);\n" +
        "  }\n" +
        "  if (req.path) {\n" +
        "    const pathOnly = req.path.split('?')[0];\n" +
        "    pathSegments(pathOnly).forEach((seg, i) => { if (isInteresting(seg)) results.push([seg, `url_path_segment:${i}`]); });\n" +
        "  }\n" +
        "  if (req.body) {\n" +
        "    try { for (const [loc, val] of flattenJSON(JSON.parse(req.body))) if (isInteresting(String(val))) results.push([String(val), `body:json:${loc}`]); } catch {}\n" +
        "    try { for (const [p, pv] of parseQS(req.body)) for (const v of pv) if (isInteresting(v)) results.push([v, `body:form:${p}`]); } catch {}\n" +
        "    try { for (const [p, pv] of parseQSRaw(req.body)) for (const v of pv) if (isInteresting(v)) results.push([v, `body:form:${p}`]); } catch {}\n" +
        "  }\n" +
        "  return results;\n" +
        "}\n" +
        "\n" +
        "// ── search in response (reverse pass) ────────────────────────────────────────\n" +
        "\n" +
        "function searchInResponse(res, value) {\n" +
        "  const v = value.trim(), locs = [];\n" +
        "  for (const [k, hv] of res.headers) {\n" +
        "    const vals = Array.isArray(hv) ? hv : [hv];\n" +
        "    if (vals.some(h => h.includes(v))) locs.push(`header:${k}`);\n" +
        "  }\n" +
        "  if (res.body && res.body.includes(v)) {\n" +
        "    try { for (const [loc, val] of flattenJSON(JSON.parse(res.body))) if (String(val).includes(v)) locs.push(`body:json:${loc}`); } catch {}\n" +
        "    if (!locs.some(l => l.startsWith('body:'))) locs.push('body:raw');\n" +
        "  }\n" +
        "  return locs;\n" +
        "}\n" +
        "\n" +
        "// ── correlate ─────────────────────────────────────────────────────────────────\n" +
        "\n" +
        "function correlate(pairs) {\n" +
        "  const allExtracted = [];\n" +
        "  for (const [, res] of pairs) allExtracted.push(...extractFromResponse(res));\n" +
        "\n" +
        "  const correlations = [], alreadyCorr = new Set();\n" +
        "\n" +
        "  // forward pass\n" +
        "  for (const ev of allExtracted) {\n" +
        "    for (const [req] of pairs) {\n" +
        "      if (req.index <= ev.source_index) continue;\n" +
        "      for (const loc of searchInRequest(req, ev.value)) {\n" +
        "        correlations.push({ extracted:ev, used_in_request_index:req.index, used_at_location:loc });\n" +
        "        alreadyCorr.add(ev.value);\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "\n" +
        "  // reverse pass\n" +
        "  for (const [req] of pairs) {\n" +
        "    for (const [val, reqLoc] of extractFromRequest(req)) {\n" +
        "      if (alreadyCorr.has(val)) continue;\n" +
        "      for (const [, res] of pairs) {\n" +
        "        if (res.index > req.index) continue;\n" +
        "        for (const resLoc of searchInResponse(res, val)) {\n" +
        "          correlations.push({ extracted:{ value:val, source_index:res.index, source_location:resLoc }, used_in_request_index:req.index, used_at_location:reqLoc });\n" +
        "          alreadyCorr.add(val);\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "  return { correlations, allExtracted };\n" +
        "}\n" +
        "\n" +
        "// ── color assignment ──────────────────────────────────────────────────────────\n" +
        "\n" +
        "function assignColors(correlations) {\n" +
        "  const corrVals = new Set(correlations.map(c => c.extracted.value));\n" +
        "  // Drop v only when the contained value o is a SUBSTANTIAL chunk of it —\n" +
        "  // not just a short, coincidental substring (e.g. the word \"code\" showing up\n" +
        "  // inside \"slashid-code:AbC123...\"). Otherwise a spurious short correlated\n" +
        "  // value can wipe out a real, unrelated, much longer one.\n" +
        "  const MIN_CONTAINED_LEN = 8, MIN_CONTAINED_RATIO = 0.5;\n" +
        "  const clean = [...corrVals].filter(v => ![...corrVals].some(o =>\n" +
        "    o !== v && v.includes(o) && o.length >= MIN_CONTAINED_LEN && o.length >= v.length * MIN_CONTAINED_RATIO\n" +
        "  ));\n" +
        "  const colorMap = new Map();\n" +
        "  [...clean].sort().forEach((v,i) => colorMap.set(v, COLORS[i % COLORS.length]));\n" +
        "  return colorMap;\n" +
        "}\n" +
        "\n" +
        "// ── highlight ─────────────────────────────────────────────────────────────────\n" +
        "\n" +
        "function escHtmlRaw(s) {\n" +
        "  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\"/g,'&quot;');\n" +
        "}\n" +
        "\n" +
        "function highlightText(raw, colorMap, suppress) {\n" +
        "  suppress = suppress || null;\n" +
        "  let segments = [escHtmlRaw(raw)];\n" +
        "  const allEsc = [...colorMap.keys()].map(v => escHtmlRaw(v));\n" +
        "  function isSuperstring(val) {\n" +
        "    const ev = escHtmlRaw(val);\n" +
        "    const MIN_CONTAINED_LEN = 8, MIN_CONTAINED_RATIO = 0.5;\n" +
        "    return allEsc.some(o => o !== ev && ev.includes(o) && o.length >= MIN_CONTAINED_LEN && o.length >= ev.length * MIN_CONTAINED_RATIO);\n" +
        "  }\n" +
        "\n" +
        "  for (const val of [...colorMap.keys()].sort((a,b) => b.length - a.length)) {\n" +
        "    if (isSuperstring(val)) continue;\n" +
        "    if (suppress && suppress.has(val)) continue;\n" +
        "    const color = colorMap.get(val), esc = escHtmlRaw(val);\n" +
        "    if (!esc) continue;\n" +
        "    const open  = `<mark class=\"corr-mark\" style=\"background:${color}22;color:${color};border:1px solid ${color}88;border-radius:3px;padding:0 2px;\" data-value=\"${esc}\">`;\n" +
        "    const close = '</mark>';\n" +
        "    const next = [];\n" +
        "    for (const seg of segments) {\n" +
        "      if (seg.startsWith('<') || !seg.includes(esc)) { next.push(seg); continue; }\n" +
        "      const parts = seg.split(esc);\n" +
        "      parts.forEach((p,i) => { next.push(p); if (i < parts.length-1) next.push(open, esc, close); });\n" +
        "    }\n" +
        "    segments = next;\n" +
        "  }\n" +
        "  return segments.join('');\n" +
        "}\n" +
        "\n" +
        "// ── main analyze ──────────────────────────────────────────────────────────────\n" +
        "\n" +
        "function analyzeFlowText(content) {\n" +
        "  const rawPairs = loadPairsFromText(content);\n" +
        "  const pairs    = rawPairs.map((p, idx) => [\n" +
        "    parseRawHTTP(p.request,  idx, 'request'),\n" +
        "    parseRawHTTP(p.response, idx, 'response'),\n" +
        "  ]);\n" +
        "\n" +
        "  const { correlations } = correlate(pairs);\n" +
        "  const colorMap = assignColors(correlations);\n" +
        "\n" +
        "  // earliest response occurrence per value — this is the true \"extraction point\".\n" +
        "  // Later responses that happen to contain the same value are NOT a new\n" +
        "  // extraction, just a coincidental repeat, so we don't highlight them there.\n" +
        "  // Requests are unaffected: every usage of a correlated value in a request\n" +
        "  // is still meaningful and stays highlighted.\n" +
        "  const siMap = new Map(), slMap = new Map();\n" +
        "  for (const c of correlations) {\n" +
        "    const v = c.extracted.value; if (!colorMap.has(v)) continue;\n" +
        "    const si = c.extracted.source_index;\n" +
        "    if (!siMap.has(v) || si < siMap.get(v)) { siMap.set(v, si); slMap.set(v, c.extracted.source_location); }\n" +
        "  }\n" +
        "\n" +
        "  const pairData = pairs.map(([req, res]) => {\n" +
        "    const responseSuppress = new Set();\n" +
        "    for (const v of colorMap.keys()) {\n" +
        "      const earliest = siMap.get(v);\n" +
        "      if (earliest !== undefined && res.index !== earliest) responseSuppress.add(v);\n" +
        "    }\n" +
        "    return {\n" +
        "      index:            req.index,\n" +
        "      request_raw:      req.raw.replace(/\\r\\n/g,'\\n').replace(/\\r/g,'\\n').replace(/\\n/g,'\\r\\n'),\n" +
        "      response_raw:     res.raw,\n" +
        "      request_hl:       highlightText(req.raw, colorMap),\n" +
        "      response_hl:      highlightText(res.raw, colorMap, responseSuppress),\n" +
        "      request_summary:  req.method ? `${req.method} ${req.path}` : req.raw.split('\\n')[0],\n" +
        "      response_summary: res.status ? String(res.status) : res.raw.split('\\n')[0],\n" +
        "    };\n" +
        "  });\n" +
        "\n" +
        "  const groups = new Map();\n" +
        "  for (const c of correlations) {\n" +
        "    const key = `${c.extracted.source_index}__${c.used_in_request_index}`;\n" +
        "    if (!groups.has(key)) groups.set(key, []);\n" +
        "    groups.get(key).push(c);\n" +
        "  }\n" +
        "  const corrData = [];\n" +
        "  for (const [, corrs] of [...groups.entries()].sort()) {\n" +
        "    for (const c of corrs) {\n" +
        "      if (!colorMap.has(c.extracted.value)) continue;\n" +
        "      corrData.push({ value:c.extracted.value, color:colorMap.get(c.extracted.value), src_index:c.extracted.source_index, src_location:c.extracted.source_location, dst_index:c.used_in_request_index, dst_location:c.used_at_location });\n" +
        "    }\n" +
        "  }\n" +
        "\n" +
        "  const legend = [...colorMap.entries()].map(([v, color]) => ({\n" +
        "    value:v, color, source_index:siMap.get(v)??0, source_location:slMap.get(v)??''\n" +
        "  }));\n" +
        "\n" +
        "  return { pairs:pairData, correlations:corrData, legend, pair_count:pairs.length, corr_count:correlations.length };\n" +
        "}\n" +
        "\n" +
        "// ═══════════════════════════════════════════════════════════════════════════════\n" +
        "// UI\n" +
        "// ═══════════════════════════════════════════════════════════════════════════════\n" +
        "\n" +
        "let activeValue=null, activeEntry=null, hiddenValues=new Set();\n" +
        "\n" +
        "function togglePanel(side) {\n" +
        "  const panel=document.getElementById(side+'-panel'), btn=document.getElementById('btn-'+side);\n" +
        "  panel.classList.toggle('collapsed');\n" +
        "  btn.classList.toggle('active', !panel.classList.contains('collapsed'));\n" +
        "}\n" +
        "\n" +
        "const fileInput=document.getElementById('file-input'), dropZone=document.getElementById('drop-zone'), fnLabel=document.getElementById('filename');\n" +
        "fileInput.addEventListener('change', e => { const f=e.target.files[0]; if(f) processFile(f); });\n" +
        "dropZone.addEventListener('dragover',  e => { e.preventDefault(); dropZone.classList.add('dragover'); });\n" +
        "dropZone.addEventListener('dragleave', () => dropZone.classList.remove('dragover'));\n" +
        "dropZone.addEventListener('drop', e => { e.preventDefault(); dropZone.classList.remove('dragover'); const f=e.dataTransfer.files[0]; if(f){fileInput.files=e.dataTransfer.files;processFile(f);} });\n" +
        "\n" +
        "function processFile(file) {\n" +
        "  fnLabel.textContent=file.name; showLoading(true); hideError(); clearResults();\n" +
        "  const r=new FileReader();\n" +
        "  r.onload=e => { try { showLoading(false); renderAll(analyzeFlowText(e.target.result)); } catch(err) { showLoading(false); showError(err.message); } };\n" +
        "  r.readAsText(file);\n" +
        "}\n" +
        "\n" +
        "function processFlowText(text, label) {\n" +
        "  fnLabel.textContent=label||'flow.txt'; showLoading(true); hideError(); clearResults();\n" +
        "  setTimeout(() => { try { showLoading(false); renderAll(analyzeFlowText(text)); } catch(err) { showLoading(false); showError(err.message); } }, 20);\n" +
        "}\n" +
        "\n" +
        "function renderAll(data) {\n" +
        "  document.getElementById('stats').style.display='flex';\n" +
        "  document.getElementById('stat-pairs').textContent=data.pair_count;\n" +
        "  document.getElementById('stat-corrs').textContent=data.corr_count;\n" +
        "  document.getElementById('stat-vals').textContent=data.legend.length;\n" +
        "  document.getElementById('corr-badge').textContent=data.corr_count;\n" +
        "  document.getElementById('empty-state').style.display='none';\n" +
        "  document.getElementById('export-btns').style.display='flex';\n" +
        "  window.flowData=data;\n" +
        "  renderPairs(data.pairs); renderCorrelations(data.correlations); renderVisibility(data.legend);\n" +
        "}\n" +
        "\n" +
        "function statusClass(s) { const m=s&&s.match(/^(\\d)/); return m?({2:'s2xx',3:'s3xx',4:'s4xx',5:'s5xx'}[m[1]]||''):''; }\n" +
        "\n" +
        "function renderPairs(pairs) {\n" +
        "  const c=document.getElementById('pairs-container'); c.innerHTML='';\n" +
        "  pairs.forEach(p => {\n" +
        "    const card=document.createElement('div');\n" +
        "    card.className='pair-card'; card.id=`pair-${p.index}`;\n" +
        "    card.innerHTML=`<div class=\"pair-header\"><span class=\"pair-index\">PAIR ${p.index}</span><span class=\"pair-summary-req\">${escHtml(p.request_summary)}</span><span class=\"arrow\">→</span><span class=\"status-badge ${statusClass(p.response_summary)}\">${escHtml(p.response_summary)}</span></div><div class=\"pair-body\"><div class=\"http-block\"><div class=\"http-block-header req-header-bar\"><span>▶</span> REQUEST</div><pre class=\"http-raw\">${p.request_hl}</pre></div><div class=\"http-block\"><div class=\"http-block-header res-header-bar\"><span>◀</span> RESPONSE</div><pre class=\"http-raw\">${p.response_hl}</pre></div></div>`;\n" +
        "    c.appendChild(card);\n" +
        "  });\n" +
        "  c.querySelectorAll('mark.corr-mark').forEach(el => el.addEventListener('click', e => { e.stopPropagation(); const dv=el.getAttribute('data-value'); if(!hiddenValues.has(dv)) focusValue(dv); }));\n" +
        "}\n" +
        "\n" +
        "function renderCorrelations(corrs) {\n" +
        "  const list=document.getElementById('corrs-list'), empty=document.getElementById('corrs-empty');\n" +
        "  list.innerHTML=''; if(!corrs.length){empty.style.display='flex';return;} empty.style.display='none';\n" +
        "  corrs.forEach(c => {\n" +
        "    const ev=escapeHtmlEntities(c.value), el=document.createElement('div');\n" +
        "    el.className='corr-entry'; el.dataset.value=c.value; el.dataset.escaped=ev;\n" +
        "    el.innerHTML=`<div class=\"corr-value-bar\"><div class=\"corr-dot\" style=\"background:${c.color}\"></div><span class=\"corr-value-text\" title=\"${escAttr(c.value)}\">${escHtml(truncate(c.value,38))}</span></div><div class=\"corr-flow\"><div class=\"corr-flow-row\"><span class=\"corr-flow-label lbl-from\">FROM</span><span class=\"corr-flow-loc\">RESPONSE_${c.src_index} · ${escHtml(c.src_location)}</span></div><div class=\"corr-flow-row\"><span class=\"corr-flow-label lbl-to\">TO</span><span class=\"corr-flow-loc\">REQUEST_${c.dst_index} · ${escHtml(c.dst_location)}</span></div></div>`;\n" +
        "    el.addEventListener('click', () => { if(!hiddenValues.has(ev)) focusValue(ev,el); });\n" +
        "    list.appendChild(el);\n" +
        "  });\n" +
        "}\n" +
        "\n" +
        "function renderVisibility(legend) {\n" +
        "  const list=document.getElementById('vis-list'), empty=document.getElementById('vis-empty');\n" +
        "  list.querySelectorAll('.vis-row').forEach(e=>e.remove());\n" +
        "  if(!legend.length){empty.style.display='flex';return;} empty.style.display='none';\n" +
        "  legend.forEach(l => {\n" +
        "    const ev=escapeHtmlEntities(l.value), row=document.createElement('div');\n" +
        "    row.className='vis-row'; row.dataset.escaped=ev;\n" +
        "    row.innerHTML=`<div class=\"vis-check checked\" style=\"background:${l.color};border-color:${l.color}\">✓</div><div class=\"vis-swatch\" style=\"background:${l.color}\"></div><span class=\"vis-label\" title=\"${escAttr(l.value)}\">${escHtml(truncate(l.value,22))}</span>`;\n" +
        "    row.addEventListener('click', () => toggleVisibility(ev,row));\n" +
        "    list.appendChild(row);\n" +
        "  });\n" +
        "}\n" +
        "\n" +
        "function marksByValue(ev) { return Array.from(document.querySelectorAll('mark.corr-mark')).filter(m=>m.getAttribute('data-value')===ev); }\n" +
        "\n" +
        "function toggleVisibility(ev, row) {\n" +
        "  const check=row.querySelector('.vis-check');\n" +
        "  if(hiddenValues.has(ev)){hiddenValues.delete(ev);check.classList.replace('unchecked','checked');check.textContent='✓';row.classList.remove('hidden-val');marksByValue(ev).forEach(m=>m.classList.remove('val-hidden'));}\n" +
        "  else{hiddenValues.add(ev);check.classList.replace('checked','unchecked');check.textContent='';row.classList.add('hidden-val');marksByValue(ev).forEach(m=>m.classList.add('val-hidden'));if(activeValue===ev)clearFocus();}\n" +
        "}\n" +
        "\n" +
        "function setAllVisibility(visible) {\n" +
        "  document.querySelectorAll('.vis-row').forEach(row => {\n" +
        "    const ev=row.dataset.escaped;\n" +
        "    if(visible&&hiddenValues.has(ev)) toggleVisibility(ev,row);\n" +
        "    if(!visible&&!hiddenValues.has(ev)) toggleVisibility(ev,row);\n" +
        "  });\n" +
        "}\n" +
        "\n" +
        "function escapeHtmlEntities(s) { return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\"/g,'&quot;').replace(/'/g,'&#x27;'); }\n" +
        "\n" +
        "function focusValue(ev, entryEl) {\n" +
        "  if(activeValue===ev){clearFocus();return;} activeValue=ev;\n" +
        "  const all=Array.from(document.querySelectorAll('mark.corr-mark')), matching=all.filter(m=>m.getAttribute('data-value')===ev);\n" +
        "  all.forEach(m=>{if(m.classList.contains('val-hidden'))return;m.classList.toggle('dimmed',m.getAttribute('data-value')!==ev);});\n" +
        "  document.querySelectorAll('.pair-card').forEach(card=>card.classList.toggle('highlighted',matching.some(m=>card.contains(m))));\n" +
        "  if(matching.length) matching[0].scrollIntoView({behavior:'smooth',block:'center'});\n" +
        "  document.querySelectorAll('.corr-entry').forEach(el=>el.classList.toggle('active-entry',el.dataset.escaped===ev));\n" +
        "  if(entryEl){if(activeEntry)activeEntry.classList.remove('active-entry');activeEntry=entryEl;entryEl.scrollIntoView({behavior:'smooth',block:'nearest'});}\n" +
        "}\n" +
        "\n" +
        "function clearFocus() {\n" +
        "  activeValue=null;\n" +
        "  document.querySelectorAll('mark.corr-mark').forEach(m=>m.classList.remove('dimmed'));\n" +
        "  document.querySelectorAll('.pair-card').forEach(c=>c.classList.remove('highlighted'));\n" +
        "  document.querySelectorAll('.corr-entry').forEach(e=>e.classList.remove('active-entry'));\n" +
        "}\n" +
        "\n" +
        "document.getElementById('flow-area').addEventListener('click', e => { if(!e.target.closest('mark.corr-mark')) clearFocus(); });\n" +
        "document.getElementById('filter-input').addEventListener('input', function() { const q=this.value.toLowerCase(); document.querySelectorAll('.corr-entry').forEach(el=>{el.style.display=el.textContent.toLowerCase().includes(q)?'':'none';}); });\n" +
        "\n" +
        "function showLoading(on){document.getElementById('loading').style.display=on?'flex':'none';if(on)document.getElementById('empty-state').style.display='none';}\n" +
        "function showError(msg){const b=document.getElementById('error-banner');b.textContent='✖ '+msg;b.style.display='block';document.getElementById('empty-state').style.display='none';}\n" +
        "function hideError(){document.getElementById('error-banner').style.display='none';}\n" +
        "function clearResults(){\n" +
        "  document.getElementById('pairs-container').innerHTML='';\n" +
        "  document.getElementById('corrs-list').innerHTML='';\n" +
        "  document.getElementById('vis-list').querySelectorAll('.vis-row').forEach(e=>e.remove());\n" +
        "  document.getElementById('stats').style.display='none';\n" +
        "  document.getElementById('corrs-empty').style.display='flex';\n" +
        "  document.getElementById('vis-empty').style.display='flex';\n" +
        "  document.getElementById('corr-badge').textContent='—';\n" +
        "  document.getElementById('export-btns').style.display='none';\n" +
        "  activeValue=null;activeEntry=null;hiddenValues.clear();\n" +
        "}\n" +
        "\n" +
        "// ── export ────────────────────────────────────────────────────────────────────\n" +
        "\n" +
        "function buildRegex(rawText, value) {\n" +
        "  const occ=[]; let from=0;\n" +
        "  while(true){const i=rawText.indexOf(value,from);if(i===-1)break;occ.push(i);from=i+1;}\n" +
        "  if(!occ.length) return null;\n" +
        "  const SOFT=new Set(['\\n','\\r','(', ',','<','?','&']);\n" +
        "  function bfi(idx){\n" +
        "    const ai=idx+value.length; let tc=ai<rawText.length?rawText[ai]:'\\n';\n" +
        "    if(!tc||/\\w/.test(tc)) tc='\\n';\n" +
        "    let ps=Math.max(0,idx-60);\n" +
        "    for(let i=idx-1;i>=ps;i--){if(SOFT.has(rawText[i])){ps=i+1;break;}}\n" +
        "    let pf=rawText.slice(ps,idx);\n" +
        "    const lc=pf.slice(-1);\n" +
        "    if((lc==='\"'||lc===\"'\")&&tc===lc){const ex=Math.max(0,ps-60);for(let i=ps-1;i>=ex;i--){if(SOFT.has(rawText[i])){ps=i+1;break;}}pf=rawText.slice(ps,idx);}\n" +
        "    return{prefix:pf,termChar:tc};\n" +
        "  }\n" +
        "  function score(i){const{prefix}=bfi(i);let s=prefix.length;if(prefix.slice(-1)==='=')s-=1000;return s;}\n" +
        "  const bi=occ.reduce((b,i)=>score(i)<score(b)?i:b,occ[0]);\n" +
        "  const{prefix,termChar}=bfi(bi);\n" +
        "  function re(s){return s.replace(/[.*+?^$()|[\\]\\\\]/g,'\\\\$&');}\n" +
        "  function te(c){if(c==='\\n')return'\\\\n';if(c==='\\r')return'\\\\r';if(c===']')return'\\\\]';if(c==='\\\\')return'\\\\\\\\';if(c==='^')return'\\\\^';return c;}\n" +
        "  // End-of-line/end-of-text terminator: exclude \\r from the captured value and\n" +
        "  // match a real CRLF (or a bare LF, or literal end of text) rather than just '\\n',\n" +
        "  // so the value isn't polluted with a trailing \\r on real \\r\\n-terminated raw HTTP.\n" +
        "  if(termChar==='\\n'){\n" +
        "    return`${re(prefix)}([^\\r]+)\\r`;\n" +
        "  }\n" +
        "  return`${re(prefix)}([^${te(termChar)}]+)${re(termChar)}`;\n" +
        "}\n" +
        "\n" +
        "function exportCoookies(){exportAs('flow.coookies');}\n" +
        "function exportFlow(){exportAs('flow.flow');}\n" +
        "\n" +
        "function exportAs(filename){\n" +
        "  const data=window.flowData;if(!data)return;\n" +
        "  const v2n={};let cnt=0;\n" +
        "  data.legend.forEach(l=>{const ev=escapeHtmlEntities(l.value);if(hiddenValues.has(ev))return;v2n[l.value]={name:`val_${cnt++}`,source_index:l.source_index,source_location:l.source_location||''};});\n" +
        "  function j2b(loc){return loc.replace(/^body:json:/,'').split('.').map(s=>`['${s}']`).join('');}\n" +
        "  const steps=data.pairs.map(p=>{\n" +
        "    const extr=[],seen=new Set();\n" +
        "    Object.entries(v2n).forEach(([val,info])=>{\n" +
        "      if((info.source_index??-1)!==p.index||seen.has(val))return;\n" +
        "      seen.add(val);\n" +
        "      if(info.source_location.startsWith('body:json:')) extr.push({name:info.name,type:1,value:j2b(info.source_location)});\n" +
        "      else{const rx=buildRegex(p.response_raw,val);if(rx)extr.push({name:info.name,type:2,value:rx});}\n" +
        "    });\n" +
        "    const prefix=filename.endsWith('.flow')?'FLOW':'COOOKIES';\n" +
        "    let rr=p.request_raw;\n" +
        "    Object.entries(v2n).sort((a,b)=>b[0].length-a[0].length).forEach(([val,info])=>{if(info.source_index>=p.index)return;rr=rr.split(val).join(`<${prefix}:${info.name}>`);});\n" +
        "    // Ensure blank line after headers when there is no body\n" +
        "    if(!rr.includes('\\r\\n\\r\\n')) rr=rr.trimEnd()+'\\r\\n\\r\\n';\n" +
        "    return{name:`step${p.index+1}`,rawRequest:rr,authExtraction:null,extractions:extr};\n" +
        "  });\n" +
        "  const out={flowName:'LazyFlow',steps};\n" +
        "  const blob=new Blob([JSON.stringify(out,null,2)],{type:'application/json'});\n" +
        "  const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=filename;a.click();URL.revokeObjectURL(url);\n" +
        "}\n" +
        "\n" +
        "function escHtml(s){return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\"/g,'&quot;');}\n" +
        "function escAttr(s){return escHtml(s);}\n" +
        "function truncate(s,n){return s.length>n?s.slice(0,n)+'…':s;}\n" +
        "\n" +
        "// ── auto-load: Java ext injects flow data by replacing the marker below ───────\n" +
        "// The ext reads this HTML, replaces __FLOW_DATA__ with the JSON-escaped flow.txt\n" +
        "// content, writes it to a temp file, and opens it in the browser.\n" +
        "// When opened manually (no injection), the marker stays and we skip.\n" +
        "(function(){\n" +
        "  const raw = `__FLOW_DATA__`;\n" +
        "  if(raw==='__FLOW_DATA__'||!raw.trim()) return;\n" +
        "  processFlowText(raw,'flow.txt (from Burp)');\n" +
        "})();\n" +
        "</script>\n" +
        "</body>\n" +
        "</html>\n" +
        "";
    
    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("LazyFlow");

        api.userInterface().registerContextMenuItemsProvider(new FlowContextMenu());

        HotKey hotKey = HotKey.hotKey("Send to LazyFlow", "Ctrl+L");
        HotKeyHandler handler = event -> {
            List<HttpRequestResponse> selected = event.selectedRequestResponses();
            if (selected == null || selected.isEmpty()) {
                api.logging().logToOutput("LazyFlow: hotkey fired but nothing selected.");
                return;
            }
            List<HttpRequestResponse> ordered = new java.util.ArrayList<>(selected);
            Collections.reverse(ordered);
            api.logging().logToOutput("LazyFlow: hotkey — " + ordered.size() + " item(s).");
            new Thread(() -> handleExport(ordered)).start();
        };
        api.userInterface().registerHotKeyHandler(hotKey, handler);

        api.logging().logToOutput("LazyFlow loaded | hotkey: Ctrl+L | no server required");
    }

    // ── context menu ──────────────────────────────────────────────────────────

    private class FlowContextMenu implements ContextMenuItemsProvider {
        @Override
        public List<Component> provideMenuItems(ContextMenuEvent event) {
            List<HttpRequestResponse> selected = event.selectedRequestResponses();
            if (selected == null || selected.isEmpty()) return List.of();
            List<HttpRequestResponse> ordered = new java.util.ArrayList<>(selected);
            Collections.reverse(ordered);
            JMenuItem item = new JMenuItem("Send to LazyFlow  [Ctrl+L]");
            item.addActionListener(e -> new Thread(() -> handleExport(ordered)).start());
            return List.of(item);
        }
    }

    // ── core: build HTML with injected flow data and open it ──────────────────

    private void handleExport(List<HttpRequestResponse> items) {
        try {
            String flowTxt = buildFlowTxt(items);
            api.logging().logToOutput(
                "LazyFlow: built flow.txt — " + items.size() + " pairs, " + flowTxt.length() + " bytes");

            String escaped = flowTxt
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$")
                .replace("</script>", "<\\/script>");

            String html = HTML_TEMPLATE.replace("`__FLOW_DATA__`", "`" + escaped + "`");

            File tmp = Files.createTempFile("lazyflow_", ".html").toFile();
            tmp.deleteOnExit();
            try (FileWriter fw = new FileWriter(tmp, StandardCharsets.UTF_8)) {
                fw.write(html);
            }

            api.logging().logToOutput("LazyFlow: opening " + tmp.getAbsolutePath());
            openBrowser(tmp.toURI());

            final File toDelete = tmp;
            new Thread(() -> {
                try { Thread.sleep(5_000); } catch (InterruptedException ignored) {}
                if (toDelete.delete()) api.logging().logToOutput("LazyFlow: temp file deleted.");
                else                  api.logging().logToOutput("LazyFlow: temp file already gone.");
            }, "LazyFlow-cleanup").start();

        } catch (Exception ex) {
            api.logging().logToError("LazyFlow handleExport error: " + ex);
        }
    }

    // ── flow.txt builder ──────────────────────────────────────────────────────

    private String buildFlowTxt(List<HttpRequestResponse> items) {
        StringBuilder sb = new StringBuilder();
        for (HttpRequestResponse rr : items) {
            sb.append("===REQUEST===\n");
            if (rr.request() != null)
                sb.append(toLF(rr.request().toString()));
            sb.append("\n===RESPONSE===\n");
            if (rr.response() != null)
                sb.append(toLF(rr.response().toString()));
            else
                sb.append("HTTP/1.1 000 No Response\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    private String toLF(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    // ── open browser ──────────────────────────────────────────────────────────

    private void openBrowser(URI uri) {
        try {
            if (Desktop.isDesktopSupported() &&
                    Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
                return;
            }
            String url = uri.toString();
            for (String cmd : new String[]{"xdg-open", "firefox", "chromium-browser", "google-chrome"}) {
                try { Runtime.getRuntime().exec(new String[]{cmd, url}); return; }
                catch (IOException ignored) {}
            }
            api.logging().logToOutput("LazyFlow: open manually: " + url);
        } catch (Exception e) {
            api.logging().logToError("LazyFlow openBrowser: " + e);
        }
    }
}