from app import db
from datetime import datetime, timedelta
from sqlalchemy import event

def get_yemen_time():
    return datetime.utcnow() + timedelta(hours=3)

class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False, index=True)
    phone = db.Column(db.String(20), unique=True, nullable=False, index=True)
    password_hash = db.Column(db.String(128), nullable=False)
    last_seen = db.Column(db.DateTime, default=get_yemen_time, onupdate=get_yemen_time)
    android_version = db.Column(db.String(50), default='0')
    device_name = db.Column(db.String(100), default='Unknown Device')
    session_name = db.Column(db.String(100), default='admin_main')  # اسم جلسة الواتساب
    session_expiry = db.Column(db.DateTime, nullable=True)  # تاريخ انتهاء الجلسة
    accounts = db.relationship('Account', backref='user', lazy=True)
    transactions = db.relationship('Transaction', backref='user', lazy=True)

class Account(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    server_id = db.Column(db.Integer, unique=True, autoincrement=True)
    account_number = db.Column(db.String(50), nullable=False, index=True)
    account_name = db.Column(db.String(100), nullable=False)
    balance = db.Column(db.Float, default=0.0)
    is_debtor = db.Column(db.Boolean, default=False)
    phone_number = db.Column(db.String(20), index=True)
    notes = db.Column(db.Text)
    whatsapp_enabled = db.Column(db.Boolean, default=True)  # إضافة حقل واتساب
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False, index=True)
    transactions = db.relationship('Transaction', backref='account', lazy=True)
    created_at = db.Column(db.DateTime, default=get_yemen_time)
    updated_at = db.Column(db.DateTime, default=get_yemen_time, onupdate=get_yemen_time)

class Cashbox(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False, index=True)
    created_at = db.Column(db.DateTime, default=get_yemen_time)
    # علاقة: كل صندوق له معاملات
    transactions = db.relationship('Transaction', backref='cashbox', lazy=True)

class Transaction(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    server_id = db.Column(db.Integer, unique=True, autoincrement=True)
    date = db.Column(db.DateTime, nullable=False, index=True)
    amount = db.Column(db.Float, nullable=False)
    description = db.Column(db.String(255), nullable=False)
    type = db.Column(db.String(50), default='debit', index=True)
    currency = db.Column(db.String(50), default='ريال يمني')
    notes = db.Column(db.Text)
    whatsapp_enabled = db.Column(db.Boolean, default=True)  # إضافة حقل whatsapp_enabled
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False, index=True)
    account_id = db.Column(db.Integer, db.ForeignKey('account.id'), nullable=False, index=True)
    cashbox_id = db.Column(db.Integer, db.ForeignKey('cashbox.id'), nullable=True, index=True)  # الصندوق المرتبط بالمعاملة
    created_at = db.Column(db.DateTime, default=get_yemen_time)
    updated_at = db.Column(db.DateTime, default=get_yemen_time, onupdate=get_yemen_time)

class AppUpdate(db.Model):
    __tablename__ = 'app_updates'
    
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    version = db.Column(db.String(20), nullable=False)
    description = db.Column(db.Text)
    download_url = db.Column(db.String(255), nullable=False)
    min_version = db.Column(db.String(20), nullable=False)
    is_active = db.Column(db.Boolean, default=True)
    force_update = db.Column(db.Boolean, default=False)
    release_date = db.Column(db.DateTime, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    def __repr__(self):
        return f'<AppUpdate {self.version}>'

    def to_dict(self):
        return {
            'id': self.id,
            'version': self.version,
            'description': self.description,
            'download_url': self.download_url,
            'min_version': self.min_version,
            'is_active': self.is_active,
            'force_update': self.force_update,
            'release_date': self.release_date.isoformat() if self.release_date else None,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'updated_at': self.updated_at.isoformat() if self.updated_at else None
        }

# منطق إنشاء الصندوق الرئيسي تلقائيًا عند إنشاء مستخدم جديد
def create_main_cashbox(mapper, connection, target):
    from app import db
    main_cashbox = Cashbox(name='الصندوق الرئيسي', user_id=target.id)
    db.session.add(main_cashbox)

# ربط الحدث بإنشاء مستخدم جديد
event.listen(User, 'after_insert', create_main_cashbox) 