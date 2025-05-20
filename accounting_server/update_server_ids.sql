-- تحديث server_id في جدول Account
WITH numbered_accounts AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) as new_server_id
    FROM account
)
UPDATE account
SET server_id = numbered_accounts.new_server_id
FROM numbered_accounts
WHERE account.id = numbered_accounts.id;

-- تحديث server_id في جدول Transaction
WITH numbered_transactions AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) as new_server_id
    FROM transaction
)
UPDATE transaction
SET server_id = numbered_transactions.new_server_id
FROM numbered_transactions
WHERE transaction.id = numbered_transactions.id; 