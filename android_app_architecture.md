# هيكل تطبيق الأندرويد المحاسبي

## 1. هيكل التطبيق (Architecture)

```
📱 تطبيق الأندرويد
├── 📂 UI Layer (طبقة واجهة المستخدم)
│   ├── Activities
│   │   ├── LoginActivity
│   │   ├── RegisterActivity
│   │   ├── MainActivity (لوحة التحكم)
│   │   ├── AccountsActivity
│   │   └── TransactionsActivity
│   └── Fragments
│       ├── DashboardFragment
│       ├── AccountsFragment
│       └── ReportsFragment
│
├── 📂 Data Layer (طبقة البيانات)
│   ├── Local Database (Room)
│   │   ├── AccountEntity
│   │   ├── TransactionEntity
│   │   └── UserEntity
│   └── Remote API
│       ├── ApiService
│       └── ApiClient
│
└── 📂 Business Logic (طبقة المنطق)
    ├── ViewModels
    ├── Repositories
    └── Use Cases
```

## 2. المكونات الرئيسية

### أ. واجهة المستخدم (UI)
1. **شاشة تسجيل الدخول**
   - حقل اسم المستخدم
   - حقل كلمة المرور
   - زر تسجيل الدخول
   - رابط للتسجيل الجديد

2. **شاشة التسجيل**
   - حقل اسم المستخدم
   - حقل رقم الهاتف
   - حقل كلمة المرور
   - زر التسجيل

3. **الشاشة الرئيسية**
   - لوحة معلومات (Dashboard)
   - قائمة الحسابات
   - قائمة المعاملات الأخيرة
   - إحصائيات سريعة

4. **شاشة الحسابات**
   - قائمة الحسابات
   - إمكانية إضافة حساب جديد
   - تفاصيل كل حساب
   - رصيد الحساب

5. **شاشة المعاملات**
   - قائمة المعاملات
   - إمكانية إضافة معاملة جديدة
   - تصفية وبحث في المعاملات
   - تفاصيل المعاملة

### ب. قاعدة البيانات المحلية (Room Database)
1. **جداول قاعدة البيانات**
   ```kotlin
   // جدول الحسابات
   @Entity
   data class Account(
       @PrimaryKey val id: String,
       val name: String,
       val balance: Double,
       val type: String,
       val lastSync: Long
   )

   // جدول المعاملات
   @Entity
   data class Transaction(
       @PrimaryKey val id: String,
       val accountId: String,
       val amount: Double,
       val type: String,
       val date: Long,
       val description: String,
       val isSynced: Boolean
   )
   ```

### ج. نظام المزامنة
1. **مزامنة تلقائية**
   - مزامنة عند الاتصال بالإنترنت
   - مزامنة دورية كل فترة
   - مزامنة عند إضافة/تعديل البيانات

2. **نظام العمل دون اتصال**
   - تخزين البيانات محلياً
   - وضع علامة على البيانات غير المتزامنة
   - مزامنة عند عودة الاتصال

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
}
```

## 4. تدفق البيانات (Data Flow)

1. **عند إضافة معاملة جديدة**:
   ```
   المستخدم -> إضافة معاملة -> Room DB -> WorkManager -> API Server
   ```

2. **عند المزامنة**:
   ```
   WorkManager -> API Server -> Room DB -> UI Update
   ```

3. **عند العمل دون اتصال**:
   ```
   المستخدم -> إضافة معاملة -> Room DB (isSynced = false)
   عند عودة الاتصال -> WorkManager -> مزامنة البيانات -> API Server
   ```

## 5. ميزات الأمان

1. **تخزين آمن**
   - تشفير البيانات المحلية
   - تخزين آمن للتوكن
   - حماية كلمة المرور

2. **مصادقة**
   - JWT Token
   - تحديث تلقائي للتوكن
   - تسجيل خروج آمن

## 6. خطوات التنفيذ المقترحة

1. **المرحلة الأولى**: إعداد المشروع
   - إنشاء المشروع
   - إعداد المكتبات
   - تصميم قاعدة البيانات

2. **المرحلة الثانية**: واجهة المستخدم
   - تصميم الشاشات
   - تنفيذ التنقل
   - إضافة الرسوم البيانية

3. **المرحلة الثالثة**: قاعدة البيانات
   - إعداد Room
   - تنفيذ DAOs
   - إضافة Repository

4. **المرحلة الرابعة**: API Integration
   - إعداد Retrofit
   - تنفيذ المزامنة
   - معالجة الأخطاء

5. **المرحلة الخامسة**: العمل دون اتصال
   - تنفيذ WorkManager
   - إضافة نظام المزامنة
   - اختبار السيناريوهات المختلفة 