import os
from datetime import timedelta

class Config:
    # مسار قاعدة البيانات
    SQLALCHEMY_DATABASE_URI = 'sqlite:////root/accounting_server/data/accounting.db'
    
    # إعدادات SQLAlchemy
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    
    # إعدادات JWT
    JWT_SECRET_KEY = 'your-secret-key-here'  # مفتاح آمن للإنتاج
    JWT_ACCESS_TOKEN_EXPIRES = timedelta(hours=24)  # زيادة مدة صلاحية التوكن
    
    # إعدادات الأمان
    SECRET_KEY = 'accounting-app-secure-key-2024'  # مفتاح آمن للإنتاج
    
    # إعدادات المزامنة
    SYNC_BATCH_SIZE = 100
    
    # إعدادات السيرفر
    SERVER_NAME = '212.224.88.122:5007'
    PREFERRED_URL_SCHEME = 'http'
    
    # إعدادات CORS
    CORS_HEADERS = 'Content-Type'
    
    # إعدادات التطبيق
    DEBUG = True  # تغيير إلى False في الإنتاج 