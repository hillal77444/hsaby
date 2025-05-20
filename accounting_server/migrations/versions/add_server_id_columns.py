"""add server_id columns

Revision ID: 4e8b763ee9fe
Revises: 4fa48bad0f9c
Create Date: 2024-03-19 10:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from alembic.operations import ops

# revision identifiers, used by Alembic.
revision = '4e8b763ee9fe'
down_revision = '4fa48bad0f9c'  # ربط مع الترحيل الأولي
branch_labels = None
depends_on = None

def upgrade():
    # إضافة عمود server_id إلى جدول account
    with op.batch_alter_table('account') as batch_op:
        batch_op.add_column(sa.Column('server_id', sa.Integer(), nullable=True))
        batch_op.create_unique_constraint('uq_account_server_id', ['server_id'])
    
    # إضافة عمود server_id إلى جدول transaction
    with op.batch_alter_table('transaction') as batch_op:
        batch_op.add_column(sa.Column('server_id', sa.Integer(), nullable=True))
        batch_op.create_unique_constraint('uq_transaction_server_id', ['server_id'])

def downgrade():
    # حذف عمود server_id من جدول transaction
    with op.batch_alter_table('transaction') as batch_op:
        batch_op.drop_constraint('uq_transaction_server_id', type_='unique')
        batch_op.drop_column('server_id')
    
    # حذف عمود server_id من جدول account
    with op.batch_alter_table('account') as batch_op:
        batch_op.drop_constraint('uq_account_server_id', type_='unique')
        batch_op.drop_column('server_id') 