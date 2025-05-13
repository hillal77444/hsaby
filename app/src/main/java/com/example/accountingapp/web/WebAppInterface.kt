@JavascriptInterface
fun getAccounts(): Response<List<Account>> {
    return try {
        val response = apiService.getAccounts()
        if (response.isSuccessful) {
            // حفظ الحسابات في قاعدة البيانات المحلية
            response.body()?.let { accounts ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.accountDao().insertAll(accounts)
                }
            }
        }
        response
    } catch (e: Exception) {
        Log.e("WebAppInterface", "Error getting accounts: ${e.message}")
        Response.error(500, ResponseBody.create(null, e.message ?: "Unknown error"))
    }
}

@JavascriptInterface
fun getEntries(): Response<List<Entry>> {
    return try {
        val response = apiService.getEntries()
        if (response.isSuccessful) {
            // حفظ القيود في قاعدة البيانات المحلية
            response.body()?.let { entries ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.entryDao().insertAll(entries)
                }
            }
        }
        response
    } catch (e: Exception) {
        Log.e("WebAppInterface", "Error getting entries: ${e.message}")
        Response.error(500, ResponseBody.create(null, e.message ?: "Unknown error"))
    }
} 