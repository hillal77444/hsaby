function refreshContent() {
    Android.refreshContent();
}

function updateContent(newHtml) {
    const parser = new DOMParser();
    const newDoc = parser.parseFromString(newHtml, 'text/html');
    const newContent = newDoc.querySelector('.card').innerHTML;
    document.querySelector('.card').innerHTML = newContent;
}

function updateProfile() {
    const username = document.getElementById('username').value;
    const phone = document.getElementById('phone').value;

    if (!username || !phone) {
        Android.showToast('يرجى ملء جميع الحقول المطلوبة');
        return;
    }

    const profileData = {
        username: username,
        phone: phone
    };

    Android.updateProfile(JSON.stringify(profileData));
}

function changePassword() {
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (!currentPassword || !newPassword || !confirmPassword) {
        Android.showToast('يرجى ملء جميع الحقول المطلوبة');
        return;
    }

    if (newPassword !== confirmPassword) {
        Android.showToast('كلمة المرور الجديدة غير متطابقة');
        return;
    }

    const passwordData = {
        current_password: currentPassword,
        new_password: newPassword
    };

    Android.changePassword(JSON.stringify(passwordData));
}

function saveAppSettings() {
    const darkMode = document.getElementById('darkMode').checked;
    const notifications = document.getElementById('notifications').checked;
    const syncInterval = document.getElementById('syncInterval').value;

    const settingsData = {
        dark_mode: darkMode,
        notifications: notifications,
        sync_interval: parseInt(syncInterval)
    };

    Android.saveAppSettings(JSON.stringify(settingsData));
}

function clearCache() {
    if (confirm('هل أنت متأكد من رغبتك في مسح الذاكرة المؤقتة؟')) {
        Android.clearCache();
    }
}

function deleteAccount() {
    if (confirm('هل أنت متأكد من رغبتك في حذف الحساب؟ هذا الإجراء لا يمكن التراجع عنه.')) {
        Android.deleteAccount();
    }
}

// تحميل الإعدادات الحالية عند فتح الصفحة
function loadSettings(settings) {
    const data = JSON.parse(settings);
    
    // تحميل بيانات الملف الشخصي
    document.getElementById('username').value = data.username || '';
    document.getElementById('phone').value = data.phone || '';
    
    // تحميل إعدادات التطبيق
    document.getElementById('darkMode').checked = data.dark_mode || false;
    document.getElementById('notifications').checked = data.notifications || false;
    document.getElementById('syncInterval').value = data.sync_interval || '15';
}

// تحديث واجهة المستخدم عند تغيير الوضع الليلي
function updateTheme(isDarkMode) {
    document.body.classList.toggle('dark-mode', isDarkMode);
} 