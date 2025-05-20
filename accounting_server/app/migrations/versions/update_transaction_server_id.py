"""update transaction server_id

Revision ID: update_transaction_server_id
Revises: # سيتم تحديثه تلقائياً
Create Date: 2025-05-19

"""
from alembic import op
import sqlalchemy as sa
from datetime import datetime

# revision identifiers, used by Alembic
revision = 'update_transaction_server_id'
down_revision = None  # سيتم تحديثه تلقائياً
branch_labels = None
depends_on = None

def upgrade():
    # 1. تعطيل المفاتيح الخارجية مؤقتاً
    op.execute('PRAGMA foreign_keys=OFF;')
    
    # 2. إنشاء جدول مؤقت
    op.create_table(
        'transaction_temp',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('server_id', sa.Integer(), nullable=True),
        sa.Column('date', sa.DateTime(), nullable=False),
        sa.Column('amount', sa.Float(), nullable=False),
        sa.Column('description', sa.String(255), nullable=False),
        sa.Column('type', sa.String(50), default='debit'),
        sa.Column('currency', sa.String(50), default='يمني'),
        sa.Column('notes', sa.Text()),
        sa.Column('whatsapp_enabled', sa.Boolean(), default=True),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('account_id', sa.Integer(), nullable=False),
        sa.Column('created_at', sa.DateTime(), default=datetime.utcnow),
        sa.Column('updated_at', sa.DateTime(), default=datetime.utcnow, onupdate=datetime.utcnow),
        sa.PrimaryKeyConstraint('id'),
        sa.ForeignKeyConstraint(['user_id'], ['user.id']),
        sa.ForeignKeyConstraint(['account_id'], ['account.id'])
    )
    
    # 3. نسخ البيانات مرتبة حسب تاريخ الإنشاء
    op.execute('''
        INSERT INTO transaction_temp 
        SELECT * FROM transaction 
        ORDER BY created_at;
    ''')
    
    # 4. حذف الجدول القديم
    op.drop_table('transaction')
    
    # 5. إعادة تسمية الجدول المؤقت
    op.rename_table('transaction_temp', 'transaction')
    
    # 6. إعادة ترقيم server_id
    op.execute('''
        UPDATE transaction 
        SET server_id = (
            SELECT COUNT(*) 
            FROM transaction t2 
            WHERE t2.created_at <= transaction.created_at
        );
    ''')
    
    # 7. إعادة تفعيل المفاتيح الخارجية
    op.execute('PRAGMA foreign_keys=ON;')

def downgrade():
    # 1. تعطيل المفاتيح الخارجية مؤقتاً
    op.execute('PRAGMA foreign_keys=OFF;')
    
    # 2. إنشاء جدول مؤقت بالهيكل القديم
    op.create_table(
        'transaction_temp',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('server_id', sa.Integer(), nullable=True),
        sa.Column('date', sa.DateTime(), nullable=False),
        sa.Column('amount', sa.Float(), nullable=False),
        sa.Column('description', sa.String(255), nullable=False),
        sa.Column('type', sa.String(50), default='debit'),
        sa.Column('currency', sa.String(50), default='يمني'),
        sa.Column('notes', sa.Text()),
        sa.Column('whatsapp_enabled', sa.Boolean(), default=True),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('account_id', sa.Integer(), nullable=False),
        sa.Column('created_at', sa.DateTime(), default=datetime.utcnow),
        sa.Column('updated_at', sa.DateTime(), default=datetime.utcnow, onupdate=datetime.utcnow),
        sa.PrimaryKeyConstraint('id'),
        sa.ForeignKeyConstraint(['user_id'], ['user.id']),
        sa.ForeignKeyConstraint(['account_id'], ['account.id'])
    )
    
    # 3. نسخ البيانات
    op.execute('''
        INSERT INTO transaction_temp 
        SELECT * FROM transaction;
    ''')
    
    # 4. حذف الجدول الجديد
    op.drop_table('transaction')
    
    # 5. إعادة تسمية الجدول المؤقت
    op.rename_table('transaction_temp', 'transaction')
    
    # 6. إعادة تفعيل المفاتيح الخارجية
    op.execute('PRAGMA foreign_keys=ON;') 