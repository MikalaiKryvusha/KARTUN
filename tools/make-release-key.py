"""make-release-key.py - creates the KARTUN release signing key once: kartun-release.jks + .env (both outside git, R2).

The owner's word (chat 2026-10-16 ≈ 14:45 +03:00): «ты создай в .env, вне гита. Я положу их еще в свое приватное
хранилище. Разрешаю тебе сгенерировать секретные ключи». The passwords are random (secrets.token_urlsafe), written only
to .env and handed to keytool through temporary files, never through argv; nothing secret is printed. Refuses to run when
.env or the keystore already exists - a second key would make every installed release impossible to update.
RSA 4096, valid 30 years (Android asks for 25+ years: https://developer.android.com/studio/publish/app-signing).

Usage (from the project root): python -I tools/make-release-key.py
"""
import os
import secrets
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ENV = ROOT / ".env"
STORE = ROOT / "kartun-release.jks"
ALIAS = "kartun"
KEYTOOL = Path(r"C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot\bin\keytool.exe")
DNAME = "CN=Mikalai Kryvusha, O=KARTUN, C=BY"
VALIDITY_DAYS = 365 * 30


def main():
    if ENV.exists() or STORE.exists():
        sys.exit(f"refused: {ENV.name} or {STORE.name} already exists - the release key is created once")
    for p in (ENV, STORE):
        # Ignore first (AGENT_GUIDE git hygiene): a secret that git could see is never written.
        if subprocess.run(["git", "check-ignore", "-q", str(p)], cwd=ROOT).returncode != 0:
            sys.exit(f"refused: {p.name} is not ignored by git")
    store_pass = secrets.token_urlsafe(24)
    key_pass = store_pass  # PKCS12 keystores keep one password for the store and the key (keytool warns otherwise)
    with tempfile.TemporaryDirectory() as tmp:
        sp = Path(tmp) / "sp"
        sp.write_text(store_pass, encoding="ascii")
        r = subprocess.run([str(KEYTOOL), "-genkeypair", "-keystore", str(STORE), "-storetype", "PKCS12",
                            "-alias", ALIAS, "-keyalg", "RSA", "-keysize", "4096", "-validity", str(VALIDITY_DAYS),
                            "-dname", DNAME, "-storepass:file", str(sp), "-keypass:file", str(sp)],
                           capture_output=True, text=True)
        if r.returncode != 0:
            sys.exit("keytool failed: " + (r.stderr or r.stdout)[-400:])
    ENV.write_text(
        "# KARTUN release signing (HOUSE_RULES R2) - never commit; the owner keeps a copy of this file and the keystore.\n"
        f"KARTUN_KEYSTORE=kartun-release.jks\nKARTUN_KEYSTORE_PASSWORD={store_pass}\n"
        f"KARTUN_KEY_ALIAS={ALIAS}\nKARTUN_KEY_PASSWORD={key_pass}\n", encoding="ascii", newline="\n")
    print(f"created {STORE.name} (RSA 4096, {VALIDITY_DAYS} days, alias {ALIAS}) and {ENV.name}; passwords not printed")


if __name__ == "__main__":
    main()
