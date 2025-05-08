"""add is_debtor to account and type currency notes to transaction

Revision ID: 3e7a652dd8ed
Revises: 5e78fb52f695
Create Date: 2024-03-19 10:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from datetime import datetime

# revision identifiers, used by Alembic.
revision = '3e7a652dd8ed'
down_revision = '5e78fb52f695'
branch_labels = None
depends_on = None

def upgrade():
    # Add columns to account table
    with op.batch_alter_table('account', schema=None) as batch_op:
        batch_op.add_column(sa.Column('is_debtor', sa.Boolean(), nullable=True, server_default='0'))
        batch_op.add_column(sa.Column('created_at', sa.DateTime(), nullable=True, server_default=sa.text('CURRENT_TIMESTAMP')))
        batch_op.add_column(sa.Column('updated_at', sa.DateTime(), nullable=True, server_default=sa.text('CURRENT_TIMESTAMP')))

    # Add columns to transaction table
    with op.batch_alter_table('transaction', schema=None) as batch_op:
        batch_op.add_column(sa.Column('type', sa.String(20), nullable=True, server_default='debit'))
        batch_op.add_column(sa.Column('currency', sa.String(20), nullable=True, server_default='ريال يمني'))
        batch_op.add_column(sa.Column('notes', sa.Text(), nullable=True, server_default=''))
        batch_op.add_column(sa.Column('created_at', sa.DateTime(), nullable=True, server_default=sa.text('CURRENT_TIMESTAMP')))
        batch_op.add_column(sa.Column('updated_at', sa.DateTime(), nullable=True, server_default=sa.text('CURRENT_TIMESTAMP')))

    # Update existing records with current timestamp
    op.execute("UPDATE account SET created_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE created_at IS NULL")
    op.execute("UPDATE transaction SET created_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE created_at IS NULL")

    # Make columns NOT NULL after setting values
    with op.batch_alter_table('account', schema=None) as batch_op:
        batch_op.alter_column('created_at', nullable=False)
        batch_op.alter_column('updated_at', nullable=False)

    with op.batch_alter_table('transaction', schema=None) as batch_op:
        batch_op.alter_column('created_at', nullable=False)
        batch_op.alter_column('updated_at', nullable=False)

def downgrade():
    # Remove columns from transaction table
    with op.batch_alter_table('transaction', schema=None) as batch_op:
        batch_op.drop_column('updated_at')
        batch_op.drop_column('created_at')
        batch_op.drop_column('notes')
        batch_op.drop_column('currency')
        batch_op.drop_column('type')

    # Remove columns from account table
    with op.batch_alter_table('account', schema=None) as batch_op:
        batch_op.drop_column('updated_at')
        batch_op.drop_column('created_at')
        batch_op.drop_column('is_debtor') 