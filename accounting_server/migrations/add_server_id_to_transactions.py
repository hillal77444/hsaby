"""add server_id to transactions

Revision ID: add_server_id_to_transactions
"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic
revision = 'add_server_id_to_transactions'
down_revision = None
branch_labels = None
depends_on = None

def upgrade():
    # إضافة عمود server_id
    op.add_column('transaction', sa.Column('server_id', sa.Integer(), nullable=True))
    
    # تحديث القيم الموجودة
    op.execute("UPDATE transaction SET server_id = id")
    
    # جعل العمود غير قابل للقيمة الفارغة
    op.alter_column('transaction', 'server_id',
                    existing_type=sa.Integer(),
                    nullable=False)
    
    # إضافة فهرس فريد
    op.create_unique_constraint('uq_transaction_server_id', 'transaction', ['server_id'])

def downgrade():
    # حذف الفهرس
    op.drop_constraint('uq_transaction_server_id', 'transaction', type_='unique')
    
    # حذف العمود
    op.drop_column('transaction', 'server_id') 