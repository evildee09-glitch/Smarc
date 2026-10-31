package ph.smarc;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class Db_Impl extends Db {
  private volatile LedgerDao _ledgerDao;

  private volatile UserDao _userDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(10) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `customers` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `address` TEXT NOT NULL, `creditLimit` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `email` TEXT NOT NULL, `notes` TEXT NOT NULL, `accountNo` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `ledger` (`id` TEXT NOT NULL, `idemKey` TEXT NOT NULL, `customerId` TEXT NOT NULL, `type` TEXT NOT NULL, `principalDelta` INTEGER NOT NULL, `feeDelta` INTEGER NOT NULL, `cashDelta` INTEGER NOT NULL, `incomeDelta` INTEGER NOT NULL, `lossDelta` INTEGER NOT NULL, `dueDate` INTEGER, `note` TEXT NOT NULL, `reversalOf` TEXT, `createdAt` INTEGER NOT NULL, `planId` TEXT, `receiptNo` INTEGER NOT NULL, `reference` TEXT NOT NULL, `tax` INTEGER NOT NULL, `items` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_customerId` ON `ledger` (`customerId`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ledger_idemKey` ON `ledger` (`idemKey`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ledger_reversalOf` ON `ledger` (`reversalOf`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cash_entries` (`id` TEXT NOT NULL, `idemKey` TEXT NOT NULL, `kind` TEXT NOT NULL, `cashDelta` INTEGER NOT NULL, `expenseDelta` INTEGER NOT NULL, `category` TEXT NOT NULL, `note` TEXT NOT NULL, `reversalOf` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cash_entries_idemKey` ON `cash_entries` (`idemKey`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cash_entries_reversalOf` ON `cash_entries` (`reversalOf`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `follow_ups` (`id` TEXT NOT NULL, `customerId` TEXT NOT NULL, `note` TEXT NOT NULL, `promiseDate` INTEGER, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_follow_ups_customerId` ON `follow_ups` (`customerId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `audit_log` (`id` TEXT NOT NULL, `action` TEXT NOT NULL, `detail` TEXT NOT NULL, `actor` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` TEXT NOT NULL, `username` TEXT NOT NULL, `displayName` TEXT NOT NULL, `role` TEXT NOT NULL, `pwHash` TEXT NOT NULL, `pwSalt` TEXT NOT NULL, `recHash` TEXT NOT NULL, `recSalt` TEXT NOT NULL, `active` INTEGER NOT NULL, `mustChange` INTEGER NOT NULL, `bio` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `totp` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `reconciliations` (`id` TEXT NOT NULL, `expected` INTEGER NOT NULL, `counted` INTEGER NOT NULL, `note` TEXT NOT NULL, `actor` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '0dd0813f94402e53972a9a1c7687b7d5')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `customers`");
        db.execSQL("DROP TABLE IF EXISTS `ledger`");
        db.execSQL("DROP TABLE IF EXISTS `cash_entries`");
        db.execSQL("DROP TABLE IF EXISTS `follow_ups`");
        db.execSQL("DROP TABLE IF EXISTS `audit_log`");
        db.execSQL("DROP TABLE IF EXISTS `users`");
        db.execSQL("DROP TABLE IF EXISTS `reconciliations`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsCustomers = new HashMap<String, TableInfo.Column>(10);
        _columnsCustomers.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("phone", new TableInfo.Column("phone", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("address", new TableInfo.Column("address", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("creditLimit", new TableInfo.Column("creditLimit", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("email", new TableInfo.Column("email", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("accountNo", new TableInfo.Column("accountNo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCustomers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCustomers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCustomers = new TableInfo("customers", _columnsCustomers, _foreignKeysCustomers, _indicesCustomers);
        final TableInfo _existingCustomers = TableInfo.read(db, "customers");
        if (!_infoCustomers.equals(_existingCustomers)) {
          return new RoomOpenHelper.ValidationResult(false, "customers(ph.smarc.Customer).\n"
                  + " Expected:\n" + _infoCustomers + "\n"
                  + " Found:\n" + _existingCustomers);
        }
        final HashMap<String, TableInfo.Column> _columnsLedger = new HashMap<String, TableInfo.Column>(18);
        _columnsLedger.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("idemKey", new TableInfo.Column("idemKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("customerId", new TableInfo.Column("customerId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("principalDelta", new TableInfo.Column("principalDelta", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("feeDelta", new TableInfo.Column("feeDelta", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("cashDelta", new TableInfo.Column("cashDelta", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("incomeDelta", new TableInfo.Column("incomeDelta", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("lossDelta", new TableInfo.Column("lossDelta", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("dueDate", new TableInfo.Column("dueDate", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("reversalOf", new TableInfo.Column("reversalOf", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("planId", new TableInfo.Column("planId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("receiptNo", new TableInfo.Column("receiptNo", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("reference", new TableInfo.Column("reference", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("tax", new TableInfo.Column("tax", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLedger.put("items", new TableInfo.Column("items", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLedger = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLedger = new HashSet<TableInfo.Index>(3);
        _indicesLedger.add(new TableInfo.Index("index_ledger_customerId", false, Arrays.asList("customerId"), Arrays.asList("ASC")));
        _indicesLedger.add(new TableInfo.Index("index_ledger_idemKey", true, Arrays.asList("idemKey"), Arrays.asList("ASC")));
        _indicesLedger.add(new TableInfo.Index("index_ledger_reversalOf", true, Arrays.asList("reversalOf"), Arrays.asList("ASC")));
        final TableInfo _infoLedger = new TableInfo("ledger", _columnsLedger, _foreignKeysLedger, _indicesLedger);
        final TableInfo _existingLedger = TableInfo.read(db, "ledger");
        if (!_infoLedger.equals(_existingLedger)) {
          return new RoomOpenHelper.ValidationResult(false, "ledger(ph.smarc.Entry).\n"
                  + " Expected:\n" + _infoLedger + "\n"
                  + " Found:\n" + _existingLedger);
        }
        final HashMap<String, TableInfo.Column> _columnsCashEntries = new HashMap<String, TableInfo.Column>(9);
        _columnsCashEntries.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCashEntries.put("idemKey", new TableInfo.Column("idemKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCashEntries.put("kind", new TableInfo.Column("kind", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCashEntries.put("cashDelta", new TableInfo.Column("cashDelta", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCashEntries.put("expenseDelta", new TableInfo.Column("expenseDelta", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCashEntries.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCashEntries.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCashEntries.put("reversalOf", new TableInfo.Column("reversalOf", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCashEntries.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCashEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCashEntries = new HashSet<TableInfo.Index>(2);
        _indicesCashEntries.add(new TableInfo.Index("index_cash_entries_idemKey", true, Arrays.asList("idemKey"), Arrays.asList("ASC")));
        _indicesCashEntries.add(new TableInfo.Index("index_cash_entries_reversalOf", true, Arrays.asList("reversalOf"), Arrays.asList("ASC")));
        final TableInfo _infoCashEntries = new TableInfo("cash_entries", _columnsCashEntries, _foreignKeysCashEntries, _indicesCashEntries);
        final TableInfo _existingCashEntries = TableInfo.read(db, "cash_entries");
        if (!_infoCashEntries.equals(_existingCashEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "cash_entries(ph.smarc.CashEntry).\n"
                  + " Expected:\n" + _infoCashEntries + "\n"
                  + " Found:\n" + _existingCashEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsFollowUps = new HashMap<String, TableInfo.Column>(5);
        _columnsFollowUps.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFollowUps.put("customerId", new TableInfo.Column("customerId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFollowUps.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFollowUps.put("promiseDate", new TableInfo.Column("promiseDate", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFollowUps.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFollowUps = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFollowUps = new HashSet<TableInfo.Index>(1);
        _indicesFollowUps.add(new TableInfo.Index("index_follow_ups_customerId", false, Arrays.asList("customerId"), Arrays.asList("ASC")));
        final TableInfo _infoFollowUps = new TableInfo("follow_ups", _columnsFollowUps, _foreignKeysFollowUps, _indicesFollowUps);
        final TableInfo _existingFollowUps = TableInfo.read(db, "follow_ups");
        if (!_infoFollowUps.equals(_existingFollowUps)) {
          return new RoomOpenHelper.ValidationResult(false, "follow_ups(ph.smarc.FollowUp).\n"
                  + " Expected:\n" + _infoFollowUps + "\n"
                  + " Found:\n" + _existingFollowUps);
        }
        final HashMap<String, TableInfo.Column> _columnsAuditLog = new HashMap<String, TableInfo.Column>(5);
        _columnsAuditLog.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditLog.put("action", new TableInfo.Column("action", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditLog.put("detail", new TableInfo.Column("detail", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditLog.put("actor", new TableInfo.Column("actor", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditLog.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAuditLog = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAuditLog = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAuditLog = new TableInfo("audit_log", _columnsAuditLog, _foreignKeysAuditLog, _indicesAuditLog);
        final TableInfo _existingAuditLog = TableInfo.read(db, "audit_log");
        if (!_infoAuditLog.equals(_existingAuditLog)) {
          return new RoomOpenHelper.ValidationResult(false, "audit_log(ph.smarc.AuditLog).\n"
                  + " Expected:\n" + _infoAuditLog + "\n"
                  + " Found:\n" + _existingAuditLog);
        }
        final HashMap<String, TableInfo.Column> _columnsUsers = new HashMap<String, TableInfo.Column>(13);
        _columnsUsers.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("username", new TableInfo.Column("username", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("displayName", new TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("role", new TableInfo.Column("role", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("pwHash", new TableInfo.Column("pwHash", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("pwSalt", new TableInfo.Column("pwSalt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("recHash", new TableInfo.Column("recHash", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("recSalt", new TableInfo.Column("recSalt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("active", new TableInfo.Column("active", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("mustChange", new TableInfo.Column("mustChange", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("bio", new TableInfo.Column("bio", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("totp", new TableInfo.Column("totp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUsers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUsers = new HashSet<TableInfo.Index>(1);
        _indicesUsers.add(new TableInfo.Index("index_users_username", true, Arrays.asList("username"), Arrays.asList("ASC")));
        final TableInfo _infoUsers = new TableInfo("users", _columnsUsers, _foreignKeysUsers, _indicesUsers);
        final TableInfo _existingUsers = TableInfo.read(db, "users");
        if (!_infoUsers.equals(_existingUsers)) {
          return new RoomOpenHelper.ValidationResult(false, "users(ph.smarc.User).\n"
                  + " Expected:\n" + _infoUsers + "\n"
                  + " Found:\n" + _existingUsers);
        }
        final HashMap<String, TableInfo.Column> _columnsReconciliations = new HashMap<String, TableInfo.Column>(6);
        _columnsReconciliations.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReconciliations.put("expected", new TableInfo.Column("expected", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReconciliations.put("counted", new TableInfo.Column("counted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReconciliations.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReconciliations.put("actor", new TableInfo.Column("actor", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReconciliations.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysReconciliations = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesReconciliations = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoReconciliations = new TableInfo("reconciliations", _columnsReconciliations, _foreignKeysReconciliations, _indicesReconciliations);
        final TableInfo _existingReconciliations = TableInfo.read(db, "reconciliations");
        if (!_infoReconciliations.equals(_existingReconciliations)) {
          return new RoomOpenHelper.ValidationResult(false, "reconciliations(ph.smarc.Reconciliation).\n"
                  + " Expected:\n" + _infoReconciliations + "\n"
                  + " Found:\n" + _existingReconciliations);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "0dd0813f94402e53972a9a1c7687b7d5", "8e277a0c9e3e84a6ca2817b2ac3bf94e");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "customers","ledger","cash_entries","follow_ups","audit_log","users","reconciliations");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `customers`");
      _db.execSQL("DELETE FROM `ledger`");
      _db.execSQL("DELETE FROM `cash_entries`");
      _db.execSQL("DELETE FROM `follow_ups`");
      _db.execSQL("DELETE FROM `audit_log`");
      _db.execSQL("DELETE FROM `users`");
      _db.execSQL("DELETE FROM `reconciliations`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(LedgerDao.class, LedgerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(UserDao.class, UserDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public LedgerDao dao() {
    if (_ledgerDao != null) {
      return _ledgerDao;
    } else {
      synchronized(this) {
        if(_ledgerDao == null) {
          _ledgerDao = new LedgerDao_Impl(this);
        }
        return _ledgerDao;
      }
    }
  }

  @Override
  public UserDao users() {
    if (_userDao != null) {
      return _userDao;
    } else {
      synchronized(this) {
        if(_userDao == null) {
          _userDao = new UserDao_Impl(this);
        }
        return _userDao;
      }
    }
  }
}
