from flask import Blueprint, jsonify, request, render_template, redirect
from app import db
from app.models import User, Account, Transaction
from app.utils import hash_password, verify_password, generate_sync_token
from flask_jwt_extended import jwt_required, get_jwt_identity, create_access_token, verify_jwt_in_request_optional
from datetime import datetime
import logging
import json
import os


main = Blueprint('main', __name__)
logger = logging.getLogger(__name__)

def json_response(data, status_code=200):
    response = json.dumps(data, ensure_ascii=False)
    return response, status_code, {'Content-Type': 'application/json; charset=utf-8'}

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
        user_id = get_jwt_identity()
        data = request.get_json()
        
        logger.info(f"Received sync request from user {user_id}")
        logger.debug(f"Sync data: {data}")
        
        if not data:
            logger.warning("Empty sync data received")
            return json_response({'error': 'لا توجد بيانات للمزامنة'}, 400)
        
        # التحقق من صحة البيانات
        if 'accounts' not in data and 'transactions' not in data:
            logger.warning("No accounts or transactions in sync data")
            return json_response({'error': 'يجب إرسال بيانات الحسابات أو المعاملات'}, 400)
        
        # التحقق من صحة بيانات الحسابات
        if 'accounts' in data:
            if not isinstance(data['accounts'], list):
                logger.error("Accounts data is not a list")
                return json_response({'error': 'بيانات الحسابات غير صحيحة'}, 400)
            
            logger.info(f"Processing {len(data['accounts'])} accounts")
            for account_data in data['accounts']:
                try:
                    # التحقق من البيانات المطلوبة للحساب
                    required_fields = ['account_number', 'account_name', 'balance']
                    missing_fields = [field for field in required_fields if field not in account_data]
                    if missing_fields:
                        logger.error(f"Missing required fields for account: {missing_fields}")
                        return json_response({'error': f'بيانات الحساب غير مكتملة: {", ".join(missing_fields)}'}, 400)
                    
                    account = Account.query.filter_by(
                        account_number=account_data['account_number'],
                        user_id=user_id
                    ).first()
                    
                    if not account:
                        # حساب جديد - نستخدم القيم المرسلة من التطبيق
                        account = Account(
                            account_number=account_data['account_number'],
                            account_name=account_data['account_name'],
                            balance=account_data['balance'],
                            is_debtor=account_data.get('is_debtor', False),
                            user_id=user_id
                        )
                        db.session.add(account)
                        logger.info(f"Added new account: {account.account_number}")
                    else:
                        # حساب موجود - نحدث القيم الأساسية
                        account.balance = account_data['balance']
                        if 'is_debtor' in account_data:
                            account.is_debtor = account_data['is_debtor']
                        if 'account_name' in account_data:
                            account.account_name = account_data['account_name']
                        if 'phone_number' in account_data:
                            account.phone_number = account_data['phone_number']
                        logger.info(f"Updated account: {account.account_number}")
                except KeyError as e:
                    logger.error(f"Missing account field: {str(e)}")
                    return json_response({'error': f'بيانات الحساب غير مكتملة: {str(e)}'}, 400)
                except Exception as e:
                    logger.error(f"Error processing account: {str(e)}")
                    return json_response({'error': f'خطأ في معالجة بيانات الحساب: {str(e)}'}, 400)
        
        # التحقق من صحة بيانات المعاملات
        if 'transactions' in data:
            if not isinstance(data['transactions'], list):
                logger.error("Transactions data is not a list")
                return json_response({'error': 'بيانات المعاملات غير صحيحة'}, 400)
            
            logger.info(f"Processing {len(data['transactions'])} transactions")
            for transaction_data in data['transactions']:
                try:
                    # التحقق من البيانات المطلوبة للمعاملة
                    required_fields = ['id', 'date', 'amount', 'description', 'account_id']
                    missing_fields = [field for field in required_fields if field not in transaction_data]
                    if missing_fields:
                        logger.error(f"Missing required fields for transaction: {missing_fields}")
                        return json_response({'error': f'بيانات المعاملة غير مكتملة: {", ".join(missing_fields)}'}, 400)
                    
                    # معالجة التاريخ
                    try:
                        if isinstance(transaction_data['date'], (int, float)):
                            # إذا كان التاريخ timestamp
                            date = datetime.fromtimestamp(transaction_data['date'] / 1000)  # تحويل من milliseconds إلى seconds
                        else:
                            # إذا كان التاريخ نص ISO
                            date = datetime.fromisoformat(transaction_data['date'].replace('Z', '+00:00'))
                    except (ValueError, TypeError) as e:
                        logger.error(f"Invalid date format: {str(e)}")
                        return json_response({'error': f'تنسيق التاريخ غير صحيح: {str(e)}'}, 400)
                    
                    transaction = Transaction.query.filter_by(
                        id=transaction_data['id'],
                        user_id=user_id
                    ).first()
                    
                    if not transaction:
                        # معاملة جديدة - نستخدم القيم المرسلة من التطبيق
                        transaction = Transaction(
                            id=transaction_data['id'],
                            date=date,
                            amount=transaction_data['amount'],
                            description=transaction_data['description'],
                            type=transaction_data.get('type', 'debit'),
                            currency=transaction_data.get('currency', 'ريال يمني'),
                            notes=transaction_data.get('notes', ''),
                            user_id=user_id,
                            account_id=transaction_data['account_id']
                        )
                        db.session.add(transaction)
                        logger.info(f"Added new transaction: {transaction.id}")
                    else:
                        # معاملة موجودة - نحدث فقط القيم الأساسية
                        transaction.date = date
                        transaction.amount = transaction_data['amount']
                        transaction.description = transaction_data['description']
                        if 'type' in transaction_data:
                            transaction.type = transaction_data['type']
                        if 'currency' in transaction_data:
                            transaction.currency = transaction_data['currency']
                        if 'notes' in transaction_data:
                            transaction.notes = transaction_data['notes']
                        logger.info(f"Updated transaction: {transaction.id}")
                except KeyError as e:
                    logger.error(f"Missing transaction field: {str(e)}")
                    return json_response({'error': f'بيانات المعاملة غير مكتملة: {str(e)}'}, 400)
                except Exception as e:
                    logger.error(f"Error processing transaction: {str(e)}")
                    return json_response({'error': f'خطأ في معالجة بيانات المعاملة: {str(e)}'}, 400)
        
        try:
            db.session.commit()
            logger.info("Sync completed successfully")
            return json_response({'message': 'تمت المزامنة بنجاح'})
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
            'server_id': acc.id,  # إضافة server_id للتوافق
            'account_number': acc.account_number,
            'account_name': acc.account_name,
            'balance': acc.balance,
            'phone_number': acc.phone_number,  # إضافة رقم الهاتف
            'is_debtor': acc.is_debtor,  # إضافة حالة المدين
            'notes': acc.notes,  # إضافة الملاحظات
            'created_at': int(acc.created_at.timestamp() * 1000) if acc.created_at else None,  # إضافة تاريخ الإنشاء
            'updated_at': int(acc.updated_at.timestamp() * 1000) if acc.updated_at else None  # إضافة تاريخ التحديث
        } for acc in accounts])
    except Exception as e:
        logger.error(f"Get accounts error: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء جلب الحسابات'}), 500

@main.route('/api/transactions', methods=['GET'])
@jwt_required()
def get_transactions():
    try:
        user_id = get_jwt_identity()
        transactions = Transaction.query.filter_by(user_id=user_id).all()
        return jsonify([{
            'id': trans.id,
            'server_id': trans.id,  # إضافة server_id للتوافق
            'date': int(trans.date.timestamp() * 1000),  # تحويل التاريخ إلى timestamp بالميلي ثانية
            'amount': trans.amount,
            'description': trans.description,
            'account_id': trans.account_id,
            'type': trans.type,
            'currency': trans.currency,
            'notes': trans.notes
        } for trans in transactions])
    except Exception as e:
        logger.error(f"Get transactions error: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء جلب المعاملات'}), 500

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
            'account_number': acc.account_number,
            'account_name': acc.account_name,
            'balance': acc.balance,
            'user_id': acc.user_id,
            'created_at': str(acc.created_at) if acc.created_at else None
        } for acc in accounts]
        
        # جلب جميع المعاملات
        transactions = Transaction.query.all()
        transactions_data = [{
            'id': trans.id,
            'date': str(trans.date),  # التاريخ الخام من قاعدة البيانات
            'date_timestamp': int(trans.date.timestamp() * 1000),  # التاريخ كـ timestamp
            'amount': trans.amount,
            'description': trans.description,
            'type': trans.type,
            'currency': trans.currency,
            'notes': trans.notes,
            'account_id': trans.account_id,
            'user_id': trans.user_id
        } for trans in transactions]
        
        return jsonify({
            'accounts': accounts_data,
            'transactions': transactions_data,
            'total_accounts': len(accounts),
            'total_transactions': len(transactions)
        })
    except Exception as e:
        logger.error(f"Public debug data error: {str(e)}")
        return jsonify({'error': f'حدث خطأ أثناء جلب بيانات التصحيح: {str(e)}'}), 500

@main.route('/api/html-content', methods=['GET'])
def get_html_content():
    try:
        template_dir = os.path.join(os.path.dirname(__file__), 'templates', 'account_statement')
        
        # قراءة ملف HTML
        with open(os.path.join(template_dir, 'index.html'), 'r', encoding='utf-8') as f:
            html_content = f.read()
            
        return html_content, 200, {'Content-Type': 'text/html; charset=utf-8'}
    except Exception as e:
        logger.error(f"Error getting HTML content: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء جلب المحتوى'}), 500

@main.route('/api/html-content/styles.css', methods=['GET'])
def get_styles():
    try:
        template_dir = os.path.join(os.path.dirname(__file__), 'templates', 'account_statement')
        with open(os.path.join(template_dir, 'styles.css'), 'r', encoding='utf-8') as f:
            css_content = f.read()
        return css_content, 200, {'Content-Type': 'text/css; charset=utf-8'}
    except Exception as e:
        logger.error(f"Error getting CSS content: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء جلب التنسيقات'}), 500

@main.route('/api/html-content/script.js', methods=['GET'])
def get_script():
    try:
        template_dir = os.path.join(os.path.dirname(__file__), 'templates', 'account_statement')
        with open(os.path.join(template_dir, 'script.js'), 'r', encoding='utf-8') as f:
            js_content = f.read()
        return js_content, 200, {'Content-Type': 'application/javascript; charset=utf-8'}
    except Exception as e:
        logger.error(f"Error getting JavaScript content: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء جلب السكربت'}), 500

@main.route('/api/accounts/html-content', methods=['GET'])
def get_accounts_html_content():
    try:
        template_dir = os.path.join(os.path.dirname(__file__), 'templates', 'accounts')
        with open(os.path.join(template_dir, 'index.html'), 'r', encoding='utf-8') as f:
            html_content = f.read()
        return html_content, 200, {'Content-Type': 'text/html; charset=utf-8'}
    except Exception as e:
        logger.error(f"Error getting accounts HTML content: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء جلب محتوى الحسابات'}), 500

@main.route('/api/accounts/html-content/styles.css', methods=['GET'])
def get_accounts_styles():
    try:
        template_dir = os.path.join(os.path.dirname(__file__), 'templates', 'accounts')
        with open(os.path.join(template_dir, 'styles.css'), 'r', encoding='utf-8') as f:
            css_content = f.read()
        return css_content, 200, {'Content-Type': 'text/css; charset=utf-8'}
    except Exception as e:
        logger.error(f"Error getting accounts CSS content: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء جلب تنسيقات الحسابات'}), 500

@main.route('/api/accounts/html-content/script.js', methods=['GET'])
def get_accounts_script():
    try:
        template_dir = os.path.join(os.path.dirname(__file__), 'templates', 'accounts')
        with open(os.path.join(template_dir, 'script.js'), 'r', encoding='utf-8') as f:
            js_content = f.read()
        return js_content, 200, {'Content-Type': 'application/javascript; charset=utf-8'}
    except Exception as e:
        logger.error(f"Error getting accounts JavaScript content: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء جلب سكربت الحسابات'}), 500

@main.route('/api/transactions/html-content', methods=['GET'])
def get_transactions_html_content():
    try:
        template_dir = os.path.join(os.path.dirname(__file__), 'templates', 'transactions')
        with open(os.path.join(template_dir, 'index.html'), 'r', encoding='utf-8') as f:
            html_content = f.read()
        return html_content, 200, {'Content-Type': 'text/html; charset=utf-8'}
    except Exception as e:
        logger.error(f"Error getting transactions HTML content: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء جلب محتوى المعاملات'}), 500

@main.route('/api/transactions/html-content/styles.css', methods=['GET'])
def get_transactions_styles():
    try:
        template_dir = os.path.join(os.path.dirname(__file__), 'templates', 'transactions')
        with open(os.path.join(template_dir, 'styles.css'), 'r', encoding='utf-8') as f:
            css_content = f.read()
        return css_content, 200, {'Content-Type': 'text/css; charset=utf-8'}
    except Exception as e:
        logger.error(f"Error getting transactions CSS content: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء جلب تنسيقات المعاملات'}), 500

@main.route('/api/transactions/html-content/script.js', methods=['GET'])
def get_transactions_script():
    try:
        template_dir = os.path.join(os.path.dirname(__file__), 'templates', 'transactions')
        with open(os.path.join(template_dir, 'script.js'), 'r', encoding='utf-8') as f:
            js_content = f.read()
        return js_content, 200, {'Content-Type': 'application/javascript; charset=utf-8'}
    except Exception as e:
        logger.error(f"Error getting transactions JavaScript content: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء جلب سكربت المعاملات'}), 500

@main.route('/', methods=['GET'])
def root_redirect():
    try:
        verify_jwt_in_request_optional()
        user_id = get_jwt_identity()
        if user_id:
            return redirect('/dashboard')
        else:
            return redirect('/login')
    except Exception:
        return redirect('/login')

@main.route('/login', methods=['GET'])
def login_page():
    return render_template('login.html')

@main.route('/dashboard', methods=['GET'])
def dashboard():
    return render_template('dashboard.html')

@main.route('/logout', methods=['GET'])
def logout():
    return redirect('/login') 