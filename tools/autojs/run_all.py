"""galtc2 游戏内回归：把 cases/ 下的用例分步推到运行中的客户端执行。

用法：
    python -X utf8 tools/autojs/run_all.py                # 跑全部
    python -X utf8 tools/autojs/run_all.py nighttimesheep # 只跑文件名含该关键字的
    python -X utf8 tools/autojs/run_all.py --list
    python -X utf8 tools/autojs/run_all.py --no-park      # 调试：不每个用例重置现场

前提：游戏已进入存档，AutoJS 的 MCP 端点监听 127.0.0.1:25585。

用例文件约定（见 cases/*.js）：
    //@steps setup,warmup,measure
    var CASE_DEF = { name: '...', setup: function () {...}, measure: function () {...} };
每一步都是一次独立的 eval，两次调用之间服务端正常跑 tick，
所以「装护甲 → 等 tick 生效 → 读数值」可以跨调用完成。

每个用例开始前都会 park（存现场 + 清护甲 + 切生存），结束后 restore，
这样用例之间不会互相污染（残留的护甲/血量/属性都会影响伤害数值）。

失败重试：本整合包的伤害管线偶尔会在「护甲 + 附魔结算之后」再乘一个约 0.90 的系数
（同一 tick 内恒定、跨 tick 随机，跟 galtc2 的特性无关），表现就是某个用例的
「受伤 == 原版护甲公式」突然差 9.7%。这种噪声换一个 tick 重测即消失，
所以失败的用例会 park 后重试，最多 RETRIES 次；重试才通过会打上 [重试] 标记。
"""

import json
import pathlib
import re
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from send import run_code  # noqa: E402

BASE = pathlib.Path(__file__).resolve().parent
CASES = BASE / "cases"
PRELUDE = BASE / "prelude.js"
STEP_GAP = 0.9  # 秒；留够服务端跑 tick
RETRIES = 2  # 失败用例换 tick 重测的次数（见模块文档里的「失败重试」）


def prelude_text():
    return PRELUDE.read_text(encoding="utf-8")


def eval_js(code):
    ok, payload = run_code(code)
    if not ok:
        raise RuntimeError(json.dumps(payload, ensure_ascii=False)[:800])
    value = payload.get("value")
    return json.loads(value) if value else {}


def site(action):
    return eval_js(prelude_text() + f"\nGALT.{action}();")


def steps_of(path):
    text = path.read_text(encoding="utf-8")
    found = re.search(r"//@steps\s+(.+)", text)
    return [s.strip() for s in found.group(1).split(",")] if found else ["setup", "measure"]


def run_step(path, step):
    code = prelude_text() + "\n" + path.read_text(encoding="utf-8")
    code += f"\n__dispatch(CASE_DEF, {json.dumps(step)});"
    return eval_js(code)


def run_case(path):
    report = {"case": path.stem, "pass": False, "checks": [], "notes": []}
    try:
        for step in steps_of(path):
            result = run_step(path, step)
            if result.get("staged"):
                time.sleep(STEP_GAP)
                continue
            report = result
            break
    except Exception as error:  # noqa: BLE001
        report = {"case": path.stem, "pass": False, "checks": [],
                  "notes": ["执行失败: " + str(error)]}
    return report


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    no_park = "--no-park" in sys.argv
    files = sorted(p for p in CASES.glob("*.js") if not p.name.startswith("_"))
    if args:
        files = [p for p in files if any(k in p.stem for k in args)]
    if "--list" in sys.argv:
        for path in files:
            print(f"{path.stem}  ({','.join(steps_of(path))})")
        return 0
    if not files:
        print("没有匹配的用例")
        return 2

    if not no_park:
        first = site("park")
        print("[park]", json.dumps(first, ensure_ascii=False))
        time.sleep(STEP_GAP * 2)
        probe = site("restore")
        if probe.get("tick", 0) <= first.get("tick", 0):
            print("[中止] 服务端一次 tick 都没跑（游戏被暂停了），先回到游戏里再跑回归")
            return 3

    failed = []
    stalled = []
    retried_cases = []
    try:
        for path in files:
            parked_tick = 0
            if not no_park:
                info = site("park")
                parked_tick = info.get("tick", 0)
                if info.get("keptOldSite"):
                    print("   [警告] 玩家不在可用状态，park 跳过了存盘（沿用了上一次的现场）")
                time.sleep(STEP_GAP)

            report = run_case(path)
            retried = 0
            while not report.get("pass") and retried < RETRIES:
                retried += 1
                print(f"   [重试] {path.stem} 第 {retried} 次失败，换 tick 重测")
                if not no_park:
                    # 先把上一次尝试留下的护甲还原成现场，再重新 park，
                    # 否则 park 会把用例穿的测试护甲当成现场存进去
                    site("restore")
                    info = site("park")
                    if info.get("tick", 0) <= parked_tick:
                        print("   [中止] 服务端没在跑 tick，重试没有意义")
                        break
                    parked_tick = info.get("tick", 0)
                time.sleep(STEP_GAP)
                report = run_case(path)

            mark = "PASS" if report.get("pass") else "FAIL"
            if report.get("pass") and retried:
                mark = f"PASS(重试{retried})"
                retried_cases.append(f"{path.stem}(×{retried})")
            print(f"[{mark}] {report.get('case', path.stem)}")
            for item in report.get("checks", []):
                flag = "ok " if item.get("ok") else "X  "
                print(f"   {flag} {item.get('label')}: 期望 {item.get('expected')} / 实际 {item.get('actual')}")
            for note in report.get("notes", []):
                if note:
                    print(f"   -  {note}")
            if not report.get("pass"):
                failed.append(path.stem)

            if not no_park:
                info = site("restore")
                armor = ",".join(str(a) for a in info.get("armor", []))
                print(f"   [现场] {info.get('gameType')} | {armor} | 清理测试生物 {info.get('killed')}")
                if info.get("tick", 0) <= parked_tick:
                    stamp = "这段用例期间服务端一次 tick 都没跑（游戏被暂停？），结果不可信"
                    print(f"   [警告] {stamp}")
                    stalled.append(path.stem)
    finally:
        print("[restore]", json.dumps(site("restore"), ensure_ascii=False))

    print()
    if stalled:
        print(f"注意：{len(stalled)} 个用例期间服务端没跑 tick（游戏被暂停），结果不可信: "
              + ", ".join(stalled))
    if failed:
        print(f"失败 {len(failed)}/{len(files)}: " + ", ".join(failed))
        return 1
    if retried_cases:
        print(f"注意：这些用例第一次跑撞上了整合包的伤害噪声，重测才通过: " + ", ".join(retried_cases))
    print(f"全部通过 {len(files)}/{len(files)}")
    return 0 if not stalled else 1


if __name__ == "__main__":
    sys.exit(main())
