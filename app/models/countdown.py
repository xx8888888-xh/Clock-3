from datetime import datetime
from typing import Optional

class Countdown:
    """倒计时数据模型"""
    
    def __init__(
        self,
        id: Optional[int] = None,
        label: str = "",
        target_time: datetime = None,
        status: str = "running",
        remaining_seconds: int = 0,
        created_at: Optional[str] = None
    ):
        self.id = id
        self.label = label
        self.target_time = target_time or datetime.now()
        self.status = status
        self.remaining_seconds = remaining_seconds
        self.created_at = created_at or datetime.now().isoformat()
    
    def update_remaining(self):
        """更新剩余时间"""
        if self.status == "running":
            delta = self.target_time - datetime.now()
            self.remaining_seconds = max(0, int(delta.total_seconds()))
            if self.remaining_seconds == 0:
                self.status = "completed"
    
    def pause(self):
        """暂停倒计时"""
        if self.status == "running":
            self.update_remaining()
            self.status = "paused"
    
    def resume(self):
        """继续倒计时"""
        if self.status == "paused":
            self.target_time = datetime.now()
            self.status = "running"
    
    def reset(self, seconds: int):
        """重置倒计时"""
        self.remaining_seconds = seconds
        self.target_time = datetime.now()
        self.status = "running"
    
    def get_formatted_time(self) -> str:
        """获取格式化时间"""
        if self.status == "paused":
            seconds = self.remaining_seconds
        else:
            seconds = max(0, int((self.target_time - datetime.now()).total_seconds()))
        
        hours = seconds // 3600
        minutes = (seconds % 3600) // 60
        secs = seconds % 60
        
        if hours > 0:
            return f"{hours:02d}:{minutes:02d}:{secs:02d}"
        else:
            return f"{minutes:02d}:{secs:02d}"
    
    def to_dict(self) -> dict:
        """转换为字典"""
        return {
            'id': self.id,
            'label': self.label,
            'target_time': self.target_time.isoformat() if isinstance(self.target_time, datetime) else self.target_time,
            'status': self.status,
            'remaining_seconds': self.remaining_seconds,
            'created_at': self.created_at
        }
    
    @classmethod
    def from_dict(cls, data: dict) -> 'Countdown':
        """从字典创建"""
        target_time = data.get('target_time')
        if isinstance(target_time, str):
            target_time = datetime.fromisoformat(target_time)
        
        return cls(
            id=data.get('id'),
            label=data.get('label', ''),
            target_time=target_time,
            status=data.get('status', 'running'),
            remaining_seconds=data.get('remaining_seconds', 0),
            created_at=data.get('created_at')
        )
    
    def __repr__(self):
        return f"Countdown(id={self.id}, label={self.label}, remaining={self.remaining_seconds}s, status={self.status})"
