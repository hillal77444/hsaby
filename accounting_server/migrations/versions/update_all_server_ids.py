"""update all server_ids

Revision ID: update_all_server_ids
Revises: 5f9c8271a2b3
Create Date: 2024-03-20

"""
from alembic import op
import sqlalchemy as sa
from datetime import datetime

# revision identifiers, used by Alembic
revision = 'update_all_server_ids'
down_revision = '5f9c8271a2b3'
branch_labels = None
depends_on = None

def upgrade():
    # 1. تعطيل المفاتيح الخارجية مؤقتاً
    op.execute('PRAGMA foreign_keys=OFF;')
    
    # 2. تحديث server_id في جدول account
    op.execute('''
        UPDATE account 
        SET server_id = (
            SELECT COUNT(*) 
            FROM account a2 
            WHERE a2.created_at <= account.created_at
        )
        WHERE server_id IS NULL OR server_id = id;
    ''')
    
    # 3. تحديث server_id في جدول transaction
    op.execute('''
        UPDATE "transaction" 
        SET server_id = (
            SELECT COUNT(*) 
            FROM "transaction" t2 
            WHERE t2.created_at <= "transaction".created_at
        )
        WHERE server_id IS NULL OR server_id = id;
    ''')
    
    # 4. إعادة تفعيل المفاتيح الخارجية
    op.execute('PRAGMA foreign_keys=ON;')

def downgrade():
    # لا يوجد تراجع لهذه العملية لأنها تحديث للبيانات فقط
    pass 