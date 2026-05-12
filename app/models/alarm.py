from datetime import datetime
from typing import Optional, List
import json

class Alarm:
    """闹钟数据模型"""
    
    def __init__(
        self,
        id: Optional[int] = None,
        label: str = "",
        time: str = "08:00",
        content: str = "",
        repeat_type: str = "once",
        repeat_days: Optional[List[int]] = None,
        enabled: bool = True,
        snooze_count: int = 0,
        snooze_time: Optional[str] = None,
        created_at: Optional[str] = None,
        updated_at: Optional[str] = None
    ):
        self.id = id
        self.label = label
        self.time = time
        self.content = content
        self.repeat_type = repeat_type
        self.repeat_days = repeat_days or []
        self.enabled = enabled
        self.snooze_count = snooze_count
        self.snooze_time = snooze_time
        self.created_at = created_at
        self.updated_at = updated_at
    
    def get_next_trigger_time(self) -> Optional[datetime]:
        """获取下次触发时间"""
        now = datetime.now()
        hour, minute = map(int, self.time.split(':'))
        
        target = now.replace(hour=hour, minute=minute, second=0, microsecond=0)
        
        if self.repeat_type == 'once':
            if target <= now:
                return None
            return target
        
        elif self.repeat_type == 'daily':
            if target <= now:
                target = target + timedelta(days=1)
            return target
        
        elif self.repeat_type == 'workdays':
            weekdays = [1, 2, 3, 4, 5]
            while True:
                if target.weekday() in weekdays and target > now:
                    return target
                target = target + timedelta(days=1)
        
        elif self.repeat_type == 'weekend':
            while True:
                if target.weekday() in [5, 6] and target > now:
                    return target
                target = target + timedelta(days=1)
        
        elif self.repeat_type == 'custom' and self.repeat_days:
            while True:
                if target.weekday() in self.repeat_days and target > now:
                    return target
                target = target + timedelta(days=1)
        
        return None
    
    def should_trigger_today(self) -> bool:
        """检查今天是否应该触发"""
        if self.repeat_type == 'once':
            return True
        elif self.repeat_type == 'daily':
            return True
        elif self.repeat_type == 'workdays':
            return datetime.now().weekday() in [1, 2, 3, 4, 5]
        elif self.repeat_type == 'weekend':
            return datetime.now().weekday() in [5, 6]
        elif self.repeat_type == 'custom':
            return datetime.now().weekday() in (self.repeat_days or [])
        return False
    
    def to_dict(self) -> dict:
        """转换为字典"""
        return {
            'id': self.id,
            'label': self.label,
            'time': self.time,
            'content': self.content,
            'repeat_type': self.repeat_type,
            'repeat_days': self.repeat_days,
            'enabled': self.enabled,
            'snooze_count': self.snooze_count,
            'snooze_time': self.snooze_time,
            'created_at': self.created_at,
            'updated_at': self.updated_at
        }
    
    @classmethod
    def from_dict(cls, data: dict) -> 'Alarm':
        """从字典创建"""
        return cls(
            id=data.get('id'),
            label=data.get('label', ''),
            time=data.get('time', '08:00'),
            content=data.get('content', ''),
            repeat_type=data.get('repeat_type', 'once'),
            repeat_days=data.get('repeat_days'),
            enabled=data.get('enabled', True),
            snooze_count=data.get('snooze_count', 0),
            snooze_time=data.get('snooze_time'),
            created_at=data.get('created_at'),
            updated_at=data.get('updated_at')
        )
    
    def __repr__(self):
        return f"Alarm(id={self.id}, label={self.label}, time={self.time}, enabled={self.enabled})"
