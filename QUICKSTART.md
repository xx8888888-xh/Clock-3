# 快速开始指南

## 📱 在 Android 上运行

### 方法一：下载已构建的 APK

1. 访问 [Releases](https://github.com/xx8888888-xh/Clock-3/releases) 页面
2. 下载最新的 `clock3-*-debug.apk` 文件
3. 安装到手机上（可能需要允许"安装未知来源应用"）

### 方法二：使用 Buildozer 本地构建

```bash
# 1. 克隆项目
git clone https://github.com/xx8888888-xh/Clock-3.git
cd Clock-3

# 2. 安装依赖
pip install -r requirements.txt

# 3. 构建 APK
buildozer android debug

# 4. 安装到手机
buildozer android debug deploy run
```

## 💻 在桌面测试

```bash
# 1. 安装依赖
pip install -r requirements.txt

# 2. 运行应用
python -m app.main
```

## 🔧 开发

### 项目结构

```
Clock3/
├── app/
│   ├── main.py              # 主程序入口
│   ├── config.py            # 配置管理
│   ├── database.py          # 数据库操作
│   ├── models/              # 数据模型
│   │   ├── alarm.py         # 闹钟模型
│   │   ├── countdown.py     # 倒计时模型
│   │   └── pet.py           # 宠物模型
│   ├── services/            # 业务逻辑
│   │   ├── alarm_service.py
│   │   ├── countdown_service.py
│   │   └── pet_service.py
│   ├── screens/             # 界面（预留）
│   └── widgets/             # 组件（预留）
├── assets/                  # 资源文件
├── buildozer.spec          # 打包配置
├── requirements.txt        # 依赖列表
└── .github/workflows/      # CI/CD配置
```

### 主要功能

#### 🐾 悬浮球宠物
- 拖动移动位置
- 单击/双击/三击/长按交互
- 心情和等级系统
- 睡眠模式

#### ⏰ 闹钟系统
- 多个闹钟
- 重复设置（一次/每天/工作日/周末/自定义）
- 贪睡功能
- 声音+振动提醒
- 数据导入/导出

#### ⏱️ 倒计时
- 精确到秒
- 暂停/继续
- 完成提醒

## 🛠️ 自定义

### 修改宠物
将自定义图片放入 `assets/` 目录，程序会自动使用。

### 修改声音
将闹钟声音文件放入 `assets/` 目录。

## 📝 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！
