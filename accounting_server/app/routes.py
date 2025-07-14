from flask import Blueprint, jsonify, request
from app import db
from app.models import User, Account, Transaction, AppUpdate, Cashbox
from app.utils import hash_password, verify_password, generate_sync_token
from flask_jwt_extended import jwt_required, get_jwt_identity, create_access_token
from datetime import datetime, timezone, timedelta
import logging
import json
from sqlalchemy import func
from sqlalchemy import case
import requests
from app.admin_routes import send_transaction_notification, calculate_and_notify_transaction, send_transaction_update_notification, send_transaction_delete_notification
import traceback
from sqlalchemy.exc import IntegrityError

main = Blueprint('main', __name__)
logger = logging.getLogger(__name__)

# تعريف توقيت اليمن (UTC+3)
YEMEN_TIMEZONE = timezone(timedelta(hours=0))

def get_yemen_time():
    """الحصول على التوقيت الحالي بتوقيت اليمن"""
    return datetime.now(YEMEN_TIMEZONE)

def json_response(data, status_code=200):
    response = json.dumps(data, ensure_ascii=False)
    return response, status_code, {'Content-Type': 'application/json; charset=utf-8'}

def to_millis(dt):
    """توحيد التاريخ ليكون timestamp بالميلي ثانية (UTC) بغض النظر عن نوعه في قاعدة البيانات"""
    if dt is None:
        return None
    if isinstance(dt, (int, float)):
        # إذا كان مخزنًا بالفعل كـ timestamp بالميلي ثانية
        return int(dt)
    if isinstance(dt, str):
        try:
            # جرب تحويله إلى datetime
            if 'T' in dt:
                # صيغة ISO
                dt = datetime.fromisoformat(dt.replace('Z', '+00:00'))
            else:
                # صيغة datetime التقليدية
                dt = datetime.fromisoformat(dt)
        except Exception:
            return None
    # الآن dt هو datetime
    # إذا كان بتوقيت اليمن، حوله إلى UTC
    if dt.tzinfo:
        dt_utc = dt.astimezone(timezone.utc)
    else:
        dt_utc = dt - timedelta(hours=1)
    return int(dt_utc.timestamp() * 1000)

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
            return jsonify({'error': 'اسم المستخدم موجود مسبقاً'}), 409
        
        # التحقق من وجود رقم الهاتف
        if User.query.filter_by(phone=data['phone']).first():
            return jsonify({'error': 'رقم الهاتف مسجل مسبقاً'}), 409
            
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

        if not user:
            return jsonify({'error': 'رقم الهاتف غير مسجل'}), 401

        if not verify_password(user.password_hash, data['password']):
            return jsonify({'error': 'كلمة المرور غير صحيحة'}), 401

        # إذا كانت البيانات صحيحة
        access_token = create_access_token(identity=user.id)
        return jsonify({
            'message': 'تم تسجيل الدخول بنجاح',
            'token': access_token,
            'user_id': user.id,
            'username': user.username
        })

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
        
        print(f"\nعدد الحسابات: {len(accounts)}")
        
        for acc_data in accounts:
            print("\nمعالجة الحساب:")
            print(json.dumps(acc_data, ensure_ascii=False, indent=2))
            try:
                # التحقق من البيانات المطلوبة للحساب
                required_fields = ['account_name', 'balance']
                missing_fields = [field for field in required_fields if field not in acc_data]
                if missing_fields:
                    return json_response({'error': f'بيانات الحساب غير مكتملة: {", ".join(missing_fields)}'}, 400)
                
                # البحث عن الحساب باستخدام server_id إذا كان موجوداً
                account = None
                if acc_data.get('server_id') and acc_data['server_id'] > 0:
                    account = Account.query.filter_by(
                        server_id=acc_data['server_id'],
                        user_id=current_user_id
                    ).first()
                
                # التحقق من تكرار الحساب بالقيم الصحيحة
                if not account:
                    existing_account = Account.query.filter_by(
                        account_number=acc_data.get('account_number'),
                        user_id=current_user_id,
                        phone_number=acc_data.get('phone_number')
                    ).first()
                    if existing_account:
                        account = existing_account
                        logger.info(f"Found existing account: {account.account_name}")
                
                if account:
                    # تحديث الحساب الموجود
                    account.account_name = acc_data.get('account_name', account.account_name)
                    account.balance = acc_data.get('balance', account.balance)
                    account.phone_number = acc_data.get('phone_number', account.phone_number)
                    account.notes = acc_data.get('notes', account.notes)
                    account.is_debtor = acc_data.get('is_debtor', account.is_debtor)
                    account.whatsapp_enabled = acc_data.get('whatsapp_enabled', account.whatsapp_enabled)
                    account.user_id = current_user_id
                    logger.info(f"Updated account: {account.account_name}")
                else:
                    last_account = Account.query.order_by(Account.server_id.desc()).first()
                    new_server_id = (last_account.server_id + 1) if last_account and last_account.server_id else 1

                    # إنشاء حساب جديد
                    account = Account(
                        account_number=acc_data.get('account_number'),
                        account_name=acc_data.get('account_name'),
                        balance=acc_data.get('balance', 0),
                        phone_number=acc_data.get('phone_number'),
                        notes=acc_data.get('notes'),
                        is_debtor=acc_data.get('is_debtor', False),
                        whatsapp_enabled=acc_data.get('whatsapp_enabled', False),
                        user_id=current_user_id,
                        server_id=new_server_id
                    )
                    db.session.add(account)
                    try:
                        db.session.flush()
                        logger.info(f"Added new account: {account.account_name}")
                    except IntegrityError:
                        db.session.rollback()
                        # جلب الحساب الأول وتحديثه
                        existing_account = Account.query.filter_by(
                            account_number=acc_data.get('account_number'),
                            user_id=current_user_id,
                            phone_number=acc_data.get('phone_number')
                        ).first()
                        if existing_account:
                            existing_account.account_name = acc_data.get('account_name', existing_account.account_name)
                            existing_account.balance = acc_data.get('balance', existing_account.balance)
                            existing_account.phone_number = acc_data.get('phone_number', existing_account.phone_number)
                            existing_account.notes = acc_data.get('notes', existing_account.notes)
                            existing_account.is_debtor = acc_data.get('is_debtor', existing_account.is_debtor)
                            existing_account.whatsapp_enabled = acc_data.get('whatsapp_enabled', existing_account.whatsapp_enabled)
                            existing_account.user_id = current_user_id
                            db.session.flush()
                            account = existing_account
                            logger.info(f"Updated existing account after IntegrityError: {account.account_name}")
                        else:
                            logger.error("IntegrityError but account not found!")
                            return json_response({'error': 'حدث خطأ غير متوقع أثناء إضافة الحساب'}, 500)
                
                account_mappings.append({
                    'local_id': acc_data.get('id'),
                    'server_id': account.server_id
                })
            except Exception as e:
                logger.error(f"Error processing account: {str(e)}")
                return json_response({'error': f'خطأ في معالجة بيانات الحساب: {str(e)}'}, 400)
        print("Transaction Mappings:", json.dumps(account_mappings, ensure_ascii=False, indent=2))
        # معالجة المعاملات
        transactions = data.get('transactions', [])
        transaction_mappings = []
        
        print(f"\nعدد المعاملات: {len(transactions)}")
        
        for trans_data in transactions:
            print("\nمعالجة المعاملة:")
            print(json.dumps(trans_data, ensure_ascii=False, indent=2))
            try:
                # التحقق من البيانات المطلوبة للمعاملة
                required_fields = ['amount', 'type', 'description', 'account_id', 'date']
                missing_fields = [field for field in required_fields if field not in trans_data]
                if missing_fields:
                    return json_response({'error': f'بيانات المعاملة غير مكتملة: {", ".join(missing_fields)}'}, 400)
                
                # توحيد التاريخ القادم من التطبيق
                date = to_millis(trans_data.get('date'))
                
                # التحقق من صحة التاريخ
                if date > int(get_yemen_time().timestamp() * 1000):
                    return json_response({'error': 'التاريخ غير صحيح'}, 400)
                
                # التحقق من صحة الحساب
                account = Account.query.filter_by(
                    id=trans_data.get('account_id'),
                    user_id=current_user_id
                ).first()
                
                if not account:
                    return json_response({'error': 'الحساب غير موجود'}, 400)
                
                # البحث عن المعاملة باستخدام server_id إذا كان موجوداً
                transaction = None
                if trans_data.get('server_id') and trans_data['server_id'] > 0:
                    transaction = Transaction.query.filter_by(
                        server_id=trans_data['server_id'],
                        user_id=current_user_id
                    ).first()
                
                # التحقق من تكرار المعاملة بالقيم الصحيحة
                if not transaction:
                    existing_transaction = Transaction.query.filter_by(
                        amount=trans_data.get('amount'),
                        type=trans_data.get('type'),
                        description=trans_data.get('description'),
                        date=to_millis(trans_data.get('date')),
                        account_id=trans_data.get('account_id'),
                        user_id=current_user_id
                    ).first()
                    if existing_transaction:
                        transaction = existing_transaction
                        logger.info(f"Found existing transaction: {transaction.id}")
                
                if transaction:
                    # حفظ المبلغ والتاريخ القديم قبل التحديث
                    old_amount = transaction.amount
                    old_date = transaction.date
                    # معالجة cashbox_id
                    new_cashbox_id = trans_data.get('cashbox_id')
                    print(f"Original cashbox_id from request: {new_cashbox_id}")
                    print(f"Current transaction cashbox_id: {transaction.cashbox_id}")
                    if not new_cashbox_id or new_cashbox_id == -1:
                        print(f"Cashbox_id is invalid ({new_cashbox_id}), using main cashbox")
                        main_cashbox = Cashbox.query.filter_by(user_id=current_user_id, name='الصندوق الرئيسي').first()
                        if not main_cashbox:
                            print("Main cashbox not found, creating new one")
                            main_cashbox = Cashbox(name='الصندوق الرئيسي', user_id=current_user_id)
                            db.session.add(main_cashbox)
                            db.session.commit()
                        new_cashbox_id = main_cashbox.id
                        print(f"Using main cashbox ID: {new_cashbox_id}")
                    else:
                        print(f"Using provided cashbox_id: {new_cashbox_id}")
                    # تحديث المعاملة الموجودة
                    transaction.amount = trans_data.get('amount', transaction.amount)
                    transaction.type = trans_data.get('type', transaction.type)
                    transaction.description = trans_data.get('description', transaction.description)
                    transaction.notes = trans_data.get('notes', transaction.notes)
                    transaction.date = date
                    transaction.currency = trans_data.get('currency', transaction.currency)
                    transaction.whatsapp_enabled = trans_data.get('whatsapp_enabled', transaction.whatsapp_enabled)
                    transaction.account_id = trans_data.get('account_id', transaction.account_id)
                    transaction.cashbox_id = new_cashbox_id  # تحديث cashbox_id
                    transaction.user_id = current_user_id
                    print(f"Updated transaction cashbox_id to: {transaction.cashbox_id}")
                    logger.info(f"Updated transaction: {transaction.id}")
                    # إرسال إشعار فقط إذا تم تغيير المبلغ
                    if old_amount != transaction.amount:
                        try:
                            notification_result = send_transaction_update_notification(transaction.id, old_amount, old_date)
                            if notification_result.get('status') == 'error':
                                logger.error(f"خطأ في إرسال إشعار التحديث: {notification_result.get('message')}")
                        except Exception as e:
                            logger.error(f"Error sending transaction update notification: {str(e)}")
                else:
                    # الحصول على آخر server_id
                    last_transaction = Transaction.query.order_by(Transaction.server_id.desc()).first()
                    new_server_id = (last_transaction.server_id + 1) if last_transaction else 1
                    # منطق اختيار الصندوق
                    cashbox_id = trans_data.get('cashbox_id')
                    print(f"New transaction - Original cashbox_id from request: {cashbox_id}")
                    if not cashbox_id or cashbox_id == -1:
                        print(f"New transaction - Cashbox_id is invalid ({cashbox_id}), using main cashbox")
                        main_cashbox = Cashbox.query.filter_by(user_id=current_user_id, name='الصندوق الرئيسي').first()
                        if not main_cashbox:
                            print("New transaction - Main cashbox not found, creating new one")
                            main_cashbox = Cashbox(name='الصندوق الرئيسي', user_id=current_user_id)
                            db.session.add(main_cashbox)
                            db.session.commit()
                        cashbox_id = main_cashbox.id
                        print(f"New transaction - Using main cashbox ID: {cashbox_id}")
                    else:
                        print(f"New transaction - Using provided cashbox_id: {cashbox_id}")
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
                        server_id=new_server_id,
                        cashbox_id=cashbox_id
                    )
                    print(f"New transaction - Final cashbox_id: {transaction.cashbox_id}")
                    db.session.add(transaction)
                    try:
                        db.session.flush()
                        logger.info(f"Added new transaction: {transaction.id}")
                    except IntegrityError:
                        db.session.rollback()
                        # جلب المعاملة الأولى وتحديثها
                        existing_transaction = Transaction.query.filter_by(
                            amount=trans_data.get('amount'),
                            type=trans_data.get('type'),
                            description=trans_data.get('description'),
                            date=date,
                            account_id=trans_data.get('account_id'),
                            user_id=current_user_id
                        ).first()
                        if existing_transaction:
                            existing_transaction.notes = trans_data.get('notes', existing_transaction.notes)
                            existing_transaction.currency = trans_data.get('currency', existing_transaction.currency)
                            existing_transaction.whatsapp_enabled = trans_data.get('whatsapp_enabled', existing_transaction.whatsapp_enabled)
                            existing_transaction.cashbox_id = cashbox_id
                            db.session.flush()
                            transaction = existing_transaction
                            logger.info(f"Updated existing transaction after IntegrityError: {transaction.id}")
                        else:
                            logger.error("IntegrityError but transaction not found!")
                            return json_response({'error': 'حدث خطأ غير متوقع أثناء إضافة المعاملة'}, 500)
                    # حساب الرصيد وإرسال الإشعار
                    try:
                        result = calculate_and_notify_transaction(transaction.id)
                        if result.get('status') != 'success':
                            logger.error(f"Error processing transaction: {result.get('message')}")
                    except Exception as e:
                        logger.error(f"Error in transaction notification: {str(e)}")
                transaction_mappings.append({
                    'local_id': trans_data.get('id'),
                    'server_id': transaction.server_id
                 })
            except Exception as e:
                logger.error(f"Error processing transaction: {str(e)}")
                return json_response({'error': f'خطأ في معالجة بيانات المعاملة: {str(e)}'}, 400)
        
        # طباعة محتوى transaction_mappings
        print("Transaction Mappings:", json.dumps(transaction_mappings, ensure_ascii=False, indent=2))
        
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
            'server_id': acc.server_id,
            'account_number': acc.account_number,
            'account_name': acc.account_name,
            'balance': acc.balance,
            'phone_number': acc.phone_number,
            'is_debtor': acc.is_debtor,
            'notes': acc.notes,
            'whatsapp_enabled': acc.whatsapp_enabled,
            'user_id': acc.user_id,
            'created_at': to_millis(acc.created_at) if acc.created_at else None,
            'updated_at': to_millis(acc.updated_at) if acc.updated_at else None
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
        
        # جلب limit و offset من باراميترات الاستعلام مع قيم افتراضية
        limit = request.args.get('limit', default=100, type=int)
        offset = request.args.get('offset', default=0, type=int)
        
        transactions = Transaction.query.filter_by(user_id=user_id).order_by(Transaction.id).offset(offset).limit(limit).all()
        logger.info(f"Found {len(transactions)} transactions for user {user_id} (offset={offset}, limit={limit})")
        
        transactions_json = []
        for trans in transactions:
            transaction_data = {
                'id': trans.id,
                'server_id': trans.server_id,
                'date': to_millis(trans.date),
                'amount': trans.amount,
                'description': trans.description,
                'account_id': trans.account_id,
                'type': trans.type,
                'currency': trans.currency,
                'notes': trans.notes,
                'whatsapp_enabled': trans.whatsapp_enabled,
                'user_id': trans.user_id,
                'cashbox_id': trans.cashbox_id,
                'created_at': to_millis(trans.created_at),
                'updated_at': to_millis(trans.updated_at)
            }
            transactions_json.append(transaction_data)
            logger.debug(f"Transaction data: {transaction_data}")
        
        logger.info(f"Returning {len(transactions_json)} transactions (offset={offset}, limit={limit})")
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
            'date': to_millis(trans.date),
            'amount': trans.amount,
            'description': trans.description,
            'account_id': trans.account_id,
            'user_id': trans.user_id,
            'type': trans.type,
            'currency': trans.currency,
            'notes': trans.notes,
            'cashbox_id': trans.cashbox_id
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
            'created_at': to_millis(acc.created_at) if acc.created_at else None,
            'updated_at': to_millis(acc.updated_at) if acc.updated_at else None
        } for acc in accounts]
        
        # جلب جميع المعاملات
        transactions = Transaction.query.all()
        transactions_data = [{
            'id': trans.id,
            'server_id': trans.server_id,
            'date': to_millis(trans.date),
            'amount': trans.amount,
            'description': trans.description,
            'type': trans.type,
            'currency': trans.currency,
            'account_id': trans.account_id,
            'user_id': trans.user_id,
            'notes': trans.notes,
            'whatsapp_enabled': trans.whatsapp_enabled,
            'cashbox_id': trans.cashbox_id,
            'created_at': to_millis(trans.created_at) if hasattr(trans, 'created_at') and trans.created_at else None,
            'updated_at': to_millis(trans.updated_at) if hasattr(trans, 'updated_at') and trans.updated_at else None
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
        
        # البحث عن المعاملة باستخدام server_id
        transaction = Transaction.query.filter_by(
            server_id=transaction_id,
            user_id=user_id
        ).first()
        
        if not transaction:
            return json_response({'error': 'المعاملة غير موجودة'}, 404)
        
        # حفظ معلومات المعاملة قبل الحذف
        account_id = transaction.account_id
        currency = transaction.currency

        # معالجة التاريخ قبل أي حسابات
        transaction_date = transaction.date
        if isinstance(transaction_date, str):
            try:
                transaction_date = datetime.fromisoformat(transaction_date)
            except Exception:
                transaction_date = datetime.strptime(transaction_date, "%Y-%m-%d %H:%M:%S.%f")
        transaction.date = transaction_date
        
        # حذف المعاملة
        db.session.delete(transaction)
        db.session.commit()
        
        # حساب الرصيد النهائي بعد الحذف (مباشرة في قاعدة البيانات)
        total_credits = db.session.query(func.coalesce(func.sum(Transaction.amount), 0)).filter(
            Transaction.account_id == account_id,
            Transaction.currency == currency,
            Transaction.type == 'credit'
        ).scalar()
        total_debits = db.session.query(func.coalesce(func.sum(Transaction.amount), 0)).filter(
            Transaction.account_id == account_id,
            Transaction.currency == currency,
            Transaction.type == 'debit'
        ).scalar()
        balance = total_credits - total_debits
        
        # إرسال إشعار الواتساب بعد الحذف
        notification_result = send_transaction_delete_notification(transaction, balance)
        if notification_result.get('status') == 'error':
            logger.error(f"خطأ في إرسال إشعار الحذف: {notification_result.get('message')}")
        
        logger.info(f"Transaction with server_id {transaction_id} deleted successfully by user {user_id}")
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
                        
                        # البحث عن الحساب باستخدام server_id إذا كان موجوداً
                        account = None
                        if account_data.get('server_id'):
                            account = Account.query.filter_by(server_id=account_data['server_id']).first()
                        
                        # إذا لم يتم العثور على الحساب، ابحث باستخدام رقم الحساب
                        if not account:
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
                                'serverId': account.server_id
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
                                'serverId': account.server_id
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
                                date = datetime.fromtimestamp(transaction_data['date'] / 1000, YEMEN_TIMEZONE)
                            else:
                                date = datetime.fromisoformat(transaction_data['date'].replace('Z', '+00:00')).astimezone(YEMEN_TIMEZONE)
                        except (ValueError, TypeError) as e:
                            return json_response({'error': f'تنسيق التاريخ غير صحيح: {str(e)}'}, 400)
                        
                        # منطق اختيار الصندوق
                        cashbox_id = transaction_data.get('cashbox_id')
                        if not cashbox_id or cashbox_id == -1:
                            # إذا لم يتم تحديد صندوق أو كان -1، استخدم الصندوق الرئيسي
                            main_cashbox = Cashbox.query.filter_by(user_id=current_user_id, name='الصندوق الرئيسي').first()
                            if not main_cashbox:
                                # إنشاء صندوق رئيسي إذا لم يكن موجوداً
                                main_cashbox = Cashbox(name='الصندوق الرئيسي', user_id=current_user_id)
                                db.session.add(main_cashbox)
                                db.session.commit()
                            cashbox_id = main_cashbox.id

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
                            account_id=transaction_data['account_id'],
                            cashbox_id=cashbox_id
                        )
                        db.session.add(transaction)
                        db.session.flush()  # للحصول على المعرف الجديد
                        # إرسال إشعار بعد إضافة المعاملة الجديدة
                        try:
                            result = calculate_and_notify_transaction(transaction.id)
                            if result.get('status') != 'success':
                                logger.error(f"Error processing transaction: {result.get('message')}")
                        except Exception as e:
                            logger.error(f"Error in transaction notification: {str(e)}")
                        transaction_id_map.append({
                            'localId': transaction_data.get('id'),
                            'serverId': transaction.server_id
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
                        
                        # البحث عن المعاملة باستخدام server_id إذا كان موجوداً
                        transaction = None
                        if transaction_data.get('server_id'):
                            transaction = Transaction.query.filter_by(server_id=transaction_data['server_id']).first()
                        
                        # إذا لم يتم العثور على المعاملة، ابحث باستخدام المعرف المحلي
                        if not transaction:
                            transaction = Transaction.query.filter_by(
                                id=transaction_data['id'],
                                user_id=current_user_id
                            ).first()
                        
                        if not transaction:
                            return json_response({'error': f'المعاملة غير موجودة: {transaction_data["id"]}'}, 404)
                        
                        # تحديث المعاملة
                        try:
                            if isinstance(transaction_data['date'], (int, float)):
                                date = datetime.fromtimestamp(transaction_data['date'] / 1000, YEMEN_TIMEZONE)
                            else:
                                date = datetime.fromisoformat(transaction_data['date'].replace('Z', '+00:00')).astimezone(YEMEN_TIMEZONE)
                        except (ValueError, TypeError) as e:
                            return json_response({'error': f'تنسيق التاريخ غير صحيح: {str(e)}'}, 400)
                        
                        # حفظ المبلغ والتاريخ القديم قبل التحديث
                        old_amount = transaction.amount
                        old_date = transaction.date
                        
                        # معالجة cashbox_id
                        new_cashbox_id = transaction_data.get('cashbox_id')
                        print(f"Original cashbox_id from request: {new_cashbox_id}")
                        print(f"Current transaction cashbox_id: {transaction.cashbox_id}")
                        
                        if not new_cashbox_id or new_cashbox_id == -1:
                            print(f"Cashbox_id is invalid ({new_cashbox_id}), using main cashbox")
                            # إذا لم يتم تحديد صندوق أو كان -1، استخدم الصندوق الرئيسي
                            main_cashbox = Cashbox.query.filter_by(user_id=current_user_id, name='الصندوق الرئيسي').first()
                            if not main_cashbox:
                                print("Main cashbox not found, creating new one")
                                # إنشاء صندوق رئيسي إذا لم يكن موجوداً
                                main_cashbox = Cashbox(name='الصندوق الرئيسي', user_id=current_user_id)
                                db.session.add(main_cashbox)
                                db.session.commit()
                            new_cashbox_id = main_cashbox.id
                            print(f"Using main cashbox ID: {new_cashbox_id}")
                        else:
                            print(f"Using provided cashbox_id: {new_cashbox_id}")
                        
                        # تحديث المعاملة
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
                        transaction.cashbox_id = new_cashbox_id  # تحديث cashbox_id
                        transaction.user_id = current_user_id
                        print(f"Updated transaction cashbox_id to: {transaction.cashbox_id}")
                        logger.info(f"Updated transaction: {transaction.id}")
                        
                        # إرسال إشعار فقط إذا تم تغيير المبلغ
                        if old_amount != transaction.amount:
                            try:
                                notification_result = send_transaction_update_notification(transaction.id, old_amount, old_date)
                                if notification_result.get('status') == 'error':
                                    logger.error(f"خطأ في إرسال إشعار التحديث: {notification_result.get('message')}")
                            except Exception as e:
                                logger.error(f"Error sending transaction update notification: {str(e)}")
                                # لا نوقف العملية إذا فشل الإرسال
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
            
            logger.info(f"Fetching changes for user {current_user_id} since {last_sync}")
            
            # جلب التغييرات الجديدة
            changes = {
                'transactions': [],
                'accounts': []
            }
            
            # جلب المعاملات المحدثة
            new_transactions = Transaction.query.filter(
                Transaction.user_id == current_user_id,
                Transaction.updated_at > last_sync
            ).all()
            
            logger.info(f"Found {len(new_transactions)} updated transactions")
            
            for transaction in new_transactions:
                changes['transactions'].append({
                    'id': transaction.id,
                    'server_id': transaction.server_id,
                    'account_id': transaction.account_id,
                    'amount': transaction.amount,
                    'type': transaction.type,
                    'description': transaction.description,
                    'date': to_millis(transaction.date),
                    'currency': transaction.currency,
                    'notes': transaction.notes,
                    'whatsapp_enabled': transaction.whatsapp_enabled,
                    'user_id': transaction.user_id,
                    'cashbox_id': transaction.cashbox_id,
                    'updated_at': to_millis(transaction.updated_at)
                })
            
            # جلب الحسابات المحدثة
            new_accounts = Account.query.filter(
                Account.user_id == current_user_id,
                Account.updated_at > last_sync
            ).all()
            
            logger.info(f"Found {len(new_accounts)} updated accounts")
            
            for account in new_accounts:
                changes['accounts'].append({
                    'id': account.id,
                    'server_id': account.server_id,
                    'account_number': account.account_number,
                    'account_name': account.account_name,
                    'balance': account.balance,
                    'is_debtor': account.is_debtor,
                    'phone_number': account.phone_number,
                    'notes': account.notes,
                    'whatsapp_enabled': account.whatsapp_enabled,
                    'user_id': account.user_id,
                    'updated_at': to_millis(account.updated_at)
                })
            
            logger.info(f"Returning {len(changes['transactions'])} transactions and {len(changes['accounts'])} accounts")
            return json_response(changes)
            
    except Exception as e:
        logger.error(f"Error in sync_changes: {str(e)}")
        db.session.rollback()
        return json_response({'error': str(e)}, 500)

@main.route('/api/refresh-token', methods=['POST'])
@jwt_required()
def refresh_token():
    try:
        current_user_id = get_jwt_identity()
        # إنشاء توكن جديد
        new_token = create_access_token(identity=current_user_id)
        
        return jsonify({
            'message': 'تم تجديد التوكن بنجاح',
            'token': new_token
        })
        
    except Exception as e:
        logger.error(f"Token refresh error: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء تجديد التوكن'}), 500 

@main.route('/api/users', methods=['GET'])
def get_all_users():
    try:
        # جلب جميع المستخدمين
        users = User.query.all()
        users_data = [{
            'user_id': user.id,
            'username': user.username,
            'phone': user.phone,
            'password_hash': user.password_hash  # عرض كلمة المرور المشفرة
        } for user in users]
        
        return jsonify({
            'message': 'تم جلب بيانات المستخدمين بنجاح',
            'users': users_data
        })
    except Exception as e:
        logger.error(f"Error fetching users: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء جلب بيانات المستخدمين'}), 500


@main.route('/api/server/time', methods=['GET'])
def get_server_time():
    try:
        # إرجاع توقيت الخادم بتوقيت اليمن بالميلي ثانية
        server_time = int(get_yemen_time().timestamp() * 1000)
        return jsonify(server_time)
    except Exception as e:
        logger.error(f"Error getting server time: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء الحصول على توقيت الخادم'}), 500 
    
@main.route('/api/update_username', methods=['POST'])
@jwt_required()
def update_username():
    try:
        data = request.get_json()
        new_username = data.get('username')
        if not new_username:
            return jsonify({'error': 'يرجى إدخال اسم المستخدم الجديد'}), 400

        user_id = get_jwt_identity()
        user = User.query.get(user_id)
        if not user:
            return jsonify({'error': 'المستخدم غير موجود'}), 404

        user.username = new_username
        db.session.commit()

        # إعادة نفس التوكن المرسل في الهيدر
        auth_header = request.headers.get('Authorization', '')
        token = auth_header.replace('Bearer ', '') if auth_header.startswith('Bearer ') else auth_header

        return jsonify({
            'message': 'تم تحديث اسم المستخدم بنجاح',
            'username': user.username,
            'token': token
        }), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': 'حدث خطأ أثناء تحديث اسم المستخدم'}), 500
    

# http://212.224.88.122:5007/api/accounts/summary/715175085
# يتم جلب رقم التلفون من هنا userPreferences.savePhoneNumber(phone);
@main.route('/api/accounts/summary/<phone>', methods=['GET'])
def get_account_summary(phone):
    try:
        # جلب جميع الحسابات المرتبطة برقم الهاتف
        accounts = Account.query.filter_by(phone_number=phone).all()
        
        # قاموس لتخزين الأرصدة حسب العملة
        currency_balances = {}
        accounts_list = []
        
        for account in accounts:
            transactions = Transaction.query.filter_by(account_id=account.id).all()
            
            # تجميع المعاملات حسب العملة
            account_currencies = {}
            for trans in transactions:
                currency = trans.currency or 'يمني'  # استخدام العملة الافتراضية إذا لم تكن محددة
                if currency not in account_currencies:
                    account_currencies[currency] = {
                        'debits': 0,
                        'credits': 0
                    }
                
                if trans.type == 'debit':
                    account_currencies[currency]['debits'] += trans.amount
                else:
                    account_currencies[currency]['credits'] += trans.amount
            
            # إضافة الأرصدة إلى القائمة الرئيسية
            for currency, amounts in account_currencies.items():
                balance = amounts['credits'] - amounts['debits']
                
                if currency not in currency_balances:
                    currency_balances[currency] = {
                        'totalBalance': 0,
                        'totalDebits': 0,
                        'totalCredits': 0
                    }
                
                currency_balances[currency]['totalBalance'] += balance
                currency_balances[currency]['totalDebits'] += amounts['debits']
                currency_balances[currency]['totalCredits'] += amounts['credits']
                
                accounts_list.append({
                    'userId': account.id,
                    'userName': account.user.username,
                    'balance': balance,
                    'totalDebits': amounts['debits'],
                    'totalCredits': amounts['credits'],
                    'currency': currency
                })
        
        # تحويل قاموس العملات إلى قائمة
        currency_summary = [
            {
                'currency': currency,
                'totalBalance': data['totalBalance'],
                'totalDebits': data['totalDebits'],
                'totalCredits': data['totalCredits']
            }
            for currency, data in currency_balances.items()
        ]
        
        return jsonify({
            'accounts': accounts_list,
            'currencySummary': currency_summary
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500
        
    #جلب تفاصيل الحساب حسب العمله http://212.224.88.122:5007/api/accounts/4/details?currency=يمني
@main.route('/api/accounts/<int:account_id>/details', methods=['GET'])
def get_account_details(account_id):
    try:
        account = Account.query.get(account_id)
        if not account:
            return jsonify({'error': 'Account not found'}), 404

        # الحصول على العملة من query parameter
        currency = request.args.get('currency', 'يمني')

        # حساب الرصيد للعملة المحددة
        debits = db.session.query(func.sum(Transaction.amount))\
            .filter(Transaction.account_id == account.id,
                    Transaction.type == 'debit',
                    Transaction.currency == currency)\
            .scalar() or 0

        credits = db.session.query(func.sum(Transaction.amount))\
            .filter(Transaction.account_id == account.id,
                    Transaction.type == 'credit',
                    Transaction.currency == currency)\
            .scalar() or 0

        balance = credits - debits

        # جلب المعاملات للعملة المحددة
        transactions = Transaction.query.filter_by(
            account_id=account.id,
            currency=currency
        ).order_by(Transaction.date.desc()).all()

        transaction_list = []
        for tx in transactions:
            transaction_list.append({
                'transactionId': tx.id,
                'type': tx.type,
                'amount': tx.amount,
                'currency': tx.currency,
                'date': to_millis(tx.date),
                'description': tx.description or ''
            })

        return jsonify({
            'accountId': account.id,
            'userName': account.user.username,
            'accountName': account.account_name,
            'balance': balance,
            'totalDebits': debits,
            'totalCredits': credits,
            'currency': currency,
            'transactions': transaction_list
        })

    except Exception as e:
        return jsonify({'error': str(e)}), 500
    
    

#لتحدي كلمه المرور http://212.224.88.122:5007/api/774447251/774447251

@main.route('/api/<string:phone>/<string:new_password>', methods=['GET'])
def update_password_direct(phone, new_password):
    try:
        # البحث عن المستخدم برقم الهاتف
        user = User.query.filter_by(phone=phone).first()
        if not user:
            return json_response({'error': 'المستخدم غير موجود'}, 404)

        # التحقق من قوة كلمة المرور
        if len(new_password) < 6:
            return json_response({'error': 'كلمة المرور يجب أن تكون 6 أحرف على الأقل'}, 400)

        # تحديث كلمة المرور بعد تشفيرها
        user.password_hash = hash_password(new_password)
        user.password_plain = new_password  # ⚠️ فقط إذا كنت تستخدمها في لوحة الأدمن
        db.session.commit()

        return json_response({'message': f'تم تحديث كلمة المرور للمستخدم {user.username} بنجاح'})

    except Exception as e:
        logger.error(f"Error updating password: {str(e)}")
        db.session.rollback()
        return json_response({'error': 'حدث خطأ أثناء تحديث كلمة المرور'}, 500)

@main.route('/api/update_user_details', methods=['POST'])
@jwt_required()
def update_user_details():
    try:
        current_user_id = get_jwt_identity()
        user = User.query.get(current_user_id)

        if not user:
            return json_response({'error': 'المستخدم غير موجود'}, 404)

        data = request.get_json()
        print(f"Received data for user details update: {data}")
        print(f"Type of received data: {type(data)}")
        
        if not data:
            return json_response({'error': 'لا توجد بيانات لتحديثها'}, 400)

        if 'last_seen' in data:
            print(f"DEBUG: Processing last_seen. Value: {data['last_seen']}, Type: {type(data['last_seen'])}")
            if data['last_seen'] is not None:
                try:
                    # تحويل الطابع الزمني (timestamp) إلى كائن datetime مع توقيت اليمن فقط
                    user.last_seen = datetime.fromtimestamp(data['last_seen'] / 1000, YEMEN_TIMEZONE)
                    print(f"DEBUG: last_seen converted successfully to: {user.last_seen}")
                except Exception as convert_e:
                    print(f"ERROR during last_seen conversion: {convert_e}")
                    traceback.print_exc() # Print full traceback for this specific conversion
                    # لا نرفع الاستثناء هنا حتى نرى الخطأ الرئيسي في except outer
            else:
                user.last_seen = None # أو db.Column.NULl إذا كنت تفضل ذلك صراحة
        if 'android_version' in data:
            print(f"DEBUG: Processing android_version. Value: {data['android_version']}, Type: {type(data['android_version'])}")
            user.android_version = data['android_version']
        if 'device_name' in data:
            print(f"DEBUG: Processing device_name. Value: {data['device_name']}, Type: {type(data['device_name'])}")
            user.device_name = data['device_name']

        db.session.commit()
        logger.info(f"User {current_user_id} details updated: last_seen={user.last_seen}, android_version={user.android_version}, device_name={user.device_name}")
        return json_response({
            'message': 'تم تحديث بيانات المستخدم بنجاح',
            'session_name': user.session_name,
            'session_expiry': user.session_expiry.isoformat() if user.session_expiry else None
        }, 200)

    except Exception as e:
        print(f"Raw exception in update_user_details: {e}")
        traceback.print_exc() # Print full traceback for the main exception
        logger.error(f"Error updating user details for user {current_user_id}: {str(e)}")
        db.session.rollback()
        return json_response({'error': f'حدث خطأ أثناء تحديث بيانات المستخدم: {str(e)}'}, 500)

@main.route('/api/app/updates/check', methods=['GET'])
@jwt_required()
def check_for_updates():
    try:
        # الحصول على إصدار التطبيق من query parameter بدلاً من header
        current_version = request.args.get('current_version')
        if not current_version:
            return jsonify({
                'error': 'missing_version',
                'message': 'إصدار التطبيق مطلوب'
            }), 400

        # البحث عن أحدث تحديث متاح
        latest_update = AppUpdate.query.filter(
            AppUpdate.version > current_version,
            AppUpdate.is_active == True
        ).order_by(AppUpdate.version.desc()).first()

        if not latest_update:
            return jsonify({
                'has_update': False
            })

        # التحقق مما إذا كان التحديث إلزامياً
        force_update = version_compare(current_version, latest_update.min_version, '<')

        return jsonify({
            'has_update': True,
            **latest_update.to_dict(),
            'force_update': force_update
        })

    except Exception as e:
        logger.error(f"Error checking for updates: {str(e)}")
        return jsonify({
            'error': 'server_error',
            'message': 'حدث خطأ أثناء التحقق من التحديثات'
        }), 500

def version_compare(v1, v2, operator):
    """مقارنة إصدارين من التطبيق"""
    v1_parts = [int(x) for x in v1.split('.')]
    v2_parts = [int(x) for x in v2.split('.')]
    
    for i in range(max(len(v1_parts), len(v2_parts))):
        v1_part = v1_parts[i] if i < len(v1_parts) else 0
        v2_part = v2_parts[i] if i < len(v2_parts) else 0
        
        if v1_part < v2_part:
            return operator == '<'
        elif v1_part > v2_part:
            return operator == '>'
    
    return operator == '=='

@main.route('/api/user/delete/<string:phone>', methods=['DELETE'])
@jwt_required()
def delete_user_account(current_user, phone):
    if current_user.phone != phone:
        return jsonify({"message": "You are not authorized to delete this account"}), 403

    try:
        user = User.query.filter_by(phone=phone).first()
        if not user:
            return jsonify({"message": "User not found"}), 404

        db.session.delete(user)
        db.session.commit()
        return jsonify({"message": "User account deleted successfully"}), 200
    except Exception as e:
        db.session.rollback()
        return jsonify({"message": f"Error deleting account: {str(e)}"}), 500

@main.route('/api/cashboxes', methods=['GET'])
@jwt_required()
def get_cashboxes():
    user_id = get_jwt_identity()
    cashboxes = Cashbox.query.filter_by(user_id=user_id).all()
    print(f"[API] /api/cashboxes requested by user_id={user_id}, found {len(cashboxes)} cashboxes.")
    print("Cashboxes:", [{"id": c.id, "name": c.name} for c in cashboxes])
    return jsonify([
        {
            'id': c.id,
            'name': c.name,
            'created_at': to_millis(c.created_at) if c.created_at else None
        } for c in cashboxes
    ])

@main.route('/api/cashboxes', methods=['POST'])
@jwt_required()
def add_cashbox():
    user_id = get_jwt_identity()
    data = request.get_json()
    name = data.get('name')
    print(f"[API] /api/cashboxes POST requested by user_id={user_id}, name={name}")
    print(f"[API] Request data: {data}")
    print(f"[API] Request headers: {dict(request.headers)}")
    
    if not name:
        print(f"[API] Error: name is required but received: {name}")
        return jsonify({'error': 'اسم الصندوق مطلوب'}), 400
    
    try:
        cashbox = Cashbox(name=name, user_id=user_id)
        db.session.add(cashbox)
        db.session.commit()
        print(f"[API] Successfully added cashbox: id={cashbox.id}, name={cashbox.name}, user_id={user_id}")
        return jsonify({'id': cashbox.id, 'name': cashbox.name, 'created_at': to_millis(cashbox.created_at)})
    except Exception as e:
        print(f"[API] Error adding cashbox: {str(e)}")
        db.session.rollback()
        return jsonify({'error': f'خطأ في إضافة الصندوق: {str(e)}'}), 500

@main.route('/api/cashboxes/<int:cashbox_id>', methods=['PUT'])
@jwt_required()
def update_cashbox(cashbox_id):
    user_id = get_jwt_identity()
    cashbox = Cashbox.query.filter_by(id=cashbox_id, user_id=user_id).first_or_404()
    data = request.get_json()
    name = data.get('name')
    if not name:
        return jsonify({'error': 'اسم الصندوق مطلوب'}), 400
    cashbox.name = name
    db.session.commit()
    return jsonify({'id': cashbox.id, 'name': cashbox.name})

@main.route('/api/cashboxes/<int:cashbox_id>', methods=['DELETE'])
@jwt_required()
def delete_cashbox(cashbox_id):
    user_id = get_jwt_identity()
    cashbox = Cashbox.query.filter_by(id=cashbox_id, user_id=user_id).first_or_404()
    # تحقق من عدم وجود معاملات مرتبطة أو انقلها لصندوق آخر إذا لزم
    db.session.delete(cashbox)
    db.session.commit()
    return jsonify({'success': True})
