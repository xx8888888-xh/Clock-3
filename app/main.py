import kivy
kivy.require('2.3.0')

from kivy.app import App
from kivy.uix.screenmanager import ScreenManager, Screen
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.floatlayout import FloatLayout
from kivy.uix.popup import Popup
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.uix.textinput import TextInput
from kivy.uix.slider import Slider
from kivy.uix.switch import Switch
from kivy.uix.checkbox import CheckBox
from kivy.uix.scrollview import ScrollView
from kivy.uix.gridlayout import GridLayout
from kivy.uix.spinner import Spinner
from kivy.clock import Clock
from kivy.properties import NumericProperty, BooleanProperty, StringProperty, ListProperty
from kivy.graphics import Color, Ellipse, Rectangle
from kivy.core.window import Window
from kivy.core.audio import SoundLoader
from kivy.uix.widget import Widget
from kivy.animation import Animation
from plyer import notification
from plyer import vibrator
import threading
import time
import json
import os
from datetime import datetime, timedelta
from app.config import config
from app.database import db
from app.models.alarm import Alarm
from app.models.countdown import Countdown
from app.models.pet import Pet
from app.services.alarm_service import alarm_service
from app.services.countdown_service import countdown_service
from app.services.pet_service import pet_service

class PetWidget(Widget):
    """悬浮球宠物组件"""
    
    size_hint = NumericProperty(0.15)
    is_dragging = BooleanProperty(False)
    last_touch_time = 0
    touch_count = 0
    
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.pos_hint = {'x': config.get('pet_x', 0.85), 'y': config.get('pet_y', 0.75)}
        self.size_hint = (config.get('pet_size', 100) / Window.width, config.get('pet_size', 100) / Window.height)
        pet_service.load_pet()
        self.update_mood()
        self.start_animations()
        self.touch_timer = None
    
    def on_touch_down(self, touch):
        if self.collide_point(*touch.pos):
            self.is_dragging = True
            touch_count = self.check_touch_count()
            
            if touch_count == 1:
                Clock.schedule_once(lambda dt: self.single_tap(), 0.3)
            elif touch_count == 2:
                Clock.unschedule(self.single_tap)
                self.double_tap()
            elif touch_count == 3:
                Clock.unschedule(self.single_tap)
                self.triple_tap()
            
            return True
        return super().on_touch_down(touch)
    
    def on_touch_move(self, touch):
        if self.is_dragging and self.collide_point(*touch.pos):
            new_x = touch.x / Window.width
            new_y = touch.y / Window.height
            
            new_x = max(0, min(0.85, new_x))
            new_y = max(0, min(0.85, new_y))
            
            self.pos_hint = {'x': new_x, 'y': new_y}
            config.set('pet_x', new_x)
            config.set('pet_y', new_y)
            return True
        return super().on_touch_move(touch)
    
    def on_touch_up(self, touch):
        if self.is_dragging:
            self.is_dragging = False
            pet_service.interact()
            self.update_mood()
        return super().on_touch_up(touch)
    
    def check_touch_count(self):
        current_time = time.time()
        if current_time - self.last_touch_time < 0.5:
            self.touch_count += 1
        else:
            self.touch_count = 1
        self.last_touch_time = current_time
        return self.touch_count
    
    def single_tap(self):
        if self.touch_count == 1:
            self.show_main_menu()
    
    def double_tap(self):
        app = App.get_running_app()
        app.show_alarms()
    
    def triple_tap(self):
        app = App.get_running_app()
        app.show_countdown()
    
    def start_animations(self):
        def float_animation(dt):
            if not self.is_dragging and not pet_service.is_sleeping():
                anim = Animation(y=self.pos_hint['y'] + 0.02, duration=1)
                anim += Animation(y=self.pos_hint['y'], duration=1)
                anim.start(self)
        
        Clock.schedule_interval(float_animation, 2)
    
    def update_mood(self):
        status = pet_service.get_status()
        self.mood = status.get('mood', 'happy')
        self.emoji = status.get('emoji', '😊')
    
    def show_main_menu(self):
        app = App.get_running_app()
        app.show_main_menu()

class FloatingPetApp(App):
    """主应用"""
    
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.main_menu_popup = None
        self.alarms_popup = None
        self.add_alarm_popup = None
        self.countdown_popup = None
        self.pet_popup = None
        self.settings_popup = None
        self.alarm_popup = None
        self.active_popup = None
    
    def build(self):
        Window.clearcolor = (0.95, 0.95, 0.95, 1)
        
        self.root_widget = FloatLayout()
        
        self.pet = PetWidget()
        self.root_widget.add_widget(self.pet)
        
        self.status_label = Label(
            text='Clock 3 已启动',
            size_hint=(1, 0.05),
            pos_hint={'x': 0, 'y': 0.95},
            font_size=14,
            color=(0.3, 0.3, 0.3, 1)
        )
        self.root_widget.add_widget(self.status_label)
        
        alarm_service.load_alarms()
        alarm_service.start_checking()
        alarm_service.on_alarm_trigger(self.on_alarm_trigger)
        
        countdown_service.load_countdowns()
        countdown_service.start_checking()
        countdown_service.on_countdown_complete(self.on_countdown_complete)
        
        Clock.schedule_interval(self.update_clock, 1)
        
        return self.root_widget
    
    def on_start(self):
        self.update_status()
    
    def update_clock(self, dt):
        current_time = datetime.now().strftime('%H:%M:%S')
        next_alarm = alarm_service.get_next_alarm()
        next_alarm_text = f" | 下一个: {next_alarm.label} @ {next_alarm.time}" if next_alarm else ""
        self.status_label.text = f'{current_time}{next_alarm_text}'
        
        if pet_service.is_sleeping() and self.pet.mood != 'sleepy':
            self.pet.update_mood()
    
    def on_alarm_trigger(self, alarm):
        self.show_alarm_popup(alarm)
        self.trigger_notification(alarm)
    
    def on_countdown_complete(self, countdown):
        self.show_countdown_complete_popup(countdown)
    
    def trigger_notification(self, alarm):
        try:
            notification.notify(
                title=f'⏰ 闹钟: {alarm.label}',
                message=alarm.content or '时间到了!',
                timeout=10
            )
            if config.get('vibration_enabled', True):
                vibrator.vibrate(2)
            if config.get('sound_enabled', True):
                self.play_alarm_sound()
        except Exception as e:
            print(f"通知失败: {e}")
    
    def play_alarm_sound(self):
        try:
            from kivy.core.audio import SoundLoader
            sound = SoundLoader.load('assets/alarm.mp3')
            if sound:
                sound.loop = True
                sound.play()
        except Exception as e:
            print(f"播放声音失败: {e}")
    
    def show_main_menu(self):
        content = BoxLayout(orientation='vertical', padding=20, spacing=10)
        
        title = Label(
            text='🐾 桌面宠物闹钟',
            font_size=24,
            size_hint_y=0.2,
            color=(0.2, 0.2, 0.2, 1)
        )
        content.add_widget(title)
        
        buttons = [
            ('⏰ 闹钟列表', self.show_alarms),
            ('➕ 新建闹钟', self.show_add_alarm),
            ('⏱️ 倒计时', self.show_countdown),
            ('🐶 宠物状态', self.show_pet_status),
            ('⚙️ 设置', self.show_settings),
            ('📤 导出数据', self.export_data),
            ('📥 导入数据', self.import_data),
            ('❌ 关闭', self.dismiss_popup),
        ]
        
        for text, callback in buttons:
            btn = Button(
                text=text,
                size_hint_y=0.1,
                background_color=(0.3, 0.5, 0.8, 1),
                color=(1, 1, 1, 1)
            )
            btn.bind(on_press=lambda b, cb=callback: cb())
            content.add_widget(btn)
        
        self.main_menu_popup = Popup(
            title='Clock 3',
            content=content,
            size_hint=(0.9, 0.7),
            auto_dismiss=True
        )
        self.main_menu_popup.open()
    
    def show_alarms(self):
        self.dismiss_popup()
        
        alarms = alarm_service.alarms
        
        content = BoxLayout(orientation='vertical', padding=10, spacing=10)
        
        if not alarms:
            content.add_widget(Label(text='暂无闹钟', font_size=16))
        else:
            for alarm in alarms:
                alarm_layout = BoxLayout(orientation='horizontal', size_hint_y=None, height=50)
                
                alarm_info = Label(
                    text=f"{alarm.time} - {alarm.label}\n{alarm.content or ''}",
                    font_size=14,
                    halign='left',
                    valign='middle'
                )
                alarm_layout.add_widget(alarm_info)
                
                switch = Switch(active=alarm.enabled, size_hint_x=0.2)
                switch.bind(active=lambda s, a, al=alarm: self.toggle_alarm(al, s.active))
                alarm_layout.add_widget(switch)
                
                delete_btn = Button(text='🗑️', size_hint_x=0.15, background_color=(0.8, 0.2, 0.2, 1))
                delete_btn.bind(on_press=lambda b, al=alarm: self.delete_alarm(al))
                alarm_layout.add_widget(delete_btn)
                
                content.add_widget(alarm_layout)
        
        close_btn = Button(text='➕ 新建闹钟', size_hint_y=0.1, background_color=(0.2, 0.7, 0.3, 1))
        close_btn.bind(on_press=lambda b: self.show_add_alarm())
        content.add_widget(close_btn)
        
        back_btn = Button(text='← 返回', size_hint_y=0.1)
        back_btn.bind(on_press=lambda b: (self.dismiss_popup(), self.show_main_menu()))
        content.add_widget(back_btn)
        
        self.alarms_popup = Popup(title='闹钟列表', content=content, size_hint=(0.95, 0.8))
        self.alarms_popup.open()
    
    def show_add_alarm(self):
        self.dismiss_popup()
        
        content = BoxLayout(orientation='vertical', padding=20, spacing=15)
        
        content.add_widget(Label(text='新建闹钟', font_size=20, size_hint_y=0.1, color=(0.2, 0.2, 0.2, 1)))
        
        label_input = TextInput(hint_text='闹钟名称', multiline=False, size_hint_y=0.1)
        content.add_widget(label_input)
        
        content_input = TextInput(hint_text='提醒内容', multiline=False, size_hint_y=0.1)
        content.add_widget(content_input)
        
        time_layout = BoxLayout(orientation='horizontal', size_hint_y=0.15)
        hour_spinner = Spinner(text='08', values=[f'{i:02d}' for i in range(24)], size_hint_x=0.3)
        minute_spinner = Spinner(text='00', values=[f'{i:02d}' for i in range(60)], size_hint_x=0.3)
        time_layout.add_widget(Label(text='时间: ', size_hint_x=0.2))
        time_layout.add_widget(hour_spinner)
        time_layout.add_widget(Label(text=':'))
        time_layout.add_widget(minute_spinner)
        content.add_widget(time_layout)
        
        repeat_layout = BoxLayout(orientation='vertical', size_hint_y=0.25)
        repeat_layout.add_widget(Label(text='重复:', size_hint_y=0.3))
        repeat_types = ['once', 'daily', 'workdays', 'weekend', 'custom']
        repeat_names = ['一次', '每天', '工作日', '周末', '自定义']
        repeat_spinner = Spinner(text='一次', values=repeat_names, size_hint_y=0.7)
        repeat_layout.add_widget(repeat_spinner)
        content.add_widget(repeat_layout)
        
        save_btn = Button(text='💾 保存', size_hint_y=0.1, background_color=(0.2, 0.7, 0.3, 1))
        save_btn.bind(on_press=lambda b: self.save_alarm(
            label_input.text or '闹钟',
            content_input.text,
            f"{hour_spinner.text}:{minute_spinner.text}",
            repeat_types[repeat_names.index(repeat_spinner.text)]
        ))
        content.add_widget(save_btn)
        
        back_btn = Button(text='← 返回', size_hint_y=0.1)
        back_btn.bind(on_press=lambda b: (self.dismiss_popup(), self.show_main_menu()))
        content.add_widget(back_btn)
        
        self.add_alarm_popup = Popup(title='新建闹钟', content=content, size_hint=(0.9, 0.85))
        self.add_alarm_popup.open()
    
    def save_alarm(self, label, content, time, repeat_type):
        alarm = Alarm(
            label=label,
            content=content,
            time=time,
            repeat_type=repeat_type,
            enabled=True
        )
        alarm_service.add_alarm(alarm)
        self.dismiss_popup()
        self.show_alarms()
    
    def toggle_alarm(self, alarm, enabled):
        alarm.enabled = enabled
        alarm_service.update_alarm(alarm)
    
    def delete_alarm(self, alarm):
        alarm_service.delete_alarm(alarm.id)
        self.show_alarms()
    
    def show_countdown(self):
        self.dismiss_popup()
        
        content = BoxLayout(orientation='vertical', padding=20, spacing=15)
        
        content.add_widget(Label(text='⏱️ 倒计时', font_size=20, size_hint_y=0.1, color=(0.2, 0.2, 0.2, 1)))
        
        self.countdown_display = Label(
            text='00:00',
            font_size=48,
            size_hint_y=0.3,
            color=(0.2, 0.4, 0.8, 1)
        )
        content.add_widget(self.countdown_display)
        
        time_input_layout = BoxLayout(orientation='horizontal', size_hint_y=0.15)
        minute_input = TextInput(hint_text='分', multiline=False, input_filter='int', size_hint_x=0.3)
        second_input = TextInput(hint_text='秒', multiline=False, input_filter='int', size_hint_x=0.3)
        label_input = TextInput(hint_text='标签', multiline=False, size_hint_x=0.4)
        time_input_layout.add_widget(minute_input)
        time_input_layout.add_widget(second_input)
        time_input_layout.add_widget(label_input)
        content.add_widget(time_input_layout)
        
        btn_layout = BoxLayout(orientation='horizontal', size_hint_y=0.15)
        start_btn = Button(text='▶️ 开始', background_color=(0.2, 0.7, 0.3, 1))
        pause_btn = Button(text='⏸️ 暂停')
        reset_btn = Button(text='🔄 重置', background_color=(0.8, 0.6, 0.2, 1))
        
        self.current_countdown = None
        self.is_counting = False
        
        def start_countdown(b):
            try:
                minutes = int(minute_input.text or 0)
                seconds = int(second_input.text or 0)
                total_seconds = minutes * 60 + seconds
                
                if total_seconds > 0:
                    self.current_countdown = countdown_service.create_countdown(
                        label_input.text or '倒计时',
                        total_seconds
                    )
                    self.is_counting = True
                    self.countdown_event = Clock.schedule_interval(self.update_countdown_display, 0.1)
            except ValueError:
                pass
        
        def pause_countdown(b):
            if self.current_countdown:
                countdown_service.pause_countdown(self.current_countdown.id)
                self.is_counting = False
        
        def reset_countdown(b):
            if self.current_countdown:
                countdown_service.delete_countdown(self.current_countdown.id)
                self.current_countdown = None
                self.countdown_display.text = '00:00'
                self.is_counting = False
        
        start_btn.bind(on_press=start_countdown)
        pause_btn.bind(on_press=pause_countdown)
        reset_btn.bind(on_press=reset_countdown)
        
        btn_layout.add_widget(start_btn)
        btn_layout.add_widget(pause_btn)
        btn_layout.add_widget(reset_btn)
        content.add_widget(btn_layout)
        
        back_btn = Button(text='← 返回', size_hint_y=0.1)
        back_btn.bind(on_press=lambda b: (self.dismiss_popup(), self.show_main_menu()))
        content.add_widget(back_btn)
        
        self.countdown_popup = Popup(title='倒计时', content=content, size_hint=(0.9, 0.8))
        self.countdown_popup.open()
    
    def update_countdown_display(self, dt):
        if self.current_countdown:
            for cd in countdown_service.countdowns:
                if cd.id == self.current_countdown.id:
                    self.current_countdown = cd
                    self.countdown_display.text = cd.get_formatted_time()
                    if cd.status == 'completed':
                        self.is_counting = False
                    break
    
    def show_countdown_complete_popup(self, countdown):
        content = BoxLayout(orientation='vertical', padding=20)
        content.add_widget(Label(text=f'🎉 {countdown.label} 完成!', font_size=24))
        
        close_btn = Button(text='确定', size_hint_y=0.3)
        close_btn.bind(on_press=lambda b: self.dismiss_popup())
        content.add_widget(close_btn)
        
        popup = Popup(
            title='倒计时结束',
            content=content,
            size_hint=(0.8, 0.4),
            auto_dismiss=False
        )
        popup.open()
        self.active_popup = popup
    
    def show_alarm_popup(self, alarm):
        content = BoxLayout(orientation='vertical', padding=20)
        content.add_widget(Label(text=f'⏰ {alarm.label}', font_size=24))
        content.add_widget(Label(text=alarm.content or '时间到了!', font_size=16))
        
        btn_layout = BoxLayout(orientation='horizontal', size_hint_y=0.3)
        
        snooze_btn = Button(text='💤 贪睡', background_color=(0.6, 0.6, 0.6, 1))
        dismiss_btn = Button(text='✓ 关闭', background_color=(0.2, 0.7, 0.3, 1))
        
        def snooze(b):
            alarm_service.snooze_alarm(config.get('snooze_duration', 5))
            self.dismiss_popup()
        
        def dismiss(b):
            alarm_service.dismiss_alarm()
            self.dismiss_popup()
        
        snooze_btn.bind(on_press=snooze)
        dismiss_btn.bind(on_press=dismiss)
        
        btn_layout.add_widget(snooze_btn)
        btn_layout.add_widget(dismiss_btn)
        content.add_widget(btn_layout)
        
        self.alarm_popup = Popup(
            title=f'闹钟 {alarm.time}',
            content=content,
            size_hint=(0.85, 0.5),
            auto_dismiss=False
        )
        self.alarm_popup.open()
        self.active_popup = self.alarm_popup
    
    def show_pet_status(self):
        self.dismiss_popup()
        
        status = pet_service.get_status()
        
        content = BoxLayout(orientation='vertical', padding=20, spacing=15)
        
        content.add_widget(Label(
            text=f"{status.get('emoji', '😊')} {status.get('name', '宠物')}",
            font_size=28,
            size_hint_y=0.15,
            color=(0.2, 0.2, 0.2, 1)
        ))
        
        content.add_widget(Label(
            text=f"心情: {status.get('mood', 'happy')} | 等级: {status.get('level', 1)}",
            font_size=16,
            size_hint_y=0.1
        ))
        
        exp_bar = Label(
            text=f"经验: {status.get('exp', 0)}/{status.get('exp_needed', 100)}",
            font_size=14,
            size_hint_y=0.1
        )
        content.add_widget(exp_bar)
        
        content.add_widget(Label(
            text=f"交互次数: {status.get('total_interactions', 0)}",
            font_size=14,
            size_hint_y=0.1
        ))
        
        content.add_widget(Label(
            text=status.get('message', pet_service.pet.get_random_message() if pet_service.pet else '你好!'),
            font_size=16,
            size_hint_y=0.2,
            color=(0.3, 0.3, 0.8, 1)
        ))
        
        btn_layout = BoxLayout(orientation='horizontal', size_hint_y=0.2)
        
        interact_btn = Button(text='😊 互动', background_color=(0.3, 0.5, 0.8, 1))
        interact_btn.bind(on_press=lambda b: self.interact_with_pet())
        btn_layout.add_widget(interact_btn)
        
        feed_btn = Button(text='🍖 喂食', background_color=(0.8, 0.6, 0.2, 1))
        feed_btn.bind(on_press=lambda b: self.feed_pet())
        btn_layout.add_widget(feed_btn)
        
        play_btn = Button(text='🎮 玩耍', background_color=(0.2, 0.7, 0.3, 1))
        play_btn.bind(on_press=lambda b: self.play_with_pet())
        btn_layout.add_widget(play_btn)
        
        content.add_widget(btn_layout)
        
        back_btn = Button(text='← 返回', size_hint_y=0.1)
        back_btn.bind(on_press=lambda b: (self.dismiss_popup(), self.show_main_menu()))
        content.add_widget(back_btn)
        
        self.pet_popup = Popup(title='宠物状态', content=content, size_hint=(0.9, 0.8))
        self.pet_popup.open()
    
    def interact_with_pet(self):
        result = pet_service.interact()
        self.show_pet_status()
    
    def feed_pet(self):
        result = pet_service.feed_pet()
        self.show_pet_status()
    
    def play_with_pet(self):
        result = pet_service.play_with_pet()
        self.show_pet_status()
    
    def show_settings(self):
        self.dismiss_popup()
        
        content = ScrollView(size_hint=(1, 1))
        layout = GridLayout(cols=1, padding=20, spacing=15, size_hint_y=None)
        layout.bind(minimum_height=layout.setter('height'))
        
        layout.add_widget(Label(text='⚙️ 设置', font_size=20, size_hint_y=None, height=40, color=(0.2, 0.2, 0.2, 1)))
        
        settings_items = [
            ('宠物大小', 'pet_size', 50, 200),
            ('宠物透明度', 'pet_opacity', 0.1, 1.0),
            ('贪睡时长(分钟)', 'snooze_duration', 1, 10),
            ('最大贪睡次数', 'max_snooze_count', 1, 5),
            ('通知显示时间(秒)', 'notification_duration', 3, 15),
        ]
        
        for label, key, min_val, max_val in settings_items:
            item_layout = BoxLayout(orientation='vertical', size_hint_y=None, height=80)
            item_layout.add_widget(Label(text=f'{label}: {config.get(key)}', size_hint_y=0.4, halign='left'))
            slider = Slider(min=min_val, max=max_val, value=config.get(key), size_hint_y=0.6)
            slider.bind(value=lambda s, v, k=key: self.update_setting(k, v))
            item_layout.add_widget(slider)
            layout.add_widget(item_layout)
        
        switch_items = [
            ('振动提醒', 'vibration_enabled'),
            ('声音提醒', 'sound_enabled'),
            ('睡眠模式', 'sleep_mode_enabled'),
        ]
        
        for label, key in switch_items:
            item_layout = BoxLayout(orientation='horizontal', size_hint_y=None, height=50)
            item_layout.add_widget(Label(text=label, size_hint_x=0.7))
            switch = Switch(active=config.get(key, True))
            switch.bind(active=lambda s, v, k=key: self.update_setting(k, v))
            item_layout.add_widget(switch)
            layout.add_widget(item_layout)
        
        reset_btn = Button(text='🔄 恢复默认设置', size_hint_y=None, height=50, background_color=(0.8, 0.3, 0.3, 1))
        reset_btn.bind(on_press=lambda b: self.reset_settings())
        layout.add_widget(reset_btn)
        
        back_btn = Button(text='← 返回', size_hint_y=None, height=50)
        back_btn.bind(on_press=lambda b: (self.dismiss_popup(), self.show_main_menu()))
        layout.add_widget(back_btn)
        
        content.add_widget(layout)
        
        self.settings_popup = Popup(title='设置', content=content, size_hint=(0.95, 0.9))
        self.settings_popup.open()
    
    def update_setting(self, key, value):
        config.set(key, value)
        if key == 'pet_size':
            self.pet.size_hint = (value / Window.width, value / Window.height)
    
    def reset_settings(self):
        config.reset()
        self.show_settings()
    
    def export_data(self):
        self.dismiss_popup()
        data = db.export_data()
        with open('clock3_backup.json', 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        
        content = BoxLayout(orientation='vertical', padding=20)
        content.add_widget(Label(text='✅ 数据已导出到\nclock3_backup.json', font_size=16))
        close_btn = Button(text='确定', size_hint_y=0.3)
        close_btn.bind(on_press=lambda b: self.dismiss_popup())
        content.add_widget(close_btn)
        
        popup = Popup(title='导出成功', content=content, size_hint=(0.8, 0.4))
        popup.open()
    
    def import_data(self):
        self.dismiss_popup()
        
        content = BoxLayout(orientation='vertical', padding=20)
        
        file_input = TextInput(hint_text='输入文件名', multiline=False, size_hint_y=0.3)
        content.add_widget(file_input)
        
        def do_import(b):
            filename = file_input.text or 'clock3_backup.json'
            try:
                with open(filename, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                db.import_data(data)
                alarm_service.load_alarms()
                pet_service.load_pet()
                self.show_message('导入成功!')
            except FileNotFoundError:
                self.show_message('文件未找到')
            except Exception as e:
                self.show_message(f'导入失败: {e}')
        
        import_btn = Button(text='📥 导入', size_hint_y=0.3, background_color=(0.2, 0.7, 0.3, 1))
        import_btn.bind(on_press=do_import)
        content.add_widget(import_btn)
        
        back_btn = Button(text='← 返回', size_hint_y=0.2)
        back_btn.bind(on_press=lambda b: (self.dismiss_popup(), self.show_main_menu()))
        content.add_widget(back_btn)
        
        popup = Popup(title='导入数据', content=content, size_hint=(0.9, 0.6))
        popup.open()
    
    def show_message(self, message):
        content = BoxLayout(orientation='vertical', padding=20)
        content.add_widget(Label(text=message, font_size=16))
        close_btn = Button(text='确定', size_hint_y=0.3)
        close_btn.bind(on_press=lambda b: self.dismiss_popup())
        content.add_widget(close_btn)
        
        popup = Popup(title='提示', content=content, size_hint=(0.8, 0.4))
        popup.open()
    
    def dismiss_popup(self):
        popups = [
            self.main_menu_popup,
            self.alarms_popup,
            self.add_alarm_popup,
            self.countdown_popup,
            self.pet_popup,
            self.settings_popup,
            self.alarm_popup,
            self.active_popup
        ]
        
        for popup in popups:
            if popup:
                try:
                    popup.dismiss()
                except Exception:
                    pass
    
    def update_status(self):
        pet_status = pet_service.get_status()
        alarm_count = len([a for a in alarm_service.alarms if a.enabled])
        self.status_label.text = f"宠物: {pet_status.get('name')} | 活跃闹钟: {alarm_count}"

    def on_stop(self):
        alarm_service.stop_checking()
        countdown_service.stop_checking()

if __name__ == '__main__':
    FloatingPetApp().run()
