import random
from datetime import datetime
from typing import Callable, Optional
from app.database import db
from app.models.pet import Pet

class PetService:
    """宠物服务"""
    
    _instance = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance
    
    def __init__(self):
        if self._initialized:
            return
        
        self._initialized = True
        self.pet: Optional[Pet] = None
        self.callbacks = []
    
    def load_pet(self):
        """加载宠物数据"""
        self.pet = db.get_pet_data()
    
    def save_pet(self):
        """保存宠物数据"""
        if self.pet:
            db.save_pet_data(self.pet)
    
    def interact(self) -> dict:
        """与宠物交互"""
        if not self.pet:
            return {'message': '宠物未加载', 'level_up': []}
        
        level_up_messages = self.pet.interact()
        self.save_pet()
        
        return {
            'message': self.pet.get_random_message(),
            'mood': self.pet.mood,
            'emoji': self.pet.get_mood_emoji(),
            'level': self.pet.level,
            'exp': self.pet.exp,
            'exp_needed': self.pet.get_exp_for_next_level(),
            'level_up': level_up_messages
        }
    
    def feed_pet(self):
        """喂宠物"""
        if not self.pet:
            return {'message': '宠物未加载', 'level_up': []}
        
        self.pet.mood = 'happy'
        self.pet.last_interaction = datetime.now().isoformat()
        self.pet.total_interactions += 1
        exp_gain = self.pet.add_exp(10)
        self.save_pet()
        
        return {
            'message': '好吃! 谢谢喂我~',
            'mood': self.pet.mood,
            'emoji': self.pet.get_mood_emoji(),
            'level': self.pet.level,
            'exp': self.pet.exp,
            'exp_needed': self.pet.get_exp_for_next_level(),
            'level_up': exp_gain
        }
    
    def play_with_pet(self):
        """和宠物玩"""
        if not self.pet:
            return {'message': '宠物未加载', 'level_up': []}
        
        self.pet.mood = 'excited'
        self.pet.last_interaction = datetime.now().isoformat()
        self.pet.total_interactions += 1
        exp_gain = self.pet.add_exp(15)
        self.save_pet()
        
        return {
            'message': '太好玩了! 再来再来!',
            'mood': self.pet.mood,
            'emoji': self.pet.get_mood_emoji(),
            'level': self.pet.level,
            'exp': self.pet.exp,
            'exp_needed': self.pet.get_exp_for_next_level(),
            'level_up': exp_gain
        }
    
    def pet_sleep(self):
        """宠物睡觉"""
        if not self.pet:
            return {'message': '宠物未加载'}
        
        self.pet.mood = 'sleepy'
        self.save_pet()
        
        return {
            'message': '困了... zzZ...',
            'mood': self.pet.mood,
            'emoji': self.pet.get_mood_emoji(),
            'level': self.pet.level,
            'exp': self.pet.exp,
            'exp_needed': self.pet.get_exp_for_next_level()
        }
    
    def is_sleeping(self) -> bool:
        """检查宠物是否在睡眠"""
        if self.pet:
            return self.pet.is_sleeping()
        return False
    
    def get_status(self) -> dict:
        """获取宠物状态"""
        if not self.pet:
            return {}
        
        return {
            'name': self.pet.name,
            'mood': self.pet.mood,
            'emoji': self.pet.get_mood_emoji(),
            'level': self.pet.level,
            'exp': self.pet.exp,
            'exp_needed': self.pet.get_exp_for_next_level(),
            'is_sleeping': self.pet.is_sleeping(),
            'total_interactions': self.pet.total_interactions
        }
    
    def update_name(self, name: str):
        """更新宠物名字"""
        if self.pet:
            self.pet.name = name
            self.save_pet()
    
    def on_status_change(self, callback: Callable):
        """注册状态变化回调"""
        self.callbacks.append(callback)
    
    def notify_status_change(self):
        """通知状态变化"""
        status = self.get_status()
        for callback in self.callbacks:
            try:
                callback(status)
            except Exception as e:
                print(f"宠物状态回调错误: {e}")

pet_service = PetService()
