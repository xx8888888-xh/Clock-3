[app]
title = Clock 3
package.name = clock3
package.domain = org.example
source.dir = .
source.include_exts = py,png,jpg,kv,atlas,ttf,mp3,wav
version = 1.0.0
requirements = python3,kivy
orientation = portrait
fullscreen = 0

# Android 权限（最基本的）
android.permissions = INTERNET

# Android SDK 配置
android.api = 33
android.minapi = 21
android.accept_sdk_license = True

# 简化配置
android.theme = @android:style/Theme.Material.Light
android.apptheme = @android:style/Theme.Material.Light

[buildozer]
log_level = 2
warn_on_root = 0
