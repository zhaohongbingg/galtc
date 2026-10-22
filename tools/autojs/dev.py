"""调试用：带上 prelude 执行一段临时 JS（不进回归集）。

用法：
    python -X utf8 tools/autojs/dev.py tools/autojs/_probe.js
"""

import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from send import run_code  # noqa: E402

BASE = pathlib.Path(__file__).resolve().parent


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    body = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8")
    code = (BASE / "prelude.js").read_text(encoding="utf-8") + "\n" + body
    ok, payload = run_code(code)
    print(json.dumps(payload, ensure_ascii=False, indent=2))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
