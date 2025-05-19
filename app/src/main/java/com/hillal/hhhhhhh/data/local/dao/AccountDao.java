@Dao
public interface AccountDao {
    @Query("SELECT * FROM accounts")
    List<Account> getAllAccounts();

    @Query("SELECT * FROM accounts WHERE id = :id")
    Account getAccountById(long id);

    @Query("SELECT * FROM accounts WHERE server_id = :serverId")
    Account getAccountByServerId(long serverId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Account account);

    @Update
    void update(Account account);

    @Delete
    void delete(Account account);

    @Query("DELETE FROM accounts")
    void deleteAll();

    @Query("SELECT * FROM accounts WHERE user_id = :userId")
    List<Account> getAccountsByUserId(long userId);
} 