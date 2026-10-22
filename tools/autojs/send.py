"""把 JS 脚本通过 AutoJS 的 MCP 端点推送到运行中的游戏实例执行。

用法：
    python tools/autojs/send.py tools/autojs/test_armor_traits.js          # server 侧
    python tools/autojs/send.py tools/autojs/test_armor_traits.js client   # client 侧

带 prelude / 分步调度 / 现场存盘还原的用例请走：
    python tools/autojs/run_all.py            # 全量回归
    python tools/autojs/dev.py <临时脚本.js>   # 临时调试（自动带上 prelude）
"""

import json
import pathlib
import sys
import urllib.error
import urllib.request

URL = "http://127.0.0.1:25585/mcp"
HEADERS = {"Content-Type": "application/json", "Accept": "application/json, text/event-stream"}


def post(payload, session=None, timeout=120):
    headers = dict(HEADERS)
    if session:
        headers["Mcp-Session-Id"] = session
    request = urllib.request.Request(
        URL, data=json.dumps(payload).encode("utf-8"), headers=headers, method="POST"
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, dict(response.headers), response.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as error:
        return error.code, dict(error.headers), error.read().decode("utf-8", "replace")


def parse_body(body):
    """MCP 的响应可能是纯 JSON，也可能是 SSE（event: message / data: {...}）"""
    text = body.strip()
    if text.startswith("event:") or "\ndata:" in text or text.startswith("data:"):
        chunks = []
        for line in text.splitlines():
            if line.startswith("data:"):
                chunks.append(line[5:].strip())
        text = "\n".join(chunks)
    return json.loads(text) if text else {}


def run_code(code, side="server", raw=False):
    """执行一段 JS，返回 (ok, payload)。

    payload 是 eval 的原始结果：{"ok":..., "value":..., "type":...}；
    raw=True 时返回 MCP 的整个 result 对象。
    """
    status, headers, body = post({
        "jsonrpc": "2.0", "id": 1, "method": "initialize",
        "params": {
            "protocolVersion": "2024-11-05", "capabilities": {},
            "clientInfo": {"name": "galtc2-test", "version": "1.0"},
        },
    })
    if status != 200:
        raise RuntimeError(f"initialize 失败: HTTP {status}\n{body[:500]}")

    session = headers.get("Mcp-Session-Id") or headers.get("mcp-session-id")
    post({"jsonrpc": "2.0", "method": "notifications/initialized"}, session)

    status, _, body = post({
        "jsonrpc": "2.0", "id": 2, "method": "tools/call",
        "params": {"name": "eval_js", "arguments": {"side": side, "code": code}},
    }, session)

    data = parse_body(body)
    result = data.get("result", data)
    if raw:
        return ("error" not in data and result.get("isError") is not True), result

    text = ""
    for item in result.get("content", []):
        if item.get("type") == "text":
            text = item.get("text", "")
            break
    if "error" in data:
        return False, {"error": data["error"]}
    try:
        payload = json.loads(text) if text else {}
    except json.JSONDecodeError:
        return False, {"error": "无法解析返回: " + text[:500]}
    if payload.get("error"):
        return False, payload
    return bool(payload.get("ok")), payload


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2

    script = pathlib.Path(sys.argv[1])
    side = sys.argv[2] if len(sys.argv) > 2 else "server"
    code = script.read_text(encoding="utf-8")

    ok, payload = run_code(code, side)
    print(json.dumps(payload, ensure_ascii=False, indent=2))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
