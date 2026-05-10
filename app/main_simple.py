#!/usr/bin/env python3
import kivy
kivy.require('2.3.0')

from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.core.window import Window
from kivy.clock import Clock
from kivy.uix.popup import Popup
from datetime import datetime
import sys
import os


class Clock3App(App):
    """主应用程序"""

    def build(self):
        # 设置窗口背景
        Window.clearcolor = (0.2, 0.4, 0.8, 1)
        
        # 创建主布局
        layout = BoxLayout(orientation='vertical', padding=20, spacing=20)
        
        # 时钟显示
        self.time_label = Label(
            text="Loading...",
            font_size=48,
            size_hint_y=0.3
        )
        layout.add_widget(self.time_label)
        
        # 日期显示
        self.date_label = Label(
            text="",
            font_size=24,
            size_hint_y=0.2
        )
        layout.add_widget(self.date_label)
        
        # 状态显示
        self.status_label = Label(
            text="Clock 3 - Ready",
            font_size=16,
            color=(0.9, 0.9, 0.9, 1),
            size_hint_y=0.2
        )
        layout.add_widget(self.status_label)
        
        # 按钮
        button_layout = BoxLayout(orientation='horizontal', spacing=10, size_hint_y=0.3)
        
        btn1 = Button(text='Info', on_press=self.show_info)
        btn2 = Button(text='Test', on_press=self.show_test)
        
        button_layout.add_widget(btn1)
        button_layout.add_widget(btn2)
        layout.add_widget(button_layout)
        
        # 更新时间
        Clock.schedule_interval(self.update_time, 1)
        
        return layout
    
    def update_time(self, dt):
        """更新时间显示"""
        now = datetime.now()
        self.time_label.text = now.strftime('%H:%M:%S')
        self.date_label.text = now.strftime('%Y-%m-%d')
    
    def show_info(self, instance):
        """显示信息弹窗"""
        content = BoxLayout(orientation='vertical', padding=20)
        content.add_widget(Label(text='Clock 3 - 1.0.0', font_size=24))
        content.add_widget(Label(text='Kivy + Python', font_size=18))
        
        popup = Popup(
            title='About',
            content=content,
            size_hint=(0.8, 0.6)
        )
        popup.open()
    
    def show_test(self, instance):
        """测试功能"""
        self.status_label.text = "Test button pressed!"


if __name__ == "__main__":
    Clock3App().run()
