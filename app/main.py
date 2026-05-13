import kivy
kivy.require('2.3.0')

from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.core.window import Window
from kivy.clock import Clock
from datetime import datetime
import sys
import logging
import os

# 设置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 尝试导入可选模块
try:
    from plyer import notification
    PLYER_AVAILABLE = True
except Exception as e:
    logger.warning(f"Plyer not available: {e}")
    PLYER_AVAILABLE = False

class Clock3App(App):
    """Clock 3 主应用 - 简化但健壮的版本"""
    
    def build(self):
        """构建应用界面"""
        try:
            Window.clearcolor = (0.15, 0.25, 0.4, 1)
            
            # 创建主布局
            layout = BoxLayout(orientation='vertical', padding=20, spacing=20)
            
            # 标题
            title = Label(
                text='🕐 Clock 3',
                font_size=36,
                size_hint_y=0.15
            )
            layout.add_widget(title)
            
            # 时钟显示
            self.time_label = Label(
                text='00:00:00',
                font_size=48,
                size_hint_y=0.25
            )
            layout.add_widget(self.time_label)
            
            # 日期显示
            self.date_label = Label(
                text='',
                font_size=24,
                size_hint_y=0.15
            )
            layout.add_widget(self.date_label)
            
            # 状态显示
            self.status_label = Label(
                text='应用已启动',
                font_size=16,
                color=(0.9, 0.9, 0.9, 1),
                size_hint_y=0.15
            )
            layout.add_widget(self.status_label)
            
            # 按钮区域
            btn_layout = BoxLayout(orientation='horizontal', spacing=10, size_hint_y=0.2)
            
            test_btn = Button(
                text='测试通知',
                background_color=(0.2, 0.6, 0.3, 1)
            )
            test_btn.bind(on_press=self.test_notification)
            btn_layout.add_widget(test_btn)
            
            info_btn = Button(
                text='关于',
                background_color=(0.3, 0.5, 0.7, 1)
            )
            info_btn.bind(on_press=self.show_info)
            btn_layout.add_widget(info_btn)
            
            layout.add_widget(btn_layout)
            
            # 开始更新时间
            Clock.schedule_interval(self.update_time, 1.0)
            
            logger.info("应用界面构建成功")
            return layout
        except Exception as e:
            logger.error(f"构建界面失败: {e}")
            return self.build_error_ui(str(e))
    
    def build_error_ui(self, error_msg):
        """构建错误界面"""
        layout = BoxLayout(orientation='vertical', padding=20, spacing=20)
        layout.add_widget(Label(text='应用启动错误', font_size=24, color=(1, 0.3, 0.3, 1)))
        layout.add_widget(Label(text=error_msg, font_size=16))
        return layout
    
    def update_time(self, dt):
        """更新时间显示"""
        try:
            now = datetime.now()
            self.time_label.text = now.strftime('%H:%M:%S')
            self.date_label.text = now.strftime('%Y年%m月%d日 %A')
        except Exception as e:
            logger.error(f"更新时间失败: {e}")
    
    def test_notification(self, instance):
        """测试通知功能"""
        try:
            if PLYER_AVAILABLE:
                notification.notify(
                    title='Clock 3 通知',
                    message='测试通知成功！',
                    timeout=10
                )
                self.status_label.text = '通知已发送'
                logger.info("测试通知已发送")
            else:
                self.status_label.text = '通知功能不可用'
        except Exception as e:
            logger.error(f"发送通知失败: {e}")
            self.status_label.text = f'发送失败: {str(e)}'
    
    def show_info(self, instance):
        """显示关于信息"""
        self.status_label.text = 'Clock 3 v1.0.0 - 简单但可靠的时钟应用'
    
    def on_start(self):
        """应用启动完成"""
        logger.info("应用已启动")
        self.status_label.text = '应用已就绪'
    
    def on_stop(self):
        """应用停止"""
        logger.info("应用已停止")

if __name__ == "__main__":
    try:
        Clock3App().run()
    except Exception as e:
        logger.critical(f"应用崩溃: {e}")
        sys.exit(1)
