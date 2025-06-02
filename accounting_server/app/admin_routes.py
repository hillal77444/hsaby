from flask import Blueprint, jsonify, request, render_template, redirect, url_for, flash
from app import db
from app.models import User, Account, Transaction
from app.utils import hash_password
from datetime import datetime, timedelta
from sqlalchemy import func, case
import json
import requests
import time
import redis
import threading
import queue
import os
import re
import psutil

admin = Blueprint('admin', __name__)

# كلمة المرور للإدارة - يمكن تغييرها من ملف الخادم
ADMIN_PASSWORD = "Hillal774447251"

# تعريف عنوان خادم الواتساب
WHATSAPP_SERVER = os.getenv('WHATSAPP_SERVER', 'http://localhost:3003')

# إعداد Redis
redis_client = redis.Redis(host='localhost', port=6379, db=0)
MESSAGE_QUEUE_KEY = 'whatsapp_message_queue'
PROCESSING_QUEUE_KEY = 'whatsapp_processing_queue'

# دالة لمعالجة قائمة الانتظار
def process_message_queue():
    while True:
        try:
            # التحقق من وجود رسائل في قائمة الانتظار
            if redis_client.llen(MESSAGE_QUEUE_KEY) > 0:
                # أخذ الرسالة التالية من قائمة الانتظار
                message_data = redis_client.lpop(MESSAGE_QUEUE_KEY)
                if message_data:
                    message = json.loads(message_data)
                    
                    # إضافة الرسالة إلى قائمة المعالجة
                    redis_client.lpush(PROCESSING_QUEUE_KEY, message_data)
                    
                    try:
                        # إرسال الرسالة
                        response = requests.post(
                            f'{WHATSAPP_SERVER}/send/{message["session_id"]}',
                            json={
                                'numbers': [message['number']],
                                'message': message['message']
                            },
                            timeout=5
                        )
                        
                        # تحديث حالة الرسالة
                        message['status'] = response.json()
                        message['processed_at'] = datetime.now().isoformat()
                        
                    except Exception as e:
                        message['status'] = {'error': str(e)}
                        message['processed_at'] = datetime.now().isoformat()
                    
                    finally:
                        # إزالة الرسالة من قائمة المعالجة
                        redis_client.lrem(PROCESSING_QUEUE_KEY, 0, message_data)
                        
                        # حفظ نتيجة المعالجة
                        result_key = f'whatsapp_result_{message["id"]}'
                        redis_client.setex(result_key, 3600, json.dumps(message))  # حفظ لمدة ساعة
                    
                    # انتظار ثانية واحدة قبل معالجة الرسالة التالية
                    time.sleep(1)
            
            else:
                # إذا لم تكن هناك رسائل، انتظر قليلاً قبل التحقق مرة أخرى
                time.sleep(0.5)
                
        except Exception as e:
            print(f"Error in message queue processing: {str(e)}")
            time.sleep(1)

# بدء معالج قائمة الانتظار في خيط منفصل
queue_processor = threading.Thread(target=process_message_queue, daemon=True)
queue_processor.start()

def admin_required(f):
    def decorated_function(*args, **kwargs):
        if request.cookies.get('admin_auth') != ADMIN_PASSWORD:
            return redirect(url_for('admin.login'))
        return f(*args, **kwargs)
    decorated_function.__name__ = f.__name__
    return decorated_function

@admin.route('/admin/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        password = request.form.get('password')
        if password == ADMIN_PASSWORD:
            response = redirect(url_for('admin.dashboard'))
            response.set_cookie('admin_auth', ADMIN_PASSWORD, max_age=3600)  # ساعة واحدة
            return response
        flash('كلمة المرور غير صحيحة')
    return render_template('admin/login.html')

@admin.route('/admin/logout')
def logout():
    response = redirect(url_for('admin.login'))
    response.delete_cookie('admin_auth')
    return response

@admin.route('/admin/dashboard')
@admin_required
def dashboard():
    # إحصائيات عامة
    total_users = User.query.count()
    total_accounts = Account.query.count()
    total_transactions = Transaction.query.count()
    
    # إحصائيات اليوم
    today = datetime.now().date()
    today_transactions = Transaction.query.filter(
        func.date(Transaction.date) == today
    ).count()
    
    # إحصائيات الأسبوع
    week_ago = today - timedelta(days=7)
    weekly_transactions = Transaction.query.filter(
        func.date(Transaction.date) >= week_ago
    ).count()
    
    # إحصائيات العملات
    currency_stats = db.session.query(
        Transaction.currency,
        func.count(Transaction.id).label('count'),
        func.sum(case((Transaction.type == 'credit', Transaction.amount), else_=0)).label('credits'),
        func.sum(case((Transaction.type == 'debit', Transaction.amount), else_=0)).label('debits')
    ).group_by(Transaction.currency).all()
    
    return render_template('admin/dashboard.html',
                         total_users=total_users,
                         total_accounts=total_accounts,
                         total_transactions=total_transactions,
                         today_transactions=today_transactions,
                         weekly_transactions=weekly_transactions,
                         currency_stats=currency_stats)

@admin.route('/admin/users')
@admin_required
def users():
    users = User.query.all()
    user_stats = []
    
    for user in users:
        accounts_count = Account.query.filter_by(user_id=user.id).count()
        transactions_count = Transaction.query.filter_by(user_id=user.id).count()
        
        # حساب إجمالي المعاملات
        total_debits = db.session.query(func.sum(Transaction.amount))\
            .filter(Transaction.user_id == user.id, Transaction.type == 'debit')\
            .scalar() or 0
        
        total_credits = db.session.query(func.sum(Transaction.amount))\
            .filter(Transaction.user_id == user.id, Transaction.type == 'credit')\
            .scalar() or 0
        
        user_stats.append({
            'user': user,
            'accounts_count': accounts_count,
            'transactions_count': transactions_count,
            'total_debits': total_debits,
            'total_credits': total_credits,
            'balance': total_credits - total_debits
        })
    
    return render_template('admin/users.html', user_stats=user_stats)

@admin.route('/admin/user/<int:user_id>')
@admin_required
def user_details(user_id):
    user = User.query.get_or_404(user_id)
    accounts = Account.query.filter_by(user_id=user_id).all()
    transactions = Transaction.query.filter_by(user_id=user_id).order_by(Transaction.date.desc()).all()
    
    return render_template('admin/user_details.html',
                         user=user,
                         accounts=accounts,
                         transactions=transactions)

@admin.route('/admin/user/<int:user_id>/update', methods=['POST'])
@admin_required
def update_user(user_id):
    user = User.query.get_or_404(user_id)
    
    if 'phone' in request.form:
        new_phone = request.form['phone']
        if User.query.filter_by(phone=new_phone).first() and new_phone != user.phone:
            flash('رقم الهاتف مستخدم بالفعل')
            return redirect(url_for('admin.user_details', user_id=user_id))
        user.phone = new_phone
    
    if 'password' in request.form and request.form['password']:
        new_password = request.form['password']
        if len(new_password) < 6:
            flash('كلمة المرور يجب أن تكون 6 أحرف على الأقل')
            return redirect(url_for('admin.user_details', user_id=user_id))
        user.password_hash = hash_password(new_password)
    
    db.session.commit()
    flash('تم تحديث بيانات المستخدم بنجاح')
    return redirect(url_for('admin.user_details', user_id=user_id))

@admin.route('/admin/accounts')
@admin_required
def accounts():
    accounts = Account.query.all()
    account_stats = []
    
    for account in accounts:
        transactions_count = Transaction.query.filter_by(account_id=account.id).count()
        
        # حساب إجمالي المعاملات
        total_debits = db.session.query(func.sum(Transaction.amount))\
            .filter(Transaction.account_id == account.id, Transaction.type == 'debit')\
            .scalar() or 0
        
        total_credits = db.session.query(func.sum(Transaction.amount))\
            .filter(Transaction.account_id == account.id, Transaction.type == 'credit')\
            .scalar() or 0
        
        account_stats.append({
            'account': account,
            'transactions_count': transactions_count,
            'total_debits': total_debits,
            'total_credits': total_credits,
            'balance': total_credits - total_debits
        })
    
    return render_template('admin/accounts.html', account_stats=account_stats)

@admin.route('/admin/transactions')
@admin_required
def transactions():
    transactions = Transaction.query.order_by(Transaction.date.desc()).all()
    return render_template('admin/transactions.html', transactions=transactions)

@admin.route('/admin/statistics')
@admin_required
def statistics():
    # إحصائيات المستخدمين
    user_stats = db.session.query(
        func.date(User.created_at).label('date'),
        func.count(User.id).label('count')
    ).group_by(func.date(User.created_at)).all()
    
    # إحصائيات الحسابات
    account_stats = db.session.query(
        func.date(Account.created_at).label('date'),
        func.count(Account.id).label('count')
    ).group_by(func.date(Account.created_at)).all()
    
    # إحصائيات المعاملات
    transaction_stats = db.session.query(
        func.date(Transaction.date).label('date'),
        func.count(Transaction.id).label('count'),
        func.sum(case((Transaction.type == 'credit', Transaction.amount), else_=0)).label('credits'),
        func.sum(case((Transaction.type == 'debit', Transaction.amount), else_=0)).label('debits')
    ).group_by(func.date(Transaction.date)).all()
    
    return render_template('admin/statistics.html',
                         user_stats=user_stats,
                         account_stats=account_stats,
                         transaction_stats=transaction_stats)

@admin.route('/admin/whatsapp')
@admin_required
def whatsapp_dashboard():
    try:
        # التحقق من حالة خادم الواتساب
        try:
            status_response = requests.get(f'{WHATSAPP_SERVER}/status', timeout=5)
            server_status = status_response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error connecting to WhatsApp server: {str(e)}")
            server_status = {'error': 'لا يمكن الاتصال بخادم الواتساب'}
        
        users = User.query.all()
        print("Users loaded successfully:", len(users))
        return render_template('admin/whatsapp_dashboard.html', 
                             users=users,
                             server_status=server_status)
    except Exception as e:
        print("Error in whatsapp_dashboard:", str(e))
        return str(e), 500

@admin.route('/admin/whatsapp/start', methods=['POST'])
@admin_required
def start_whatsapp_session():
    try:
        session_id = 'admin_main'
        print(f"Starting new WhatsApp session: {session_id}")
        
        # بدء الجلسة
        response = requests.get(f'{WHATSAPP_SERVER}/start/{session_id}', timeout=10)
        if response.status_code != 200:
            print(f"Failed to start session: {response.text}")
            return jsonify({'error': 'فشل في بدء الجلسة'}), response.status_code
            
        # التحقق من حالة الجلسة
        status_response = requests.get(f'{WHATSAPP_SERVER}/status', timeout=10)
        if status_response.status_code == 200:
            status_data = status_response.json()
            print(f"Session status: {status_data}")
            
            # البحث عن الجلسة الحالية
            current_session = next((s for s in status_data.get('sessions', []) if s['id'] == session_id), None)
            if current_session:
                # محاولة جلب الباركود مباشرة
                qr_response = requests.get(f'{WHATSAPP_SERVER}/qr/{session_id}', timeout=10)
                if qr_response.status_code == 200:
                    print("QR code is available")
                    return jsonify({
                        'status': 'success',
                        'session': current_session,
                        'message': 'تم بدء الجلسة بنجاح',
                        'qr_available': True
                    })
                else:
                    print(f"QR code not available: {qr_response.text}")
                    return jsonify({
                        'status': 'success',
                        'session': current_session,
                        'message': 'تم بدء الجلسة بنجاح',
                        'qr_available': False,
                        'qr_error': qr_response.text
                    })
        
        return jsonify({'error': 'فشل في التحقق من حالة الجلسة'}), 500
        
    except requests.exceptions.RequestException as e:
        print(f"Error starting WhatsApp session: {str(e)}")
        return jsonify({
            'error': 'لا يمكن الاتصال بخادم الواتساب',
            'details': str(e)
        }), 500

@admin.route('/admin/whatsapp/qr/<session_id>')
@admin_required
def get_whatsapp_qr(session_id):
    try:
        print(f"Fetching QR code for session: {session_id}")
        response = requests.get(f'{WHATSAPP_SERVER}/qr/{session_id}', timeout=10)
        
        if response.status_code == 200:
            print("QR code fetched successfully")
            # التحقق من نوع المحتوى
            content_type = response.headers.get('content-type', '')
            if 'image' in content_type:
                return response.content, 200, {'Content-Type': content_type}
            elif 'text/html' in content_type:
                # استخراج صورة QR من HTML
                qr_match = re.search(r'src="(data:image/[^"]+)"', response.text)
                if qr_match:
                    qr_data = qr_match.group(1)
                    return qr_data, 200, {'Content-Type': 'text/plain'}
                else:
                    return response.text, 200, {'Content-Type': 'text/html'}
            else:
                print(f"Unexpected content type: {content_type}")
                return jsonify({
                    'error': 'تم استلام استجابة غير صحيحة من الخادم',
                    'details': f'نوع المحتوى: {content_type}'
                }), 500
        else:
            print(f"Failed to fetch QR code: {response.text}")
            return jsonify({
                'error': 'فشل في جلب رمز QR',
                'details': response.text
            }), response.status_code
            
    except requests.exceptions.RequestException as e:
        print(f"Error getting QR code: {str(e)}")
        return jsonify({
            'error': 'لا يمكن الاتصال بخادم الواتساب',
            'details': str(e)
        }), 500

@admin.route('/admin/whatsapp/send', methods=['POST'])
@admin_required
def send_whatsapp_message():
    try:
        data = request.json
        if not data:
            return jsonify({'error': 'لم يتم استلام بيانات الرسالة'}), 400
            
        session_id = 'admin_main'
        
        # دالة للتحقق من مفتاح الدولة وإضافته
        def format_phone_number(phone):
            if not phone:
                return None
            # إزالة أي مسافات أو رموز غير ضرورية
            phone = ''.join(filter(str.isdigit, phone))
            
            # إذا كان الرقم يبدأ بـ 0، نستبدله بـ 967
            if phone.startswith('0'):
                phone = '967' + phone[1:]
            
            # إذا كان الرقم لا يبدأ بمفتاح دولة، نضيف 967
            if not phone.startswith('967'):
                phone = '967' + phone
            
            return phone
        
        # تجهيز قائمة الأرقام
        numbers = []
        
        try:
            if data['type'] == 'single_user':
                user = User.query.get(data['user_id'])
                if user and user.phone:
                    formatted_number = format_phone_number(user.phone)
                    if formatted_number:
                        numbers.append(formatted_number)
                    else:
                        print(f"رقم الهاتف غير صالح للمستخدم: {user.id}")
            
            elif data['type'] == 'multiple_users':
                users = User.query.filter(User.id.in_(data['user_ids'])).all()
                for user in users:
                    if user.phone:
                        formatted_number = format_phone_number(user.phone)
                        if formatted_number:
                            numbers.append(formatted_number)
                        else:
                            print(f"رقم الهاتف غير صالح للمستخدم: {user.id}")
            
            elif data['type'] == 'user_accounts':
                user = User.query.get(data['user_id'])
                if user:
                    accounts = Account.query.filter_by(user_id=user.id).all()
                    for account in accounts:
                        if account.phone:
                            formatted_number = format_phone_number(account.phone)
                            if formatted_number:
                                numbers.append(formatted_number)
                            else:
                                print(f"رقم الهاتف غير صالح للحساب: {account.id}")
            
            else:
                return jsonify({'error': 'نوع الإرسال غير صالح'}), 400
                
        except KeyError as e:
            print(f"خطأ في البيانات المستلمة: {str(e)}")
            return jsonify({'error': f'بيانات غير مكتملة: {str(e)}'}), 400
        except Exception as e:
            print(f"خطأ في معالجة الأرقام: {str(e)}")
            return jsonify({'error': f'خطأ في معالجة الأرقام: {str(e)}'}), 500
        
        if not numbers:
            return jsonify({'error': 'لم يتم العثور على أرقام هواتف صالحة'})
        
        # إذا كان هناك رقم واحد فقط، نرسل مباشرة بدون قائمة انتظار
        if len(numbers) == 1:
            try:
                response = requests.post(
                    f'{WHATSAPP_SERVER}/send/{session_id}',
                    json={
                        'numbers': numbers,
                        'message': data['message']
                    },
                    timeout=5
                )
                return jsonify(response.json())
            except requests.exceptions.RequestException as e:
                print(f"خطأ في إرسال الرسالة: {str(e)}")
                return jsonify({'error': 'لا يمكن الاتصال بخادم الواتساب'}), 500
        
        # إذا كان هناك أكثر من رقم، نستخدم قائمة الانتظار
        try:
            message_ids = []
            for number in numbers:
                message_id = f"{int(time.time() * 1000)}_{len(message_ids)}"
                message = {
                    'id': message_id,
                    'session_id': session_id,
                    'number': number,
                    'message': data['message'],
                    'created_at': datetime.now().isoformat(),
                    'status': 'pending'
                }
                
                # إضافة الرسالة إلى قائمة الانتظار
                redis_client.rpush(MESSAGE_QUEUE_KEY, json.dumps(message))
                message_ids.append(message_id)
            
            return jsonify({
                'status': 'queued',
                'message_ids': message_ids,
                'total_messages': len(numbers),
                'numbers': numbers
            })
            
        except redis.RedisError as e:
            print(f"خطأ في الاتصال بـ Redis: {str(e)}")
            return jsonify({'error': 'خطأ في الاتصال بقاعدة البيانات المؤقتة'}), 500
        except Exception as e:
            print(f"خطأ غير متوقع: {str(e)}")
            return jsonify({'error': f'حدث خطأ غير متوقع: {str(e)}'}), 500
        
    except Exception as e:
        print(f"خطأ عام في إرسال الرسالة: {str(e)}")
        return jsonify({'error': f'حدث خطأ أثناء إضافة الرسائل إلى قائمة الانتظار: {str(e)}'}), 500

@admin.route('/admin/whatsapp/status/<message_id>')
@admin_required
def check_message_status(message_id):
    try:
        # البحث عن حالة الرسالة في Redis
        result_key = f'whatsapp_result_{message_id}'
        result = redis_client.get(result_key)
        
        if result:
            return jsonify(json.loads(result))
        
        # إذا لم يتم العثور على النتيجة، تحقق من قائمة المعالجة
        processing_messages = redis_client.lrange(PROCESSING_QUEUE_KEY, 0, -1)
        for msg in processing_messages:
            msg_data = json.loads(msg)
            if msg_data['id'] == message_id:
                return jsonify({
                    'status': 'processing',
                    'message': 'الرسالة قيد المعالجة'
                })
        
        # إذا لم يتم العثور على الرسالة في أي مكان
        return jsonify({
            'status': 'not_found',
            'message': 'لم يتم العثور على الرسالة'
        }), 404
        
    except Exception as e:
        print(f"Error checking message status: {str(e)}")
        return jsonify({'error': 'حدث خطأ أثناء التحقق من حالة الرسالة'}), 500

@admin.route('/admin/whatsapp/status')
@admin_required
def whatsapp_status():
    try:
        print("Checking WhatsApp server status")
        response = requests.get(f'{WHATSAPP_SERVER}/status', timeout=5)
        
        if response.status_code == 200:
            status_data = response.json()
            print(f"Server status: {status_data}")
            
            # البحث عن الجلسة الرئيسية
            admin_session = next((s for s in status_data.get('sessions', []) if s['id'] == 'admin_main'), None)
            if admin_session:
                status_data['current_session'] = admin_session
                
            return jsonify(status_data)
        else:
            print(f"Failed to get status: {response.text}")
            return jsonify({
                'error': 'فشل في جلب حالة الخادم',
                'details': response.text
            }), response.status_code
            
    except requests.exceptions.RequestException as e:
        print(f"Error getting WhatsApp status: {str(e)}")
        return jsonify({
            'error': 'لا يمكن الاتصال بخادم الواتساب',
            'details': str(e)
        }), 500

@admin.route('/admin/whatsapp/delete/<session_id>', methods=['POST'])
@admin_required
def delete_whatsapp_session(session_id):
    try:
        print(f"Attempting to delete session: {session_id}")
        
        # محاولة حذف الجلسة
        response = requests.delete(f'{WHATSAPP_SERVER}/delete/{session_id}', timeout=10)
        
        if response.status_code == 200:
            print(f"Successfully deleted session: {session_id}")
            return jsonify({'status': 'success', 'message': 'تم حذف الجلسة بنجاح'})
        else:
            print(f"Failed to delete session: {session_id}, Status code: {response.status_code}")
            return jsonify({
                'error': 'فشل في حذف الجلسة',
                'details': response.text if response.text else 'لا توجد تفاصيل إضافية'
            }), response.status_code
            
    except requests.exceptions.RequestException as e:
        print(f"Error deleting WhatsApp session: {str(e)}")
        return jsonify({
            'error': 'لا يمكن الاتصال بخادم الواتساب',
            'details': str(e)
        }), 500

@admin.route('/admin/whatsapp/restart/<session_id>', methods=['POST'])
@admin_required
def restart_whatsapp_session(session_id):
    try:
        print(f"Attempting to restart session: {session_id}")
        
        # إغلاق جميع الجلسات أولاً
        try:
            close_response = requests.post(f'{WHATSAPP_SERVER}/close-all-sessions', timeout=10)
            if close_response.status_code != 200:
                print(f"Warning: Failed to close all sessions: {close_response.text}")
        except Exception as e:
            print(f"Warning: Error while closing sessions: {str(e)}")

        # انتظار قليلاً للتأكد من إغلاق جميع الجلسات
        time.sleep(3)

        # بدء جلسة جديدة
        print(f"Starting new session: {session_id}")
        start_response = requests.get(f'{WHATSAPP_SERVER}/start/{session_id}', timeout=10)
        
        if start_response.status_code == 200:
            print(f"Successfully started new session: {session_id}")
            return jsonify({
                'status': 'success',
                'message': 'تم إعادة تشغيل الجلسة بنجاح',
                'data': start_response.json()
            })
        else:
            print(f"Failed to start new session: {session_id}, Status code: {start_response.status_code}")
            return jsonify({
                'error': 'فشل في بدء الجلسة الجديدة',
                'details': start_response.text if start_response.text else 'لا توجد تفاصيل إضافية'
            }), start_response.status_code
            
    except requests.exceptions.RequestException as e:
        print(f"Error restarting WhatsApp session: {str(e)}")
        return jsonify({
            'error': 'لا يمكن الاتصال بخادم الواتساب',
            'details': str(e)
        }), 500

@admin.route('/admin/whatsapp/close-all', methods=['POST'])
@admin_required
def close_all_whatsapp_sessions():
    try:
        print("Attempting to close all WhatsApp sessions")
        response = requests.post(f'{WHATSAPP_SERVER}/close-all-sessions', timeout=10)
        
        if response.status_code == 200:
            print("Successfully closed all sessions")
            return jsonify({
                'status': 'success',
                'message': 'تم إغلاق جميع الجلسات بنجاح',
                'data': response.json()
            })
        else:
            print(f"Failed to close all sessions: {response.text}")
            return jsonify({
                'error': 'فشل في إغلاق جميع الجلسات',
                'details': response.text if response.text else 'لا توجد تفاصيل إضافية'
            }), response.status_code
            
    except requests.exceptions.RequestException as e:
        print(f"Error closing all WhatsApp sessions: {str(e)}")
        return jsonify({
            'error': 'لا يمكن الاتصال بخادم الواتساب',
            'details': str(e)
        }), 500

@admin.route('/admin/transaction/notify', methods=['POST'])
@admin_required
def send_transaction_notification():
    try:
        data = request.json
        if not data or 'transaction_id' not in data:
            return jsonify({'error': 'بيانات المعاملة غير مكتملة'}), 400

        result = calculate_and_notify_transaction(data['transaction_id'])
        if result.get('status') == 'success':
            return jsonify({
                'status': 'success',
                'message': 'تم إرسال الإشعار بنجاح'
            })
        else:
            return jsonify({
                'error': result.get('message', 'حدث خطأ أثناء إرسال الإشعار')
            }), 500

    except Exception as e:
        print(f"خطأ في إرسال إشعار المعاملة: {str(e)}")
        return jsonify({
            'error': 'حدث خطأ أثناء إرسال الإشعار',
            'details': str(e)
        }), 500

def calculate_and_notify_transaction(transaction_id):
    try:
        # جلب المعاملة والحساب
        transaction = Transaction.query.get(transaction_id)
        if not transaction:
            return {'status': 'error', 'message': 'المعاملة غير موجودة'}

        account = Account.query.get(transaction.account_id)
        if not account:
            return {'status': 'error', 'message': 'الحساب غير موجود'}

        # جلب معلومات المستخدم
        user = User.query.get(account.user_id)
        if not user:
            return {'status': 'error', 'message': 'المستخدم غير موجود'}

        # حساب الرصيد حتى هذه المعاملة لنفس الحساب ونفس العملة
        transactions = Transaction.query.filter(
            Transaction.account_id == account.id,  # تأكيد نفس الحساب
            Transaction.currency == transaction.currency,  # تأكيد نفس العملة
            Transaction.date <= transaction.date,  # المعاملات حتى تاريخ هذه المعاملة
            Transaction.id <= transaction.id  # المعاملات حتى هذه المعاملة
        ).order_by(
            Transaction.date,  # ترتيب حسب التاريخ
            Transaction.id  # ثم حسب رقم المعاملة
        ).all()

        # حساب الرصيد النهائي
        balance = 0
        for trans in transactions:
            if trans.type == 'credit':
                balance += trans.amount
            else:  # debit
                balance -= trans.amount

        # تنسيق الرسالة
        transaction_type = "قيدنا الى حسابكم" if transaction.type == 'credit' else "قيدنا على حسابكم"
        balance_text = f"الرصيد لكم: {balance} {transaction.currency or 'ريال'}" if balance >= 0 else f"الرصيد عليكم: {abs(balance)} {transaction.currency or 'ريال'}"
        message = f"""
🏦 إشعار معاملة جديدة

🏛️ الاخ/: {account.account_name}

💰 تفاصيل المعاملة:
•  {transaction_type}
• المبلغ: {transaction.amount} {transaction.currency or 'ريال'}
• الوصف: {transaction.description or 'لا يوجد وصف'}
• التاريخ: {transaction.date.strftime('%Y-%m-%d %H:%M')}

💳 {balance_text}

تم الإرسال بواسطة: {user.username}
        """.strip()

        # تنسيق رقم الهاتف
        phone = account.phone_number
        if phone:
            phone = ''.join(filter(str.isdigit, phone))
            if phone.startswith('0'):
                phone = '967' + phone[1:]
            if not phone.startswith('967'):
                phone = '967' + phone

        # إرسال الرسالة
        response = requests.post(
            'http://212.224.88.122:3003/send/admin_main',
            json={
                'numbers': [phone],
                'message': message
            },
            timeout=5
        )

        if response.status_code == 200:
            return {'status': 'success', 'message': 'تم إرسال الإشعار بنجاح'}
        else:
            return {'status': 'error', 'message': 'فشل في إرسال الإشعار'}

    except Exception as e:
        logger.error(f"Error in calculate_and_notify_transaction: {str(e)}")
        return {'status': 'error', 'message': str(e)}

@admin.route('/admin/whatsapp/kill-chrome', methods=['POST'])
@admin_required
def kill_chrome_processes():
    try:
        # البحث عن جميع عمليات كروم
        chrome_processes = []
        for proc in psutil.process_iter(['pid', 'name']):
            try:
                if 'chrome' in proc.info['name'].lower():
                    chrome_processes.append(proc)
            except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                pass
        
        # إغلاق جميع عمليات كروم
        for proc in chrome_processes:
            try:
                proc.kill()
            except (psutil.NoSuchProcess, psutil.AccessDenied):
                pass
        
        # إغلاق جميع الجلسات في خادم الواتساب
        try:
            response = requests.post(f'{WHATSAPP_SERVER}/close-all-sessions', timeout=10)
            if response.status_code != 200:
                print(f"Warning: Failed to close WhatsApp sessions: {response.text}")
        except Exception as e:
            print(f"Warning: Error while closing WhatsApp sessions: {str(e)}")
        
        return jsonify({
            'status': 'success',
            'message': f'تم إغلاق {len(chrome_processes)} عملية كروم'
        })
        
    except Exception as e:
        print(f"Error killing Chrome processes: {str(e)}")
        return jsonify({
            'status': 'error',
            'message': str(e)
        }), 500 