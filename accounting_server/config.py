import os
from datetime import timedelta

class Config:
    # إعدادات قاعدة البيانات
    SQLALCHEMY_DATABASE_URI = 'postgresql://accounting_user:your_password@localhost/accounting_db'
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    
    # إعدادات JWT
    JWT_SECRET_KEY = os.environ.get('JWT_SECRET_KEY', 'your-secret-key')  # يجب تغييره في الإنتاج
    JWT_ACCESS_TOKEN_EXPIRES = timedelta(hours=1)
    
    # إعدادات الأمان
    SECRET_KEY = os.environ.get('SECRET_KEY', 'your-secret-key')  # يجب تغييره في الإنتاج
    
    # إعدادات المزامنة
    SYNC_BATCH_SIZE = 100
    
    # إعدادات السيرفر
    SERVER_NAME = '212.224.88.122:5007'
    PREFERRED_URL_SCHEME = 'http' 