from app import create_app, db
from sqlalchemy import text

def update_server_ids():
    app = create_app()
    with app.app_context():
        try:
            # تحديث جدول Account
            account_update = """
            WITH numbered_accounts AS (
                SELECT id, ROW_NUMBER() OVER (ORDER BY id) as new_server_id
                FROM account
            )
            UPDATE account
            SET server_id = numbered_accounts.new_server_id
            FROM numbered_accounts
            WHERE account.id = numbered_accounts.id
            """
            
            # تحديث جدول Transaction
            transaction_update = """
            WITH numbered_transactions AS (
                SELECT id, ROW_NUMBER() OVER (ORDER BY id) as new_server_id
                FROM "transaction"
            )
            UPDATE "transaction"
            SET server_id = numbered_transactions.new_server_id
            FROM numbered_transactions
            WHERE "transaction".id = numbered_transactions.id
            """
            
            # تنفيذ تحديث جدول Account
            print("جاري تحديث جدول Account...")
            db.session.execute(text(account_update))
            db.session.commit()
            print("تم تحديث جدول Account بنجاح!")
            
            # تنفيذ تحديث جدول Transaction
            print("جاري تحديث جدول Transaction...")
            db.session.execute(text(transaction_update))
            db.session.commit()
            print("تم تحديث جدول Transaction بنجاح!")
            
            print("تم تحديث جميع server_ids بنجاح!")
            
        except Exception as e:
            print(f"حدث خطأ أثناء التحديث: {str(e)}")
            try:
                db.session.rollback()
            except:
                pass

if __name__ == '__main__':
    update_server_ids() 