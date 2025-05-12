function onAccountSelected() {
    const accountId = document.getElementById('accountDropdown').value;
    if (accountId) {
        Android.onAccountSelected(accountId);
    }
}

function onDateChanged() {
    Android.onDateChanged();
}

function showReport() {
    Android.showReport();
}

function updateAccounts(accounts) {
    const dropdown = document.getElementById('accountDropdown');
    dropdown.innerHTML = '<option value="">-- اختر الحساب --</option>' + accounts;
}

function updateDates(startDate, endDate) {
    document.getElementById('startDateInput').value = startDate;
    document.getElementById('endDateInput').value = endDate;
}

function updateReport(html) {
    document.getElementById('reportContainer').innerHTML = html;
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