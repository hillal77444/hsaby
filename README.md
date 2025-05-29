# تطبيق المالي برو

## هيكل المشروع

```
app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/hillal/acc/
│   │   │       ├── App.java
│   │   │       ├── MainActivity.java
│   │   │       ├── viewmodel/
│   │   │       │   ├── AccountStatementViewModel.java
│   │   │       │   ├── AccountViewModel.java
│   │   │       │   ├── AuthViewModel.java
│   │   │       │   ├── SettingsViewModel.java
│   │   │       │   └── TransactionViewModel.java
│   │   │       ├── ui/
│   │   │       │   ├── transactions/
│   │   │       │   ├── settings/
│   │   │       │   ├── reports/
│   │   │       │   ├── profile/
│   │   │       │   ├── home/
│   │   │       │   ├── gallery/
│   │   │       │   ├── dashboard/
│   │   │       │   ├── auth/
│   │   │       │   ├── adapters/
│   │   │       │   ├── accounts/
│   │   │       │   ├── account/
│   │   │       │   └── AccountStatementActivity.java
│   │   │       ├── repository/
│   │   │       │   ├── AccountStatementRepository.java
│   │   │       │   └── TransactionRepository.java
│   │   │       └── data/
│   │   │           ├── sync/
│   │   │           ├── security/
│   │   │           ├── room/
│   │   │           ├── repository/
│   │   │           ├── remote/
│   │   │           ├── preferences/
│   │   │           ├── model/
│   │   │           ├── local/
│   │   │           ├── entities/
│   │   │           ├── dao/
│   │   │           ├── backup/
│   │   │           ├── Converters.java
│   │   │           └── DateConverter.java
│   │   ├── res/
│   │   ├── assets/
│   │   └── AndroidManifest.xml
│   ├── test/
│   └── androidTest/
├── build.gradle
├── proguard-rules.pro
├── gradle.properties
├── keystore.jks
├── schemas/
└── .gitignore
```

## شرح المكونات الرئيسية

### 1. الملفات الرئيسية في المجلد الجذر
- **build.gradle**: ملف تكوين Gradle للتطبيق، يحدد التبعيات وإعدادات البناء
- **proguard-rules.pro**: قواعد ProGuard لتحسين وتأمين التطبيق
- **gradle.properties**: خصائص Gradle العامة للتطبيق
- **keystore.jks**: ملف مفتاح التوقيع للتطبيق
- **schemas/**: مجلد يحتوي على مخططات قاعدة البيانات
- **.gitignore**: ملف يحدد الملفات التي يجب تجاهلها في Git

### 2. مجلد src/
يحتوي على كود المصدر للتطبيق:
- **main/**: الكود الرئيسي للتطبيق
- **test/**: اختبارات الوحدة
- **androidTest/**: اختبارات واجهة المستخدم

## شرح المكونات الرئيسية

### 1. الملفات الرئيسية
- **App.java**: فئة التطبيق الرئيسية
- **MainActivity.java**: النشاط الرئيسي للتطبيق

### 2. مجلد viewmodel/
يحتوي على نماذج العرض التي تتعامل مع منطق الأعمال:
- **AccountStatementViewModel.java**: إدارة كشف الحساب
- **AccountViewModel.java**: إدارة الحسابات
- **AuthViewModel.java**: إدارة المصادقة
- **SettingsViewModel.java**: إدارة الإعدادات
- **TransactionViewModel.java**: إدارة المعاملات

### 3. مجلد ui/
يحتوي على واجهات المستخدم المختلفة:

#### 3.1 مجلد transactions/
- **AccountPickerAdapter.java**: محول لعرض قائمة الحسابات في نافذة اختيار الحساب
- **AddTransactionFragment.java**: شاشة إضافة معاملة جديدة
- **EditTransactionFragment.java**: شاشة تعديل المعاملة
- **TransactionsFragment.java**: شاشة عرض قائمة المعاملات
- **TransactionsViewModel.java**: نموذج عرض لإدارة قائمة المعاملات
- **TransactionViewModel.java**: نموذج عرض لإدارة معاملة واحدة
- **TransactionViewModelFactory.java**: مصنع لإنشاء نماذج عرض المعاملات

#### 3.2 مجلد settings/
- **SettingsFragment.java**: شاشة إعدادات التطبيق
- **SettingsViewModel.java**: نموذج عرض لإدارة الإعدادات

#### 3.3 مجلد reports/
- **ReportsFragment.java**: شاشة التقارير
- **ReportsViewModel.java**: نموذج عرض لإدارة التقارير

#### 3.4 مجلد profile/
- **ProfileFragment.java**: شاشة الملف الشخصي
- **ProfileViewModel.java**: نموذج عرض لإدارة الملف الشخصي

#### 3.5 مجلد home/
- **HomeFragment.java**: الشاشة الرئيسية
- **HomeViewModel.java**: نموذج عرض للشاشة الرئيسية

#### 3.6 مجلد gallery/
- **GalleryFragment.java**: معرض الصور
- **GalleryViewModel.java**: نموذج عرض لإدارة معرض الصور

#### 3.7 مجلد dashboard/
- **DashboardFragment.java**: لوحة التحكم
- **DashboardViewModel.java**: نموذج عرض لإدارة لوحة التحكم

#### 3.8 مجلد auth/
- **LoginFragment.java**: شاشة تسجيل الدخول
- **RegisterFragment.java**: شاشة التسجيل
- **AuthViewModel.java**: نموذج عرض لإدارة المصادقة

#### 3.9 مجلد adapters/
- **TransactionAdapter.java**: محول لعرض قائمة المعاملات
- **AccountAdapter.java**: محول لعرض قائمة الحسابات
- **ReportAdapter.java**: محول لعرض قائمة التقارير

#### 3.10 مجلد accounts/
- **AccountsFragment.java**: شاشة قائمة الحسابات
- **AccountsViewModel.java**: نموذج عرض لإدارة الحسابات

#### 3.11 مجلد account/
- **AccountFragment.java**: شاشة تفاصيل الحساب
- **AccountViewModel.java**: نموذج عرض لإدارة حساب واحد

### 4. مجلد data/
يحتوي على طبقة البيانات:

#### 4.1 مجلد model/
- **Account.java**: نموذج بيانات الحساب
- **AuthState.java**: حالة المصادقة
- **PendingOperation.java**: العمليات المعلقة
- **Report.java**: نموذج بيانات التقرير
- **Settings.java**: نموذج بيانات الإعدادات
- **Transaction.java**: نموذج بيانات المعاملة
- **User.java**: نموذج بيانات المستخدم

#### 4.2 مجلد sync/
- **SyncManager.java**: مدير المزامنة
- **SyncService.java**: خدمة المزامنة

#### 4.3 مجلد security/
- **EncryptionManager.java**: مدير التشفير
- **SecurityUtils.java**: أدوات الأمان

#### 4.4 مجلد room/
- **AppDatabase.java**: قاعدة البيانات المحلية
- **DatabaseConfig.java**: إعدادات قاعدة البيانات

#### 4.5 مجلد remote/
- **ApiService.java**: خدمة API
- **RemoteDataSource.java**: مصدر البيانات عن بعد

#### 4.6 مجلد preferences/
- **PreferencesManager.java**: مدير الإعدادات
- **SharedPreferencesHelper.java**: مساعد الإعدادات المشتركة

#### 4.7 مجلد local/
- **LocalDataSource.java**: مصدر البيانات المحلي
- **FileManager.java**: مدير الملفات

#### 4.8 مجلد entities/
- **AccountEntity.java**: كيان الحساب
- **TransactionEntity.java**: كيان المعاملة
- **UserEntity.java**: كيان المستخدم

#### 4.9 مجلد dao/
- **AccountDao.java**: واجهة وصول بيانات الحساب
- **TransactionDao.java**: واجهة وصول بيانات المعاملات
- **UserDao.java**: واجهة وصول بيانات المستخدم

#### 4.10 مجلد backup/
- **BackupManager.java**: مدير النسخ الاحتياطي
- **BackupService.java**: خدمة النسخ الاحتياطي

## نمط التصميم
هذا التطبيق يتبع نمط MVVM (Model-View-ViewModel) مع Clean Architecture، حيث:
- **ViewModel**: يتعامل مع منطق الأعمال
- **UI**: يتعامل مع واجهة المستخدم
- **Repository**: يتعامل مع مصادر البيانات
- **Data**: يتعامل مع طبقة البيانات الأساسية

## التقنيات المستخدمة
- Android SDK
- Room Database
- RecyclerView
- Material Design Components
- ViewModel & LiveData
- Navigation Component
- Retrofit للاتصال بالخادم
- Kotlin Coroutines للعمليات غير المتزامنة

## متطلبات النظام
- Android 5.0 (API level 21) أو أحدث
- مساحة تخزين: 10MB
- ذاكرة عشوائية: 100MB
- اتصال بالإنترنت للاتصال بالخادم
