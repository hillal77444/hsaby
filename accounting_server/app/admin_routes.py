from flask import Blueprint, jsonify, request, render_template, redirect, url_for, flash, Response
from app import db
from app.models import User, Account, Transaction, AppUpdate
from app.utils import hash_password
from datetime import datetime, timedelta, timezone
from sqlalchemy import func, case
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
        'expires_at': datetime.now() + timedelta(seconds=expires_sec)
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
    if datetime.now() > data['expires_at']:
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

@admin.route('/api/admin/users')
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
    
    db.session.commit()
    flash('تم تحديث بيانات المستخدم بنجاح')
    return redirect(url_for('admin.user_details', user_id=user_id))

@admin.route('/api/admin/accounts')
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

@admin.route('/api/admin/transactions')
@admin_required
def transactions():
    transactions = Transaction.query.order_by(Transaction.date.desc()).all()
    return render_template('admin/transactions.html', transactions=transactions)

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

        # حساب الرصيد حتى هذه المعاملة لنفس الحساب ونفس العملة
        transactions = Transaction.query.filter(
            Transaction.account_id == account.id,
            Transaction.currency == transaction.currency,
            Transaction.date <= transaction.date,
            Transaction.id <= transaction.id
        ).order_by(
            Transaction.date,
            Transaction.id
        ).all()

        # حساب الرصيد النهائي
        balance = 0
        for trans in transactions:
            if trans.type == 'credit':
                balance += trans.amount
            else:  # debit
                balance -= trans.amount

        # توليد رابط كشف الحساب المؤقت القصير
        statement_link = generate_short_statement_link(account.id)

        # تنسيق الرسالة مع الرابط المؤقت
        transaction_type = "قيدنا الى حسابكم" if transaction.type == 'credit' else "قيدنا على حسابكم"
        balance_text = f"الرصيد لكم: {balance} {transaction.currency or 'ريال'}" if balance >= 0 else f"الرصيد عليكم: {abs(balance)} {transaction.currency or 'ريال'}"
        message = f"""
🏦 إشعار قيد جديدة

🏛️ الاخ/: *{account.account_name}*

💰 تفاصيل القيد :
•  {transaction_type}
• المبلغ: {transaction.amount} {transaction.currency or 'ريال'}
• الوصف: {transaction.description or 'لا يوجد وصف'}
• التاريخ: {transaction.date.strftime('%Y-%m-%d')}

💳 {balance_text}

📄 كشف الحساب : {statement_link}

تم الإرسال بواسطة: *{user.username}*
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
            f"{WHATSAPP_API}/send/admin_main",
            json={
                'number': phone,
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

@admin.route('/api/admin/account/<int:account_id>/statement')
def account_statement(account_id):
    # جلب معلومات الحساب
    account = Account.query.get_or_404(account_id)
    user = User.query.get(account.user_id)
    
    # العملة المحددة من الطلب
    selected_currency = request.args.get('currency', 'all')
    
    # جلب المعاملات حسب العملة المختارة
    query = Transaction.query.filter_by(account_id=account_id)
    if selected_currency != 'all':
        query = query.filter_by(currency=selected_currency)
    
    # جلب المعاملات مرتبة حسب التاريخ
    transactions = query.order_by(Transaction.date, Transaction.id).all()
    
    # جلب قائمة العملات الفريدة
    currencies = db.session.query(Transaction.currency)\
        .filter_by(account_id=account_id)\
        .distinct()\
        .all()
    currencies = [c[0] for c in currencies]
    
    # حساب الرصيد المتراكم حسب العملة المحددة
    if selected_currency != 'all':
        # جلب جميع المعاملات للعملة المحددة مرتبة حسب التاريخ
        currency_transactions = Transaction.query.filter_by(
            account_id=account_id,
            currency=selected_currency
        ).order_by(Transaction.date, Transaction.id).all()
        
        # حساب الرصيد المتراكم
        balance = 0
        for transaction in currency_transactions:
            if transaction.type == 'credit':
                balance += transaction.amount
            else:
                balance -= transaction.amount
            transaction.balance = balance
    else:
        # إذا كانت جميع العملات، نحسب الرصيد لكل معاملة حسب عملتها
        currency_balances = {}
        for transaction in transactions:
            currency = transaction.currency
            if currency not in currency_balances:
                currency_balances[currency] = 0
            
            if transaction.type == 'credit':
                currency_balances[currency] += transaction.amount
            else:
                currency_balances[currency] -= transaction.amount
            
            transaction.balance = currency_balances[currency]
    
    # حساب الرصيد النهائي لكل عملة
    currency_balances = {}
    for currency in currencies:
        currency_transactions = Transaction.query.filter_by(
            account_id=account_id,
            currency=currency
        ).order_by(Transaction.date, Transaction.id).all()
        
        currency_balance = 0
        for trans in currency_transactions:
            if trans.type == 'credit':
                currency_balance += trans.amount
            else:
                currency_balance -= trans.amount
        currency_balances[currency] = currency_balance
    
    # تحديد الرصيد النهائي حسب العملة المحددة
    final_balance = currency_balances.get(selected_currency, 0) if selected_currency != 'all' else None
    
    return render_template('admin/account_statement.html',
                         account=account,
                         user=user,
                         transactions=transactions,
                         currencies=currencies,
                         selected_currency=selected_currency,
                         final_balance=final_balance,
                         currency_balances=currency_balances,
                         now=datetime.now())

def send_transaction_update_notification(transaction_id, old_amount, old_date):
    try:
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

        # حساب الرصيد النهائي الكامل لنفس الحساب ونفس العملة
        transactions = Transaction.query.filter(
            Transaction.account_id == account.id,
            Transaction.currency == transaction.currency
        ).order_by(
            Transaction.date,
            Transaction.id
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
        
        # تنسيق التاريخ القديم والجديد
        old_date_str = old_date.strftime('%Y-%m-%d')
        new_date_str = transaction.date.strftime('%Y-%m-%d')
        
        # تحديد نوع التغيير
        changes = []
        if old_amount != transaction.amount:
            changes.append(f"• المبلغ: من {old_amount} الى {transaction.amount} {transaction.currency or 'ريال'}")
        if old_date != transaction.date:
            changes.append(f"• التاريخ: من {old_date_str} الى {new_date_str}")
        
        # إذا لم يكن هناك تغييرات، نرجع رسالة
        if not changes:
            return {'status': 'success', 'message': 'لم يتم اكتشاف أي تغييرات في المعاملة'}

        message = f"""
🏦 إشعار تحديث قيد

🏛️ الاخ/: *{account.account_name}*

💰 تفاصيل التحديث:
{chr(10).join(changes)}
• نوع القيد: {transaction_type}
• الوصف: {transaction.description or 'لا يوجد وصف'}

💳 {balance_text}

تم التحديث بواسطة: *{user.username}*
        """.strip()

        # تنسيق رقم الهاتف
        phone = account.phone_number
        if not phone:
            return {'status': 'error', 'message': 'رقم الهاتف غير متوفر'}
            
        phone = ''.join(filter(str.isdigit, phone))
        if phone.startswith('0'):
            phone = '967' + phone[1:]
        if not phone.startswith('967'):
            phone = '967' + phone

        # إرسال الرسالة
        response = requests.post(
            f"{WHATSAPP_API}/send/admin_main",
            json={
                'number': phone,
                'message': message
            },
            timeout=5
        )

        if response.status_code == 200:
            return {'status': 'success', 'message': 'تم إرسال إشعار التحديث بنجاح'}
        else:
            return {'status': 'error', 'message': 'فشل في إرسال إشعار التحديث'}

    except Exception as e:
        logger.error(f"Error in send_transaction_update_notification: {str(e)}")
        return {'status': 'error', 'message': str(e)}

def send_transaction_delete_notification(transaction, final_balance):
    try:
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

        # تنسيق الرسالة
        transaction_type = "قيدنا الى حسابكم" if transaction.type == 'credit' else "قيدنا على حسابكم"
        balance_text = f"الرصيد لكم: {final_balance} {transaction.currency or 'ريال'}" if final_balance >= 0 else f"الرصيد عليكم: {abs(final_balance)} {transaction.currency or 'ريال'}"
        
        message = f"""
🏦 إشعار حذف قيد

🏛️ الاخ/: *{account.account_name}*

💰 تفاصيل القيد المحذوف:
• نوع القيد: {transaction_type}
• المبلغ: {transaction.amount} {transaction.currency or 'ريال'}
• الوصف: {transaction.description or 'لا يوجد وصف'}
• التاريخ: {transaction.date.strftime('%Y-%m-%d')}

💳 {balance_text}

تم الحذف بواسطة: *{user.username}*
        """.strip()

        # تنسيق رقم الهاتف
        phone = account.phone_number
        if not phone:
            return {'status': 'error', 'message': 'رقم الهاتف غير متوفر'}
            
        phone = ''.join(filter(str.isdigit, phone))
        if phone.startswith('0'):
            phone = '967' + phone[1:]
        if not phone.startswith('967'):
            phone = '967' + phone

        # إرسال الرسالة
        response = requests.post(
            f"{WHATSAPP_API}/send/admin_main",
            json={
                'number': phone,
                'message': message
            },
            timeout=5
        )

        if response.status_code == 200:
            return {'status': 'success', 'message': 'تم إرسال إشعار الحذف بنجاح'}
        else:
            return {'status': 'error', 'message': 'فشل في إرسال إشعار الحذف'}

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
