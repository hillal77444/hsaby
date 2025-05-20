# هيكل تطبيق الأندرويد المحاسبي المحدث

## 1. هيكل التطبيق (Architecture)

```
📱 تطبيق الأندرويد
├── 📂 UI Layer (طبقة واجهة المستخدم)
│   ├── Activities
│   │   ├── SplashActivity (شاشة البداية)
│   │   ├── LoginActivity (تسجيل الدخول)
│   │   ├── RegisterActivity (التسجيل)
│   │   ├── MainActivity (الشاشة الرئيسية)
│   │   ├── AccountsActivity (الحسابات)
│   │   ├── TransactionsActivity (المعاملات)
│   │   └── SettingsActivity (الإعدادات)
│   └── Fragments
│       ├── DashboardFragment (لوحة التحكم)
│       ├── AccountsFragment (قائمة الحسابات)
│       ├── TransactionsFragment (قائمة المعاملات)
│       ├── ReportsFragment (التقارير)
│       └── SettingsFragment (الإعدادات)
│
├── 📂 Data Layer (طبقة البيانات)
│   ├── Local Database (Room)
│   │   ├── Entities
│   │   │   ├── UserEntity
│   │   │   ├── AccountEntity
│   │   │   └── TransactionEntity
│   │   ├── DAOs
│   │   │   ├── UserDao
│   │   │   ├── AccountDao
│   │   │   └── TransactionDao
│   │   └── AppDatabase
│   └── Remote API
│       ├── ApiService
│       ├── ApiClient
│       └── Models
│           ├── User
│           ├── Account
│           └── Transaction
│
└── 📂 Business Logic (طبقة المنطق)
    ├── ViewModels
    │   ├── LoginViewModel
    │   ├── RegisterViewModel
    │   ├── MainViewModel
    │   ├── AccountsViewModel
    │   └── TransactionsViewModel
    ├── Repositories
    │   ├── UserRepository
    │   ├── AccountRepository
    │   └── TransactionRepository
    └── Use Cases
        ├── AuthUseCase
        ├── SyncUseCase
        └── CurrencyUseCase
```

## 2. المكونات الرئيسية

### أ. واجهة المستخدم (UI)
1. **شاشة البداية (SplashActivity)**
   - شعار التطبيق
   - التحقق من حالة الاتصال
   - التحقق من وجود بيانات تسجيل الدخول

2. **شاشة تسجيل الدخول (LoginActivity)**
   - حقل رقم الهاتف
   - حقل كلمة المرور
   - زر تسجيل الدخول
   - رابط للتسجيل الجديد
   - خيار "تذكرني"
   - **بعد تسجيل الدخول الناجح**:
     - جلب بيانات المستخدم
     - جلب الحسابات الخاصة بالمستخدم
     - جلب المعاملات الخاصة بالمستخدم
     - تخزين البيانات محلياً
     - الانتقال للشاشة الرئيسية

3. **شاشة التسجيل (RegisterActivity)**
   - حقل اسم المستخدم
   - حقل رقم الهاتف
   - حقل كلمة المرور
   - تأكيد كلمة المرور
   - زر التسجيل

4. **الشاشة الرئيسية (MainActivity)**
   - **شريط الأدوات العلوي (Toolbar)**
     - زر تحديث البيانات
     - مؤشر حالة المزامنة
     - زر الإعدادات
   - Bottom Navigation Bar
   - لوحة معلومات (Dashboard)
     - إجمالي الرصيد
     - إحصائيات سريعة
     - آخر المعاملات
   - قائمة الحسابات
   - قائمة المعاملات
   - التقارير
   - الإعدادات

5. **شاشة الحسابات (AccountsActivity)**
   - **شريط الأدوات العلوي (Toolbar)**
     - زر تحديث البيانات
     - زر إضافة حساب جديد
     - زر البحث
   - قائمة الحسابات
     - عرض الحسابات الخاصة بالمستخدم فقط
     - ترتيب حسب الرصيد/التاريخ
     - تصفية حسب العملة
   - إمكانية إضافة حساب جديد
   - تفاصيل كل حساب
   - رصيد الحساب
   - تصفية حسب العملة (يمني/سعودي/دولار)

6. **شاشة المعاملات (TransactionsActivity)**
   - **شريط الأدوات العلوي (Toolbar)**
     - زر تحديث البيانات
     - زر إضافة معاملة جديدة
     - زر البحث
   - قائمة المعاملات
     - عرض المعاملات الخاصة بالمستخدم فقط
     - ترتيب حسب التاريخ
     - تصفية حسب الحساب/العملة/النوع
   - إمكانية إضافة معاملة جديدة
   - تصفية وبحث في المعاملات
   - تفاصيل المعاملة
   - تصفية حسب العملة
   - تصفية حسب التاريخ
   - تصفية حسب نوع المعاملة (له/عليه)

7. **شاشة التقارير (ReportsFragment)**
   - **التقارير الإجمالية**
     - إجمالي الأرصدة حسب العملة
     - إجمالي المعاملات (له/عليه)
     - حركة الأرصدة خلال الفترة
     - رسم بياني دائري لتوزيع العملات
     - رسم بياني خطي لحركة الأرصدة
   
   - **كشف الحساب التفصيلي**
     - اختيار الحساب
     - تحديد الفترة الزمنية
     - عرض الرصيد الافتتاحي
     - قائمة المعاملات مرتبة حسب التاريخ
       - معاملات له
       - معاملات عليه
     - إجمالي المعاملات (له/عليه)
     - الرصيد الختامي
     - إمكانية تصدير التقرير (PDF/Excel)
     - إمكانية مشاركة التقرير
   
   - **تقارير إضافية**
     - تقرير العملاء (المدينين)
     - تقرير الموردين (الدائنين)
     - تقرير حركة العملات
     - تقرير الأرباح والخسائر

### ب. قاعدة البيانات المحلية (Room Database)
1. **جداول قاعدة البيانات**
   ```kotlin
   // جدول المستخدمين
   @Entity
   data class UserEntity(
       @PrimaryKey val id: String,
       val username: String,
       val phone: String,
       val passwordHash: String,
       val lastSync: Long
   )

   // جدول الحسابات
   @Entity
   data class AccountEntity(
       @PrimaryKey val id: String,
       val serverId: Long,
       val accountName: String,
       val balance: Double,
       val currency: String, // يمني/سعودي/دولار
       val phoneNumber: String?,
       val notes: String?,
       val isDebtor: Boolean,
       val whatsappEnabled: Boolean,
       val userId: String,
       val lastSync: Long
   )

   // جدول المعاملات
   @Entity
   data class TransactionEntity(
       @PrimaryKey val id: String,
       val serverId: Long,
       val accountId: String,
       val amount: Double,
       val type: String, // له/عليه
       val description: String,
       val date: Long,
       val currency: String,
       val notes: String?,
       val whatsappEnabled: Boolean,
       val userId: String,
       val isSynced: Boolean,
       val lastSync: Long
   )

   // جدول التقارير المحفوظة
   @Entity
   data class SavedReportEntity(
       @PrimaryKey val id: String,
       val reportType: String, // "account_statement", "balance_sheet", "profit_loss"
       val accountId: String?,
       val startDate: Long,
       val endDate: Long,
       val reportData: String, // JSON string containing report data
       val createdAt: Long,
       val userId: String
   )

   // جدول إعدادات التقارير
   @Entity
   data class ReportSettingsEntity(
       @PrimaryKey val id: String,
       val userId: String,
       val defaultCurrency: String,
       val defaultPeriod: String, // "daily", "weekly", "monthly", "yearly"
       val showGraphs: Boolean,
       val autoSaveReports: Boolean,
       val reportFormat: String // "pdf", "excel"
   )
   ```

### ج. نظام المزامنة
1. **مزامنة تلقائية**
   - مزامنة عند تسجيل الدخول
   - مزامنة عند الاتصال بالإنترنت
   - مزامنة دورية كل 15 دقيقة
   - مزامنة عند إضافة/تعديل البيانات

2. **مزامنة يدوية**
   - زر تحديث في كل شاشة
   - مؤشر حالة المزامنة
   - عرض آخر وقت تحديث
   - إمكانية إلغاء المزامنة

3. **نظام العمل دون اتصال**
   - تخزين البيانات محلياً
   - وضع علامة على البيانات غير المتزامنة
   - مزامنة عند عودة الاتصال
   - إدارة صراعات المزامنة

## 3. المكتبات المقترحة

```gradle
dependencies {
    // Android Architecture Components
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1"
    implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.6.1"
    
    // Room Database
    implementation "androidx.room:room-runtime:2.5.1"
    implementation "androidx.room:room-ktx:2.5.1"
    
    // Retrofit for API calls
    implementation "com.squareup.retrofit2:retrofit:2.9.0"
    implementation "com.squareup.retrofit2:converter-gson:2.9.0"
    
    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4"
    
    // Material Design
    implementation "com.google.android.material:material:1.9.0"
    
    // WorkManager for background sync
    implementation "androidx.work:work-runtime-ktx:2.8.1"
    
    // DataStore for preferences
    implementation "androidx.datastore:datastore-preferences:1.0.0"
    
    // Navigation Component
    implementation "androidx.navigation:navigation-fragment-ktx:2.5.3"
    implementation "androidx.navigation:navigation-ui-ktx:2.5.3"
    
    // Charts for reports
    implementation "com.github.PhilJay:MPAndroidChart:v3.1.0"
    
    // Image loading
    implementation "io.coil-kt:coil:2.4.0"
    
    // PDF Generation
    implementation "com.itextpdf:itext7-core:7.2.5"
    
    // Excel Generation
    implementation "org.apache.poi:poi:5.2.3"
    implementation "org.apache.poi:poi-ooxml:5.2.3"
    
    // Charts and Graphs
    implementation "com.github.PhilJay:MPAndroidChart:v3.1.0"
    implementation "com.github.AnyChart:AnyChart-Android:1.1.2"
    
    // Date Range Picker
    implementation "com.github.kizitonwose:CalendarView:1.0.4"
}
```

## 4. ميزات الأمان

1. **تخزين آمن**
   - تشفير البيانات المحلية باستخدام EncryptedSharedPreferences
   - تخزين آمن للتوكن
   - حماية كلمة المرور باستخدام Hashing

2. **مصادقة**
   - JWT Token
   - تحديث تلقائي للتوكن
   - تسجيل خروج آمن
   - حماية من هجمات MITM

## 5. خطوات التنفيذ المقترحة

1. **المرحلة الأولى**: إعداد المشروع
   - إنشاء المشروع
   - إعداد المكتبات
   - تصميم قاعدة البيانات
   - إعداد نظام المصادقة
   - تنفيذ جلب البيانات الأولي

2. **المرحلة الثانية**: واجهة المستخدم
   - تصميم الشاشات
   - تنفيذ التنقل
   - إضافة الرسوم البيانية
   - تنفيذ التصميم المتجاوب

3. **المرحلة الثالثة**: قاعدة البيانات
   - إعداد Room
   - تنفيذ DAOs
   - إضافة Repository
   - تنفيذ نظام المزامنة

4. **المرحلة الرابعة**: API Integration
   - إعداد Retrofit
   - تنفيذ المزامنة
   - معالجة الأخطاء
   - تنفيذ العمل دون اتصال

5. **المرحلة الخامسة**: التحسينات
   - تحسين الأداء
   - إضافة الاختبارات
   - تحسين تجربة المستخدم
   - إضافة التقارير والإحصائيات

6. **المرحلة السادسة**: التقارير والإحصائيات
   - تنفيذ التقارير الإجمالية
   - تنفيذ كشف الحساب التفصيلي
   - إضافة الرسوم البيانية
   - تنفيذ تصدير التقارير
   - إضافة خيارات المشاركة
   - تحسين أداء التقارير 