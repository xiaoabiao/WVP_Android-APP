#!/bin/bash
# =============================================
#  WVP Android-APP 自动编译脚本 (Linux / macOS)
#  用法: chmod +x build.sh && ./build.sh [debug|release|clean]
# =============================================

MODE=${1:-debug}

echo "============================================"
echo " WVP Android-APP 构建脚本"
echo " 模式: $MODE"
echo "============================================"

case "$MODE" in
  clean)
    echo "| 正在清理..."
    ./gradlew clean
    echo "| 清理完成"
    ;;
  release)
    echo "| 正在编译 Release APK..."
    ./gradlew assembleRelease
    ;;
  *)
    echo "| 正在编译 Debug APK..."
    ./gradlew assembleDebug
    ;;
esac

if [ $? -eq 0 ]; then
  echo "============================================"
  echo " ✓ 编译成功！"
  echo "   输出目录: app/build/outputs/apk/$MODE"
  echo "============================================"
else
  echo "============================================"
  echo " ✗ 编译失败，请检查错误信息"
  echo "============================================"
  exit 1
fi
