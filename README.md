# تطبيق المحاسبة

## هيكل التطبيق

```
app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/accounting/
│   │   │       ├── activities/
│   │   │       │   ├── MainActivity.java
│   │   │       │   ├── AddTransactionActivity.java
│   │   │       │   └── AccountDetailsActivity.java
│   │   │       ├── adapters/
│   │   │       │   ├── TransactionAdapter.java
│   │   │       │   └── AccountAdapter.java
│   │   │       ├── models/
│   │   │       │   ├── Transaction.java
│   │   │       │   └── Account.java
│   │   │       ├── utils/
│   │   │       │   ├── DateUtils.java
│   │   │       │   └── CurrencyFormatter.java
│   │   │       └── database/
│   │   │           └── AppDatabase.java
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml
│   │   │   │   ├── activity_add_transaction.xml
│   │   │   │   ├── activity_account_details.xml
│   │   │   │   ├── item_transaction.xml
│   │   │   │   └── item_account.xml
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── styles.xml
│   │   │   └── drawable/
│   │   │       ├── ic_add.xml
│   │   │       ├── ic_edit.xml
│   │   │       └── ic_delete.xml
│   │   └── AndroidManifest.xml
│   └── test/
└── build.gradle
```

## شرح المكونات الرئيسية

### 1. الأنشطة (Activities)
- **MainActivity**: النشاط الرئيسي للتطبيق، يعرض قائمة الحسابات والمعاملات
- **AddTransactionActivity**: نشاط إضافة معاملة جديدة
- **AccountDetailsActivity**: نشاط عرض تفاصيل الحساب

### 2. المحولات (Adapters)
- **TransactionAdapter**: يعرض قائمة المعاملات في RecyclerView
- **AccountAdapter**: يعرض قائمة الحسابات في RecyclerView

### 3. النماذج (Models)
- **Transaction**: نموذج يمثل المعاملة المالية
- **Account**: نموذج يمثل الحساب المالي

### 4. الأدوات المساعدة (Utils)
- **DateUtils**: أدوات للتعامل مع التواريخ
- **CurrencyFormatter**: تنسيق العملات

### 5. قاعدة البيانات (Database)
- **AppDatabase**: قاعدة البيانات المحلية للتطبيق

### 6. الموارد (Resources)
- **layout/**: ملفات تخطيط واجهة المستخدم
- **values/**: ملفات القيم (الألوان، النصوص، الأنماط)
- **drawable/**: الصور والأيقونات

## الوظائف الرئيسية

1. **إدارة الحسابات**
   - إنشاء حسابات جديدة
   - تعديل الحسابات الحالية
   - حذف الحسابات
   - عرض رصيد كل حساب

2. **إدارة المعاملات**
   - إضافة معاملات جديدة (إيداع/سحب)
   - تعديل المعاملات
   - حذف المعاملات
   - تصفية المعاملات حسب التاريخ/النوع

3. **التقارير**
   - عرض ملخص الحسابات
   - عرض حركة الحساب
   - تصدير التقارير

## التقنيات المستخدمة

- Android SDK
- Room Database
- RecyclerView
- Material Design Components
- ViewModel & LiveData
- Navigation Component

## متطلبات النظام

- Android 5.0 (API level 21) أو أحدث
- مساحة تخزين: 10MB
- ذاكرة عشوائية: 100MB 