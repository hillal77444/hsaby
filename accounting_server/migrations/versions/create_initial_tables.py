"""create initial tables

Revision ID: create_initial_tables
Revises: 
Create Date: 2025-05-20

"""
from alembic import op
import sqlalchemy as sa
from datetime import datetime

# revision identifiers, used by Alembic.
revision = 'create_initial_tables'
down_revision = None
branch_labels = None
depends_on = None

def upgrade():
    # إنشاء جدول المستخدمين
    op.create_table('user',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('username', sa.String(80), nullable=False),
        sa.Column('phone', sa.String(20), nullable=False),
        sa.Column('password_hash', sa.String(128), nullable=False),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('username'),
        sa.UniqueConstraint('phone')
    )

    # إنشاء جدول الحسابات
    op.create_table('accounts',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('server_id', sa.Integer(), nullable=False),
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
        sa.ForeignKeyConstraint(['user_id'], ['user.id'], ),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('server_id')
    )

    # إنشاء جدول المعاملات
    op.create_table('transactions',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('server_id', sa.Integer(), nullable=False),
        sa.Column('date', sa.DateTime(), nullable=False),
        sa.Column('amount', sa.Float(), nullable=False),
        sa.Column('description', sa.String(255), nullable=False),
        sa.Column('type', sa.String(50), default='debit'),
        sa.Column('currency', sa.String(50), default='ريال يمني'),
        sa.Column('notes', sa.Text()),
        sa.Column('whatsapp_enabled', sa.Boolean(), default=True),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('account_id', sa.Integer(), nullable=False),
        sa.Column('created_at', sa.DateTime(), default=datetime.utcnow),
        sa.Column('updated_at', sa.DateTime(), default=datetime.utcnow, onupdate=datetime.utcnow),
        sa.ForeignKeyConstraint(['user_id'], ['user.id'], ),
        sa.ForeignKeyConstraint(['account_id'], ['accounts.id'], ),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('server_id')
    )

def downgrade():
    op.drop_table('transactions')
    op.drop_table('accounts')
    op.drop_table('user') 