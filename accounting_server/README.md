# Accounting Server

خادم API لتطبيق المحاسبة مع دعم المزامنة.

## المتطلبات

- Python 3.x
- PostgreSQL
- pip

## التثبيت

1. إنشاء بيئة افتراضية:
```bash
python -m venv venv
```

2. تفعيل البيئة الافتراضية:
```bash
# في Windows
venv\Scripts\activate

# في Linux/Mac
source venv/bin/activate
```

3. تثبيت المتطلبات:
```bash
pip install -r requirements.txt
```

4. تعديل إعدادات قاعدة البيانات في `config.py`

5. إنشاء قاعدة البيانات:
```bash
flask db init
flask db migrate
flask db upgrade
```

## التشغيل

```bash
python run.py
```

السيرفر سيعمل على المنفذ 5007 ويمكن الوصول إليه عبر `http://localhost:5007`

## API Endpoints

### تسجيل مستخدم جديد
- POST `/api/register`
- Body: `{ "username": "", "phone": "", "password": "" }`

### تسجيل الدخول
- POST `/api/login`
- Body: `{ "username": "", "password": "" }`

### مزامنة البيانات
- POST `/api/sync`
- Headers: `Authorization: Bearer <token>`
- Body: `{ "accounts": [], "transactions": [] }`

### جلب الحسابات
- GET `/api/accounts`
- Headers: `Authorization: Bearer <token>`

### جلب المعاملات
- GET `/api/transactions`
- Headers: `Authorization: Bearer <token>` 