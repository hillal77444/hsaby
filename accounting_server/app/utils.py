import hashlib
import secrets
import string
from datetime import datetime, timedelta
import jwt
from flask import current_app
from config import Config

def hash_password(password):
    """تشفير كلمة المرور باستخدام SHA-256"""
    salt = secrets.token_hex(16)
    hash_obj = hashlib.sha256((password + salt).encode())
    return f"{salt}${hash_obj.hexdigest()}"

def verify_password(stored_password, provided_password):
    """التحقق من كلمة المرور"""
    try:
        salt, stored_hash = stored_password.split('$')
        hash_obj = hashlib.sha256((provided_password + salt).encode())
        return hash_obj.hexdigest() == stored_hash
    except:
        return False

def generate_password(length=12):
    """إنشاء كلمة مرور قوية"""
    characters = string.ascii_letters + string.digits + string.punctuation
    return ''.join(secrets.choice(characters) for _ in range(length))

def validate_phone_number(phone):
    """التحقق من صحة رقم الهاتف"""
    # يمكنك تعديل هذا النمط حسب متطلباتك
    import re
    pattern = r'^\+?[0-9]{10,15}$'
    return bool(re.match(pattern, phone))

def validate_password_strength(password):
    """التحقق من قوة كلمة المرور"""
    if len(password) < 8:
        return False, "كلمة المرور يجب أن تكون 8 أحرف على الأقل"
    
    if not any(c.isupper() for c in password):
        return False, "كلمة المرور يجب أن تحتوي على حرف كبير على الأقل"
    
    if not any(c.islower() for c in password):
        return False, "كلمة المرور يجب أن تحتوي على حرف صغير على الأقل"
    
    if not any(c.isdigit() for c in password):
        return False, "كلمة المرور يجب أن تحتوي على رقم على الأقل"
    
    if not any(c in string.punctuation for c in password):
        return False, "كلمة المرور يجب أن تحتوي على رمز خاص على الأقل"
    
    return True, "كلمة المرور قوية"

def generate_sync_token(user_id):
    payload = {
        'user_id': user_id,
        'exp': datetime.utcnow() + timedelta(hours=24)
    }
    return jwt.encode(payload, Config.JWT_SECRET_KEY, algorithm='HS256')

def verify_sync_token(token):
    try:
        payload = jwt.decode(token, Config.JWT_SECRET_KEY, algorithms=['HS256'])
        return payload['user_id']
    except:
        return None 