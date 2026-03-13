# LazyFlow — Burp Suite Extension

A zero-setup flow analyzer for Burp Suite. Select a set of requests in the Proxy, send them to LazyFlow, and instantly get a browser-based visual map of every value flowing between requests — cookies, tokens, CSRF values, and more — all highlighted and color-coded.

No server. No dependencies. Everything runs locally in the browser.

---

## How It Works

LazyFlow captures the sequence of HTTP request/response pairs you select and runs a correlation engine over them entirely client-side. It finds values that appear in a response and are later reused in a subsequent request, highlights every occurrence across all pairs with a matching color, lets you filter out false positives, and exports the result as a ready-to-use flow file.

---

## Features

### One-Click Flow Analysis
Select any number of requests in the Burp Proxy, then either right-click → **Send to LazyFlow** or hit **Ctrl+L**. A static HTML page opens in your browser instantly with the full analysis pre-loaded — no manual copy-pasting required.

### Correlation Engine
LazyFlow automatically detects values extracted from a response and reused in a later request. It runs two passes:

- **Forward pass** — finds values in a response that show up in a later request
- **Reverse pass** — traces values in a request back to an earlier response

It looks across all the common locations:

| Side | Locations inspected |
|------|-------------------|
| **Response** | Headers, `Set-Cookie` values, JSON fields, hidden HTML inputs, `<meta>` tags, inline script variables, form-encoded fields |
| **Request** | Cookies, custom headers, URL query params, JSON body fields, form-encoded body fields |

### Color-Coded Highlighting
Every correlated value gets a unique color. Matching occurrences are highlighted consistently across all request/response pairs so you can visually trace exactly where a token is born and where it gets used.

### Visibility Filters
False positives are inevitable. The left panel lists every detected value with a toggle — uncheck anything that isn't actually interesting and the highlights disappear instantly across the whole view.

### Export to `.flow` or `.coookies`
Once you're happy with the correlation map, export it directly as a `.flow` or `.coookies` file. These files contain the full request sequence with placeholders already substituted in, and extraction rules auto-generated for each correlated value. Each format is designed to be imported directly into its companion extension — [FlowRepeater](https://github.com/I-blank-I/FlowRepeater) and [COOOKIES](https://github.com/I-blank-I/COOOKIES) respectively — so you go from traffic capture to a fully configured, ready-to-run test in seconds.

---

## Quick Start

1. Load `LazyFlow.jar` as a Burp extension (Extender → Add → Java)
2. In the Proxy, select one or more requests from a flow you want to analyze
3. Right-click → **Send to LazyFlow** (or press **Ctrl+L**)
4. Your browser opens with the highlighted flow analysis
5. Toggle off any false positives in the left panel
6. Hit **⬇ .flow** or **⬇ .coookies** to export
7. Import in **FlowRepeater** or **COOOKIES**

---

## Notes

- Uses the **Montoya API** exclusively — compatible with Burp Suite 2026.2.3+
- The HTML viewer is fully self-contained — no network requests, no external dependencies
- The temp HTML file is automatically deleted 5 seconds after being opened