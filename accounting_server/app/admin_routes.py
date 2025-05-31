from flask import Blueprint, jsonify, request, render_template, redirect, url_for, flash
from app import db
from app.models import User, Account, Transaction
from app.utils import hash_password
from datetime import datetime, timedelta
from sqlalchemy import func, case
import json
import requests

admin = Blueprint('admin', __name__)

# كلمة المرور للإدارة - يمكن تغييرها من ملف الخادم
ADMIN_PASSWORD = "Hillal774447251"

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
            status_response = requests.get('http://localhost:3002/status', timeout=5)
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
        response = requests.get(f'http://localhost:3002/start/{session_id}', timeout=5)
        return jsonify(response.json())
    except requests.exceptions.RequestException as e:
        print(f"Error starting WhatsApp session: {str(e)}")
        return jsonify({'error': 'لا يمكن الاتصال بخادم الواتساب'}), 500

@admin.route('/admin/whatsapp/qr/<session_id>')
@admin_required
def get_whatsapp_qr(session_id):
    try:
        response = requests.get(f'http://localhost:3002/qr/{session_id}', timeout=5)
        return response.text
    except requests.exceptions.RequestException as e:
        print(f"Error getting QR code: {str(e)}")
        return jsonify({'error': 'لا يمكن الاتصال بخادم الواتساب'}), 500

@admin.route('/admin/whatsapp/send', methods=['POST'])
@admin_required
def send_whatsapp_message():
    try:
        data = request.json
        session_id = 'admin_main'
        
        # تجهيز قائمة الأرقام
        numbers = []
        
        if data['type'] == 'single_user':
            user = User.query.get(data['user_id'])
            if user and user.phone:
                numbers.append(user.phone)
        
        elif data['type'] == 'multiple_users':
            users = User.query.filter(User.id.in_(data['user_ids'])).all()
            numbers.extend([user.phone for user in users if user.phone])
        
        elif data['type'] == 'user_accounts':
            user = User.query.get(data['user_id'])
            if user:
                accounts = Account.query.filter_by(user_id=user.id).all()
                numbers.extend([account.phone for account in accounts if account.phone])
        
        if not numbers:
            return jsonify({'error': 'لم يتم العثور على أرقام هواتف صالحة'})
        
        response = requests.post(
            f'http://localhost:3002/send/{session_id}',
            json={
                'numbers': numbers,
                'message': data['message']
            },
            timeout=5
        )
        return jsonify(response.json())
    except requests.exceptions.RequestException as e:
        print(f"Error sending WhatsApp message: {str(e)}")
        return jsonify({'error': 'لا يمكن الاتصال بخادم الواتساب'}), 500

@admin.route('/admin/whatsapp/status')
@admin_required
def whatsapp_status():
    try:
        response = requests.get('http://localhost:3002/status', timeout=5)
        return jsonify(response.json())
    except requests.exceptions.RequestException as e:
        print(f"Error getting WhatsApp status: {str(e)}")
        return jsonify({'error': 'لا يمكن الاتصال بخادم الواتساب'}), 500 