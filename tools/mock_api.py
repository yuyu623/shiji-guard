import json
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

LOG = Path(__file__).resolve().parent.parent / 'build' / 'api_prompt.log'


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get('Content-Length', '0'))
        raw = self.rfile.read(length)
        try:
            body = json.loads(raw.decode('utf-8'))
            with LOG.open('a', encoding='utf-8') as f:
                f.write(json.dumps(body.get('messages', []), ensure_ascii=False) + '\n')
            answer = "模拟点评：今天的短视频差不多占了大头，算法比你更清楚你的作息。先把睡前那半小时拿回来，别让手机替你安排明天。"
        except Exception as exc:  # pragma: no cover - test helper
            answer = 'mock error: %s' % exc
        payload = {"choices": [{"message": {"role": "assistant", "content": answer}}]}
        data = json.dumps(payload, ensure_ascii=False).encode('utf-8')
        self.send_response(200)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, *args):
        pass


if __name__ == '__main__':
    HTTPServer(('127.0.0.1', 8765), Handler).serve_forever()
