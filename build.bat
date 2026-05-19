@echo off
REM =============================================
REM  WVP Android-APP 自动编译脚本 (Windows)
REM  用法: build.bat [debug|release|clean]
REM =============================================

set MODE=%1
if "%MODE%"=="" set MODE=debug

echo ============================================
echo  WVP Android-APP 构建脚本
echo  模式: %MODE%
echo ============================================

if /i "%MODE%"=="clean" (
    echo ^| 正在清理...
    call gradlew clean
    echo ^| 清理完成
    goto :end
)

if /i "%MODE%"=="release" (
    echo ^| 正在编译 Release APK...
    call gradlew assembleRelease
) else (
    echo ^| 正在编译 Debug APK...
    call gradlew assembleDebug
)

if %ERRORLEVEL% EQU 0 (
    echo ============================================
    echo  ✓ 编译成功！
    echo  输出目录: app\build\outputs\apk\%MODE%
    echo ============================================
) else (
    echo ============================================
    echo  ✗ 编译失败，请检查错误信息
    echo ============================================
    exit /b 1
)

:end
