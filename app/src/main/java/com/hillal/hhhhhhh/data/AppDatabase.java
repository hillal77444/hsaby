@Database(entities = {Transaction.class, Account.class, User.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();
    public abstract AccountDao accountDao();
    public abstract UserDao userDao();
} 