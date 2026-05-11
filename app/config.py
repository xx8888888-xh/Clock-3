import os
import json
from pathlib import Path

class Config:
    """应用配置管理"""
    
    def __init__(self):
        import platform
        system = platform.system()
        
        if system == 'Android':
            from android.storage import app_storage_path
            self.config_dir = Path(app_storage_path()) / '.clock3'
        else:
            self.config_dir = Path.home() / '.clock3'
        
        self.config_dir.mkdir(parents=True, exist_ok=True)
        self.config_file = self.config_dir / 'config.json'
        self.default_config = {
            'pet_size': 100,
            'pet_opacity': 1.0,
            'pet_x': 0.9,
            'pet_y': 0.8,
            'snooze_duration': 5,
            'max_snooze_count': 3,
            'notification_duration': 10,
            'vibration_enabled': True,
            'sound_enabled': True,
            'sound_volume': 0.8,
            'pet_mood': 'happy',
            'pet_level': 1,
            'pet_exp': 0,
            'sleep_mode_enabled': True,
            'sleep_start_hour': 22,
            'sleep_end_hour': 7,
            'theme': 'light'
        }
        self.load()
    
    def load(self):
        """加载配置"""
        import logging
        logger = logging.getLogger(__name__)
        if self.config_file.exists():
            try:
                with open(self.config_file, 'r', encoding='utf-8') as f:
                    loaded = json.load(f)
                    self.settings = {**self.default_config, **loaded}
            except json.JSONDecodeError as e:
                logger.warning(f"配置文件格式错误，使用默认配置: {e}")
                self.settings = self.default_config.copy()
            except Exception as e:
                logger.error(f"加载配置失败: {e}")
                self.settings = self.default_config.copy()
        else:
            self.settings = self.default_config.copy()
    
    def save(self):
        """保存配置"""
        try:
            with open(self.config_file, 'w', encoding='utf-8') as f:
                json.dump(self.settings, f, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"保存配置失败: {e}")
    
    def get(self, key, default=None):
        """获取配置项"""
        return self.settings.get(key, default)
    
    def set(self, key, value):
        """设置配置项"""
        self.settings[key] = value
        self.save()
    
    def update(self, updates):
        """批量更新配置"""
        self.settings.update(updates)
        self.save()
    
    def reset(self):
        """重置为默认配置"""
        self.settings = self.default_config.copy()
        self.save()

config = Config()
