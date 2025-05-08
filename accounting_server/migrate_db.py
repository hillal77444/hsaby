import os
import subprocess
import sys
import time

def run_command(command):
    """تنفيذ أمر في النظام"""
    try:
        result = subprocess.run(command, shell=True, check=True, capture_output=True, text=True)
        print(f"✅ تم تنفيذ الأمر بنجاح: {command}")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ فشل تنفيذ الأمر: {command}")
        print(f"الخطأ: {e.stderr}")
        return False

def setup_database():
    """إعداد قاعدة البيانات"""
    commands = [
        # إنشاء المجلدات
        "mkdir -p /root/accounting_server/data",
        "chown -R postgres:postgres /root/accounting_server/data",
        
        # تعديل إعدادات PostgreSQL
        "sed -i 's|data_directory = .*|data_directory = \'/root/accounting_server/data\'|' /etc/postgresql/*/main/postgresql.conf",
        
        # إعادة تشغيل PostgreSQL
        "systemctl restart postgresql",
        
        # انتظار بدء الخدمة
        "sleep 5",
        
        # إنشاء قاعدة البيانات والمستخدم
        "sudo -u postgres psql -c \"CREATE DATABASE accounting_db;\"",
        "sudo -u postgres psql -c \"CREATE USER accounting_user WITH PASSWORD 'Accounting@123';\"",
        "sudo -u postgres psql -c \"GRANT ALL PRIVILEGES ON DATABASE accounting_db TO accounting_user;\"",
        
        # تهيئة قاعدة البيانات
        "flask db upgrade"
    ]
    
    for command in commands:
        if not run_command(command):
            print("❌ فشل في إعداد قاعدة البيانات")
            return False
    
    print("✅ تم إعداد قاعدة البيانات بنجاح")
    return True

if __name__ == "__main__":
    print("🚀 بدء عملية نقل قاعدة البيانات...")
    if setup_database():
        print("✨ تم نقل قاعدة البيانات بنجاح!")
    else:
        print("❌ فشل نقل قاعدة البيانات")
        sys.exit(1) 