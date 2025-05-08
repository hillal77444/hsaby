# معلومات الاتصال
$server = "root@212.224.88.122"
$password = "Hillal774447251"

# نسخ الملفات إلى السيرفر
Write-Host "📤 نسخ الملفات إلى السيرفر..."
$securePassword = ConvertTo-SecureString $password -AsPlainText -Force
$credential = New-Object System.Management.Automation.PSCredential("root", $securePassword)

try {
    # نسخ الملفات
    Copy-Item -Path "config.py" -Destination "/root/accounting_server/" -ToSession (New-PSSession -HostName $server -Credential $credential)
    Copy-Item -Path "migrate_db.py" -Destination "/root/accounting_server/" -ToSession (New-PSSession -HostName $server -Credential $credential)

    # تنفيذ سكريبت النقل
    Write-Host "🚀 تنفيذ سكريبت نقل قاعدة البيانات..."
    Invoke-Command -HostName $server -Credential $credential -ScriptBlock {
        cd /root/accounting_server
        python3 migrate_db.py
    }

    Write-Host "✅ تم نقل قاعدة البيانات بنجاح!"
} catch {
    Write-Host "❌ فشل نقل قاعدة البيانات: $_"
    exit 1
} 