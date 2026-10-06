#!/usr/bin/env bash
#
# 生成 release 签名密钥，并打印出 GitHub Secrets 需要的三个值。
#
# 用法：
#   ./scripts/generate-keystore.sh                # 交互式生成 PKCS12 密钥库
#   ./scripts/generate-keystore.sh --print-env    # 只打印已有 keystore 的环境变量形式
#
# 生成后需要配置到仓库 Secrets（Settings → Secrets and variables → Actions）：
#   ANDROID_KEYSTORE_BASE64    base64 -w0 < keystore/errorbook-release.p12
#   ANDROID_KEYSTORE_PASSWORD  keystore 口令
#   ANDROID_KEY_ALIAS          key alias
#   ANDROID_KEY_PASSWORD       同 keystore 口令（PKCS12 是单口令容器）

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYSTORE_DIR="$REPO_ROOT/keystore"
KEYSTORE_PATH="$KEYSTORE_DIR/errorbook-release.p12"
KEY_ALIAS_DEFAULT="errorbook"
DNAME_DEFAULT="CN=Cuoyinben, OU=Dev, O=Cuoyinben, C=CN"

if ! command -v keytool >/dev/null 2>&1; then
  echo "错误：找不到 keytool，请先安装 JDK 17 并把 JAVA_HOME/bin 加入 PATH" >&2
  exit 1
fi

if [ "${1:-}" = "--print-env" ]; then
  if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "错误：$KEYSTORE_PATH 不存在" >&2
    exit 1
  fi
  echo "# 复制以下内容到 GitHub Secrets（ANDROID_KEYSTORE_BASE64 用 base64 -w0 生成）"
  echo "ANDROID_KEYSTORE_BASE64=$(base64 -w0 < "$KEYSTORE_PATH")"
  exit 0
fi

if [ -f "$KEYSTORE_PATH" ]; then
  echo "错误：$KEYSTORE_PATH 已存在。" >&2
  echo "      覆盖会导致已发布 APK 的签名不一致、系统无法覆盖安装。" >&2
  echo "      如确需更换，请改名归档后重新生成（并同步更新 Secrets）。" >&2
  exit 1
fi

KEY_ALIAS="${KEY_ALIAS:-$KEY_ALIAS_DEFAULT}"
DNAME="${DNAME:-$DNAME_DEFAULT}"

echo "将生成签名密钥：$KEYSTORE_PATH"
echo "key alias：$KEY_ALIAS"
echo "证书主体：$DNAME"
echo
echo "注意：口令请用强随机值，并妥善备份——密钥一旦丢失就无法再发布可覆盖安装的更新。"
echo

# PKCS12 是单口令容器：key 口令必须与 store 口令一致，否则 keytool 会忽略 keypass。
# 这里只问一次口令，同时用作两者。
read -r -s -p "keystore 口令（PKCS12 单口令，key 口令与之相同）：" STORE_PASSWORD; echo
KEY_PASSWORD="$STORE_PASSWORD"

mkdir -p "$KEYSTORE_DIR"
keytool -genkeypair \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -sigalg SHA256withRSA \
  -validity 10000 \
  -storetype PKCS12 \
  -keystore "$KEYSTORE_PATH" \
  -storepass "$STORE_PASSWORD" \
  -dname "$DNAME"

echo
echo "已生成：$KEYSTORE_PATH（PKCS12）"
echo
echo "下一步："
echo "  1) 把下面四个值配置到仓库 Secrets（Settings → Secrets and variables → Actions）："
echo "     ANDROID_KEYSTORE_BASE64   = \$(base64 -w0 < '$KEYSTORE_PATH')"
echo "     ANDROID_KEYSTORE_PASSWORD = <keystore 口令>"
echo "     ANDROID_KEY_ALIAS         = $KEY_ALIAS"
echo "     ANDROID_KEY_PASSWORD      = 同 keystore 口令（PKCS12 单口令）"
echo "  2) 确认 keystore/ 已被 .gitignore 忽略（不应进版本库）"
echo "  3) 到 Actions → Release → Run workflow 发布 dev release"
echo
echo "本地签名构建建议用环境变量（避免口令进入 shell history）："
echo "  export ERRORBOOK_KEYSTORE=keystore/errorbook-release.p12"
echo "  export ERRORBOOK_KEYSTORE_PASSWORD='<keystore 口令>'"
echo "  export ERRORBOOK_KEY_ALIAS=$KEY_ALIAS"
echo "  export ERRORBOOK_KEY_PASSWORD='<与 store 相同'"
echo "  ./gradlew :app:assembleRelease"
