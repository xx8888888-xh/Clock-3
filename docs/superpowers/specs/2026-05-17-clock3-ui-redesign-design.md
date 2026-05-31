# Clock 3 UI 现代化重构设计方案 v2.1

## 文档信息

- **项目名称**: Clock 3 - 桌面宠物闹钟
- **设计版本**: v2.1 (增强版)
- **设计日期**: 2026-05-17
- **设计风格**: 温柔治愈系 (Warm & Healing)
- **状态**: 已完成设计，即将实现

---

## 一、设计理念

### 1.1 核心目标

重新设计 Clock 3 应用的用户界面，打造一个**温柔、治愈、可爱**的桌面宠物闹钟应用，包含完整的番茄钟功能和经验值激励系统。

### 1.2 设计关键词

- **温柔** - 柔和的色彩，舒适的视觉体验
- **治愈** - 可爱的宠物表情，缓解压力
- **激励** - 经验值系统，激励持续专注
- **智能** - 自动计算奖励，灵活配置

---

## 二、色彩系统

### 2.1 主色调

| 色彩名称 | 色值 | 用途 |
|---------|------|------|
| **Primary Pink** | `#FFCDD2` | 主色调，柔和淡粉色 |
| **Coral Pink** | `#FF8A80` | 强调色，珊瑚粉 |
| **Soft Coral** | `#FFAB91` | 辅助强调色 |
| **Baby Pink** | `#F8BBD9` | 卡片背景色 |

### 2.2 中性色

| 色彩名称 | 色值 | 用途 |
|---------|------|------|
| **Cream White** | `#FFF8E7` | 页面背景色 |
| **Pure White** | `#FFFFFF` | 卡片背景色 |
| **Soft Gray** | `#F5F5F5` | 分割线背景 |
| **Text Primary** | `#424242` | 主要文字（深灰，非纯黑） |
| **Text Secondary** | `#757575` | 次要文字 |
| **Text Hint** | `#BDBDBD` | 提示文字 |

### 2.3 功能色

| 色彩名称 | 色值 | 用途 |
|---------|------|------|
| **Mint Blue** | `#81D4FA` | 番茄钟进度环 |
| **Mint Light** | `#B3E5FC` | 番茄钟辅助色 |
| **Success Green** | `#A5D6A7` | 成功/完成状态 |
| **Warning Orange** | `#FFCC80` | 警告状态 |
| **Error Red** | `#EF9A9A` | 错误状态 |

### 2.4 宠物心情色

| 心情状态 | 背景色 | 表情 |
|---------|--------|------|
| **开心 Happy** | `#FFCDD2` (淡粉) | 眯眼笑 |
| **专注 Focused** | `#81D4FA` (薄荷蓝) | 认真脸 |
| **休息 Resting** | `#A5D6A7` (浅绿) | 放松脸 |
| **兴奋 Excited** | `#FFCC80` (橙黄) | 大眼睛笑 |
| **饿了 Hungry** | `#B3E5FC` (浅蓝) | 可怜脸 |
| **无聊 Bored** | `#E0E0E0` (灰色) | 无表情 |

---

## 三、界面设计

### 3.1 主界面 (MainActivity)

```
┌─────────────────────────────────┐
│  Clock 3           🐾Lv.5 120✨│  ← 标题 + 宠物等级 + 经验
│                                 │
│         07:30:45               │  ← 当前时间
│                                 │
│        ┌──────────┐            │
│        │   😊     │            │  ← 宠物圆形视图
│        │  圆形    │            │
│        │  色块    │            │
│        └──────────┘            │
│                                 │
│      下一闹钟: 08:30           │
│                                 │
│  ┌─────────┐ ┌─────────┐      │
│  │  ⏰闹钟  │ │ 🍅番茄  │      │
│  └─────────┘ └─────────┘      │
│  ┌─────────┐ ┌─────────┐      │
│  │  🐶宠物  │ │  ⏱️倒计时│      │
│  └─────────┘ └─────────┘      │
│  ┌─────────────────────┐      │
│  │      🐾 悬浮球      │      │
│  └─────────────────────┘      │
│                                 │
│       导入 / 导出              │
└─────────────────────────────────┘
```

### 3.2 番茄钟界面 (PomodoroActivity)

```
┌─────────────────────────────────┐
│  ← 返回      🍅番茄钟      ⚙️设置 │
│                                 │
│       📝 事件: 读书             │  ← 当前事件输入框
│                                 │
│         ┌───────────┐          │
│         │           │          │
│         │   25:00   │          │  ← 大号倒计时
│         │  [进度环]  │          │
│         │   😊专注   │          │  ← 宠物表情
│         │           │          │
│         └───────────┘          │
│                                 │
│      今日: 4 🍅  经验: +40     │
│                                 │
│  ┌─────┐ ┌─────┐ ┌─────────┐  │
│  │ 跳过 │ │暂停/ │ │ 保存    │  │
│  │     │ │开始  │ │ 配置    │  │
│  └─────┘ └─────┘ └─────────┘  │
│                                 │
│      ┌─────────────────┐       │
│      │  💰 经验商店     │       │
│      └─────────────────┘       │
└─────────────────────────────────┘
```

### 3.3 宠物圆形视图设计

```
┌────────────────────────┐
│                        │
│    ┌──────────────┐    │
│    │              │    │
│    │   ◉     ◉   │    │  ← 眼睛 (两个小圆)
│    │              │    │
│    │      ◡      │    │  ← 嘴巴 (微笑弧线)
│    │              │    │
│    └──────────────┘    │
│         120dp           │
│                        │
└────────────────────────┘
```

---

## 四、番茄钟功能模块

### 4.1 数据结构

```kotlin
// 番茄钟配置模板
data class PomodoroTemplate(
    val id: String,
    val name: String,              // 模板名称，如"读书"、"工作"
    val workDuration: Int,          // 工作时长（分钟）
    val shortBreakDuration: Int,    // 短休息时长（分钟）
    val longBreakDuration: Int,     // 长休息时长（分钟）
    val isDefault: Boolean = false  // 是否为默认模板
)

// 番茄钟记录
data class PomodoroRecord(
    val id: String,
    val templateId: String,        // 关联的配置模板ID
    val eventName: String,         // 当前事件名称
    val startTime: Long,           // 开始时间戳
    val endTime: Long,             // 结束时间戳
    val actualDuration: Int,       // 实际专注时长（分钟）
    val expEarned: Int,            // 获得的经验值
    val completed: Boolean         // 是否完成
)

// 番茄钟状态
data class PomodoroState(
    val isRunning: Boolean = false,
    val currentPhase: PomodoroPhase = PomodoroPhase.IDLE,
    val remainingSeconds: Int = 0,
    val completedPomodoros: Int = 0,
    val currentEvent: String = "",  // 当前事件
    val currentTemplateId: String = ""  // 当前使用的模板
)

enum class PomodoroPhase {
    IDLE, WORKING, SHORT_BREAK, LONG_BREAK
}
```

### 4.2 智能经验计算

```kotlin
object ExpCalculator {
    
    // 基础公式：经验 = floor(实际专注时长分钟数 × 2)
    // 例如：25分钟 → +50经验，50分钟 → +100经验
    
    // 额外奖励：
    // - 连续完成4个番茄：+20额外经验
    // - 完成100%专注（无跳过）：+10奖励
    // - 事件名称完整：+5奖励
    
    fun calculateExp(actualMinutes: Int, completedFully: Boolean = true, hasEvent: Boolean = true): Int {
        var exp = actualMinutes * 2  // 基础经验
        
        if (completedFully) {
            exp += 10  // 完整完成奖励
        }
        
        if (hasEvent) {
            exp += 5   // 有事件名称奖励
        }
        
        return exp
    }
    
    // 长休息奖励
    fun calculateLongBreakBonus(consecutivePomodoros: Int): Int {
        return if (consecutivePomodoros % 4 == 0) 20 else 0
    }
}
```

### 4.3 经验商店

```kotlin
object ExpShop {
    
    // 可用物品
    data class ShopItem(
        val id: String,
        val name: String,
        val description: String,
        val cost: Int,
        val type: ItemType,
        val unlockValue: String? = null  // 解锁的皮肤ID或奖励内容
    )
    
    enum class ItemType {
        THEME,      // 主题皮肤
        BREAK_TIME, // 休息时间
        REWARD      // 自定义奖励
    }
    
    // 预定义商店物品
    val defaultItems = listOf(
        // 主题皮肤
        ShopItem("theme_1", "蓝色天空", "解锁蓝色主题", 100, ItemType.THEME, "sky_blue"),
        ShopItem("theme_2", "森林绿", "解锁绿色主题", 100, ItemType.THEME, "forest_green"),
        ShopItem("theme_3", "浪漫紫", "解锁紫色主题", 150, ItemType.THEME, "romantic_purple"),
        
        // 休息时间
        ShopItem("break_15", "15分钟休息券", "可兑换15分钟休息", 80, ItemType.BREAK_TIME, "15"),
        ShopItem("break_30", "30分钟休息券", "可兑换30分钟休息", 150, ItemType.BREAK_TIME, "30"),
        
        // 自定义奖励
        ShopItem("reward_coffee", "一杯咖啡", "自定义奖励", 200, ItemType.REWARD, "coffee"),
        ShopItem("reward_game", "游戏时间30分钟", "自定义奖励", 300, ItemType.REWARD, "game_30")
    )
}
```

### 4.4 宠物升级和奖励系统

```kotlin
object PetUpgradeSystem {
    
    // 宠物等级数据
    data class PetLevel(
        val level: Int,
        val expRequired: Int,       // 升级所需经验
        val title: String,         // 等级称号
        val reward: String?        // 升级奖励描述
    )
    
    // 等级列表
    val levels = listOf(
        PetLevel(1, 0, "新手", null),
        PetLevel(2, 100, "学徒", "解锁新表情"),
        PetLevel(3, 250, "练习生", "解锁新颜色"),
        PetLevel(4, 500, "熟手", "解锁背景装饰"),
        PetLevel(5, 800, "专家", "解锁特效动画"),
        // ... 更多等级
    )
    
    // 解锁的主题皮肤
    data class ThemeSkin(
        val id: String,
        val name: String,
        val primaryColor: String,
        val secondaryColor: String,
        val petBackground: String,
        val unlocked: Boolean = false
    )
}
```

---

## 五、功能流程

### 5.1 番茄钟使用流程

```
1. 选择或创建配置模板
   ├─ 选择已有模板（读书/工作/运动等）
   └─ 创建新模板

2. 输入当前事件
   └─ 例如："完成第一章阅读"

3. 开始专注
   ├─ 倒计时开始
   ├─ 宠物显示专注表情
   └─ 进度环显示进度

4. 专注完成
   ├─ 通知提醒
   ├─ 宠物显示开心表情
   ├─ 计算经验奖励
   └─ 更新宠物经验值

5. 休息阶段
   ├─ 自动进入休息
   ├─ 宠物显示休息表情
   └─ 休息完成后返回步骤3或结束
```

### 5.2 经验商店使用流程

```
1. 点击经验商店按钮
   └─ 进入商店界面

2. 查看可用物品
   ├─ 主题皮肤
   ├─ 休息券
   └─ 自定义奖励

3. 选择物品
   ├─ 查看物品详情
   └─ 查看所需经验

4. 兑换物品
   ├─ 验证经验是否足够
   ├─ 扣除经验
   └─ 解锁/获得物品
```

---

## 六、技术实现

### 6.1 需要创建/修改的文件

#### 布局文件
- `res/layout/activity_main.xml` - 重构主界面
- `res/layout/activity_pomodoro.xml` - 番茄钟界面
- `res/layout/activity_shop.xml` - 经验商店
- `res/layout/widget_circle_pet.xml` - 宠物视图
- `res/layout/item_pomodoro_template.xml` - 模板项
- `res/layout/item_shop.xml` - 商店项

#### Kotlin 文件
- `ui/MainActivity.kt` - 重构主界面
- `ui/PomodoroActivity.kt` - 番茄钟界面
- `ui/ShopActivity.kt` - 经验商店
- `widget/CirclePetView.kt` - 自定义宠物视图
- `service/PomodoroService.kt` - 番茄钟计时服务
- `model/PomodoroTemplate.kt` - 配置模板模型
- `model/PomodoroRecord.kt` - 番茄钟记录
- `model/ShopItem.kt` - 商店物品模型
- `repository/PomodoroRepository.kt` - 番茄钟数据仓库
- `utils/ExpCalculator.kt` - 经验计算工具
- `utils/PetUpgradeSystem.kt` - 宠物升级系统

#### 资源文件
- `res/values/colors.xml` - 更新颜色
- `res/values/themes.xml` - 更新主题
- `res/values/strings.xml` - 添加新字符串

### 6.2 实现步骤

**Phase 1: UI 基础重构**
1. 更新颜色系统
2. 重构主界面布局
3. 创建圆形宠物视图

**Phase 2: 番茄钟功能**
1. 创建番茄钟数据模型
2. 实现番茄钟界面
3. 实现计时服务
4. 添加配置模板功能

**Phase 3: 经验系统**
1. 实现经验计算
2. 创建经验商店界面
3. 实现宠物升级系统
4. 添加奖励兑换功能

**Phase 4: 测试优化**
1. 构建 APK
2. 真实环境测试
3. 截图记录结果

---

## 七、测试清单

### 7.1 UI 测试
- [ ] 主界面显示正确
- [ ] 宠物视图显示正常
- [ ] 颜色配置正确
- [ ] 按钮样式符合设计

### 7.2 功能测试
- [ ] 番茄钟计时准确
- [ ] 经验计算正确
- [ ] 配置模板保存成功
- [ ] 商店兑换功能正常
- [ ] 宠物升级系统正常

### 7.3 真实环境测试
- [ ] APK 安装成功
- [ ] 所有界面显示正常
- [ ] 所有功能可用
- [ ] 截图记录完成

---

## 八、总结

本设计将 Clock 3 打造成一个**温柔治愈 + 激励专注**的桌面宠物闹钟应用：

- 🎨 **温柔粉彩色系** - 淡粉 + 珊瑚色主调
- 🐾 **可爱圆形宠物** - 表情随心情变化
- 🍅 **智能番茄钟** - 自动计算经验，支持事件和模板
- 💰 **经验商店** - 解锁主题、兑换休息、自定义奖励
- ✨ **升级系统** - 宠物升级解锁更多内容
- 📱 **现代化 UI** - 卡片式设计，简洁大方

---

**文档版本**: v2.1
**最后更新**: 2026-05-17
**状态**: 准备实现
