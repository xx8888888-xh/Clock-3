from datetime import datetime
from typing import Optional

class Pet:
    """宠物数据模型"""
    
    MOODS = ['happy', 'sad', 'sleepy', 'excited', 'hungry', 'bored']
    MOOD_MESSAGES = {
        'happy': ['今天心情真好!', '好开心呀~', '看到你真高兴!'],
        'sad': ['有点难过...', '不理我了?', '想你了...'],
        'sleepy': ['好困啊...', '想睡觉了', 'zzZ...'],
        'excited': ['太棒了!', '耶耶耶!', '好兴奋!'],
        'hungry': ['肚子饿了...', '想吃东西', '喂我吃饭吧~'],
        'bored': ['好无聊啊', '陪我玩嘛', '没人理我...']
    }
    
    def __init__(
        self,
        id: Optional[int] = None,
        mood: str = 'happy',
        level: int = 1,
        exp: int = 0,
        name: str = '小宠物',
        last_interaction: Optional[str] = None,
        total_interactions: int = 0
    ):
        self.id = id
        self.mood = mood
        self.level = level
        self.exp = exp
        self.name = name
        self.last_interaction = last_interaction
        self.total_interactions = total_interactions
    
    def get_exp_for_next_level(self) -> int:
        """获取升级所需经验"""
        return self.level * 100
    
    def add_exp(self, amount: int):
        """添加经验值"""
        self.exp += amount
        messages = []
        while self.exp >= self.get_exp_for_next_level():
            self.exp -= self.get_exp_for_next_level()
            self.level += 1
            messages.append(f"🎉 恭喜升级到 {self.level} 级!")
        
        return messages
    
    def interact(self):
        """宠物交互"""
        self.last_interaction = datetime.now().isoformat()
        self.total_interactions += 1
        
        import random
        self.mood = random.choice(self.MOODS)
        
        exp_gain = 5 + (self.level * 2)
        return self.add_exp(exp_gain)
    
    def get_random_message(self) -> str:
        """获取随机消息"""
        import random
        messages = self.MOOD_MESSAGES.get(self.mood, ['你好!'])
        return random.choice(messages)
    
    def get_mood_emoji(self) -> str:
        """获取心情表情"""
        emoji_map = {
            'happy': '😊',
            'sad': '😢',
            'sleepy': '😴',
            'excited': '🤩',
            'hungry': '🤤',
            'bored': '😐'
        }
        return emoji_map.get(self.mood, '😊')
    
    def is_sleeping(self) -> bool:
        """检查是否在睡眠时间"""
        current_hour = datetime.now().hour
        return 22 <= current_hour or current_hour < 7
    
    def to_dict(self) -> dict:
        """转换为字典"""
        return {
            'id': self.id,
            'mood': self.mood,
            'level': self.level,
            'exp': self.exp,
            'name': self.name,
            'last_interaction': self.last_interaction,
            'total_interactions': self.total_interactions
        }
    
    @classmethod
    def from_dict(cls, data: dict) -> 'Pet':
        """从字典创建"""
        return cls(
            id=data.get('id'),
            mood=data.get('mood', 'happy'),
            level=data.get('level', 1),
            exp=data.get('exp', 0),
            name=data.get('name', '小宠物'),
            last_interaction=data.get('last_interaction'),
            total_interactions=data.get('total_interactions', 0)
        )
    
    def __repr__(self):
        return f"Pet(name={self.name}, mood={self.mood}, level={self.level}, exp={self.exp}/{self.get_exp_for_next_level()})"
