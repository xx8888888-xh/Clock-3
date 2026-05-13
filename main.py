#!/usr/bin/env python3
"""
Clock 3 - 主程序入口
使用 Kivy 构建的智能桌面宠物闹钟应用
"""

import sys
import logging

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

try:
    from app.main import Clock3App

    if __name__ == "__main__":
        logger.info("正在启动 Clock 3 应用...")
        app = Clock3App()
        app.run()
except Exception as e:
    logger.critical(f"启动失败: {e}", exc_info=True)
    sys.exit(1)
