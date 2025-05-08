#!/bin/bash

# معلومات الاتصال
SERVER="root@212.224.88.122"
PASSWORD="Hillal774447251"

# نسخ الملفات إلى السيرفر
echo "📤 نسخ الملفات إلى السيرفر..."
sshpass -p "$PASSWORD" scp -r config.py migrate_db.py "$SERVER:/root/accounting_server/"

# تنفيذ سكريبت النقل
echo "🚀 تنفيذ سكريبت نقل قاعدة البيانات..."
sshpass -p "$PASSWORD" ssh "$SERVER" "cd /root/accounting_server && python3 migrate_db.py"

# التحقق من النتيجة
if [ $? -eq 0 ]; then
    echo "✅ تم نقل قاعدة البيانات بنجاح!"
else
    echo "❌ فشل نقل قاعدة البيانات"
    exit 1
fi 