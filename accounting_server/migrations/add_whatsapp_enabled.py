"""add whatsapp_enabled column

Revision ID: add_whatsapp_enabled
Revises: 
Create Date: 2024-03-19

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = 'add_whatsapp_enabled'
down_revision = None
branch_labels = None
depends_on = None

def upgrade():
    # إضافة عمود whatsapp_enabled إلى جدول account
    op.add_column('account', sa.Column('whatsapp_enabled', sa.Boolean(), nullable=False, server_default='true'))

def downgrade():
    # حذف عمود whatsapp_enabled من جدول account
    op.drop_column('account', 'whatsapp_enabled') 