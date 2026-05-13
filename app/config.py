import os
import json
from pathlib import Path
import logging

class Config:
    """应用配置管理"""
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
        try:
            self.config_dir = self._get_config_dir()
            self.config_dir.mkdir(parents=True, exist_ok=True)
        except Exception as e:
            self.logger.error(f"无法创建配置目录，使用临时目录: {e}")
            self.config_dir = Path("/tmp/.clock3")
            try:
                self.config_dir.mkdir(parents=True, exist_ok=True)
            except:
                pass
        
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
    
    def _get_config_dir(self):
        """获取配置目录"""
        try:
            import platform
            system = platform.system()
            
            if system == 'Android':
                try:
                    from android.storage import app_storage_path
                    return Path(app_storage_path()) / '.clock3'
                except ImportError:
                    # 如果 android.storage 不可用，尝试其他方法
                    pass
            
            # 对于桌面平台和 Android 降级方案
            try:
                from kivy.utils import platform
                if platform == 'android':
                    try:
                        from jnius import autoclass
                        Environment = autoclass('android.os.Environment')
                        Context = autoclass('android.content.Context')
                        context = autoclass('org.kivy.android.PythonActivity').mActivity
                        return Path(context.getFilesDir().getAbsolutePath()) / '.clock3'
                    except Exception as e:
                        self.logger.warning(f"无法通过 jnius 获取 Android 目录: {e}")
            
            # 默认使用主目录
            return Path.home() / '.clock3'
        except Exception as e:
            self.logger.error(f"获取配置目录失败: {e}")
            return Path("/tmp/.clock3")
    
    def load(self):
        """加载配置"""
        try:
            if self.config_file.exists():
                with open(self.config_file, 'r', encoding='utf-8') as f:
                    loaded = json.load(f)
                    self.settings = {**self.default_config, **loaded}
            else:
                self.settings = self.default_config.copy()
        except Exception as e:
            self.logger.error(f"加载配置失败，使用默认配置: {e}")
            self.settings = self.default_config.copy()
    
    def save(self):
        """保存配置"""
        try:
            with open(self.config_file, 'w', encoding='utf-8') as f:
                json.dump(self.settings, f, ensure_ascii=False, indent=2)
        except Exception as e:
            self.logger.error(f"保存配置失败: {e}")
    
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
