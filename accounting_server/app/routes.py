from flask import Blueprint, jsonify, request
from app import db
from app.models import User, Account, Transaction
from app.utils import hash_password, verify_password, generate_sync_token
from flask_jwt_extended import jwt_required, get_jwt_identity, create_access_token
from datetime import datetime
import logging
import json
import uuid

main = Blueprint('main', __name__)
logger = logging.getLogger(__name__)

def json_response(data, status_code=200):
    response = json.dumps(data, ensure_ascii=False)
    return response, status_code, {'Content-Type': 'application/json; charset=utf-8'}

def generate_server_id():
    """توليد معرف فريد للسيرفر"""
    return int(uuid.uuid4().int % (10 ** 9))  # توليد رقم فريد من 9 أرقام

@main.route('/api/register', methods=['POST'])
def register():
    try:
        data = request.get_json()
        
        # التحقق من البيانات المطلوبة
        required_fields = ['username', 'phone', 'password']
        for field in required_fields:
            if field not in data:
                return jsonify({'error': f'Missing required field: {field}'}), 400
        
        # التحقق من طول كلمة المرور
        if len(data['password']) < 6:
            return jsonify({'error': 'Password must be at least 6 characters long'}), 400
        
        # التحقق من وجود المستخدم
        if User.query.filter_by(username=data['username']).first():
            return jsonify({'error': 'اسم المستخدم موجود مسبقاً'}), 400
        
        # التحقق من وجود رقم الهاتف
        if User.query.filter_by(phone=data['phone']).first():
            return jsonify({'error': 'رقم الهاتف مسجل مسبقاً'}), 400
            
        # إنشاء مستخدم جديد
        user = User(
            username=data['username'],
            phone=data['phone'],
            password_hash=hash_password(data['password'])
        )
        db.session.add(user)
        db.session.commit()
        
        # إنشاء توكن للمستخدم الجديد
        access_token = create_access_token(identity=user.id)
        
        return jsonify({
            'message': 'تم إنشاء الحساب بنجاح',
            'token': access_token,
            'user_id': user.id,
            'username': user.username
        }), 201
        
    except Exception as e:
        logger.error(f"Registration error: {str(e)}")
        db.session.rollback()
        return jsonify({'error': 'حدث خطأ أثناء إنشاء الحساب'}), 500

@main.route('/api/login', methods=['POST'])
def login():
    try:
        data = request.get_json()
        
        # التحقق من البيانات المطلوبة
        if 'phone' not in data or 'password' not in data:
            return jsonify({'error': 'يرجى إدخال رقم الهاتف وكلمة المرور'}), 400
        
        # البحث عن المستخدم برقم الهاتف
        user = User.query.filter_by(phone=data['phone']).first()
        
        if user and verify_password(user.password_hash, data['password']):
            access_token = create_access_token(identity=user.id)
            return jsonify({
                'message': 'تم تسجيل الدخول بنجاح',
                'token': access_token,
                'user_id': user.id,
                'username': user.username
            })
        
        return jsonify({'error': 'رقم الهاتف أو كلمة المرور غير صحيحة'}), 401
        
    except Exception as e:
        logger.error(f"Login error: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء تسجيل الدخول'}), 500

@main.route('/api/sync', methods=['POST'])
@jwt_required()
def sync_data():
    try:
        data = request.get_json()
        current_user_id = get_jwt_identity()
        
        logger.info(f"Received sync request from user {current_user_id}")
        logger.debug(f"Sync data: {data}")
        
        if not data:
            logger.warning("Empty sync data received")
            return json_response({'error': 'لا توجد بيانات للمزامنة'}, 400)
        
        # التحقق من البيانات
        if 'accounts' not in data and 'transactions' not in data:
            logger.warning("No accounts or transactions in sync data")
            return json_response({'error': 'يجب إرسال بيانات الحسابات أو المعاملات'}, 400)
        
        # معالجة الحسابات
        accounts = data.get('accounts', [])
        account_mappings = []
        
        for acc_data in accounts:
            try:
                # التحقق من البيانات المطلوبة للحساب
                required_fields = ['account_name', 'balance']
                missing_fields = [field for field in required_fields if field not in acc_data]
                if missing_fields:
                    return json_response({'error': f'بيانات الحساب غير مكتملة: {", ".join(missing_fields)}'}, 400)
                
                # البحث عن الحساب باستخدام server_id إذا كان موجوداً
                account = None
                if acc_data.get('server_id') and acc_data['server_id'] > 0:  # تجاهل أي server_id سالب
                    account = Account.query.filter_by(server_id=acc_data['server_id']).first()
                
                # إذا لم يتم العثور على الحساب، ابحث باستخدام المعرف المحلي
                if not account and acc_data.get('id'):
                    account = Account.query.filter_by(id=acc_data['id']).first()
                
                if account:
                    # تحديث الحساب الموجود
                    account.account_name = acc_data.get('account_name', account.account_name)
                    account.balance = acc_data.get('balance', account.balance)
                    account.phone_number = acc_data.get('phone_number', account.phone_number)
                    account.notes = acc_data.get('notes', account.notes)
                    account.is_debtor = acc_data.get('is_debtor', account.is_debtor)
                    account.whatsapp_enabled = acc_data.get('whatsapp_enabled', account.whatsapp_enabled)
                    account.user_id = current_user_id
                    # لا نقوم بتحديث server_id إذا كان موجوداً بالفعل
                    if not account.server_id:
                        account.server_id = generate_server_id()  # توليد معرف فريد للسيرفر
                    logger.info(f"Updated account: {account.account_name}")
                else:
                    # إنشاء حساب جديد
                    account = Account(
                        account_name=acc_data.get('account_name'),
                        balance=acc_data.get('balance', 0),
                        phone_number=acc_data.get('phone_number'),
                        notes=acc_data.get('notes'),
                        is_debtor=acc_data.get('is_debtor', False),
                        whatsapp_enabled=acc_data.get('whatsapp_enabled', False),
                        user_id=current_user_id,
                        server_id=generate_server_id()  # توليد معرف فريد للسيرفر للحساب الجديد
                    )
                    db.session.add(account)
                    db.session.flush()  # للحصول على معرف الحساب
                    logger.info(f"Added new account: {account.account_name}")
                
                account_mappings.append({
                    'local_id': acc_data.get('id'),
                    'server_id': account.server_id  # استخدام server_id الفعلي
                })
            except Exception as e:
                logger.error(f"Error processing account: {str(e)}")
                return json_response({'error': f'خطأ في معالجة بيانات الحساب: {str(e)}'}, 400)
        
        # معالجة المعاملات
        transactions = data.get('transactions', [])
        transaction_mappings = []
        
        for trans_data in transactions:
            try:
                # التحقق من البيانات المطلوبة للمعاملة
                required_fields = ['amount', 'type', 'description', 'account_id', 'date']
                missing_fields = [field for field in required_fields if field not in trans_data]
                if missing_fields:
                    return json_response({'error': f'بيانات المعاملة غير مكتملة: {", ".join(missing_fields)}'}, 400)
                
                # معالجة التاريخ
                try:
                    if isinstance(trans_data.get('date'), (int, float)):
                        date = datetime.fromtimestamp(trans_data['date'] / 1000)
                    else:
                        date = datetime.fromisoformat(trans_data['date'].replace('Z', '+00:00'))
                except (ValueError, TypeError) as e:
                    return json_response({'error': f'تنسيق التاريخ غير صحيح: {str(e)}'}, 400)
                
                # البحث عن المعاملة باستخدام server_id إذا كان موجوداً
                transaction = None
                if trans_data.get('server_id') and trans_data['server_id'] > 0:  # تجاهل أي server_id سالب
                    transaction = Transaction.query.filter_by(server_id=trans_data['server_id']).first()
                
                # إذا لم يتم العثور على المعاملة، ابحث باستخدام المعرف المحلي
                if not transaction and trans_data.get('id'):
                    transaction = Transaction.query.filter_by(id=trans_data['id']).first()
                
                if transaction:
                    # تحديث المعاملة الموجودة
                    transaction.amount = trans_data.get('amount', transaction.amount)
                    transaction.type = trans_data.get('type', transaction.type)
                    transaction.description = trans_data.get('description', transaction.description)
                    transaction.notes = trans_data.get('notes', transaction.notes)
                    transaction.date = date
                    transaction.currency = trans_data.get('currency', transaction.currency)
                    transaction.whatsapp_enabled = trans_data.get('whatsapp_enabled', transaction.whatsapp_enabled)
                    transaction.account_id = trans_data.get('account_id', transaction.account_id)
                    transaction.user_id = current_user_id
                    # لا نقوم بتحديث server_id إذا كان موجوداً بالفعل
                    if not transaction.server_id:
                        transaction.server_id = generate_server_id()  # توليد معرف فريد للسيرفر
                    logger.info(f"Updated transaction: {transaction.id}")
                else:
                    # إنشاء معاملة جديدة
                    transaction = Transaction(
                        amount=trans_data.get('amount'),
                        type=trans_data.get('type'),
                        description=trans_data.get('description'),
                        notes=trans_data.get('notes'),
                        date=date,
                        currency=trans_data.get('currency'),
                        whatsapp_enabled=trans_data.get('whatsapp_enabled', False),
                        account_id=trans_data.get('account_id'),
                        user_id=current_user_id,
                        server_id=generate_server_id()  # توليد معرف فريد للسيرفر للمعاملة الجديدة
                    )
                    db.session.add(transaction)
                    db.session.flush()  # للحصول على معرف المعاملة
                    logger.info(f"Added new transaction: {transaction.id}")
                
                transaction_mappings.append({
                    'local_id': trans_data.get('id'),
                    'server_id': transaction.server_id  # استخدام server_id الفعلي
                })
            except Exception as e:
                logger.error(f"Error processing transaction: {str(e)}")
                return json_response({'error': f'خطأ في معالجة بيانات المعاملة: {str(e)}'}, 400)
        
        try:
            db.session.commit()
            logger.info("Sync completed successfully")
            return jsonify({
                'message': 'تمت المزامنة بنجاح',
                'account_mappings': account_mappings,
                'transaction_mappings': transaction_mappings
            })
        except Exception as e:
            logger.error(f"Database commit error: {str(e)}")
            db.session.rollback()
            return json_response({'error': f'خطأ في حفظ البيانات: {str(e)}'}, 500)
        
    except Exception as e:
        logger.error(f"Unexpected sync error: {str(e)}")
        db.session.rollback()
        return json_response({'error': f'حدث خطأ غير متوقع أثناء المزامنة: {str(e)}'}, 500)

@main.route('/api/accounts', methods=['GET'])
@jwt_required()
def get_accounts():
    try:
        user_id = get_jwt_identity()
        accounts = Account.query.filter_by(user_id=user_id).all()
        return jsonify([{
            'id': acc.id,
            'server_id': acc.server_id,  # استخدام server_id الفعلي
            'account_number': acc.account_number,
            'account_name': acc.account_name,
            'balance': acc.balance,
            'phone_number': acc.phone_number,
            'is_debtor': acc.is_debtor,
            'notes': acc.notes,
            'created_at': int(acc.created_at.timestamp() * 1000) if acc.created_at else None,
            'updated_at': int(acc.updated_at.timestamp() * 1000) if acc.updated_at else None
        } for acc in accounts])
    except Exception as e:
        logger.error(f"Get accounts error: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء جلب الحسابات'}), 500

@main.route('/api/transactions', methods=['GET'])
@jwt_required()
def get_transactions():
    try:
        user_id = get_jwt_identity()
        logger.info(f"Fetching transactions for user {user_id}")
        
        # جلب المعاملات
        transactions = Transaction.query.filter_by(user_id=user_id).all()
        logger.info(f"Found {len(transactions)} transactions for user {user_id}")
        
        # تحويل المعاملات إلى JSON
        transactions_json = []
        for trans in transactions:
            transaction_data = {
                'id': trans.id,
                'server_id': trans.server_id,
                'date': int(trans.date.timestamp() * 1000),
                'amount': trans.amount,
                'description': trans.description,
                'account_id': trans.account_id,
                'type': trans.type,
                'currency': {
                    'ريال يمني': 'يمني',
                    'ريال سعودي': 'سعودي',
                    'دولار أمريكي': 'دولار'
                }.get(trans.currency, trans.currency),
                'notes': trans.notes,
                'whatsapp_enabled': trans.whatsapp_enabled,
                'user_id': trans.user_id
            }
            transactions_json.append(transaction_data)
            logger.debug(f"Transaction data: {transaction_data}")
        
        logger.info(f"Returning {len(transactions_json)} transactions")
        return jsonify(transactions_json)
    except Exception as e:
        logger.error(f"Error fetching transactions: {str(e)}")
        return json_response({'error': str(e)}, 500)

@main.route('/api/debug/data', methods=['GET'])
@jwt_required()
def debug_data():
    try:
        user_id = get_jwt_identity()
        
        # جلب الحسابات
        accounts = Account.query.filter_by(user_id=user_id).all()
        accounts_data = [{
            'id': acc.id,
            'account_number': acc.account_number,
            'account_name': acc.account_name,
            'balance': acc.balance,
            'user_id': acc.user_id,
            'raw_date': str(acc.created_at) if hasattr(acc, 'created_at') else None
        } for acc in accounts]
        
        # جلب المعاملات
        transactions = Transaction.query.filter_by(user_id=user_id).all()
        transactions_data = [{
            'id': trans.id,
            'date': str(trans.date),  # التاريخ الخام من قاعدة البيانات
            'date_timestamp': int(trans.date.timestamp() * 1000),  # التاريخ كـ timestamp
            'amount': trans.amount,
            'description': trans.description,
            'account_id': trans.account_id,
            'user_id': trans.user_id,
            'type': trans.type,
            'currency': trans.currency,
            'notes': trans.notes
        } for trans in transactions]
        
        return jsonify({
            'accounts': accounts_data,
            'transactions': transactions_data,
            'user_id': user_id
        })
    except Exception as e:
        logger.error(f"Debug data error: {str(e)}")
        return jsonify({'error': f'حدث خطأ أثناء جلب بيانات التصحيح: {str(e)}'}), 500

@main.route('/api/debug/public', methods=['GET'])
def debug_public():
    try:
        # جلب جميع الحسابات
        accounts = Account.query.all()
        accounts_data = [{
            'id': acc.id,
            'server_id': acc.server_id,
            'account_number': acc.account_number,
            'account_name': acc.account_name,
            'balance': acc.balance,
            'user_id': acc.user_id,
            'phone_number': acc.phone_number,
            'is_debtor': acc.is_debtor,
            'notes': acc.notes,
            'whatsapp_enabled': acc.whatsapp_enabled,
            'created_at': str(acc.created_at) if acc.created_at else None,
            'updated_at': str(acc.updated_at) if acc.updated_at else None
        } for acc in accounts]
        
        # جلب جميع المعاملات
        transactions = Transaction.query.all()
        transactions_data = [{
            'id': trans.id,
            'server_id': trans.server_id,
            'date': str(trans.date),
            'date_timestamp': int(trans.date.timestamp() * 1000),
            'amount': trans.amount,
            'description': trans.description,
            'type': trans.type,
            'currency': trans.currency,
            'account_id': trans.account_id,
            'user_id': trans.user_id,
            'notes': trans.notes,
            'whatsapp_enabled': trans.whatsapp_enabled,
            'created_at': str(trans.created_at) if hasattr(trans, 'created_at') and trans.created_at else None,
            'updated_at': str(trans.updated_at) if hasattr(trans, 'updated_at') and trans.updated_at else None
        } for trans in transactions]
        
        return jsonify({
            'accounts': accounts_data,
            'transactions': transactions_data
        })
    except Exception as e:
        logger.error(f"Public debug data error: {str(e)}")
        return jsonify({'error': f'Error fetching debug data: {str(e)}'}), 500

@main.route('/api/transactions/<int:transaction_id>', methods=['DELETE'])
@jwt_required()
def delete_transaction_by_id(transaction_id):
    try:
        user_id = get_jwt_identity()
        
        # البحث عن المعاملة
        transaction = Transaction.query.filter_by(
            id=transaction_id,
            user_id=user_id
        ).first()
        
        if not transaction:
            return json_response({'error': 'المعاملة غير موجودة'}, 404)
        
        # حذف المعاملة
        db.session.delete(transaction)
        db.session.commit()
        
        logger.info(f"Transaction {transaction_id} deleted successfully by user {user_id}")
        return json_response({'message': 'تم حذف القيد بنجاح'})
        
    except Exception as e:
        logger.error(f"Error deleting transaction: {str(e)}")
        db.session.rollback()
        return json_response({'error': 'حدث خطأ أثناء حذف المعاملة'}, 500)

@main.route('/api/sync/changes', methods=['POST', 'GET'])
@jwt_required()
def sync_changes():
    try:
        current_user_id = get_jwt_identity()
        
        if request.method == 'POST':
            # استقبال التغييرات من التطبيق
            data = request.get_json()
            
            # إنشاء خرائط لتخزين المعرفات الجديدة
            account_id_map = []
            transaction_id_map = []
            
            # معالجة الحسابات
            if 'accounts' in data:
                for account_data in data['accounts']:
                    try:
                        # التحقق من البيانات المطلوبة
                        required_fields = ['account_number', 'account_name', 'balance']
                        missing_fields = [field for field in required_fields if field not in account_data]
                        if missing_fields:
                            return json_response({'error': f'بيانات الحساب غير مكتملة: {", ".join(missing_fields)}'}, 400)
                        
                        # البحث عن الحساب
                        account = Account.query.filter_by(
                            account_number=account_data['account_number'],
                            user_id=current_user_id
                        ).first()
                        
                        if not account:
                            # حساب جديد
                            account = Account(
                                account_number=account_data['account_number'],
                                account_name=account_data['account_name'],
                                balance=account_data['balance'],
                                is_debtor=account_data.get('is_debtor', False),
                                phone_number=account_data.get('phone_number'),
                                notes=account_data.get('notes'),
                                whatsapp_enabled=account_data.get('whatsapp_enabled', False),
                                user_id=current_user_id
                            )
                            db.session.add(account)
                            db.session.flush()  # للحصول على المعرف الجديد
                            account_id_map.append({
                                'localId': account_data.get('id'),
                                'serverId': account.id
                            })
                        else:
                            # تحديث الحساب الموجود
                            account.balance = account_data['balance']
                            if 'is_debtor' in account_data:
                                account.is_debtor = account_data['is_debtor']
                            if 'phone_number' in account_data:
                                account.phone_number = account_data['phone_number']
                            if 'notes' in account_data:
                                account.notes = account_data['notes']
                            if 'whatsapp_enabled' in account_data:
                                account.whatsapp_enabled = account_data['whatsapp_enabled']
                            account_id_map.append({
                                'localId': account_data.get('id'),
                                'serverId': account.id
                            })
                    except Exception as e:
                        logger.error(f"Error processing account: {str(e)}")
                        return json_response({'error': f'خطأ في معالجة بيانات الحساب: {str(e)}'}, 400)
            
            # معالجة المعاملات الجديدة
            if 'new_transactions' in data:
                for transaction_data in data['new_transactions']:
                    try:
                        # التحقق من البيانات المطلوبة
                        required_fields = ['date', 'amount', 'description', 'account_id']
                        missing_fields = [field for field in required_fields if field not in transaction_data]
                        if missing_fields:
                            return json_response({'error': f'بيانات المعاملة غير مكتملة: {", ".join(missing_fields)}'}, 400)
                        
                        # معالجة التاريخ
                        try:
                            if isinstance(transaction_data['date'], (int, float)):
                                date = datetime.fromtimestamp(transaction_data['date'] / 1000)
                            else:
                                date = datetime.fromisoformat(transaction_data['date'].replace('Z', '+00:00'))
                        except (ValueError, TypeError) as e:
                            return json_response({'error': f'تنسيق التاريخ غير صحيح: {str(e)}'}, 400)
                        
                        # إنشاء معاملة جديدة
                        transaction = Transaction(
                            date=date,
                            amount=transaction_data['amount'],
                            description=transaction_data['description'],
                            type=transaction_data.get('type', 'debit'),
                            currency=transaction_data.get('currency', 'ريال يمني'),
                            notes=transaction_data.get('notes', ''),
                            whatsapp_enabled=transaction_data.get('whatsapp_enabled', True),
                            user_id=current_user_id,
                            account_id=transaction_data['account_id']
                        )
                        db.session.add(transaction)
                        db.session.flush()  # للحصول على المعرف الجديد
                        transaction_id_map.append({
                            'localId': transaction_data.get('id'),
                            'serverId': transaction.id
                        })
                    except Exception as e:
                        logger.error(f"Error processing new transaction: {str(e)}")
                        return json_response({'error': f'خطأ في معالجة بيانات المعاملة الجديدة: {str(e)}'}, 400)
            
            # معالجة المعاملات المعدلة
            if 'modified_transactions' in data:
                for transaction_data in data['modified_transactions']:
                    try:
                        # التحقق من البيانات المطلوبة
                        required_fields = ['id', 'date', 'amount', 'description', 'account_id']
                        missing_fields = [field for field in required_fields if field not in transaction_data]
                        if missing_fields:
                            return json_response({'error': f'بيانات المعاملة المعدلة غير مكتملة: {", ".join(missing_fields)}'}, 400)
                        
                        # البحث عن المعاملة
                        transaction = Transaction.query.filter_by(
                            id=transaction_data['id'],
                            user_id=current_user_id
                        ).first()
                        
                        if not transaction:
                            return json_response({'error': f'المعاملة غير موجودة: {transaction_data["id"]}'}, 404)
                        
                        # تحديث المعاملة
                        try:
                            if isinstance(transaction_data['date'], (int, float)):
                                date = datetime.fromtimestamp(transaction_data['date'] / 1000)
                            else:
                                date = datetime.fromisoformat(transaction_data['date'].replace('Z', '+00:00'))
                        except (ValueError, TypeError) as e:
                            return json_response({'error': f'تنسيق التاريخ غير صحيح: {str(e)}'}, 400)
                        
                        transaction.date = date
                        transaction.amount = transaction_data['amount']
                        transaction.description = transaction_data['description']
                        if 'type' in transaction_data:
                            transaction.type = transaction_data['type']
                        if 'currency' in transaction_data:
                            transaction.currency = transaction_data['currency']
                        if 'notes' in transaction_data:
                            transaction.notes = transaction_data['notes']
                        if 'whatsapp_enabled' in transaction_data:
                            transaction.whatsapp_enabled = transaction_data['whatsapp_enabled']
                        transaction.account_id = transaction_data['account_id']
                    except Exception as e:
                        logger.error(f"Error processing modified transaction: {str(e)}")
                        return json_response({'error': f'خطأ في معالجة بيانات المعاملة المعدلة: {str(e)}'}, 400)
            
            try:
                db.session.commit()
                return json_response({
                    'message': 'تمت المزامنة بنجاح',
                    'accountIdMap': account_id_map,
                    'transactionIdMap': transaction_id_map
                })
            except Exception as e:
                logger.error(f"Database commit error: {str(e)}")
                db.session.rollback()
                return json_response({'error': f'خطأ في حفظ البيانات: {str(e)}'}, 500)
            
        elif request.method == 'GET':
            # إرسال التغييرات إلى التطبيق
            last_sync = request.args.get('last_sync', 0)
            last_sync = int(last_sync)
            
            # جلب التغييرات الجديدة
            changes = {
                'transactions': [],
                'accounts': []
            }
            
            # جلب المعاملات الجديدة
            new_transactions = Transaction.query.filter(
                Transaction.user_id == current_user_id,
                Transaction.updated_at > last_sync
            ).all()
            
            for transaction in new_transactions:
                changes['transactions'].append({
                    'id': transaction.id,
                    'account_id': transaction.account_id,
                    'amount': transaction.amount,
                    'type': transaction.type,
                    'description': transaction.description,
                    'date': transaction.date.timestamp() * 1000,
                    'currency': transaction.currency,
                    'notes': transaction.notes,
                    'whatsapp_enabled': transaction.whatsapp_enabled,
                    'user_id': transaction.user_id,
                    'updated_at': transaction.updated_at.timestamp() * 1000
                })
            
            # جلب الحسابات الجديدة
            new_accounts = Account.query.filter(
                Account.user_id == current_user_id,
                Account.updated_at > last_sync
            ).all()
            
            for account in new_accounts:
                changes['accounts'].append({
                    'id': account.id,
                    'account_number': account.account_number,
                    'account_name': account.account_name,
                    'balance': account.balance,
                    'is_debtor': account.is_debtor,
                    'phone_number': account.phone_number,
                    'notes': account.notes,
                    'whatsapp_enabled': account.whatsapp_enabled,
                    'user_id': account.user_id,
                    'updated_at': account.updated_at.timestamp() * 1000
                })
            
            return json_response(changes)
            
    except Exception as e:
        logger.error(f"Error in sync_changes: {str(e)}")
        db.session.rollback()
        return json_response({'error': str(e)}, 500) 