public double getAccountBalance(long accountId) {
    return transactionDao.getAccountBalanceSync(accountId);
} 