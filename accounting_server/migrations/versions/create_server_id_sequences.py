"""create server_id sequences for accounts and transactions

Revision ID: create_server_id_sequences
Revises: create_initial_tables
Create Date: 2025-05-20

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = 'create_server_id_sequences'
down_revision = 'create_initial_tables'
branch_labels = None
depends_on = None

def upgrade():
    # تحديث القيم الموجودة في الحسابات
    op.execute('''
        UPDATE accounts 
        SET server_id = (
            SELECT COALESCE(MAX(server_id), 0) + 1 
            FROM accounts AS a2 
            WHERE a2.id <= accounts.id
        )
        WHERE server_id IS NULL
    ''')
    
    # تحديث القيم الموجودة في المعاملات
    op.execute('''
        UPDATE transactions 
        SET server_id = (
            SELECT COALESCE(MAX(server_id), 0) + 1 
            FROM transactions AS t2 
            WHERE t2.id <= transactions.id
        )
        WHERE server_id IS NULL
    ''')

def downgrade():
    # لا يوجد حاجة للتراجع عن التغييرات
    pass 