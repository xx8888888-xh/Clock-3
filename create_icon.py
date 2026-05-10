#!/usr/bin/env python3
"""
简单的图标生成脚本 - 用于创建占位图标
"""

from PIL import Image, ImageDraw
import os

def create_icon():
    """创建一个简单的时钟图标"""
    
    # 创建一个 256x256 的图像
    size = 256
    img = Image.new('RGB', (size, size), color='#4a90d9')
    draw = ImageDraw.Draw(img)
    
    # 绘制时钟圆形
    center = size // 2
    radius = 100
    draw.ellipse([center-radius, center-radius, center+radius, center+radius], 
                 outline='white', fill='none', width=8)
    
    # 绘制时针和分针
    # 短针
    draw.line([center, center, center, center-40], fill='white', width=6)
    # 长针
    draw.line([center, center, center+50, center], fill='white', width=4)
    
    # 保存图标
    os.makedirs('assets', exist_ok=True)
    img.save('assets/icon.png')
    print("Icon created: assets/icon.png")

if __name__ == "__main__":
    try:
        create_icon()
    except ImportError:
        print("Pillow not installed, skipping icon creation")
    except Exception as e:
        print(f"Icon creation failed: {e}")
