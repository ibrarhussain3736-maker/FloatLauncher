#!/data/data/com.termux/files/usr/bin/bash
# Usage: bash push_to_github.sh https://github.com/USERNAME/REPO.git
set -e
[ -z "$1" ] && { echo "Usage: bash push_to_github.sh <repo-url>"; exit 1; }
pkg install -y git >/dev/null
git init
git add .
git commit -m "Initial commit: Float Launcher"
git branch -M main
git remote add origin "$1" 2>/dev/null || git remote set-url origin "$1"
echo "When asked for a password, paste your GitHub Personal Access Token."
git push -u origin main
