let currentReportType = 'daily';

function showReport(type) {
    currentReportType = type;
    const buttons = document.querySelectorAll('.report-type-btn');
    buttons.forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');
    
    // تحديث التواريخ حسب نوع التقرير
    const today = new Date();
    const startDate = new Date();
    
    switch(type) {
        case 'daily':
            startDate.setDate(today.getDate() - 7); // آخر 7 أيام
            break;
        case 'monthly':
            startDate.setMonth(today.getMonth() - 1); // آخر شهر
            break;
        case 'yearly':
            startDate.setFullYear(today.getFullYear() - 1); // آخر سنة
            break;
    }
    
    document.getElementById('startDateInput').value = startDate.toISOString().split('T')[0];
    document.getElementById('endDateInput').value = today.toISOString().split('T')[0];
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
    const dropdown = document.getElementById('accountDropdown');
    dropdown.innerHTML = '<option value="">-- اختر الحساب --</option>';
    
    accounts.forEach(account => {
        const option = document.createElement('option');
        option.value = account.id;
        option.textContent = `${account.account_name} (${account.account_number})`;
        dropdown.appendChild(option);
    });
}

function onAccountSelected() {
    const accountId = document.getElementById('accountDropdown').value;
    if (accountId) {
        Android.onAccountSelected(accountId);
    }
}

function onDateChanged() {
    Android.onDateChanged();
}

function onCurrencyChanged() {
    const currency = document.getElementById('currencyFilter').value;
    Android.onCurrencyChanged(currency);
}

function generateReport() {
    const reportData = {
        type: currentReportType,
        account_id: document.getElementById('accountDropdown').value,
        start_date: document.getElementById('startDateInput').value,
        end_date: document.getElementById('endDateInput').value,
        currency: document.getElementById('currencyFilter').value
    };

    if (!reportData.account_id) {
        Android.showToast('يرجى اختيار الحساب');
        return;
    }

    if (!reportData.start_date || !reportData.end_date) {
        Android.showToast('يرجى تحديد الفترة الزمنية');
        return;
    }

    Android.generateReport(JSON.stringify(reportData));
}

function updateReportSummary(summary) {
    const totalDebit = document.querySelector('.total-debit .amount');
    const totalCredit = document.querySelector('.total-credit .amount');
    const balance = document.querySelector('.balance .amount');

    totalDebit.textContent = `${summary.total_debit} ${summary.currency}`;
    totalCredit.textContent = `${summary.total_credit} ${summary.currency}`;
    balance.textContent = `${summary.balance} ${summary.currency}`;
}

function updateTransactionsList(transactions) {
    const container = document.getElementById('transactionsList');
    let html = '<div class="transactions-grid">';
    
    transactions.forEach(transaction => {
        const date = new Date(transaction.date);
        const formattedDate = date.toLocaleDateString('ar-SA');
        
        html += `
            <div class="transaction-card ${transaction.type}">
                <div class="transaction-header">
                    <h3>${transaction.description}</h3>
                    <span class="transaction-date">${formattedDate}</span>
                </div>
                <div class="transaction-details">
                    <p class="amount">${transaction.amount} ${transaction.currency}</p>
                    <p class="type">${transaction.type === 'debit' ? 'مدين' : 'دائن'}</p>
                </div>
                ${transaction.notes ? `<p class="notes">${transaction.notes}</p>` : ''}
            </div>
        `;
    });
    
    html += '</div>';
    container.innerHTML = html;
} 