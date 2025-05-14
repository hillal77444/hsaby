"""add whatsapp_enabled column to transactions

Revision ID: add_whatsapp_enabled_to_transactions
Revises: add_whatsapp_enabled
Create Date: 2024-03-19

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = 'add_whatsapp_enabled_to_transactions'
down_revision = 'add_whatsapp_enabled'
branch_labels = None
depends_on = None

def upgrade():
    # إضافة عمود whatsapp_enabled إلى جدول transactions
    op.add_column('transaction', sa.Column('whatsapp_enabled', sa.Boolean(), nullable=False, server_default='true'))

def downgrade():
    # حذف عمود whatsapp_enabled من جدول transactions
    op.drop_column('transaction', 'whatsapp_enabled') 