import threading
import time
from datetime import datetime
from typing import Callable, Optional, List
from app.database import db
from app.models.countdown import Countdown

class CountdownService:
    """倒计时服务"""
    
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
        self.countdowns: List[Countdown] = []
        self.is_running = False
        self.check_thread = None
        self.callbacks = []
    
    def load_countdowns(self):
        """加载所有倒计时"""
        self.countdowns = db.get_all_countdowns()
        for cd in self.countdowns:
            if cd.status == "running":
                cd.update_remaining()
    
    def add_countdown(self, countdown: Countdown) -> int:
        """添加倒计时"""
        countdown_id = db.add_countdown(countdown)
        countdown.id = countdown_id
        self.countdowns.append(countdown)
        return countdown_id
    
    def update_countdown(self, countdown: Countdown):
        """更新倒计时"""
        db.update_countdown(countdown)
        for i, cd in enumerate(self.countdowns):
            if cd.id == countdown.id:
                self.countdowns[i] = countdown
                break
    
    def delete_countdown(self, countdown_id: int):
        """删除倒计时"""
        db.delete_countdown(countdown_id)
        self.countdowns = [cd for cd in self.countdowns if cd.id != countdown_id]
    
    def create_countdown(self, label: str, seconds: int) -> Countdown:
        """创建新倒计时"""
        from datetime import timedelta
        target_time = datetime.now() + timedelta(seconds=seconds)
        countdown = Countdown(
            label=label,
            target_time=target_time,
            remaining_seconds=seconds
        )
        self.add_countdown(countdown)
        return countdown
    
    def start_checking(self):
        """开始检查倒计时"""
        if self.is_running:
            return
        
        self.is_running = True
        self.check_thread = threading.Thread(target=self._check_loop, daemon=True)
        self.check_thread.start()
    
    def stop_checking(self):
        """停止检查倒计时"""
        self.is_running = False
        if self.check_thread:
            self.check_thread.join(timeout=2)
    
    def _check_loop(self):
        """检查循环"""
        while self.is_running:
            self._check_countdowns()
            time.sleep(0.1)
    
    def _check_countdowns(self):
        """检查倒计时"""
        for countdown in self.countdowns:
            if countdown.status != "running":
                continue
            
            countdown.update_remaining()
            
            if countdown.remaining_seconds <= 0:
                countdown.status = "completed"
                self.update_countdown(countdown)
                
                for callback in self.callbacks:
                    try:
                        callback(countdown)
                    except Exception as e:
                        print(f"倒计时回调错误: {e}")
    
    def on_countdown_complete(self, callback: Callable):
        """注册倒计时完成回调"""
        self.callbacks.append(callback)
    
    def pause_countdown(self, countdown_id: int):
        """暂停倒计时"""
        for cd in self.countdowns:
            if cd.id == countdown_id:
                cd.pause()
                self.update_countdown(cd)
                break
    
    def resume_countdown(self, countdown_id: int):
        """继续倒计时"""
        for cd in self.countdowns:
            if cd.id == countdown_id:
                cd.resume()
                self.update_countdown(cd)
                break

countdown_service = CountdownService()
