"""add whatsapp notifications

Revision ID: 5f9c8271a2b3
Revises: 4e8b763ee9fe
Create Date: 2024-03-19 11:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '5f9c8271a2b3'
down_revision = '4e8b763ee9fe'
branch_labels = None
depends_on = None

def upgrade():
    with op.batch_alter_table('user') as batch_op:
        batch_op.add_column(sa.Column('whatsapp_notifications', sa.Boolean(), nullable=False, server_default='1'))

def downgrade():
    with op.batch_alter_table('user') as batch_op:
        batch_op.drop_column('whatsapp_notifications') 