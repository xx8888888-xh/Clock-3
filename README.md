# Clock 3 - 智能桌面宠物闹钟

> 一款功能丰富的安卓桌面宠物闹钟应用，支持悬浮球宠物、闹钟提醒、倒计时等实用功能。

## 功能特性

### 🐾 悬浮球宠物系统
- 可爱的宠物悬浮球显示
- 触摸拖动移动位置
- 单击/双击/三击/长按交互
- 待机动画（上下浮动）
- 睡眠模式（自动切换）

### ⏰ 闹钟提醒系统
- 多个闹钟设置
- 灵活的重复设置（一次/每天/工作日/周末/自定义）
- 贪睡功能（可设置次数和间隔）
- 声音+振动提醒
- 通知横幅显示
- 数据导入/导出

### ⏱️ 倒计时/计时器
- 精确到秒的倒计时
- 多个计时器同时运行
- 暂停/继续功能
- 完成提醒

### 🎨 个性化设置
- 宠物大小调整
- 宠物透明度设置
- 贪睡参数配置
- 声音/振动开关

### 🐶 扩展功能
- 宠物心情系统
- 宠物对话系统
- 宠物成长系统
- 智能提醒

## 技术栈

- **Python 3.8+** - 编程语言
- **Kivy** - 跨平台GUI框架
- **KivyMD** - Material Design组件
- **SQLite** - 数据持久化
- **Buildozer** - Android打包
- **GitHub Actions** - 自动化CI/CD

## 构建 APK

### 使用 Docker（推荐，自动在 GitHub Actions 上运行）

当你推送到 `main` 或 `master` 分支时，GitHub Actions 会自动触发构建：

1. 推送代码到仓库
2. 打开 Actions 标签页查看构建进度
3. 下载自动生成的 APK 附件

### 本地构建

```bash
# 方法1: 使用官方 Docker 镜像（推荐）
docker run --rm -v $(pwd):/home/user/clock3 -w /home/user/clock3 kivy/buildozer buildozer android debug

# 方法2: 使用 Python 环境
pip install buildozer kivy plyer
buildozer android debug
```

## 快速开始

### 桌面测试

```bash
# 1. 克隆项目
git clone https://github.com/xx8888888-xh/Clock-3.git
cd Clock-3

# 2. 安装依赖
pip install -r requirements.txt

# 3. 运行程序
python -m app.main
```

### Android打包

```bash
# 1. 安装Buildozer
pip install buildozer

# 2. 打包APK
buildozer android debug

# 3. 安装到手机
buildozer android debug deploy run
```

### GitHub Actions自动打包

推送代码到main分支会自动触发构建，APK会作为artifact下载。

## 项目结构

```
Clock3/
├── app/
│   ├── main.py              # 主入口
│   ├── config.py            # 配置管理
│   ├── database.py           # 数据库
│   ├── models/              # 数据模型
│   ├── services/            # 服务层
│   ├── screens/            # 界面
│   └── widgets/             # 组件
├── assets/                  # 资源文件
├── buildozer.spec          # 打包配置
├── requirements.txt        # 依赖
└── .github/workflows/      # CI/CD
```

## 基本操作

| 操作 | 功能 |
|------|------|
| 拖动宠物 | 移动悬浮球位置 |
| 单击宠物 | 打开主菜单 |
| 双击宠物 | 快速打开闹钟 |
| 三击宠物 | 打开倒计时 |
| 长按宠物 | 显示快捷菜单 |

## 开发说明

### 环境要求
- Python 3.8+
- Android SDK (打包用)
- Git

### 代码规范
- PEP 8 代码规范
- 模块化架构
- 完善的日志记录

## 许可证

MIT License

## 作者

xx8888888-xh
