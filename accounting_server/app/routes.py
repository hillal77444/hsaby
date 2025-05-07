from flask import Blueprint, jsonify, request
from app import db
from app.models import User, Account, Transaction
from app.utils import hash_password, verify_password, generate_sync_token
from flask_jwt_extended import jwt_required, get_jwt_identity
from datetime import datetime

main = Blueprint('main', __name__)

@main.route('/api/register', methods=['POST'])
def register():
    data = request.get_json()
    
    # التحقق من وجود المستخدم
    if User.query.filter_by(username=data['username']).first():
        return jsonify({'error': 'Username already exists'}), 400
    
    # التحقق من وجود رقم الهاتف
    if User.query.filter_by(phone=data['phone']).first():
        return jsonify({'error': 'Phone number already exists'}), 400
        
    # إنشاء مستخدم جديد
    user = User(
        username=data['username'],
        phone=data['phone'],
        password_hash=hash_password(data['password'])
    )
    db.session.add(user)
    db.session.commit()
    
    return jsonify({'message': 'User registered successfully'}), 201

@main.route('/api/login', methods=['POST'])
def login():
    data = request.get_json()
    user = User.query.filter_by(username=data['username']).first()
    
    if user and verify_password(user.password_hash, data['password']):
        token = generate_sync_token(user.id)
        return jsonify({
            'token': token,
            'user_id': user.id,
            'username': user.username
        })
    
    return jsonify({'error': 'Invalid credentials'}), 401

@main.route('/api/sync', methods=['POST'])
@jwt_required()
def sync_data():
    user_id = get_jwt_identity()
    data = request.get_json()
    
    # معالجة الحسابات
    for account_data in data.get('accounts', []):
        account = Account.query.filter_by(
            account_number=account_data['account_number'],
            user_id=user_id
        ).first()
        
        if not account:
            account = Account(
                account_number=account_data['account_number'],
                account_name=account_data['account_name'],
                balance=account_data['balance'],
                user_id=user_id
            )
            db.session.add(account)
        else:
            account.balance = account_data['balance']
    
    # معالجة المعاملات
    for transaction_data in data.get('transactions', []):
        transaction = Transaction.query.filter_by(
            id=transaction_data['id'],
            user_id=user_id
        ).first()
        
        if not transaction:
            transaction = Transaction(
                id=transaction_data['id'],
                date=datetime.fromisoformat(transaction_data['date']),
                amount=transaction_data['amount'],
                description=transaction_data['description'],
                user_id=user_id,
                account_id=transaction_data['account_id']
            )
            db.session.add(transaction)
    
    db.session.commit()
    return jsonify({'message': 'Sync completed successfully'})

@main.route('/api/accounts', methods=['GET'])
@jwt_required()
def get_accounts():
    user_id = get_jwt_identity()
    accounts = Account.query.filter_by(user_id=user_id).all()
    return jsonify([{
        'id': acc.id,
        'account_number': acc.account_number,
        'account_name': acc.account_name,
        'balance': acc.balance
    } for acc in accounts])

@main.route('/api/transactions', methods=['GET'])
@jwt_required()
def get_transactions():
    user_id = get_jwt_identity()
    transactions = Transaction.query.filter_by(user_id=user_id).all()
    return jsonify([{
        'id': trans.id,
        'date': trans.date.isoformat(),
        'amount': trans.amount,
        'description': trans.description,
        'account_id': trans.account_id
    } for trans in transactions]) 