import sqlite3
import json
from pathlib import Path
from datetime import datetime
from app.models.alarm import Alarm
from app.models.countdown import Countdown
from app.models.pet import Pet

class Database:
    """数据库管理"""
    
    def __init__(self):
        self.db_dir = Path.home() / '.clock3'
        self.db_dir.mkdir(exist_ok=True)
        self.db_path = self.db_dir / 'clock3.db'
        self.init_db()
    
    def init_db(self):
        """初始化数据库"""
        conn = sqlite3.connect(str(self.db_path))
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
        conn.close()
    
    def get_connection(self):
        """获取数据库连接"""
        return sqlite3.connect(str(self.db_path))
    
    def add_alarm(self, alarm):
        """添加闹钟"""
        conn = self.get_connection()
        cursor = conn.cursor()
        now = datetime.now().isoformat()
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
        conn.close()
        return alarm_id
    
    def update_alarm(self, alarm):
        """更新闹钟"""
        conn = self.get_connection()
        cursor = conn.cursor()
        now = datetime.now().isoformat()
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
        conn.close()
    
    def delete_alarm(self, alarm_id):
        """删除闹钟"""
        conn = self.get_connection()
        cursor = conn.cursor()
        cursor.execute('DELETE FROM alarms WHERE id = ?', (alarm_id,))
        conn.commit()
        conn.close()
    
    def get_all_alarms(self):
        """获取所有闹钟"""
        conn = self.get_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT * FROM alarms ORDER BY time')
        rows = cursor.fetchall()
        conn.close()
        
        alarms = []
        for row in rows:
            alarm = Alarm(
                id=row[0],
                label=row[1],
                time=row[2],
                content=row[3],
                repeat_type=row[4],
                repeat_days=json.loads(row[5]) if row[5] else None,
                enabled=bool(row[6]),
                snooze_count=row[7],
                snooze_time=row[8],
                created_at=row[9],
                updated_at=row[10]
            )
            alarms.append(alarm)
        return alarms
    
    def add_countdown(self, countdown):
        """添加倒计时"""
        conn = self.get_connection()
        cursor = conn.cursor()
        now = datetime.now().isoformat()
        cursor.execute('''
            INSERT INTO countdowns (label, target_time, status, remaining_seconds, created_at)
            VALUES (?, ?, ?, ?, ?)
        ''', (
            countdown.label,
            countdown.target_time.isoformat(),
            countdown.status,
            countdown.remaining_seconds,
            now
        ))
        countdown_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return countdown_id
    
    def update_countdown(self, countdown):
        """更新倒计时"""
        conn = self.get_connection()
        cursor = conn.cursor()
        cursor.execute('''
            UPDATE countdowns SET
                label = ?, target_time = ?, status = ?, remaining_seconds = ?
            WHERE id = ?
        ''', (
            countdown.label,
            countdown.target_time.isoformat(),
            countdown.status,
            countdown.remaining_seconds,
            countdown.id
        ))
        conn.commit()
        conn.close()
    
    def delete_countdown(self, countdown_id):
        """删除倒计时"""
        conn = self.get_connection()
        cursor = conn.cursor()
        cursor.execute('DELETE FROM countdowns WHERE id = ?', (countdown_id,))
        conn.commit()
        conn.close()
    
    def get_all_countdowns(self):
        """获取所有倒计时"""
        conn = self.get_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT * FROM countdowns ORDER BY target_time')
        rows = cursor.fetchall()
        conn.close()
        
        countdowns = []
        for row in rows:
            countdown = Countdown(
                id=row[0],
                label=row[1],
                target_time=datetime.fromisoformat(row[2]),
                status=row[3],
                remaining_seconds=row[4],
                created_at=row[5]
            )
            countdowns.append(countdown)
        return countdowns
    
    def get_pet_data(self):
        """获取宠物数据"""
        conn = self.get_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT * FROM pet_data WHERE id = 1')
        row = cursor.fetchone()
        conn.close()
        
        if row:
            return Pet(
                id=row[0],
                mood=row[1],
                level=row[2],
                exp=row[3],
                name=row[4],
                last_interaction=row[5],
                total_interactions=row[6]
            )
        else:
            pet = Pet()
            self.save_pet_data(pet)
            return pet
    
    def save_pet_data(self, pet):
        """保存宠物数据"""
        conn = self.get_connection()
        cursor = conn.cursor()
        cursor.execute('''
            INSERT OR REPLACE INTO pet_data (id, mood, level, exp, name, last_interaction, total_interactions)
            VALUES (1, ?, ?, ?, ?, ?, ?)
        ''', (
            pet.mood,
            pet.level,
            pet.exp,
            pet.name,
            pet.last_interaction,
            pet.total_interactions
        ))
        conn.commit()
        conn.close()
    
    def export_data(self):
        """导出所有数据"""
        return {
            'alarms': [alarm.to_dict() for alarm in self.get_all_alarms()],
            'countdowns': [cd.to_dict() for cd in self.get_all_countdowns()],
            'pet': self.get_pet_data().to_dict(),
            'export_time': datetime.now().isoformat()
        }
    
    def import_data(self, data):
        """导入数据"""
        if 'alarms' in data:
            for alarm_dict in data['alarms']:
                alarm = Alarm.from_dict(alarm_dict)
                if alarm.id:
                    self.update_alarm(alarm)
                else:
                    self.add_alarm(alarm)
        
        if 'countdowns' in data:
            for cd_dict in data['countdowns']:
                cd = Countdown.from_dict(cd_dict)
                if cd.id:
                    self.update_countdown(cd)
                else:
                    self.add_countdown(cd)
        
        if 'pet' in data:
            pet = Pet.from_dict(data['pet'])
            self.save_pet_data(pet)

db = Database()
