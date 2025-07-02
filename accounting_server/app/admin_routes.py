from flask import Blueprint, jsonify, request, render_template, redirect, url_for, flash, Response, session
from app import db
from app.models import User, Account, Transaction, AppUpdate
from app.utils import hash_password
from datetime import datetime, timedelta, timezone
from sqlalchemy import func, case, and_, or_
import json
import requests
import os
import uuid
import subprocess
import time
import logging
import random
import string
from werkzeug.utils import secure_filename
from functools import wraps
import threading
import queue
from dateutil.parser import parse as date_parse

admin = Blueprint('admin', __name__)

# كلمة المرور للإدارة - يمكن تغييرها من ملف الخادم
ADMIN_PASSWORD = "Hillal774447251"

# إعدادات الواتساب
WHATSAPP_API = 'http://localhost:3002'
NODE_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'node-whatsapp-api')
UPLOAD_FOLDER = 'uploads'
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

# إعداد التسجيل
logger = logging.getLogger(__name__)

# إضافة تخزين مؤقت للكودات القصيرة (في الذاكرة)
short_links = {}

# تعريف توقيت اليمن (UTC+3) - يجب أن يكون متطابقاً مع models.py و routes.py
YEMEN_TIMEZONE = timezone(timedelta(hours=3))

# طابور مركزي لرسائل واتساب
whatsapp_queue = queue.Queue()

def whatsapp_worker():
    while True:
        send_args = whatsapp_queue.get()
        try:
            response = requests.post(
                send_args['url'],
                json=send_args['json'],
                timeout=5
            )
            # يمكن إضافة منطق تسجيل أو معالجة الرد هنا إذا لزم
        except Exception as e:
            logger.error(f"WhatsApp send error: {e}")
        time.sleep(1)  # انتظر ثانية بين كل رسالة
        whatsapp_queue.task_done()

# شغّل الـ worker في Thread منفصل عند بدء التطبيق (مرة واحدة فقط)
if not hasattr(globals(), '_whatsapp_worker_started'):
    threading.Thread(target=whatsapp_worker, daemon=True).start()
    globals()['_whatsapp_worker_started'] = True

def get_yemen_time():
    """الحصول على التوقيت الحالي بتوقيت اليمن"""
    return datetime.now(YEMEN_TIMEZONE)

def format_last_seen(last_seen_dt):
    if last_seen_dt is None:
        return "غير متاح", "text-muted"
    
    now = get_yemen_time()

    # Make last_seen_dt timezone-aware if it's naive
    if last_seen_dt.tzinfo is None or last_seen_dt.tzinfo.utcoffset(last_seen_dt) is None:
        last_seen_dt = last_seen_dt.replace(tzinfo=YEMEN_TIMEZONE)

    diff = now - last_seen_dt
    
    if diff < timedelta(minutes=1):
        return "متصل الآن", "last-seen-active"
    elif diff < timedelta(hours=1):
        minutes = int(diff.total_seconds() / 60)
        return f"منذ {minutes} دقيقة", "last-seen-recent"
    elif diff < timedelta(days=1):
        hours = int(diff.total_seconds() / 3600)
        return f"منذ {hours} ساعة", "last-seen-recent"
    elif diff < timedelta(days=30):
        days = diff.days
        return f"منذ {days} يوم", "last-seen-recent"
    else:
        return last_seen_dt.strftime('%Y-%m-%d %H:%M'), "last-seen-inactive"

# دالة توليد رابط كشف حساب مؤقت بكود قصير (6 أحرف)
def generate_short_statement_link(account_id, expires_sec=3600):
    code = ''.join(random.choices(string.ascii_letters + string.digits, k=6))
    short_links[code] = {
        'account_id': account_id,
        'expires_at': datetime.now(YEMEN_TIMEZONE) + timedelta(seconds=expires_sec)
    }
    url = f"https://malyp.com/api/{code}"
    return url

# مسار مختصر جداً لعرض كشف الحساب المؤقت
@admin.route('/api/<code>')
def short_statement(code):
    data = short_links.get(code)
    if not data:
        # عرض صفحة مخصصة عند الرابط غير صالح
        return render_template('admin/invalid_or_expired_link.html', reason="invalid")
    if datetime.now(YEMEN_TIMEZONE) > data['expires_at']:
        # عرض صفحة مخصصة عند انتهاء الصلاحية
        return render_template('admin/invalid_or_expired_link.html', reason="expired")
    return account_statement(data['account_id'])

def start_node_server():
    try:
        if not os.path.exists(NODE_DIR):
            raise Exception("مجلد Node.js غير موجود")
        
        # تثبيت المكتبات إذا لم تكن موجودة
        if not os.path.exists(os.path.join(NODE_DIR, 'node_modules')):
            subprocess.run(['npm', 'install'], cwd=NODE_DIR, check=True)
        
        process = subprocess.Popen(
            ['node', 'server.js'],
            cwd=NODE_DIR,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            env={**os.environ, 'DEBUG': '1'}
        )
        time.sleep(5)
        logger.info("✅ تم تشغيل خادم Node.js")
        return process
    except Exception as e:
        logger.error(f"فشل تشغيل Node.js: {e}")
        raise

def check_api_health():
    try:
        response = requests.get(f"{WHATSAPP_API}/status/admin_main", timeout=5)
        return response.status_code == 200
    except:
        return False

def admin_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if request.cookies.get('admin_auth') != ADMIN_PASSWORD:
            return redirect(url_for('admin.login'))
        return f(*args, **kwargs)
    decorated_function.__name__ = f.__name__
    return decorated_function

@admin.route('/api/admin/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        password = request.form.get('password')
        if password == ADMIN_PASSWORD:
            response = redirect(url_for('admin.dashboard'))
            response.set_cookie('admin_auth', ADMIN_PASSWORD, max_age=3600)  # ساعة واحدة
            return response
        flash('كلمة المرور غير صحيحة')
    return render_template('admin/login.html')

@admin.route('/api/admin/logout')
def logout():
    response = redirect(url_for('admin.login'))
    response.delete_cookie('admin_auth')
    return response

@admin.route('/api/admin/dashboard')
@admin_required
def dashboard():
    # إحصائيات عامة
    total_users = User.query.count()
    total_accounts = Account.query.count()
    total_transactions = Transaction.query.count()
    
    # إحصائيات اليوم
    today = datetime.now(YEMEN_TIMEZONE).date()
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

@admin.route('/api/admin/users')
@admin_required
def users():
    users = User.query.order_by(User.id.desc()).all()
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
        
        # جلب معلومات الجهاز وآخر ظهور
        last_seen_formatted, last_seen_color = format_last_seen(user.last_seen)
        
        user_stats.append({
            'user': user,
            'accounts_count': accounts_count,
            'transactions_count': transactions_count,
            'total_debits': total_debits,
            'total_credits': total_credits,
            'balance': total_credits - total_debits,
            'last_seen': last_seen_formatted,
            'last_seen_color': last_seen_color,
            'android_version': user.android_version if user.android_version else "غير متاح",
            'device_name': user.device_name if user.device_name else "غير متاح"
        })
    
    return render_template('admin/users.html', user_stats=user_stats)

@admin.route('/api/admin/user/<int:user_id>')
@admin_required
def user_details(user_id):
    user = User.query.get_or_404(user_id)
    accounts = Account.query.filter_by(user_id=user_id).all()
    transactions = Transaction.query.filter_by(user_id=user_id).order_by(Transaction.date.desc()).all()
    
    return render_template('admin/user_details.html',
                         user=user,
                         accounts=accounts,
                         transactions=transactions)

@admin.route('/api/admin/user/<int:user_id>/update', methods=['POST'])
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
    
    if 'session_name' in request.form:
        user.session_name = request.form['session_name'] or 'admin_main'
    if 'session_expiry' in request.form:
        session_expiry_val = request.form['session_expiry']
        if session_expiry_val:
            user.session_expiry = datetime.fromisoformat(session_expiry_val)
        else:
            user.session_expiry = None
    
    db.session.commit()
    flash('تم تحديث بيانات المستخدم بنجاح')
    return redirect(url_for('admin.user_details', user_id=user_id))

@admin.route('/api/admin/accounts')
@admin_required
def accounts():
    users = User.query.all()
    return render_template('admin/accounts.html', users=users)

@admin.route('/api/admin/transactions')
@admin_required
def transactions():
    account_id = request.args.get('account_id', type=int)
    user_id = request.args.get('user_id', type=int)
    currency = request.args.get('currency', type=str)

    query = Transaction.query.order_by(Transaction.date.desc())
    if account_id:
        query = query.filter(Transaction.account_id == account_id)
    if user_id:
        query = query.filter(Transaction.user_id == user_id)
    if currency:
        query = query.filter(Transaction.currency == currency)

    transactions = query.all()
    accounts = Account.query.all()
    users = User.query.all()
    currencies = db.session.query(Transaction.currency).distinct().all()
    currencies = [c[0] for c in currencies if c[0]]

    return render_template('admin/transactions.html',
        transactions=transactions,
        accounts=accounts,
        users=users,
        currencies=currencies,
        selected_account_id=account_id,
        selected_user_id=user_id,
        selected_currency=currency)

@admin.route('/api/admin/statistics')
@admin_required
def statistics():
    # إحصائيات المستخدمين
    total_users = User.query.count()
    
    # إحصائيات الحسابات
    total_accounts = Account.query.count()
    account_stats = db.session.query(
        func.date(Account.created_at).label('date'),
        func.count(Account.id).label('count')
    ).group_by(func.date(Account.created_at)).all()
    
    # إحصائيات المعاملات
    total_transactions = Transaction.query.count()
    transaction_stats = db.session.query(
        func.date(Transaction.date).label('date'),
        func.count(Transaction.id).label('count'),
        func.sum(case((Transaction.type == 'credit', Transaction.amount), else_=0)).label('credits'),
        func.sum(case((Transaction.type == 'debit', Transaction.amount), else_=0)).label('debits')
    ).group_by(func.date(Transaction.date)).all()
    
    # إحصائيات العملات
    currency_stats = db.session.query(
        Transaction.currency,
        func.count(Transaction.id).label('count'),
        func.sum(case((Transaction.type == 'credit', Transaction.amount), else_=0)).label('credits'),
        func.sum(case((Transaction.type == 'debit', Transaction.amount), else_=0)).label('debits')
    ).group_by(Transaction.currency).all()
    
    # تحويل التواريخ إلى كائنات datetime
    account_stats = [{'date': datetime.strptime(str(stat.date), '%Y-%m-%d'), 'count': stat.count} for stat in account_stats]
    transaction_stats = [{'date': datetime.strptime(str(stat.date), '%Y-%m-%d'), 'count': stat.count, 'credits': stat.credits or 0, 'debits': stat.debits or 0} for stat in transaction_stats]
    
    return render_template('admin/statistics.html',
                         total_users=total_users,
                         total_accounts=total_accounts,
                         total_transactions=total_transactions,
                         account_stats=account_stats,
                         transaction_stats=transaction_stats,
                         currency_stats=currency_stats)

# مسارات الواتساب
@admin.route('/api/admin/whatsapp')
@admin_required
def whatsapp_dashboard():
    return render_template('admin/whatsapp_dashboard.html')

def get_session_size(session_id):
    session_dir = os.path.join(NODE_DIR, '.wwebjs_auth', session_id)
    if not os.path.exists(session_dir):
        return 0
    
    total_size = 0
    for dirpath, dirnames, filenames in os.walk(session_dir):
        for f in filenames:
            fp = os.path.join(dirpath, f)
            total_size += os.path.getsize(fp)
    return round(total_size / (1024 * 1024), 2)

@admin.route('/api/admin/whatsapp/session/<session_id>/<action>', methods=['POST'])
@admin_required
def manage_whatsapp_session(session_id, action):
    try:
        # حذف ملف الجلسة
        if action == 'delete_file':
            session_dir = os.path.join(NODE_DIR, '.wwebjs_auth', session_id)
            if os.path.exists(session_dir):
                import shutil
                shutil.rmtree(session_dir)
                flash("🗑 تم حذف ملف الجلسة", "success")
        
        # إيقاف الجلسة
        elif action == 'stop':
            response = requests.delete(f"{WHATSAPP_API}/reset/{session_id}")
            if response.status_code == 200:
                flash("⏹ تم إيقاف الجلسة", "warning")
            else:
                flash("فشل إيقاف الجلسة", "error")
        
        # تشغيل الجلسة
        elif action == 'start':
            response = requests.get(f"{WHATSAPP_API}/start/{session_id}")
            if response.status_code == 200:
                flash("▶ بدء تشغيل الجلسة", "success")
            else:
                flash("فشل بدء تشغيل الجلسة", "error")

        # عرض حجم الجلسة
        elif action == 'size':
            size = get_session_size(session_id)
            flash(f"📦 حجم الجلسة: {size} ميجابايت", "info")
        
        return redirect(url_for('admin.whatsapp_sessions'))
    except Exception as e:
        flash(f"حدث خطأ: {str(e)}", "error")
        return redirect(url_for('admin.whatsapp_sessions'))

@admin.route('/api/admin/whatsapp/sessions')
@admin_required
def whatsapp_sessions():
    try:
        if not check_api_health():
            try:
                start_node_server()
                time.sleep(5)
            except Exception as e:
                flash(f'فشل تشغيل خادم الواتساب: {str(e)}', 'error')
                return redirect(url_for('admin.dashboard'))
        
        response = requests.get(f"{WHATSAPP_API}/status")
        if response.status_code == 200:
            sessions_data = response.json()
            # إضافة معلومات حجم الجلسة لكل جلسة
            for session in sessions_data:
                session['size'] = get_session_size(session['id'])
            return render_template('admin/whatsapp_sessions.html', sessions=sessions_data)
        else:
            flash('خادم الواتساب غير متصل', 'error')
            return redirect(url_for('admin.dashboard'))
    except:
        flash('خادم الواتساب غير متصل', 'error')
        return redirect(url_for('admin.dashboard'))

@admin.route('/api/admin/whatsapp/start_session', methods=['POST'])
@admin_required
def start_whatsapp_session():
    session_id = "admin_main"
    try:
        # تحقق من حالة الجلسة
        status_resp = requests.get(f"{WHATSAPP_API}/status/{session_id}")
        if status_resp.status_code == 200:
            status = status_resp.json().get('status', '').lower()
            if status == 'connected':
                return jsonify({'success': True, 'session_id': session_id})
        
        # إذا لم تكن الجلسة نشطة، قم بإنشائها
        response = requests.post(f"{WHATSAPP_API}/start/{session_id}")
        if response.status_code == 200:
            return jsonify({'success': True, 'session_id': session_id})
        return jsonify({'success': False, 'error': 'فشل بدء الجلسة'})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)})

@admin.route('/api/admin/whatsapp/qr/<session_id>')
@admin_required
def get_whatsapp_qr(session_id):
    # إعادة التوجيه مباشرة إلى صفحة Node.js لعرض رمز QR
    return redirect(f"http://212.224.88.122:3002/qr/{session_id}")

@admin.route('/api/admin/whatsapp/send', methods=['POST'])
@admin_required
def send_whatsapp_message():
    session_id = request.form.get('session_id')
    number = request.form.get('number')
    message = request.form.get('message')
    image = request.files.get('image')

    if not all([session_id, number, message]):
        return jsonify({'success': False, 'error': 'البيانات ناقصة'})

    try:
        data = {'number': number, 'message': message}
        files = {}
        
        if image:
            filename = secure_filename(image.filename)
            filepath = os.path.join(UPLOAD_FOLDER, f"{uuid.uuid4()}_{filename}")
            image.save(filepath)
            files['media'] = open(filepath, 'rb')

        response = requests.post(
            f"{WHATSAPP_API}/send/{session_id}",
            json=data,
            files=files
        )

        if files:
            files['media'].close()
            os.remove(filepath)

        if response.status_code == 200:
            return jsonify({'success': True})
        return jsonify({'success': False, 'error': 'فشل إرسال الرسالة'})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)})

@admin.route('/api/admin/whatsapp/send_bulk', methods=['POST'])
@admin_required
def send_whatsapp_bulk():
    session_id = request.form.get('session_id')
    numbers = request.form.get('numbers')
    message = request.form.get('message')
    delay = request.form.get('delay', '1000')
    image = request.files.get('image')

    if not all([session_id, numbers, message]):
        return jsonify({'success': False, 'error': 'البيانات ناقصة'})

    try:
        data = {
            'numbers': numbers,
            'message': message,
            'delay': delay
        }
        files = {}
        
        if image:
            filename = secure_filename(image.filename)
            filepath = os.path.join(UPLOAD_FOLDER, f"{uuid.uuid4()}_{filename}")
            image.save(filepath)
            files['media'] = open(filepath, 'rb')

        response = requests.post(
            f"{WHATSAPP_API}/send_bulk/{session_id}",
            data=data,
            files=files
        )

        if files:
            files['media'].close()
            os.remove(filepath)

        if response.status_code == 200:
            return jsonify(response.json())
        return jsonify({'success': False, 'error': 'فشل إرسال الرسائل'})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)})

@admin.route('/api/admin/whatsapp/contacts/<session_id>')
@admin_required
def get_whatsapp_contacts(session_id):
    try:
        response = requests.get(f"{WHATSAPP_API}/contacts/{session_id}")
        if response.status_code == 200:
            return jsonify(response.json())
        return jsonify({'error': 'فشل جلب جهات الاتصال'})
    except Exception as e:
        return jsonify({'error': str(e)})

@admin.route('/api/admin/whatsapp/chats/<session_id>')
@admin_required
def get_whatsapp_chats(session_id):
    try:
        response = requests.get(f"{WHATSAPP_API}/chats/{session_id}")
        if response.status_code == 200:
            return jsonify(response.json())
        return jsonify({'error': 'فشل جلب المحادثات'})
    except Exception as e:
        return jsonify({'error': str(e)})

@admin.route('/api/admin/whatsapp/messages/<session_id>/<chat_id>')
@admin_required
def get_whatsapp_messages(session_id, chat_id):
    try:
        response = requests.get(f"{WHATSAPP_API}/messages/{session_id}/{chat_id}")
        if response.status_code == 200:
            return jsonify(response.json())
        return jsonify({'error': 'فشل جلب الرسائل'})
    except Exception as e:
        return jsonify({'error': str(e)})

@admin.route('/api/admin/whatsapp/reset_session/<session_id>', methods=['POST'])
@admin_required
def reset_whatsapp_session(session_id):
    try:
        response = requests.delete(f"{WHATSAPP_API}/reset/{session_id}")
        if response.status_code == 200:
            return jsonify({'success': True})
        return jsonify({'success': False, 'error': 'فشل إعادة تعيين الجلسة'})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)})

@admin.route('/api/admin/whatsapp/session_status/<session_id>')
@admin_required
def get_whatsapp_session_status(session_id):
    try:
        response = requests.get(f"{WHATSAPP_API}/session_status/{session_id}")
        if response.status_code == 200:
            return jsonify(response.json())
        return jsonify({'error': 'فشل جلب حالة الجلسة'})
    except Exception as e:
        return jsonify({'error': str(e)})

@admin.route('/api/admin/transaction/notify', methods=['POST'])
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
        from dateutil.parser import parse as date_parse
        # جلب المعاملة والحساب
        transaction = Transaction.query.get(transaction_id)
        if not transaction:
            return {'status': 'error', 'message': 'المعاملة غير موجودة'}

        # التحقق من تفعيل الواتساب
        if not getattr(transaction, 'whatsapp_enabled', True):
            return {'status': 'success', 'message': 'تم تخطي الإشعار - الواتساب غير مفعل لهذه المعاملة'}

        account = Account.query.get(transaction.account_id)
        if not account:
            return {'status': 'error', 'message': 'الحساب غير موجود'}

        # جلب معلومات المستخدم
        user = User.query.get(account.user_id)
        if not user:
            return {'status': 'error', 'message': 'المستخدم غير موجود'}

        # تحويل التاريخ للعرض فقط
        transaction_dt = to_datetime(transaction.date)
        if not transaction_dt:
            return {'status': 'error', 'message': 'صيغة تاريخ المعاملة غير مدعومة'}

        # حساب الرصيد حتى (بما فيها) المعاملة الحالية باستخدام id فقط
        total_credits = db.session.query(func.coalesce(func.sum(Transaction.amount), 0)).filter(
            Transaction.account_id == account.id,
            Transaction.currency == transaction.currency,
            Transaction.type == 'credit',
            Transaction.id <= transaction.id
        ).scalar()

        total_debits = db.session.query(func.coalesce(func.sum(Transaction.amount), 0)).filter(
            Transaction.account_id == account.id,
            Transaction.currency == transaction.currency,
            Transaction.type == 'debit',
            Transaction.id <= transaction.id
        ).scalar()

        balance = total_credits - total_debits

        # توليد رابط كشف الحساب المؤقت القصير
        statement_link = generate_short_statement_link(account.id)

        # تنسيق الرسالة مع الرابط المؤقت
        transaction_type = "قيدنا الى حسابكم" if transaction.type == 'credit' else "قيدنا على حسابكم"
        balance_text = f"الرصيد لكم: {balance:g} {transaction.currency or 'ريال'}" if balance >= 0 else f"الرصيد عليكم: {abs(balance):g} {transaction.currency or 'ريال'}"
        account_name_clean = account.account_name.strip() if account.account_name else ''
        message = f"""
🏦 إشعار قيد جديد

🏛️ الاخ/: *{account_name_clean}*
🔢 رقم الحساب: *{account.server_id}*

💰 تفاصيل القيد :
•  {transaction_type}
• المبلغ: {transaction.amount:g} {transaction.currency or 'ريال'}
• الوصف: {transaction.description or 'لا يوجد وصف'}
• التاريخ: {transaction_dt.strftime('%Y-%m-%d')}

💳 {balance_text}

📄 كشف الحساب : {statement_link}

تم الإرسال بواسطة: 
*{user.username}*
        """.strip()

        # تنسيق رقم الهاتف
        phone = account.phone_number
        if phone:
            phone = ''.join(filter(str.isdigit, phone))
            if phone.startswith('966'):
                pass  # يظل كما هو
            elif phone.startswith('0'):
                phone = '967' + phone[1:]
            elif not phone.startswith('967'):
                phone = '967' + phone

        # --- منطق الجلسة وتاريخ الانتهاء ---
        session_name = user.session_name or 'admin_main'
        if session_name != 'admin_main':
            if not user.session_expiry or user.session_expiry < datetime.now(YEMEN_TIMEZONE):
                return  # لا ترسل الرسالة ولا ترجع رد
        # --- نهاية المنطق ---

        # إرسال الرسالة عبر الطابور
        whatsapp_queue.put({
            'url': f"{WHATSAPP_API}/send/{session_name}",
            'json': {
                'number': phone,
                'message': message
            }
        })
        return {'status': 'success', 'message': 'تمت إضافة الرسالة للطابور وسيتم إرسالها خلال ثوانٍ'}

    except Exception as e:
        logger.error(f"Error in calculate_and_notify_transaction: {str(e)}")
        return {'status': 'error', 'message': str(e)}

@admin.route('/api/admin/account/<int:account_id>/statement')
def account_statement(account_id):
    # جلب معلومات الحساب
    account = Account.query.get_or_404(account_id)
    user = User.query.get(account.user_id)

    # العملة المحددة من الطلب
    selected_currency = request.args.get('currency', 'all')
    from_date_str = request.args.get('from_date')
    to_date_str = request.args.get('to_date')

    from datetime import datetime, timedelta
    from sqlalchemy import func, case

    # تحديد الفترة الزمنية بتوقيت اليمن
    yemen_now = get_yemen_time()
    if from_date_str:
        start_date = datetime.strptime(from_date_str, '%Y-%m-%d').replace(tzinfo=YEMEN_TIMEZONE)
    else:
        start_date = yemen_now - timedelta(days=4)
    if to_date_str:
        end_date = datetime.strptime(to_date_str, '%Y-%m-%d').replace(tzinfo=YEMEN_TIMEZONE) + timedelta(days=1)  # نهاية اليوم
    else:
        end_date = yemen_now

    # --- دعم اختلاف تنسيق التاريخ ---
    def to_timestamp_ms(dt):
        if dt is None:
            return None
        if isinstance(dt, (int, float)):
            return int(dt)
        if isinstance(dt, str):
            try:
                from dateutil.parser import parse as date_parse
                dt = date_parse(dt)
            except Exception:
                return None
        if hasattr(dt, 'timestamp'):
            return int(dt.timestamp() * 1000)
        return None

    start_date_ts = to_timestamp_ms(start_date)
    end_date_ts = to_timestamp_ms(end_date)

    # جلب قائمة العملات الفريدة
    currencies = db.session.query(Transaction.currency)\
        .filter_by(account_id=account_id)\
        .distinct()\
        .all()
    currencies = [c[0] for c in currencies]

    # إذا لم يتم تحديد عملة، اختر أول عملة تلقائيًا (إن وجدت)
    if selected_currency == 'all' and currencies:
        selected_currency = currencies[0]

    # فلتر العملة
    currency_filter = []
    if selected_currency != 'all':
        currency_filter = [Transaction.currency == selected_currency]

    # فلتر التاريخ يدعم النوعين (int أو نص)
    from sqlalchemy import or_, and_
    base_query = Transaction.query.filter(
        Transaction.account_id == account_id,
        or_(
            # إذا كان التاريخ رقم (timestamp ms)
            and_(func.typeof(Transaction.date) == 'integer',
                 Transaction.date >= start_date_ts,
                 Transaction.date < end_date_ts),
            # إذا كان التاريخ نص
            and_(func.typeof(Transaction.date) != 'integer',
                 func.strftime('%s', Transaction.date) >= str(int(start_date.timestamp())),
                 func.strftime('%s', Transaction.date) < str(int(end_date.timestamp())))
        ),
        *currency_filter
    )

    # ترتيب حسب التاريخ (مع تحويل النص إلى رقم عند الحاجة)
    transactions = base_query.order_by(
        # إذا كان int استخدم كما هو، إذا نص استخدم strftime
        func.coalesce(
            func.nullif(func.typeof(Transaction.date) == 'integer', False) * Transaction.date,
            func.strftime('%s', Transaction.date) * 1000
        ),
        Transaction.id
    ).all()

    # حساب الرصيد النهائي لكل عملة (لكل الفترة)
    currency_balances = {}
    for currency in currencies:
        currency_filter2 = [Transaction.currency == currency]
        bal_query = db.session.query(
            func.sum(case((Transaction.type == 'credit', Transaction.amount), else_=-Transaction.amount))
        ).filter(
            Transaction.account_id == account_id,
            or_(
                and_(func.typeof(Transaction.date) == 'integer',
                     Transaction.date >= start_date_ts,
                     Transaction.date < end_date_ts),
                and_(func.typeof(Transaction.date) != 'integer',
                     func.strftime('%s', Transaction.date) >= str(int(start_date.timestamp())),
                     func.strftime('%s', Transaction.date) < str(int(end_date.timestamp())))
            ),
            *currency_filter2
        )
        currency_balance = bal_query.scalar() or 0
        currency_balances[currency] = currency_balance

    # تحديد الرصيد النهائي حسب العملة المحددة
    final_balance = currency_balances.get(selected_currency, 0) if selected_currency != 'all' else None

    # حساب الرصيد السابق قبل الفترة
    prev_bal_query = db.session.query(
        func.sum(case((Transaction.type == 'credit', Transaction.amount), else_=-Transaction.amount))
    ).filter(
        Transaction.account_id == account_id,
        or_(
            and_(func.typeof(Transaction.date) == 'integer', Transaction.date < start_date_ts),
            and_(func.typeof(Transaction.date) != 'integer', func.strftime('%s', Transaction.date) < str(int(start_date.timestamp())))
        )
    )
    if selected_currency != 'all':
        prev_bal_query = prev_bal_query.filter(Transaction.currency == selected_currency)
    previous_balance = prev_bal_query.scalar() or 0

    # تجهيز المعاملات مع الرصيد التراكمي الصحيح
    class TxnObj:
        def __init__(self, txn, balance):
            self.__dict__.update(txn.__dict__)
            self.balance = balance
    running_balance = previous_balance
    txn_objs = []
    for txn in transactions:
        if txn.type == 'credit':
            running_balance += txn.amount
        else:
            running_balance -= txn.amount
        txn_objs.append(TxnObj(txn, running_balance))

    # استخدام توقيت اليمن للتواريخ الافتراضية
    default_from_date = (yemen_now - timedelta(days=4)).strftime('%Y-%m-%d')
    default_to_date = yemen_now.strftime('%Y-%m-%d')
    return render_template('admin/account_statement.html',
                         account=account,
                         user=user,
                         transactions=txn_objs,
                         currencies=currencies,
                         selected_currency=selected_currency,
                         final_balance=final_balance,
                         currency_balances=currency_balances,
                         now=yemen_now,
                         default_from_date=default_from_date,
                         default_to_date=default_to_date,
                         previous_balance=previous_balance)


def send_transaction_update_notification(transaction_id, old_amount, old_date):
    try:
        from dateutil.parser import parse as date_parse
        # جلب المعاملة والحساب
        transaction = Transaction.query.get(transaction_id)
        if not transaction:
            return {'status': 'error', 'message': 'المعاملة غير موجودة'}

        # التحقق من تفعيل الواتساب للحساب
        account = Account.query.get(transaction.account_id)
        if not account:
            return {'status': 'error', 'message': 'الحساب غير موجود'}
        if not account.whatsapp_enabled:
            return {'status': 'success', 'message': 'تم تخطي الإشعار - الواتساب غير مفعل لهذا الحساب'}

        # جلب معلومات المستخدم
        user = User.query.get(account.user_id)
        if not user:
            return {'status': 'error', 'message': 'المستخدم غير موجود'}

        # توحيد التواريخ
        transaction_dt = to_datetime(transaction.date)
        old_date_dt = to_datetime(old_date)
        if not transaction_dt or not old_date_dt:
            return {'status': 'error', 'message': 'صيغة تاريخ المعاملة غير مدعومة'}

        # حساب الرصيد النهائي الكامل لنفس الحساب ونفس العملة (مباشرة في قاعدة البيانات)
        total_credits = db.session.query(func.coalesce(func.sum(Transaction.amount), 0)).filter(
            Transaction.account_id == account.id,
            Transaction.currency == transaction.currency,
            Transaction.type == 'credit'
        ).scalar()
        total_debits = db.session.query(func.coalesce(func.sum(Transaction.amount), 0)).filter(
            Transaction.account_id == account.id,
            Transaction.currency == transaction.currency,
            Transaction.type == 'debit'
        ).scalar()
        balance = total_credits - total_debits

        # تنسيق الرسالة
        transaction_type = "قيدنا الى حسابكم" if transaction.type == 'credit' else "قيدنا على حسابكم"
        balance_text = f"الرصيد لكم: {balance} {transaction.currency or 'ريال'}" if balance >= 0 else f"الرصيد عليكم: {abs(balance)} {transaction.currency or 'ريال'}"
        
        # تنسيق التاريخ القديم والجديد
        old_date_str = old_date_dt.strftime('%Y-%m-%d')
        new_date_str = transaction_dt.strftime('%Y-%m-%d')
        
        # تحديد نوع التغيير
        changes = []
        if old_amount != transaction.amount:
            changes.append(f"• المبلغ: من {old_amount} الى {transaction.amount} {transaction.currency or 'ريال'}")
        if old_date_dt != transaction_dt:
            changes.append(f"• التاريخ: من {old_date_str} الى {new_date_str}")
        
        # إذا لم يكن هناك تغييرات، نرجع رسالة
        if not changes:
            return {'status': 'success', 'message': 'لم يتم اكتشاف أي تغييرات في المعاملة'}

        message = f"""
🏦 إشعار تعديل قيد

🏛️ الاخ/: *{account.account_name}*
🔢 رقم الحساب: *{account.server_id}*


💰 تفاصيل التعديل:
{chr(10).join(changes)}
• نوع القيد: {transaction_type}
• الوصف: {transaction.description or 'لا يوجد وصف'}

💳 {balance_text}

تم التعديل بواسطة: *{user.username}*
        """.strip()

        # تنسيق رقم الهاتف
        phone = account.phone_number
        if not phone:
            return {'status': 'error', 'message': 'رقم الهاتف غير متوفر'}
        phone = ''.join(filter(str.isdigit, phone))
        if phone.startswith('966'):
            pass  # يظل كما هو
        elif phone.startswith('0'):
            phone = '967' + phone[1:]
        elif not phone.startswith('967'):
            phone = '967' + phone

        # --- منطق الجلسة وتاريخ الانتهاء ---
        session_name = user.session_name or 'admin_main'
        if session_name != 'admin_main':
            if not user.session_expiry or user.session_expiry < datetime.now(YEMEN_TIMEZONE):
                return  # لا ترسل الرسالة ولا ترجع رد
        # --- نهاية المنطق ---

        # إرسال الرسالة عبر الطابور
        whatsapp_queue.put({
            'url': f"{WHATSAPP_API}/send/{session_name}",
            'json': {
                'number': phone,
                'message': message
            }
        })
        return {'status': 'success', 'message': 'تمت إضافة الرسالة للطابور وسيتم إرسالها خلال ثوانٍ'}

    except Exception as e:
        logger.error(f"Error in send_transaction_update_notification: {str(e)}")
        return {'status': 'error', 'message': str(e)}
    
def send_transaction_delete_notification(transaction, final_balance):
    try:
        from dateutil.parser import parse as date_parse
        # التحقق من تفعيل الواتساب للحساب
        account = Account.query.get(transaction.account_id)
        if not account:
            return {'status': 'error', 'message': 'الحساب غير موجود'}
        if not account.whatsapp_enabled:
            return {'status': 'success', 'message': 'تم تخطي الإشعار - الواتساب غير مفعل لهذا الحساب'}

        # جلب معلومات المستخدم
        user = User.query.get(account.user_id)
        if not user:
            return {'status': 'error', 'message': 'المستخدم غير موجود'}

        # توحيد التاريخ
        transaction_dt = to_datetime(transaction.date)
        if not transaction_dt:
            return {'status': 'error', 'message': 'صيغة تاريخ المعاملة غير مدعومة'}

        # تنسيق الرسالة
        transaction_type = "قيدنا الى حسابكم" if transaction.type == 'credit' else "قيدنا على حسابكم"
        balance_text = f"الرصيد لكم: {final_balance} {transaction.currency or 'ريال'}" if final_balance >= 0 else f"الرصيد عليكم: {abs(final_balance)} {transaction.currency or 'ريال'}"
        
        message = f"""
🏦 إشعار حذف قيد

🏛️ الاخ/: *{account.account_name}*
🔢 رقم الحساب: *{account.server_id}*


💰 تفاصيل القيد المحذوف:
• نوع القيد: {transaction_type}
• المبلغ: {transaction.amount} {transaction.currency or 'ريال'}
• الوصف: {transaction.description or 'لا يوجد وصف'}
• التاريخ: {transaction_dt.strftime('%Y-%m-%d')}

💳 {balance_text}

تم الحذف بواسطة: *{user.username}*
        """.strip()

        # تنسيق رقم الهاتف
        phone = account.phone_number
        if not phone:
            return {'status': 'error', 'message': 'رقم الهاتف غير متوفر'}
        phone = ''.join(filter(str.isdigit, phone))
        if phone.startswith('966'):
            pass  # يظل كما هو
        elif phone.startswith('0'):
            phone = '967' + phone[1:]
        elif not phone.startswith('967'):
            phone = '967' + phone

        # --- منطق الجلسة وتاريخ الانتهاء ---
        session_name = user.session_name or 'admin_main'
        if session_name != 'admin_main':
            if not user.session_expiry or user.session_expiry < datetime.now(YEMEN_TIMEZONE):
                return  # لا ترسل الرسالة ولا ترجع رد
        # --- نهاية المنطق ---

        # إرسال الرسالة عبر الطابور
        whatsapp_queue.put({
            'url': f"{WHATSAPP_API}/send/{session_name}",
            'json': {
                'number': phone,
                'message': message
            }
        })
        return {'status': 'success', 'message': 'تمت إضافة الرسالة للطابور وسيتم إرسالها خلال ثوانٍ'}

    except Exception as e:
        logger.error(f"Error in send_transaction_delete_notification: {str(e)}")
        return {'status': 'error', 'message': str(e)}

@admin.route('/api/admin/updates/')
@admin_required
def updates():
    updates = AppUpdate.query.order_by(AppUpdate.release_date.desc()).all()
    return render_template('admin/updates.html', updates=updates)

@admin.route('/api/admin/updates/add', methods=['GET', 'POST'])
@admin_required
def add_update():
    if request.method == 'POST':
        try:
            update = AppUpdate(
                version=request.form['version'],
                description=request.form['description'],
                download_url=request.form['download_url'],
                min_version=request.form['min_version'],
                release_date=datetime.strptime(request.form['release_date'], '%Y-%m-%dT%H:%M'),
                force_update='force_update' in request.form,
                is_active='is_active' in request.form
            )
            db.session.add(update)
            db.session.commit()
            flash('تم إضافة التحديث بنجاح', 'success')
            return redirect(url_for('admin.updates'))
        except Exception as e:
            db.session.rollback()
            flash(f'حدث خطأ أثناء إضافة التحديث: {str(e)}', 'error')
    return render_template('admin/add_update.html')

@admin.route('/api/admin/updates/<int:update_id>/edit', methods=['GET', 'POST'])
@admin_required
def edit_update(update_id):
    update = AppUpdate.query.get_or_404(update_id)
    if request.method == 'POST':
        try:
            update.version = request.form['version']
            update.description = request.form['description']
            update.download_url = request.form['download_url']
            update.min_version = request.form['min_version']
            update.release_date = datetime.strptime(request.form['release_date'], '%Y-%m-%dT%H:%M')
            update.force_update = 'force_update' in request.form
            update.is_active = 'is_active' in request.form
            db.session.commit()
            flash('تم تحديث البيانات بنجاح', 'success')
            return redirect(url_for('admin.updates'))
        except Exception as e:
            db.session.rollback()
            flash(f'حدث خطأ أثناء تحديث البيانات: {str(e)}', 'error')
    return render_template('admin/edit_update.html', update=update)

@admin.route('/api/privacy-policy')
def privacy_policy():
    return render_template('privacy_policy.html')

@admin.route('/api/admin/import_accounts_text/<int:user_id>', methods=['GET', 'POST'])
@admin_required
def import_accounts_text(user_id):
    user = User.query.get_or_404(user_id)
    if request.method == 'POST':
        data = request.form.get('accounts_data', '')
        accounts = []
        for line in data.strip().split('\n'):
            line = line.strip()
            if not line:
                continue  # تجاهل الأسطر الفارغة
            # تقسيم السطر على أول tab أو أكثر من space
            parts = line.split('\t')
            if len(parts) < 2:
                parts = line.split()
                if len(parts) < 2:
                    continue
                account_name = ' '.join(parts[:-1])
                phone_number = parts[-1]
            else:
                account_name, phone_number = parts[0], parts[1]
            accounts.append({
                'account_name': account_name.strip(),
                'phone_number': phone_number.strip()
            })
        # حفظ الحسابات مؤقتاً في session لعرضها للموافقة
        session['pending_accounts'] = accounts
        session['pending_user_id'] = user_id
        return render_template('admin/confirm_import_accounts.html', user=user, accounts=accounts)
    return render_template('admin/import_accounts_text.html', user=user)

@admin.route('/api/admin/confirm_import_accounts', methods=['POST'])
@admin_required
def confirm_import_accounts():
    accounts = session.get('pending_accounts', [])
    user_id = session.get('pending_user_id')
    if not accounts or not user_id:
        flash('لا توجد بيانات لاستيرادها', 'error')
        return redirect(url_for('admin.users'))
    user = User.query.get_or_404(user_id)
    count = 0
    skipped = 0  # عدد الحسابات التي تم تجاهلها بسبب التكرار
    # جلب آخر server_id مستخدم
    last_account = Account.query.order_by(Account.server_id.desc()).first()
    new_server_id = (last_account.server_id + 1) if last_account and last_account.server_id else 1
    for acc in accounts:
        # تحقق من وجود حساب بنفس رقم الهاتف لنفس المستخدم
        existing_account = Account.query.filter_by(
            user_id=user.id,
            phone_number=acc['phone_number']
        ).first()
        if existing_account:
            skipped += 1
            continue  # تجاهل الحساب المكرر
        account = Account(
            user_id=user.id,
            account_name=acc['account_name'],
            phone_number=acc['phone_number'],
            account_number=str(uuid.uuid4())[:8],  # رقم حساب عشوائي قصير
            server_id=new_server_id,
            created_at=datetime.now(YEMEN_TIMEZONE)
        )
        db.session.add(account)
        count += 1
        new_server_id += 1
    db.session.commit()
    session.pop('pending_accounts', None)
    session.pop('pending_user_id', None)
    msg = f'تم استيراد {count} حساب بنجاح.'
    if skipped:
        msg += f' تم تجاهل {skipped} حساب بسبب التكرار.'
    flash(msg, 'success')
    return redirect(url_for('admin.user_details', user_id=user.id))

@admin.route('/api/admin/delete_account_with_transactions/<int:account_id>', methods=['POST'])
@admin_required
def delete_account_with_transactions(account_id):
    account = Account.query.get_or_404(account_id)
    # حذف جميع العمليات المرتبطة بالحساب
    Transaction.query.filter_by(account_id=account.id).delete()
    # حذف الحساب نفسه
    db.session.delete(account)
    db.session.commit()
    flash('تم حذف الحساب وجميع العمليات المرتبطة به بنجاح.', 'success')
    return redirect(url_for('admin.accounts'))

@admin.route('/api/admin/transactions/data')
@admin_required
def transactions_data():
    # باراميترات DataTables
    draw = int(request.args.get('draw', 1))
    start = int(request.args.get('start', 0))
    length = int(request.args.get('length', 30))
    search_value = request.args.get('search[value]', '').strip()
    order_column_index = int(request.args.get('order[0][column]', 0))
    order_dir = request.args.get('order[0][dir]', 'desc')

    # فلاتر إضافية
    account_id = request.args.get('account_id', type=int)
    user_id = request.args.get('user_id', type=int)
    currency = request.args.get('currency', type=str)

    # الأعمدة بالترتيب
    columns = ['date', 'account_name', 'type', 'amount', 'currency', 'description', 'username']
    order_column = columns[order_column_index] if order_column_index < len(columns) else 'date'

    # بناء الاستعلام
    query = db.session.query(Transaction, Account, User).join(Account, Transaction.account_id == Account.id).join(User, Transaction.user_id == User.id)
    if account_id:
        query = query.filter(Transaction.account_id == account_id)
    if user_id:
        query = query.filter(Transaction.user_id == user_id)
    if currency:
        query = query.filter(Transaction.currency == currency)
    if search_value:
        like = f"%{search_value}%"
        query = query.filter(
            db.or_(
                Account.account_name.ilike(like),
                User.username.ilike(like),
                Transaction.description.ilike(like),
                Transaction.currency.ilike(like),
                func.cast(Transaction.amount, db.String).ilike(like)
            )
        )
    records_total = db.session.query(func.count(Transaction.id)).scalar()
    records_filtered = query.count()
    # ترتيب
    if order_column == 'date':
        order_by = Transaction.id.desc() if order_dir == 'desc' else Transaction.id.asc()
    elif order_column == 'account_name':
        order_by = Account.account_name.desc() if order_dir == 'desc' else Account.account_name.asc()
    elif order_column == 'username':
        order_by = User.username.desc() if order_dir == 'desc' else User.username.asc()
    elif order_column == 'amount':
        order_by = Transaction.amount.desc() if order_dir == 'desc' else Transaction.amount.asc()
    else:
        order_by = Transaction.id.desc()
    query = query.order_by(order_by)
    # pagination
    results = query.offset(start).limit(length).all()
    data = []
    for t, a, u in results:
        data.append({
            'date': t.date,  # عرض التاريخ كما هو في قاعدة البيانات
            'account_name': a.account_name,
            'type': t.type,
            'amount': t.amount,
            'currency': t.currency or 'ريال',
            'description': t.description or '-',
            'username': u.username,
            'id': t.id
        })
    return jsonify({
        'draw': draw,
        'recordsTotal': records_total,
        'recordsFiltered': records_filtered,
        'data': data
    })

@admin.route('/api/admin/users/data')
@admin_required
def users_data():
    draw = int(request.args.get('draw', 1))
    start = int(request.args.get('start', 0))
    length = int(request.args.get('length', 30))
    search_value = request.args.get('search[value]', '').strip()
    order_column_index = int(request.args.get('order[0][column]', 0))
    order_dir = request.args.get('order[0][dir]', 'desc')

    columns = ['id', 'username', 'phone', 'accounts_count', 'transactions_count', 'total_credits', 'total_debits', 'balance', 'device_name', 'android_version', 'last_seen']
    order_column = columns[order_column_index] if order_column_index < len(columns) else 'username'

    query = db.session.query(User)
    if search_value:
        like = f"%{search_value}%"
        query = query.filter(
            db.or_(
                User.username.ilike(like),
                User.phone.ilike(like)
            )
        )
    records_total = db.session.query(func.count(User.id)).scalar()
    records_filtered = query.count()
    # ترتيب
    if order_column == 'id':
        order_by = User.id.desc() if order_dir == 'desc' else User.id.asc()
    elif order_column == 'username':
        order_by = User.username.desc() if order_dir == 'desc' else User.username.asc()
    elif order_column == 'phone':
        order_by = User.phone.desc() if order_dir == 'desc' else User.phone.asc()
    elif order_column == 'accounts_count':
        subq = db.session.query(Account.user_id, func.count(Account.id).label('ac')).group_by(Account.user_id).subquery()
        query = query.outerjoin(subq, User.id == subq.c.user_id)
        order_by = subq.c.ac.desc() if order_dir == 'desc' else subq.c.ac.asc()
    elif order_column == 'transactions_count':
        subq = db.session.query(Transaction.user_id, func.count(Transaction.id).label('tc')).group_by(Transaction.user_id).subquery()
        query = query.outerjoin(subq, User.id == subq.c.user_id)
        order_by = subq.c.tc.desc() if order_dir == 'desc' else subq.c.tc.asc()
    elif order_column == 'total_credits':
        subq = db.session.query(Transaction.user_id, func.coalesce(func.sum(case((Transaction.type == 'credit', Transaction.amount), else_=0)), 0).label('credits')).group_by(Transaction.user_id).subquery()
        query = query.outerjoin(subq, User.id == subq.c.user_id)
        order_by = subq.c.credits.desc() if order_dir == 'desc' else subq.c.credits.asc()
    elif order_column == 'total_debits':
        subq = db.session.query(Transaction.user_id, func.coalesce(func.sum(case((Transaction.type == 'debit', Transaction.amount), else_=0)), 0).label('debits')).group_by(Transaction.user_id).subquery()
        query = query.outerjoin(subq, User.id == subq.c.user_id)
        order_by = subq.c.debits.desc() if order_dir == 'desc' else subq.c.debits.asc()
    elif order_column == 'balance':
        subq = db.session.query(
            Transaction.user_id,
            (func.coalesce(func.sum(case((Transaction.type == 'credit', Transaction.amount), else_=0)), 0) -
             func.coalesce(func.sum(case((Transaction.type == 'debit', Transaction.amount), else_=0)), 0)).label('balance')
        ).group_by(Transaction.user_id).subquery()
        query = query.outerjoin(subq, User.id == subq.c.user_id)
        order_by = subq.c.balance.desc() if order_dir == 'desc' else subq.c.balance.asc()
    elif order_column == 'device_name':
        order_by = User.device_name.desc() if order_dir == 'desc' else User.device_name.asc()
    elif order_column == 'android_version':
        order_by = User.android_version.desc() if order_dir == 'desc' else User.android_version.asc()
    elif order_column == 'last_seen':
        order_by = User.last_seen.desc() if order_dir == 'desc' else User.last_seen.asc()
    else:
        order_by = User.id.desc()
    query = query.order_by(order_by)
    results = query.offset(start).limit(length).all()
    data = []
    for u in results:
        accounts_count = Account.query.filter_by(user_id=u.id).count()
        transactions_count = Transaction.query.filter_by(user_id=u.id).count()
        total_credits = db.session.query(func.sum(Transaction.amount)).filter(Transaction.user_id == u.id, Transaction.type == 'credit').scalar() or 0
        total_debits = db.session.query(func.sum(Transaction.amount)).filter(Transaction.user_id == u.id, Transaction.type == 'debit').scalar() or 0
        balance = total_credits - total_debits
        last_seen, last_seen_color = format_last_seen(u.last_seen)
        data.append({
            'id': u.id,
            'username': u.username,
            'phone': u.phone,
            'accounts_count': accounts_count,
            'transactions_count': transactions_count,
            'total_credits': total_credits,
            'total_debits': total_debits,
            'balance': balance,
            'device_name': u.device_name,
            'android_version': u.android_version,
            'last_seen': last_seen,
            'last_seen_color': last_seen_color
        })
    return jsonify({
        'draw': draw,
        'recordsTotal': records_total,
        'recordsFiltered': records_filtered,
        'data': data
    })

@admin.route('/api/admin/accounts/data')
@admin_required
def accounts_data():
    draw = int(request.args.get('draw', 1))
    start = int(request.args.get('start', 0))
    length = int(request.args.get('length', 30))
    search_value = request.args.get('search[value]', '').strip()
    user_id = request.args.get('user_id', type=int)
    currency = request.args.get('currency', type=str)
    order_column_index = int(request.args.get('order[0][column]', 0))
    order_dir = request.args.get('order[0][dir]', 'desc')

    columns = ['server_id', 'account_name', 'phone_number', 'user', 'currency', 'transactions_count', 'total_debits', 'total_credits', 'balance', 'created_at', 'updated_at', 'notes', 'whatsapp_enabled']
    order_column = columns[order_column_index] if order_column_index < len(columns) else 'server_id'

    query = db.session.query(Account, User).join(User, Account.user_id == User.id)
    if user_id:
        query = query.filter(Account.user_id == user_id)
    if currency:
        query = query.filter(Account.currency == currency)
    if search_value:
        like = f"%{search_value}%"
        query = query.filter(
            db.or_(
                Account.account_name.ilike(like),
                Account.phone_number.ilike(like),
                User.username.ilike(like)
            )
        )
    records_total = db.session.query(func.count(Account.id)).scalar()
    records_filtered = query.count()
    # ترتيب
    if order_column == 'server_id':
        order_by = Account.server_id.desc() if order_dir == 'desc' else Account.server_id.asc()
    elif order_column == 'account_name':
        order_by = Account.account_name.desc() if order_dir == 'desc' else Account.account_name.asc()
    elif order_column == 'phone_number':
        order_by = Account.phone_number.desc() if order_dir == 'desc' else Account.phone_number.asc()
    elif order_column == 'user':
        order_by = User.username.desc() if order_dir == 'desc' else User.username.asc()
    elif order_column == 'transactions_count':
        # ترتيب حسب عدد المعاملات
        subq = db.session.query(Transaction.account_id, func.count(Transaction.id).label('tc')).group_by(Transaction.account_id).subquery()
        query = query.outerjoin(subq, Account.id == subq.c.account_id)
        order_by = subq.c.tc.desc() if order_dir == 'desc' else subq.c.tc.asc()
    elif order_column == 'total_debits':
        # ترتيب حسب إجمالي المدين
        subq = db.session.query(Transaction.account_id, func.coalesce(func.sum(case((Transaction.type == 'debit', Transaction.amount), else_=0)), 0).label('debits')).group_by(Transaction.account_id).subquery()
        query = query.outerjoin(subq, Account.id == subq.c.account_id)
        order_by = subq.c.debits.desc() if order_dir == 'desc' else subq.c.debits.asc()
    elif order_column == 'total_credits':
        # ترتيب حسب إجمالي الدائن
        subq = db.session.query(Transaction.account_id, func.coalesce(func.sum(case((Transaction.type == 'credit', Transaction.amount), else_=0)), 0).label('credits')).group_by(Transaction.account_id).subquery()
        query = query.outerjoin(subq, Account.id == subq.c.account_id)
        order_by = subq.c.credits.desc() if order_dir == 'desc' else subq.c.credits.asc()
    elif order_column == 'balance':
        # ترتيب حسب الرصيد (الدائن - المدين)
        subq = db.session.query(
            Transaction.account_id,
            (func.coalesce(func.sum(case((Transaction.type == 'credit', Transaction.amount), else_=0)), 0) -
             func.coalesce(func.sum(case((Transaction.type == 'debit', Transaction.amount), else_=0)), 0)).label('balance')
        ).group_by(Transaction.account_id).subquery()
        query = query.outerjoin(subq, Account.id == subq.c.account_id)
        order_by = subq.c.balance.desc() if order_dir == 'desc' else subq.c.balance.asc()
    elif order_column == 'created_at':
        order_by = Account.created_at.desc() if order_dir == 'desc' else Account.created_at.asc()
    elif order_column == 'updated_at':
        order_by = Account.updated_at.desc() if order_dir == 'desc' else Account.updated_at.asc()
    elif order_column == 'notes':
        order_by = Account.notes.desc() if order_dir == 'desc' else Account.notes.asc()
    elif order_column == 'whatsapp_enabled':
        order_by = Account.whatsapp_enabled.desc() if order_dir == 'desc' else Account.whatsapp_enabled.asc()
    else:
        order_by = Account.server_id.desc()
    query = query.order_by(order_by)
    results = query.offset(start).limit(length).all()
    data = []
    for a, u in results:
        transactions_count = Transaction.query.filter_by(account_id=a.id).count()
        total_debits = db.session.query(func.sum(Transaction.amount)).filter(Transaction.account_id == a.id, Transaction.type == 'debit').scalar() or 0
        total_credits = db.session.query(func.sum(Transaction.amount)).filter(Transaction.account_id == a.id, Transaction.type == 'credit').scalar() or 0
        balance = total_credits - total_debits
        data.append({
            'server_id': a.server_id,
            'account_name': a.account_name,
            'phone_number': a.phone_number,
            'user': u.username,
            'transactions_count': transactions_count,
            'total_debits': total_debits,
            'total_credits': total_credits,
            'balance': balance,
            'created_at': to_datetime(a.created_at).strftime('%Y-%m-%d %H:%M') if to_datetime(a.created_at) else '',
            'updated_at': to_datetime(a.updated_at).strftime('%Y-%m-%d %H:%M') if to_datetime(a.updated_at) else '',
            'notes': a.notes or '',
            'whatsapp_enabled': 'نعم' if a.whatsapp_enabled else 'لا',
            'id': a.id
        })
    return jsonify({
        'draw': draw,
        'recordsTotal': records_total,
        'recordsFiltered': records_filtered,
        'data': data
    })

def version_compare_simple(v1, v2, op):
    def normalize(v):
        return [int(x) for x in str(v).split('.') if x.isdigit()]
    a = normalize(v1)
    b = normalize(v2)
    # اجعل الطول متساوي
    maxlen = max(len(a), len(b))
    a += [0] * (maxlen - len(a))
    b += [0] * (maxlen - len(b))
    if op == '<':
        return a < b
    elif op == '<=':
        return a <= b
    elif op == '==':
        return a == b
    elif op == '>=':
        return a >= b
    elif op == '>':
        return a > b
    else:
        return False

@admin.route('/api/admin/whatsapp/send_to_users', methods=['POST'])
@admin_required
def send_whatsapp_to_users():
    try:
        data = request.json
        message = data.get('message')
        android_version = data.get('android_version')  # يمكن أن يكون None
        version_operator = data.get('version_operator')  # مثل '<' أو '<=' أو '=='
        if not message:
            return jsonify({'success': False, 'error': 'الرسالة مطلوبة'}), 400

        # جلب المستخدمين المستهدفين
        query = User.query
        if android_version and version_operator:
            users = query.all()
            filtered_users = []
            for user in users:
                user_ver = user.android_version or ''
                try:
                    if user_ver and version_compare_simple(user_ver, android_version, version_operator):
                        filtered_users.append(user)
                except Exception:
                    continue
            users = filtered_users
        elif android_version:
            query = query.filter(User.android_version == android_version)
            users = query.all()
        else:
            users = query.all()
        if not users:
            return jsonify({'success': False, 'error': 'لا يوجد مستخدمين مطابقين'}), 404

        count = 0
        for user in users:
            # جلب رقم الهاتف من الحسابات المرتبطة
            account = Account.query.filter_by(user_id=user.id).filter(Account.phone_number != None).first()
            if not account or not account.phone_number:
                continue
            phone = ''.join(filter(str.isdigit, account.phone_number))
            if phone.startswith('966'):
                pass
            elif phone.startswith('0'):
                phone = '967' + phone[1:]
            elif not phone.startswith('967'):
                phone = '967' + phone
            whatsapp_queue.put({
                'url': f"{WHATSAPP_API}/send/admin_main",
                'json': {
                    'number': phone,
                    'message': message
                }
            })
            count += 1
        return jsonify({'success': True, 'count': count})
    except Exception as e:
        logger.error(f"Error in send_whatsapp_to_users: {str(e)}")
        print(f"Error in send_whatsapp_to_users: {str(e)}")  # طباعة الخطأ مباشرة للسجل
        return jsonify({'success': False, 'error': str(e)})

# فلتر Jinja2 لتحويل التاريخ (تايم ستامب أو نص) إلى تاريخ مقروء
@admin.app_template_filter('datetimeformat')
def datetimeformat(value, format='%Y-%m-%d'):
    from datetime import datetime
    try:
        # إذا كان value رقم (تايم ستامب بالمللي ثانية)
        if str(value).isdigit():
            return datetime.fromtimestamp(int(value) / 1000, YEMEN_TIMEZONE).strftime(format)
        # جرب مكتبة dateutil إذا كانت متوفرة
        try:
            from dateutil.parser import parse as date_parse
            d = date_parse(value)
            if d.tzinfo is None:
                d = d.replace(tzinfo=YEMEN_TIMEZONE)
            return d.astimezone(YEMEN_TIMEZONE).strftime(format)
        except Exception:
            pass
        # جرب عدة صيغ شائعة
        for fmt in ('%Y-%m-%dT%H:%M:%S.%f', '%Y-%m-%dT%H:%M:%S', '%Y-%m-%d %H:%M:%S.%f', '%Y-%m-%d %H:%M:%S', '%Y-%m-%d'):
            try:
                d = datetime.strptime(value, fmt)
                d = d.replace(tzinfo=YEMEN_TIMEZONE)
                return d.strftime(format)
            except Exception:
                continue
        # جرب fromisoformat (يدعم بعض الصيغ)
        try:
            d = datetime.fromisoformat(value)
            if d.tzinfo is None:
                d = d.replace(tzinfo=YEMEN_TIMEZONE)
            return d.astimezone(YEMEN_TIMEZONE).strftime(format)
        except Exception:
            pass
        return value
    except Exception:
        return value

# أضف دالة توحيد التاريخ في الأعلى (إذا لم تكن موجودة)
def to_millis(dt):
    if dt is None:
        return None
    if isinstance(dt, (int, float)):
        return int(dt)
    if isinstance(dt, str):
        try:
            if 'T' in dt:
                from datetime import datetime, timezone
                dt = datetime.fromisoformat(dt.replace('Z', '+00:00'))
            else:
                from datetime import datetime
                dt = datetime.fromisoformat(dt)
        except Exception:
            return None
    if hasattr(dt, 'tzinfo') and dt.tzinfo:
        dt_utc = dt.astimezone(timezone.utc)
    else:
        from datetime import timedelta
        dt_utc = dt - timedelta(hours=3)
    return int(dt_utc.timestamp() * 1000)

def to_datetime(dt):
    """تحويل أي قيمة إلى datetime بتوقيت اليمن (timezone-aware)"""
    if dt is None:
        return None
    if isinstance(dt, datetime):
        # إذا كان بدون timezone، أضف توقيت اليمن
        if dt.tzinfo is None:
            return dt.replace(tzinfo=YEMEN_TIMEZONE)
        return dt.astimezone(YEMEN_TIMEZONE)
    if isinstance(dt, (int, float)):
        # timestamp بالميلي ثانية
        return datetime.fromtimestamp(dt / 1000, YEMEN_TIMEZONE)
    if isinstance(dt, str):
        try:
            if 'T' in dt:
                d = datetime.fromisoformat(dt.replace('Z', '+00:00'))
            else:
                d = datetime.fromisoformat(dt)
            # إذا كان بدون timezone، أضف توقيت اليمن
            if d.tzinfo is None:
                d = d.replace(tzinfo=YEMEN_TIMEZONE)
            return d.astimezone(YEMEN_TIMEZONE)
        except Exception:
            return None
    return None

# دالة تحويل datetime إلى millis بتوقيت اليمن

def to_millis(dt):
    dt = to_datetime(dt)
    if dt is None:
        return None
    return int(dt.timestamp() * 1000)
