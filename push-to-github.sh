#!/bin/bash

# 推送到 GitHub 并触发自动构建

echo "🦞 针刺 - 推送到 GitHub"
echo "========================"

# 检查 git
if ! command -v git &> /dev/null; then
    echo "❌ 未安装 Git"
    echo "请安装: brew install git"
    exit 1
fi

# 进入项目目录
cd "$(dirname "$0")"

# 检查是否已初始化 git
if [ ! -d ".git" ]; then
    echo "📦 初始化 Git 仓库..."
    git init
    git add .
    git commit -m "Initial commit"
    echo "✅ Git 仓库已初始化"
    echo ""
    echo "请在 GitHub 创建新仓库，然后运行:"
    echo "  git remote add origin https://github.com/你的用户名/仓库名.git"
    echo "  git branch -M main"
    echo "  git push -u origin main"
    exit 0
fi

# 检查远程仓库
if ! git remote -v &> /dev/null; then
    echo "⚠️  未配置远程仓库"
    echo "请先在 GitHub 创建仓库，然后运行:"
    echo "  git remote add origin https://github.com/你的用户名/仓库名.git"
    exit 1
fi

echo "📤 推送代码到 GitHub..."

# 添加所有更改
git add .

# 提交更改
read -p "输入提交信息 (直接回车使用默认): " msg
if [ -z "$msg" ]; then
    msg="Update: $(date '+%Y-%m-%d %H:%M:%S')"
fi
git commit -m "$msg"

# 推送到 main 分支
git push origin main

echo ""
echo "✅ 推送成功!"
echo ""
echo "🚀 GitHub Actions 正在自动构建 APK..."
echo ""
echo "查看构建状态:"
echo "  1. 打开 https://github.com/$(git remote get-url origin | sed 's/.*github.com[:/]\([^/]*\)\/\([^/]*\).*/\1\/\2/')/actions"
echo "  2. 等待构建完成"
echo "  3. 下载 APK"
echo ""
