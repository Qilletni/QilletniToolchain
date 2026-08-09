#!/bin/bash
INSTALL_DIR="$HOME/.qilletni"
mkdir -p "$INSTALL_DIR"
url=$(curl -s https://api.github.com/repos/Qilletni/QilletniToolchain/releases/latest | grep "browser_download_url.*tar.gz" | cut -d'"' -f4)
curl -L "$url" -o /tmp/qilletni.tar.gz
tar -xzf /tmp/qilletni.tar.gz -C "$INSTALL_DIR" && rm /tmp/qilletni.tar.gz

if ! grep -q 'export PATH="$HOME/.qilletni:$PATH"' "$HOME/.bashrc"; then
  echo 'export PATH="$HOME/.qilletni:$PATH"' >> "$HOME/.bashrc"
fi

export PATH="$HOME/.qilletni:$PATH"

echo "Installed Qilletni, please reload your terminal or run 'source ~/.bashrc'"
