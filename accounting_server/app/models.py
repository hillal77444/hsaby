from app import db
from datetime import datetime, timedelta

def get_yemen_time():
    return datetime.utcnow() + timedelta(hours=3)

class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False, index=True)
    phone = db.Column(db.String(20), unique=True, nullable=False, index=True)
    password_hash = db.Column(db.String(128), nullable=False)
    last_seen = db.Column(db.DateTime, default=get_yemen_time, onupdate=get_yemen_time)
    android_version = db.Column(db.String(50), default='Unknown')
    device_name = db.Column(db.String(100), default='Unknown Device')
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
    created_at = db.Column(db.DateTime, default=get_yemen_time)
    updated_at = db.Column(db.DateTime, default=get_yemen_time, onupdate=get_yemen_time) 