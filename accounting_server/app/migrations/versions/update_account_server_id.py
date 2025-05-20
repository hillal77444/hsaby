"""update account server_id

Revision ID: update_account_server_id
Revises: update_transaction_server_id
Create Date: 2025-05-19

"""
from alembic import op
import sqlalchemy as sa
from datetime import datetime

# revision identifiers, used by Alembic
revision = 'update_account_server_id'
down_revision = 'update_transaction_server_id'
branch_labels = None
depends_on = None

def upgrade():
    # 1. تعطيل المفاتيح الخارجية مؤقتاً
    op.execute('PRAGMA foreign_keys=OFF;')
    
    # 2. إنشاء جدول مؤقت
    op.create_table(
        'account_temp',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('server_id', sa.Integer(), nullable=True),
        sa.Column('account_number', sa.String(50), nullable=False),
        sa.Column('account_name', sa.String(100), nullable=False),
        sa.Column('balance', sa.Float(), default=0.0),
        sa.Column('is_debtor', sa.Boolean(), default=False),
        sa.Column('phone_number', sa.String(20)),
        sa.Column('notes', sa.Text()),
        sa.Column('whatsapp_enabled', sa.Boolean(), default=True),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('created_at', sa.DateTime(), default=datetime.utcnow),
        sa.Column('updated_at', sa.DateTime(), default=datetime.utcnow, onupdate=datetime.utcnow),
        sa.PrimaryKeyConstraint('id'),
        sa.ForeignKeyConstraint(['user_id'], ['user.id'])
    )
    
    # 3. نسخ البيانات مرتبة حسب تاريخ الإنشاء
    op.execute('''
        INSERT INTO account_temp 
        SELECT * FROM account 
        ORDER BY created_at;
    ''')
    
    # 4. حذف الجدول القديم
    op.drop_table('account')
    
    # 5. إعادة تسمية الجدول المؤقت
    op.rename_table('account_temp', 'account')
    
    # 6. إعادة ترقيم server_id
    op.execute('''
        UPDATE account 
        SET server_id = (
            SELECT COUNT(*) 
            FROM account a2 
            WHERE a2.created_at <= account.created_at
        );
    ''')
    
    # 7. إعادة تفعيل المفاتيح الخارجية
    op.execute('PRAGMA foreign_keys=ON;')

def downgrade():
    # 1. تعطيل المفاتيح الخارجية مؤقتاً
    op.execute('PRAGMA foreign_keys=OFF;')
    
    # 2. إنشاء جدول مؤقت بالهيكل القديم
    op.create_table(
        'account_temp',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('server_id', sa.Integer(), nullable=True),
        sa.Column('account_number', sa.String(50), nullable=False),
        sa.Column('account_name', sa.String(100), nullable=False),
        sa.Column('balance', sa.Float(), default=0.0),
        sa.Column('is_debtor', sa.Boolean(), default=False),
        sa.Column('phone_number', sa.String(20)),
        sa.Column('notes', sa.Text()),
        sa.Column('whatsapp_enabled', sa.Boolean(), default=True),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('created_at', sa.DateTime(), default=datetime.utcnow),
        sa.Column('updated_at', sa.DateTime(), default=datetime.utcnow, onupdate=datetime.utcnow),
        sa.PrimaryKeyConstraint('id'),
        sa.ForeignKeyConstraint(['user_id'], ['user.id'])
    )
    
    # 3. نسخ البيانات
    op.execute('''
        INSERT INTO account_temp 
        SELECT * FROM account;
    ''')
    
    # 4. حذف الجدول الجديد
    op.drop_table('account')
    
    # 5. إعادة تسمية الجدول المؤقت
    op.rename_table('account_temp', 'account')
    
    # 6. إعادة تفعيل المفاتيح الخارجية
    op.execute('PRAGMA foreign_keys=ON;') 