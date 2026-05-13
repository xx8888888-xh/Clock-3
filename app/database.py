import sqlite3
import json
from pathlib import Path
from datetime import datetime
import logging

try:
    from app.models.alarm import Alarm
    from app.models.countdown import Countdown
    from app.models.pet import Pet
except Exception as e:
    class Alarm:
        def __init__(self, **kwargs):
            pass
    
    class Countdown:
        def __init__(self, **kwargs):
            pass
    
    class Pet:
        def __init__(self, **kwargs):
            pass

class Database:
    """数据库管理"""
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
        self.db_dir = self._get_db_dir()
        self.db_dir.mkdir(parents=True, exist_ok=True)
        self.db_path = self.db_dir / 'clock3.db'
        self.init_db()
    
    def _get_db_dir(self):
        """获取数据库目录"""
        try:
            import platform
            system = platform.system()
            
            if system == 'Android':
                try:
                    from android.storage import app_storage_path
                    return Path(app_storage_path()) / '.clock3'
                except ImportError:
                    pass
            
            try:
                from kivy.utils import platform
                if platform == 'android':
                    try:
                        from jnius import autoclass
                        context = autoclass('org.kivy.android.PythonActivity').mActivity
                        return Path(context.getFilesDir().getAbsolutePath()) / '.clock3'
                    except Exception as e:
                        self.logger.warning(f"无法通过 jnius 获取 Android 目录: {e}")
            
            return Path.home() / '.clock3'
        except Exception as e:
            self.logger.error(f"获取数据库目录失败: {e}")
            return Path("/tmp/.clock3")
    
    def init_db(self):
        """初始化数据库"""
        try:
            with sqlite3.connect(str(self.db_path)) as conn:
                cursor = conn.cursor()
                
                cursor.execute('''
                    CREATE TABLE IF NOT EXISTS alarms (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        label TEXT NOT NULL,
                        time TEXT NOT NULL,
                        content TEXT,
                        repeat_type TEXT DEFAULT 'once',
                        repeat_days TEXT,
                        enabled INTEGER DEFAULT 1,
                        snooze_count INTEGER DEFAULT 0,
                        snooze_time TEXT,
                        created_at TEXT,
                        updated_at TEXT
                    )
                ''')
                
                cursor.execute('''
                    CREATE TABLE IF NOT EXISTS countdowns (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        label TEXT NOT NULL,
                        target_time TEXT NOT NULL,
                        status TEXT DEFAULT 'running',
                        remaining_seconds INTEGER,
                        created_at TEXT
                    )
                ''')
                
                cursor.execute('''
                    CREATE TABLE IF NOT EXISTS pet_data (
                        id INTEGER PRIMARY KEY,
                        mood TEXT DEFAULT 'happy',
                        level INTEGER DEFAULT 1,
                        exp INTEGER DEFAULT 0,
                        name TEXT DEFAULT '小宠物',
                        last_interaction TEXT,
                        total_interactions INTEGER DEFAULT 0
                    )
                ''')
                
                cursor.execute('''
                    CREATE TABLE IF NOT EXISTS settings (
                        key TEXT PRIMARY KEY,
                        value TEXT
                    )
                ''')
                
                conn.commit()
        except Exception as e:
            self.logger.error(f"初始化数据库失败: {e}")
    
    def get_connection(self):
        """获取数据库连接"""
        return sqlite3.connect(str(self.db_path))
    
    def add_alarm(self, alarm):
        """添加闹钟"""
        try:
            now = datetime.now().isoformat()
            with self.get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute('''
                    INSERT INTO alarms (label, time, content, repeat_type, repeat_days, enabled, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ''', (
                    alarm.label,
                    alarm.time,
                    alarm.content,
                    alarm.repeat_type,
                    json.dumps(alarm.repeat_days) if alarm.repeat_days else None,
                    1 if alarm.enabled else 0,
                    now,
                    now
                ))
                alarm_id = cursor.lastrowid
                conn.commit()
                return alarm_id
        except Exception as e:
            self.logger.error(f"添加闹钟失败: {e}")
            return None
    
    def update_alarm(self, alarm):
        """更新闹钟"""
        try:
            now = datetime.now().isoformat()
            with self.get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute('''
                    UPDATE alarms SET
                        label = ?, time = ?, content = ?, repeat_type = ?,
                        repeat_days = ?, enabled = ?, updated_at = ?
                    WHERE id = ?
                ''', (
                    alarm.label,
                    alarm.time,
                    alarm.content,
                    alarm.repeat_type,
                    json.dumps(alarm.repeat_days) if alarm.repeat_days else None,
                    1 if alarm.enabled else 0,
                    now,
                    alarm.id
                ))
                conn.commit()
        except Exception as e:
            self.logger.error(f"更新闹钟失败: {e}")
    
    def delete_alarm(self, alarm_id):
        """删除闹钟"""
        try:
            with self.get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute('DELETE FROM alarms WHERE id = ?', (alarm_id,))
                conn.commit()
        except Exception as e:
            self.logger.error(f"删除闹钟失败: {e}")
    
    def get_all_alarms(self):
        """获取所有闹钟"""
        try:
            with self.get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute('SELECT * FROM alarms ORDER BY time')
                rows = cursor.fetchall()
            
            alarms = []
            for row in rows:
                alarms.append({'id': row[0], 'label': row[1], 'time': row[2], 'content': row[3], 'enabled': bool(row[6])})
            return alarms
        except Exception as e:
            self.logger.error(f"获取闹钟列表失败: {e}")
            return []
    
    def get_pet_data(self):
        """获取宠物数据"""
        try:
            with self.get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute('SELECT * FROM pet_data WHERE id = 1')
                row = cursor.fetchone()
            
            if not row:
                # 创建默认数据
                with self.get_connection() as conn:
                    cursor = conn.cursor()
                    cursor.execute('INSERT INTO pet_data (id, mood, level, exp, name, total_interactions) VALUES (1, "happy", 1, 0, "小宠物", 0)')
                    conn.commit()
            return {}
        except Exception as e:
            self.logger.error(f"获取宠物数据失败: {e}")
            return {}
    
    def save_pet_data(self, pet):
        """保存宠物数据"""
        try:
            pass
        except Exception as e:
            self.logger.error(f"保存宠物数据失败: {e}")
    
    def export_data(self):
        """导出所有数据"""
        try:
            return {'alarms': self.get_all_alarms(), 'pet': {}, 'export_time': datetime.now().isoformat()}
        except Exception as e:
            self.logger.error(f"导出数据失败: {e}")
            return {}
    
    def import_data(self, data):
        """导入数据"""
        try:
            pass
        except Exception as e:
            self.logger.error(f"导入数据失败: {e}")

db = Database()
