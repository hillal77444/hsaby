# هيكل تطبيق المحاسبة

```
📱 تطبيق المحاسبة
├── 📂 UI Layer (طبقة واجهة المستخدم)
│   ├── Activities
│   │   ├── SplashActivity (شاشة البداية)
│   │   ├── LoginActivity (تسجيل الدخول)
│   │   └── MainActivity (الشاشة الرئيسية)
│   └── Fragments
│       └── NotificationsFragment (الإشعارات)
│
├── 📂 Data Layer (طبقة البيانات)
│   ├── Remote
│   │   ├── ApiService (خدمة API)
│   │   └── Models
│   │       ├── User (نموذج المستخدم)
│   │       ├── Account (نموذج الحساب)
│   │       └── Transaction (نموذج المعاملة)
│   └── Local
│       ├── PreferencesManager (إدارة التفضيلات)
│       └── Database
│           └── NotificationDao (إدارة الإشعارات)
│
└── 📂 Sync Layer (طبقة المزامنة)
    └── SyncManager (مدير المزامنة)
```

## المكونات الجاهزة

### 1. واجهة المستخدم (UI)
#### 1.1 شاشة تسجيل الدخول (`LoginActivity`)
- **الموقع**: `android_app/app/src/main/java/com/hsaby/accounting/ui/login/LoginActivity.kt`
- **الوظائف**:
  - عرض نموذج تسجيل الدخول
  - التحقق من صحة رقم الهاتف وكلمة المرور
  - إرسال طلب تسجيل الدخول للخادم
  - حفظ بيانات تسجيل الدخول
  - الانتقال للشاشة الرئيسية بعد نجاح تسجيل الدخول

#### 1.2 شاشة الإشعارات (`NotificationsFragment`)
- **الموقع**: `android_app/app/src/main/java/com/hsaby/accounting/ui/notifications/NotificationsFragment.kt`
- **الوظائف**:
  - عرض قائمة الإشعارات في RecyclerView
  - تمييز الإشعارات المقروءة وغير المقروءة
  - تحديث تلقائي للإشعارات الجديدة
  - معالجة النقر على الإشعارات

### 2. إدارة البيانات (Data Management)
#### 2.1 إدارة التفضيلات (`PreferencesManager`)
- **الموقع**: `android_app/app/src/main/java/com/hsaby/accounting/data/local/PreferencesManager.kt`
- **الوظائف**:
  - حفظ بيانات تسجيل الدخول (رقم الهاتف، التوكن)
  - إدارة حالة تسجيل الدخول
  - حفظ معرف المستخدم
  - التحقق من حالة تسجيل الدخول
  - حذف بيانات تسجيل الدخول

#### 2.2 إدارة الإشعارات (`NotificationManager`)
- **الموقع**: `android_app/app/src/main/java/com/hsaby/accounting/notifications/NotificationManager.kt`
- **الوظائف**:
  - إنشاء قنوات الإشعارات
  - إرسال إشعارات المزامنة
  - إرسال إشعارات المعاملات
  - إرسال إشعارات الحسابات
  - إدارة قاعدة بيانات الإشعارات المحلية

### 3. نماذج البيانات (Data Models)
#### 3.1 نموذج المستخدم (`User`)
- **الموقع**: `android_app/app/src/main/java/com/hsaby/accounting/data/remote/model/User.kt`
- **الحقول**:
  - `id`: معرف المستخدم
  - `phone`: رقم الهاتف
  - `name`: اسم المستخدم
  - `createdAt`: تاريخ الإنشاء
  - `updatedAt`: تاريخ التحديث

#### 3.2 نموذج الحساب (`Account`)
- **الموقع**: `android_app/app/src/main/java/com/hsaby/accounting/data/remote/model/Account.kt`
- **الحقول**:
  - `id`: معرف الحساب المحلي
  - `serverId`: معرف الحساب على الخادم
  - `accountName`: اسم الحساب
  - `balance`: الرصيد
  - `phoneNumber`: رقم الهاتف
  - `notes`: الملاحظات
  - `isDebtor`: حالة المدين
  - `whatsappEnabled`: تفعيل واتساب
  - `userId`: معرف المستخدم
  - `createdAt`: تاريخ الإنشاء
  - `updatedAt`: تاريخ التحديث

#### 3.3 نموذج المعاملة (`Transaction`)
- **الموقع**: `android_app/app/src/main/java/com/hsaby/accounting/data/remote/model/Transaction.kt`
- **الحقول**:
  - `id`: معرف المعاملة المحلي
  - `serverId`: معرف المعاملة على الخادم
  - `amount`: المبلغ
  - `type`: نوع المعاملة (دخل/مصروف)
  - `description`: الوصف
  - `notes`: الملاحظات
  - `date`: التاريخ
  - `currency`: العملة
  - `whatsappEnabled`: تفعيل واتساب
  - `accountId`: معرف الحساب
  - `userId`: معرف المستخدم
  - `createdAt`: تاريخ الإنشاء
  - `updatedAt`: تاريخ التحديث

### 4. واجهة برمجة التطبيقات (API)
#### 4.1 خدمة API (`ApiService`)
- **الموقع**: `android_app/app/src/main/java/com/hsaby/accounting/data/remote/ApiService.kt`
- **الوظائف**:
  - `login`: تسجيل الدخول
  - `register`: التسجيل
  - `getAccounts`: جلب الحسابات
  - `getTransactions`: جلب المعاملات
  - `syncData`: مزامنة البيانات
  - `syncChanges`: تحديث البيانات
  - `refreshToken`: تحديث التوكن
  - `deleteTransaction`: حذف المعاملات

### 5. قاعدة البيانات المحلية (Local Database)
#### 5.1 كائنات الوصول للبيانات (DAO)
##### 5.1.1 NotificationDao
- **الموقع**: `android_app/app/src/main/java/com/hsaby/accounting/data/local/dao/NotificationDao.kt`
- **الوظائف**:
  - `getAllNotifications`: جلب جميع الإشعارات
  - `getUnreadNotifications`: جلب الإشعارات غير المقروءة
  - `getUnreadCount`: عدد الإشعارات غير المقروءة
  - `insertNotification`: إدخال إشعار جديد
  - `insertNotifications`: إدخال عدة إشعارات
  - `updateNotification`: تحديث إشعار
  - `markAsRead`: تمييز إشعار كمقروء
  - `markAllAsRead`: تمييز جميع الإشعارات كمقروءة
  - `deleteNotification`: حذف إشعار
  - `deleteOldNotifications`: حذف الإشعارات القديمة

### 6. المزامنة (Sync)
#### 6.1 مدير المزامنة (`SyncManager`)
- **الموقع**: `android_app/app/src/main/java/com/hsaby/accounting/sync/SyncManager.kt`
- **الوظائف**:
  - `syncAccounts`: مزامنة الحسابات
  - `syncTransactions`: مزامنة المعاملات
  - `isNetworkAvailable`: التحقق من توفر الإنترنت
  - `manualSync`: مزامنة يدوية
  - `syncNewData`: مزامنة البيانات الجديدة
  - معالجة الأخطاء وإعادة المحاولة

## المكونات المتبقية للتطوير
1. شاشة الحسابات
2. شاشة المعاملات
3. شاشة إضافة/تعديل الحساب
4. شاشة إضافة/تعديل المعاملة
5. شاشة التقارير
6. شاشة الإعدادات
7. شاشة الملف الشخصي
8. نظام النسخ الاحتياطي
9. نظام استعادة البيانات
10. نظام التصدير والاستيراد 