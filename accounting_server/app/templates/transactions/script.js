function saveTransaction() {
    const transactionData = {
        account_id: document.getElementById('accountDropdown').value,
        type: document.getElementById('transactionType').value,
        amount: parseFloat(document.getElementById('amount').value) || 0,
        description: document.getElementById('description').value,
        currency: document.getElementById('currency').value,
        notes: document.getElementById('notes').value
    };

    if (!transactionData.account_id) {
        Android.showToast('يرجى اختيار الحساب');
        return;
    }

    if (!transactionData.amount || transactionData.amount <= 0) {
        Android.showToast('يرجى إدخال مبلغ صحيح');
        return;
    }

    if (!transactionData.description) {
        Android.showToast('يرجى إدخال وصف للمعاملة');
        return;
    }

    Android.saveTransaction(JSON.stringify(transactionData));
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
                <div class="transaction-actions">
                    <button onclick="editTransaction(${transaction.id})">تعديل</button>
                    <button onclick="deleteTransaction(${transaction.id})">حذف</button>
                </div>
            </div>
        `;
    });
    
    html += '</div>';
    container.innerHTML = html;
}

function editTransaction(transactionId) {
    Android.editTransaction(transactionId);
}

function deleteTransaction(transactionId) {
    if (confirm('هل أنت متأكد من حذف هذه المعاملة؟')) {
        Android.deleteTransaction(transactionId);
    }
}

function onAccountSelected() {
    const accountId = document.getElementById('accountDropdown').value;
    if (accountId) {
        Android.onAccountSelected(accountId);
    }
} 