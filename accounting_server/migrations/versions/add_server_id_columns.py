"""add server_id columns

Revision ID: 4e8b763ee9fe
Revises: 4fa48bad0f9c
Create Date: 2024-03-19 10:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '4e8b763ee9fe'
down_revision = '4fa48bad0f9c'  # ربط مع الترحيل الأولي
branch_labels = None
depends_on = None

def upgrade():
    # إضافة عمود server_id إلى جدول account
    op.add_column('account', sa.Column('server_id', sa.Integer(), nullable=True))
    op.create_unique_constraint('uq_account_server_id', 'account', ['server_id'])
    
    # إضافة عمود server_id إلى جدول transaction
    op.add_column('transaction', sa.Column('server_id', sa.Integer(), nullable=True))
    op.create_unique_constraint('uq_transaction_server_id', 'transaction', ['server_id'])

def downgrade():
    # حذف عمود server_id من جدول transaction
    op.drop_constraint('uq_transaction_server_id', 'transaction', type_='unique')
    op.drop_column('transaction', 'server_id')
    
    # حذف عمود server_id من جدول account
    op.drop_constraint('uq_account_server_id', 'account', type_='unique')
    op.drop_column('account', 'server_id') 