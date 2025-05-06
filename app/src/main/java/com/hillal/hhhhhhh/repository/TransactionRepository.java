public LiveData<List<Transaction>> getTransactionsByDateRange(long accountId, long fromDate, long toDate) {
    return transactionDao.getTransactionsByDateRange(accountId, fromDate, toDate);
} 