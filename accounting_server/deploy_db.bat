@echo off
set SERVER=212.224.88.122
set USER=root
set PASS=Hillal774447251

echo Creating directories...
echo mkdir -p /root/accounting_server/data > commands.txt
echo chown -R postgres:postgres /root/accounting_server/data >> commands.txt
echo sed -i 's|data_directory = .*|data_directory = \'/root/accounting_server/data\'|' /etc/postgresql/*/main/postgresql.conf >> commands.txt
echo systemctl restart postgresql >> commands.txt
echo sleep 5 >> commands.txt
echo sudo -u postgres psql -c "CREATE DATABASE accounting_db;" >> commands.txt
echo sudo -u postgres psql -c "CREATE USER accounting_user WITH PASSWORD 'Accounting@123';" >> commands.txt
echo sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE accounting_db TO accounting_user;" >> commands.txt

echo Deploying database...
plink -ssh %USER%@%SERVER% -pw %PASS% < commands.txt

if %ERRORLEVEL% EQU 0 (
    echo Database deployment successful!
) else (
    echo Database deployment failed!
)

del commands.txt 