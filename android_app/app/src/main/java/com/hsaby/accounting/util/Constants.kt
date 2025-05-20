package com.hsaby.accounting.util

object Constants {
    const val BASE_URL = "http://your-server-url:5007/api/"
    
    const val PREF_NAME = "accounting_prefs"
    const val PREF_TOKEN = "token"
    const val PREF_USER_ID = "user_id"
    const val PREF_USERNAME = "username"
    const val PREF_REMEMBER_ME = "remember_me"
    
    const val SYNC_INTERVAL = 15L // minutes
    
    const val CURRENCY_YEMENI = "YER"
    const val CURRENCY_SAUDI = "SAR"
    const val CURRENCY_DOLLAR = "USD"
    
    const val TRANSACTION_TYPE_TO = "له"
    const val TRANSACTION_TYPE_FROM = "عليه"
    
    const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    const val DATE_FORMAT_DISPLAY = "dd/MM/yyyy HH:mm"
} 