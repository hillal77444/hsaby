function saveAccount() {
    const accountData = {
        account_name: document.getElementById('accountName').value,
        account_number: document.getElementById('accountNumber').value,
        phone_number: document.getElementById('phoneNumber').value,
        balance: parseFloat(document.getElementById('initialBalance').value) || 0,
        is_debtor: document.getElementById('isDebtor').checked
    };

    if (!accountData.account_name || !accountData.account_number) {
        Android.showToast('يرجى إدخال اسم الحساب ورقمه');
        return;
    }

    Android.saveAccount(JSON.stringify(accountData));
}

function refreshContent() {
    Android.refreshContent();
}

function updateContent(newHtml) {
    const parser = new DOMParser();
    const newDoc = parser.parseFromString(newHtml, 'text/html');
    const newContent = newDoc.querySelector('.card').innerHTML;
    document.querySelector('.card').innerHTML = newContent;
}

function updateAccountsList(accounts) {
    const container = document.getElementById('accountsList');
    let html = '<div class="accounts-grid">';
    
    accounts.forEach(account => {
        html += `
            <div class="account-card">
                <h3>${account.account_name}</h3>
                <p>رقم الحساب: ${account.account_number}</p>
                <p>الرصيد: ${account.balance}</p>
                <p>نوع الحساب: ${account.is_debtor ? 'مدين' : 'دائن'}</p>
                <div class="account-actions">
                    <button onclick="editAccount(${account.id})">تعديل</button>
                    <button onclick="deleteAccount(${account.id})">حذف</button>
                </div>
            </div>
        `;
    });
    
    html += '</div>';
    container.innerHTML = html;
}

function editAccount(accountId) {
    Android.editAccount(accountId);
}

function deleteAccount(accountId) {
    if (confirm('هل أنت متأكد من حذف هذا الحساب؟')) {
        Android.deleteAccount(accountId);
    }
} 