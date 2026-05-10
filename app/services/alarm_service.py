import threading
import time
from datetime import datetime, timedelta
from typing import Callable, Optional
from app.database import db
from app.models.alarm import Alarm

class AlarmService:
    """闹钟服务"""
    
    _instance = None
    _lock = threading.Lock()
    
    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._initialized = False
        return cls._instance
    
    def __init__(self):
        if self._initialized:
            return
        
        self._initialized = True
        self.alarms = []
        self.is_running = False
        self.check_thread = None
        self.callbacks = []
        self.active_alarm = None
        self.snooze_until = None
    
    def load_alarms(self):
        """加载所有闹钟"""
        self.alarms = db.get_all_alarms()
    
    def add_alarm(self, alarm: Alarm) -> int:
        """添加闹钟"""
        alarm_id = db.add_alarm(alarm)
        alarm.id = alarm_id
        self.alarms.append(alarm)
        return alarm_id
    
    def update_alarm(self, alarm: Alarm):
        """更新闹钟"""
        db.update_alarm(alarm)
        for i, a in enumerate(self.alarms):
            if a.id == alarm.id:
                self.alarms[i] = alarm
                break
    
    def delete_alarm(self, alarm_id: int):
        """删除闹钟"""
        db.delete_alarm(alarm_id)
        self.alarms = [a for a in self.alarms if a.id != alarm_id]
    
    def toggle_alarm(self, alarm_id: int):
        """切换闹钟状态"""
        for alarm in self.alarms:
            if alarm.id == alarm_id:
                alarm.enabled = not alarm.enabled
                self.update_alarm(alarm)
                break
    
    def get_next_alarm(self) -> Optional[Alarm]:
        """获取下一个要触发的闹钟"""
        now = datetime.now()
        next_alarm = None
        min_diff = float('inf')
        
        for alarm in self.alarms:
            if not alarm.enabled:
                continue
            
            if alarm.snooze_time:
                snooze_time = datetime.fromisoformat(alarm.snooze_time)
                if snooze_time > now:
                    diff = (snooze_time - now).total_seconds()
                    if diff < min_diff:
                        min_diff = diff
                        next_alarm = alarm
                continue
            
            next_time = alarm.get_next_trigger_time()
            if next_time:
                diff = (next_time - now).total_seconds()
                if 0 <= diff < min_diff:
                    min_diff = diff
                    next_alarm = alarm
        
        return next_alarm
    
    def start_checking(self):
        """开始检查闹钟"""
        if self.is_running:
            return
        
        self.is_running = True
        self.check_thread = threading.Thread(target=self._check_loop, daemon=True)
        self.check_thread.start()
    
    def stop_checking(self):
        """停止检查闹钟"""
        self.is_running = False
        if self.check_thread:
            self.check_thread.join(timeout=2)
    
    def _check_loop(self):
        """检查循环"""
        while self.is_running:
            self._check_alarms()
            time.sleep(1)
    
    def _check_alarms(self):
        """检查是否应该触发闹钟"""
        now = datetime.now()
        
        if self.snooze_until and now < self.snooze_until:
            return
        
        for alarm in self.alarms:
            if not alarm.enabled:
                continue
            
            if alarm.snooze_time:
                snooze_time = datetime.fromisoformat(alarm.snooze_time)
                if now >= snooze_time:
                    self._trigger_alarm(alarm)
                    return
                continue
            
            next_time = alarm.get_next_trigger_time()
            if next_time and next_time <= now + timedelta(seconds=1):
                self._trigger_alarm(alarm)
                return
    
    def _trigger_alarm(self, alarm: Alarm):
        """触发闹钟"""
        self.active_alarm = alarm
        
        for callback in self.callbacks:
            try:
                callback(alarm)
            except Exception as e:
                print(f"闹钟回调错误: {e}")
    
    def on_alarm_trigger(self, callback: Callable):
        """注册闹钟触发回调"""
        self.callbacks.append(callback)
    
    def snooze_alarm(self, minutes: int = 5):
        """贪睡闹钟"""
        if self.active_alarm:
            self.active_alarm.snooze_count += 1
            self.snooze_until = datetime.now() + timedelta(minutes=minutes)
            self.active_alarm.snooze_time = self.snooze_until.isoformat()
            self.update_alarm(self.active_alarm)
            self.active_alarm = None
    
    def dismiss_alarm(self):
        """关闭闹钟"""
        if self.active_alarm:
            self.active_alarm.snooze_count = 0
            self.active_alarm.snooze_time = None
            
            if self.active_alarm.repeat_type == 'once':
                self.active_alarm.enabled = False
                self.update_alarm(self.active_alarm)
            
            self.active_alarm = None
            self.snooze_until = None

alarm_service = AlarmService()
