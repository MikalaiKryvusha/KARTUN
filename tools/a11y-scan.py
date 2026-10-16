# TalkBack check of the screen on the device: every clickable, long-clickable, scrollable or focusable node must have a
# name - its own text / content-desc or a named descendant (Compose merges a row's texts into it). Prints each unnamed
# node with its class and bounds; exit 1 when there is one. The owner, 2026-10-16 ≈ 17:43: «TalkBack должен работать
# корректно. Все им должно быть описано».
# Usage: python -I tools/a11y-scan.py [--serial <ip:port>] [--name <label for the report>]
# Needs the UI agent stopped (`ui.py quit`): one UiAutomation client at a time. [NOT-TESTED]
import argparse
import subprocess
import sys
import xml.etree.ElementTree as ET

ADB = r"C:\Android\Sdk\platform-tools\adb.exe"
ap = argparse.ArgumentParser()
ap.add_argument("--serial", default="100.99.111.48:5555")
ap.add_argument("--name", default="screen")
a = ap.parse_args()


def adb(*args):
    return subprocess.run([ADB, "-s", a.serial, *args], capture_output=True, text=True, encoding="utf-8")


adb("shell", "uiautomator", "dump", "/data/local/tmp/a11y.xml")
xml = adb("exec-out", "cat", "/data/local/tmp/a11y.xml").stdout
if "<hierarchy" not in xml:
    print("no dump:", xml[:200])
    sys.exit(2)
root = ET.fromstring(xml[xml.index("<hierarchy"):])
ACTIVE = ("clickable", "long-clickable", "scrollable", "focusable")
SYSTEM = ("com.android.systemui",)


def named(n):
    if (n.get("text") or "").strip() or (n.get("content-desc") or "").strip():
        return True
    return any(named(c) for c in n)


bad = []
for n in root.iter("node"):
    if n.get("package", "").startswith(SYSTEM):
        continue
    if any(n.get(k) == "true" for k in ACTIVE) and not named(n):
        bad.append(n)
pkg = next((n.get("package") for n in root.iter("node") if n.get("package", "").startswith("com.kriniks")), "?")
print(f"{a.name} ({pkg}): {len(bad)} unnamed active node(s)")
for n in bad:
    acts = ",".join(k for k in ACTIVE if n.get(k) == "true")
    print(f"  {n.get('class')} [{acts}] {n.get('bounds')} id={n.get('resource-id') or '-'}")
sys.exit(1 if bad else 0)
