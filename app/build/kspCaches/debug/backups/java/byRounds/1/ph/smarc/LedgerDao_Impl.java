package ph.smarc;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LedgerDao_Impl implements LedgerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Customer> __insertionAdapterOfCustomer;

  private final EntityInsertionAdapter<Entry> __insertionAdapterOfEntry;

  private final EntityInsertionAdapter<CashEntry> __insertionAdapterOfCashEntry;

  private final EntityInsertionAdapter<FollowUp> __insertionAdapterOfFollowUp;

  private final EntityInsertionAdapter<AuditLog> __insertionAdapterOfAuditLog;

  private final EntityInsertionAdapter<Customer> __insertionAdapterOfCustomer_1;

  private final EntityInsertionAdapter<Entry> __insertionAdapterOfEntry_1;

  private final EntityInsertionAdapter<CashEntry> __insertionAdapterOfCashEntry_1;

  private final EntityInsertionAdapter<FollowUp> __insertionAdapterOfFollowUp_1;

  private final EntityInsertionAdapter<AuditLog> __insertionAdapterOfAuditLog_1;

  private final EntityInsertionAdapter<Reconciliation> __insertionAdapterOfReconciliation;

  private final EntityInsertionAdapter<Reconciliation> __insertionAdapterOfReconciliation_1;

  private final EntityDeletionOrUpdateAdapter<Customer> __updateAdapterOfCustomer;

  private final SharedSQLiteStatement __preparedStmtOfScrubFollowUps;

  private final SharedSQLiteStatement __preparedStmtOfClearLedger;

  private final SharedSQLiteStatement __preparedStmtOfClearCustomers;

  private final SharedSQLiteStatement __preparedStmtOfClearCash;

  private final SharedSQLiteStatement __preparedStmtOfClearFollow;

  private final SharedSQLiteStatement __preparedStmtOfClearAudit;

  private final SharedSQLiteStatement __preparedStmtOfClearRecons;

  public LedgerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCustomer = new EntityInsertionAdapter<Customer>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `customers` (`id`,`name`,`phone`,`address`,`creditLimit`,`status`,`createdAt`,`email`,`notes`,`accountNo`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Customer entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getPhone());
        statement.bindString(4, entity.getAddress());
        statement.bindLong(5, entity.getCreditLimit());
        statement.bindString(6, entity.getStatus());
        statement.bindLong(7, entity.getCreatedAt());
        statement.bindString(8, entity.getEmail());
        statement.bindString(9, entity.getNotes());
        statement.bindString(10, entity.getAccountNo());
      }
    };
    this.__insertionAdapterOfEntry = new EntityInsertionAdapter<Entry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `ledger` (`id`,`idemKey`,`customerId`,`type`,`principalDelta`,`feeDelta`,`cashDelta`,`incomeDelta`,`lossDelta`,`dueDate`,`note`,`reversalOf`,`createdAt`,`planId`,`receiptNo`,`reference`,`tax`,`items`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Entry entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getIdemKey());
        statement.bindString(3, entity.getCustomerId());
        statement.bindString(4, entity.getType());
        statement.bindLong(5, entity.getPrincipalDelta());
        statement.bindLong(6, entity.getFeeDelta());
        statement.bindLong(7, entity.getCashDelta());
        statement.bindLong(8, entity.getIncomeDelta());
        statement.bindLong(9, entity.getLossDelta());
        if (entity.getDueDate() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getDueDate());
        }
        statement.bindString(11, entity.getNote());
        if (entity.getReversalOf() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getReversalOf());
        }
        statement.bindLong(13, entity.getCreatedAt());
        if (entity.getPlanId() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getPlanId());
        }
        statement.bindLong(15, entity.getReceiptNo());
        statement.bindString(16, entity.getReference());
        statement.bindLong(17, entity.getTax());
        statement.bindString(18, entity.getItems());
      }
    };
    this.__insertionAdapterOfCashEntry = new EntityInsertionAdapter<CashEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `cash_entries` (`id`,`idemKey`,`kind`,`cashDelta`,`expenseDelta`,`category`,`note`,`reversalOf`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CashEntry entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getIdemKey());
        statement.bindString(3, entity.getKind());
        statement.bindLong(4, entity.getCashDelta());
        statement.bindLong(5, entity.getExpenseDelta());
        statement.bindString(6, entity.getCategory());
        statement.bindString(7, entity.getNote());
        if (entity.getReversalOf() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getReversalOf());
        }
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfFollowUp = new EntityInsertionAdapter<FollowUp>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `follow_ups` (`id`,`customerId`,`note`,`promiseDate`,`createdAt`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FollowUp entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getCustomerId());
        statement.bindString(3, entity.getNote());
        if (entity.getPromiseDate() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getPromiseDate());
        }
        statement.bindLong(5, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfAuditLog = new EntityInsertionAdapter<AuditLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `audit_log` (`id`,`action`,`detail`,`actor`,`createdAt`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AuditLog entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getAction());
        statement.bindString(3, entity.getDetail());
        statement.bindString(4, entity.getActor());
        statement.bindLong(5, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfCustomer_1 = new EntityInsertionAdapter<Customer>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `customers` (`id`,`name`,`phone`,`address`,`creditLimit`,`status`,`createdAt`,`email`,`notes`,`accountNo`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Customer entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getPhone());
        statement.bindString(4, entity.getAddress());
        statement.bindLong(5, entity.getCreditLimit());
        statement.bindString(6, entity.getStatus());
        statement.bindLong(7, entity.getCreatedAt());
        statement.bindString(8, entity.getEmail());
        statement.bindString(9, entity.getNotes());
        statement.bindString(10, entity.getAccountNo());
      }
    };
    this.__insertionAdapterOfEntry_1 = new EntityInsertionAdapter<Entry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `ledger` (`id`,`idemKey`,`customerId`,`type`,`principalDelta`,`feeDelta`,`cashDelta`,`incomeDelta`,`lossDelta`,`dueDate`,`note`,`reversalOf`,`createdAt`,`planId`,`receiptNo`,`reference`,`tax`,`items`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Entry entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getIdemKey());
        statement.bindString(3, entity.getCustomerId());
        statement.bindString(4, entity.getType());
        statement.bindLong(5, entity.getPrincipalDelta());
        statement.bindLong(6, entity.getFeeDelta());
        statement.bindLong(7, entity.getCashDelta());
        statement.bindLong(8, entity.getIncomeDelta());
        statement.bindLong(9, entity.getLossDelta());
        if (entity.getDueDate() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getDueDate());
        }
        statement.bindString(11, entity.getNote());
        if (entity.getReversalOf() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getReversalOf());
        }
        statement.bindLong(13, entity.getCreatedAt());
        if (entity.getPlanId() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getPlanId());
        }
        statement.bindLong(15, entity.getReceiptNo());
        statement.bindString(16, entity.getReference());
        statement.bindLong(17, entity.getTax());
        statement.bindString(18, entity.getItems());
      }
    };
    this.__insertionAdapterOfCashEntry_1 = new EntityInsertionAdapter<CashEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cash_entries` (`id`,`idemKey`,`kind`,`cashDelta`,`expenseDelta`,`category`,`note`,`reversalOf`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CashEntry entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getIdemKey());
        statement.bindString(3, entity.getKind());
        statement.bindLong(4, entity.getCashDelta());
        statement.bindLong(5, entity.getExpenseDelta());
        statement.bindString(6, entity.getCategory());
        statement.bindString(7, entity.getNote());
        if (entity.getReversalOf() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getReversalOf());
        }
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfFollowUp_1 = new EntityInsertionAdapter<FollowUp>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `follow_ups` (`id`,`customerId`,`note`,`promiseDate`,`createdAt`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FollowUp entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getCustomerId());
        statement.bindString(3, entity.getNote());
        if (entity.getPromiseDate() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getPromiseDate());
        }
        statement.bindLong(5, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfAuditLog_1 = new EntityInsertionAdapter<AuditLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `audit_log` (`id`,`action`,`detail`,`actor`,`createdAt`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AuditLog entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getAction());
        statement.bindString(3, entity.getDetail());
        statement.bindString(4, entity.getActor());
        statement.bindLong(5, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfReconciliation = new EntityInsertionAdapter<Reconciliation>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `reconciliations` (`id`,`expected`,`counted`,`note`,`actor`,`createdAt`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Reconciliation entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getExpected());
        statement.bindLong(3, entity.getCounted());
        statement.bindString(4, entity.getNote());
        statement.bindString(5, entity.getActor());
        statement.bindLong(6, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfReconciliation_1 = new EntityInsertionAdapter<Reconciliation>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `reconciliations` (`id`,`expected`,`counted`,`note`,`actor`,`createdAt`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Reconciliation entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getExpected());
        statement.bindLong(3, entity.getCounted());
        statement.bindString(4, entity.getNote());
        statement.bindString(5, entity.getActor());
        statement.bindLong(6, entity.getCreatedAt());
      }
    };
    this.__updateAdapterOfCustomer = new EntityDeletionOrUpdateAdapter<Customer>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `customers` SET `id` = ?,`name` = ?,`phone` = ?,`address` = ?,`creditLimit` = ?,`status` = ?,`createdAt` = ?,`email` = ?,`notes` = ?,`accountNo` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Customer entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getPhone());
        statement.bindString(4, entity.getAddress());
        statement.bindLong(5, entity.getCreditLimit());
        statement.bindString(6, entity.getStatus());
        statement.bindLong(7, entity.getCreatedAt());
        statement.bindString(8, entity.getEmail());
        statement.bindString(9, entity.getNotes());
        statement.bindString(10, entity.getAccountNo());
        statement.bindString(11, entity.getId());
      }
    };
    this.__preparedStmtOfScrubFollowUps = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE follow_ups SET note='' WHERE customerId=?";
        return _query;
      }
    };
    this.__preparedStmtOfClearLedger = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM ledger";
        return _query;
      }
    };
    this.__preparedStmtOfClearCustomers = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM customers";
        return _query;
      }
    };
    this.__preparedStmtOfClearCash = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cash_entries";
        return _query;
      }
    };
    this.__preparedStmtOfClearFollow = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM follow_ups";
        return _query;
      }
    };
    this.__preparedStmtOfClearAudit = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM audit_log";
        return _query;
      }
    };
    this.__preparedStmtOfClearRecons = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM reconciliations";
        return _query;
      }
    };
  }

  @Override
  public Object addCustomer(final Customer c, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCustomer.insert(c);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insert(final Entry e, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfEntry.insertAndReturnId(e);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertCash(final CashEntry e, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfCashEntry.insertAndReturnId(e);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertFollowUp(final FollowUp f, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFollowUp.insert(f);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAudit(final AuditLog a, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAuditLog.insert(a);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object putCustomers(final List<Customer> l, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCustomer_1.insert(l);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object putLedger(final List<Entry> l, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfEntry_1.insert(l);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object putCash(final List<CashEntry> l, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCashEntry_1.insert(l);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object putFollow(final List<FollowUp> l, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFollowUp_1.insert(l);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object putAudit(final List<AuditLog> l, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAuditLog_1.insert(l);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertRecon(final Reconciliation r, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfReconciliation.insert(r);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object putRecons(final List<Reconciliation> l,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfReconciliation_1.insert(l);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateCustomer(final Customer c, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCustomer.handle(c);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object scrubFollowUps(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfScrubFollowUps.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfScrubFollowUps.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearLedger(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearLedger.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearLedger.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearCustomers(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearCustomers.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearCustomers.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearCash(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearCash.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearCash.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearFollow(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearFollow.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearFollow.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAudit(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAudit.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAudit.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearRecons(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearRecons.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearRecons.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object principalOf(final String id, final Continuation<? super Long> $completion) {
    final String _sql = "SELECT COALESCE(SUM(principalDelta),0) FROM ledger WHERE customerId=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object feeOf(final String id, final Continuation<? super Long> $completion) {
    final String _sql = "SELECT COALESCE(SUM(feeDelta),0) FROM ledger WHERE customerId=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object entry(final String id, final Continuation<? super Entry> $completion) {
    final String _sql = "SELECT * FROM ledger WHERE id=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Entry>() {
      @Override
      @Nullable
      public Entry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfIdemKey = CursorUtil.getColumnIndexOrThrow(_cursor, "idemKey");
          final int _cursorIndexOfCustomerId = CursorUtil.getColumnIndexOrThrow(_cursor, "customerId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfPrincipalDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "principalDelta");
          final int _cursorIndexOfFeeDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "feeDelta");
          final int _cursorIndexOfCashDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "cashDelta");
          final int _cursorIndexOfIncomeDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "incomeDelta");
          final int _cursorIndexOfLossDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "lossDelta");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReversalOf = CursorUtil.getColumnIndexOrThrow(_cursor, "reversalOf");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfPlanId = CursorUtil.getColumnIndexOrThrow(_cursor, "planId");
          final int _cursorIndexOfReceiptNo = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptNo");
          final int _cursorIndexOfReference = CursorUtil.getColumnIndexOrThrow(_cursor, "reference");
          final int _cursorIndexOfTax = CursorUtil.getColumnIndexOrThrow(_cursor, "tax");
          final int _cursorIndexOfItems = CursorUtil.getColumnIndexOrThrow(_cursor, "items");
          final Entry _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpIdemKey;
            _tmpIdemKey = _cursor.getString(_cursorIndexOfIdemKey);
            final String _tmpCustomerId;
            _tmpCustomerId = _cursor.getString(_cursorIndexOfCustomerId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpPrincipalDelta;
            _tmpPrincipalDelta = _cursor.getLong(_cursorIndexOfPrincipalDelta);
            final long _tmpFeeDelta;
            _tmpFeeDelta = _cursor.getLong(_cursorIndexOfFeeDelta);
            final long _tmpCashDelta;
            _tmpCashDelta = _cursor.getLong(_cursorIndexOfCashDelta);
            final long _tmpIncomeDelta;
            _tmpIncomeDelta = _cursor.getLong(_cursorIndexOfIncomeDelta);
            final long _tmpLossDelta;
            _tmpLossDelta = _cursor.getLong(_cursorIndexOfLossDelta);
            final Long _tmpDueDate;
            if (_cursor.isNull(_cursorIndexOfDueDate)) {
              _tmpDueDate = null;
            } else {
              _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReversalOf;
            if (_cursor.isNull(_cursorIndexOfReversalOf)) {
              _tmpReversalOf = null;
            } else {
              _tmpReversalOf = _cursor.getString(_cursorIndexOfReversalOf);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpPlanId;
            if (_cursor.isNull(_cursorIndexOfPlanId)) {
              _tmpPlanId = null;
            } else {
              _tmpPlanId = _cursor.getString(_cursorIndexOfPlanId);
            }
            final long _tmpReceiptNo;
            _tmpReceiptNo = _cursor.getLong(_cursorIndexOfReceiptNo);
            final String _tmpReference;
            _tmpReference = _cursor.getString(_cursorIndexOfReference);
            final long _tmpTax;
            _tmpTax = _cursor.getLong(_cursorIndexOfTax);
            final String _tmpItems;
            _tmpItems = _cursor.getString(_cursorIndexOfItems);
            _result = new Entry(_tmpId,_tmpIdemKey,_tmpCustomerId,_tmpType,_tmpPrincipalDelta,_tmpFeeDelta,_tmpCashDelta,_tmpIncomeDelta,_tmpLossDelta,_tmpDueDate,_tmpNote,_tmpReversalOf,_tmpCreatedAt,_tmpPlanId,_tmpReceiptNo,_tmpReference,_tmpTax,_tmpItems);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Entry>> entries(final String id) {
    final String _sql = "SELECT * FROM ledger WHERE customerId=? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"ledger"}, new Callable<List<Entry>>() {
      @Override
      @NonNull
      public List<Entry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfIdemKey = CursorUtil.getColumnIndexOrThrow(_cursor, "idemKey");
          final int _cursorIndexOfCustomerId = CursorUtil.getColumnIndexOrThrow(_cursor, "customerId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfPrincipalDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "principalDelta");
          final int _cursorIndexOfFeeDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "feeDelta");
          final int _cursorIndexOfCashDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "cashDelta");
          final int _cursorIndexOfIncomeDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "incomeDelta");
          final int _cursorIndexOfLossDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "lossDelta");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReversalOf = CursorUtil.getColumnIndexOrThrow(_cursor, "reversalOf");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfPlanId = CursorUtil.getColumnIndexOrThrow(_cursor, "planId");
          final int _cursorIndexOfReceiptNo = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptNo");
          final int _cursorIndexOfReference = CursorUtil.getColumnIndexOrThrow(_cursor, "reference");
          final int _cursorIndexOfTax = CursorUtil.getColumnIndexOrThrow(_cursor, "tax");
          final int _cursorIndexOfItems = CursorUtil.getColumnIndexOrThrow(_cursor, "items");
          final List<Entry> _result = new ArrayList<Entry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Entry _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpIdemKey;
            _tmpIdemKey = _cursor.getString(_cursorIndexOfIdemKey);
            final String _tmpCustomerId;
            _tmpCustomerId = _cursor.getString(_cursorIndexOfCustomerId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpPrincipalDelta;
            _tmpPrincipalDelta = _cursor.getLong(_cursorIndexOfPrincipalDelta);
            final long _tmpFeeDelta;
            _tmpFeeDelta = _cursor.getLong(_cursorIndexOfFeeDelta);
            final long _tmpCashDelta;
            _tmpCashDelta = _cursor.getLong(_cursorIndexOfCashDelta);
            final long _tmpIncomeDelta;
            _tmpIncomeDelta = _cursor.getLong(_cursorIndexOfIncomeDelta);
            final long _tmpLossDelta;
            _tmpLossDelta = _cursor.getLong(_cursorIndexOfLossDelta);
            final Long _tmpDueDate;
            if (_cursor.isNull(_cursorIndexOfDueDate)) {
              _tmpDueDate = null;
            } else {
              _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReversalOf;
            if (_cursor.isNull(_cursorIndexOfReversalOf)) {
              _tmpReversalOf = null;
            } else {
              _tmpReversalOf = _cursor.getString(_cursorIndexOfReversalOf);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpPlanId;
            if (_cursor.isNull(_cursorIndexOfPlanId)) {
              _tmpPlanId = null;
            } else {
              _tmpPlanId = _cursor.getString(_cursorIndexOfPlanId);
            }
            final long _tmpReceiptNo;
            _tmpReceiptNo = _cursor.getLong(_cursorIndexOfReceiptNo);
            final String _tmpReference;
            _tmpReference = _cursor.getString(_cursorIndexOfReference);
            final long _tmpTax;
            _tmpTax = _cursor.getLong(_cursorIndexOfTax);
            final String _tmpItems;
            _tmpItems = _cursor.getString(_cursorIndexOfItems);
            _item = new Entry(_tmpId,_tmpIdemKey,_tmpCustomerId,_tmpType,_tmpPrincipalDelta,_tmpFeeDelta,_tmpCashDelta,_tmpIncomeDelta,_tmpLossDelta,_tmpDueDate,_tmpNote,_tmpReversalOf,_tmpCreatedAt,_tmpPlanId,_tmpReceiptNo,_tmpReference,_tmpTax,_tmpItems);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<CustomerSummary>> summaries() {
    final String _sql = "SELECT c.*, COALESCE(SUM(l.principalDelta+l.feeDelta),0) AS balance,\n"
            + "        MIN(CASE WHEN l.type='CREDIT' THEN l.dueDate END) AS firstDue\n"
            + "        FROM customers c LEFT JOIN ledger l ON l.customerId=c.id WHERE c.status!='ARCHIVED' GROUP BY c.id ORDER BY c.name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"customers",
        "ledger"}, new Callable<List<CustomerSummary>>() {
      @Override
      @NonNull
      public List<CustomerSummary> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfAccountNo = CursorUtil.getColumnIndexOrThrow(_cursor, "accountNo");
          final int _cursorIndexOfBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "balance");
          final int _cursorIndexOfFirstDue = CursorUtil.getColumnIndexOrThrow(_cursor, "firstDue");
          final List<CustomerSummary> _result = new ArrayList<CustomerSummary>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CustomerSummary _item;
            final long _tmpBalance;
            _tmpBalance = _cursor.getLong(_cursorIndexOfBalance);
            final Long _tmpFirstDue;
            if (_cursor.isNull(_cursorIndexOfFirstDue)) {
              _tmpFirstDue = null;
            } else {
              _tmpFirstDue = _cursor.getLong(_cursorIndexOfFirstDue);
            }
            final Customer _tmpC;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final long _tmpCreditLimit;
            _tmpCreditLimit = _cursor.getLong(_cursorIndexOfCreditLimit);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpAccountNo;
            _tmpAccountNo = _cursor.getString(_cursorIndexOfAccountNo);
            _tmpC = new Customer(_tmpId,_tmpName,_tmpPhone,_tmpAddress,_tmpCreditLimit,_tmpStatus,_tmpCreatedAt,_tmpEmail,_tmpNotes,_tmpAccountNo);
            _item = new CustomerSummary(_tmpC,_tmpBalance,_tmpFirstDue);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Totals> totals() {
    final String _sql = "SELECT COALESCE(SUM(cashDelta),0) collected, COALESCE(SUM(incomeDelta),0) income,\n"
            + "        COALESCE(SUM(lossDelta),0) loss, COALESCE(SUM(principalDelta+feeDelta),0) receivables, COALESCE(SUM(principalDelta),0) principal,\n"
            + "        COALESCE(SUM(CASE WHEN type IN ('CREDIT','ADJUSTMENT') OR (type='REVERSAL' AND reversalOf IN (SELECT id FROM ledger WHERE type IN ('CREDIT','ADJUSTMENT'))) THEN principalDelta - tax ELSE 0 END),0) lent FROM ledger";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"ledger"}, new Callable<Totals>() {
      @Override
      @NonNull
      public Totals call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCollected = 0;
          final int _cursorIndexOfIncome = 1;
          final int _cursorIndexOfLoss = 2;
          final int _cursorIndexOfReceivables = 3;
          final int _cursorIndexOfPrincipal = 4;
          final int _cursorIndexOfLent = 5;
          final Totals _result;
          if (_cursor.moveToFirst()) {
            final long _tmpCollected;
            _tmpCollected = _cursor.getLong(_cursorIndexOfCollected);
            final long _tmpIncome;
            _tmpIncome = _cursor.getLong(_cursorIndexOfIncome);
            final long _tmpLoss;
            _tmpLoss = _cursor.getLong(_cursorIndexOfLoss);
            final long _tmpReceivables;
            _tmpReceivables = _cursor.getLong(_cursorIndexOfReceivables);
            final long _tmpPrincipal;
            _tmpPrincipal = _cursor.getLong(_cursorIndexOfPrincipal);
            final long _tmpLent;
            _tmpLent = _cursor.getLong(_cursorIndexOfLent);
            _result = new Totals(_tmpCollected,_tmpIncome,_tmpLoss,_tmpReceivables,_tmpPrincipal,_tmpLent);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object cashEntry(final String id, final Continuation<? super CashEntry> $completion) {
    final String _sql = "SELECT * FROM cash_entries WHERE id=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CashEntry>() {
      @Override
      @Nullable
      public CashEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfIdemKey = CursorUtil.getColumnIndexOrThrow(_cursor, "idemKey");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfCashDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "cashDelta");
          final int _cursorIndexOfExpenseDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "expenseDelta");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReversalOf = CursorUtil.getColumnIndexOrThrow(_cursor, "reversalOf");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final CashEntry _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpIdemKey;
            _tmpIdemKey = _cursor.getString(_cursorIndexOfIdemKey);
            final String _tmpKind;
            _tmpKind = _cursor.getString(_cursorIndexOfKind);
            final long _tmpCashDelta;
            _tmpCashDelta = _cursor.getLong(_cursorIndexOfCashDelta);
            final long _tmpExpenseDelta;
            _tmpExpenseDelta = _cursor.getLong(_cursorIndexOfExpenseDelta);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReversalOf;
            if (_cursor.isNull(_cursorIndexOfReversalOf)) {
              _tmpReversalOf = null;
            } else {
              _tmpReversalOf = _cursor.getString(_cursorIndexOfReversalOf);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new CashEntry(_tmpId,_tmpIdemKey,_tmpKind,_tmpCashDelta,_tmpExpenseDelta,_tmpCategory,_tmpNote,_tmpReversalOf,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CashEntry>> cashEntries() {
    final String _sql = "SELECT * FROM cash_entries ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"cash_entries"}, new Callable<List<CashEntry>>() {
      @Override
      @NonNull
      public List<CashEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfIdemKey = CursorUtil.getColumnIndexOrThrow(_cursor, "idemKey");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfCashDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "cashDelta");
          final int _cursorIndexOfExpenseDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "expenseDelta");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReversalOf = CursorUtil.getColumnIndexOrThrow(_cursor, "reversalOf");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<CashEntry> _result = new ArrayList<CashEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CashEntry _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpIdemKey;
            _tmpIdemKey = _cursor.getString(_cursorIndexOfIdemKey);
            final String _tmpKind;
            _tmpKind = _cursor.getString(_cursorIndexOfKind);
            final long _tmpCashDelta;
            _tmpCashDelta = _cursor.getLong(_cursorIndexOfCashDelta);
            final long _tmpExpenseDelta;
            _tmpExpenseDelta = _cursor.getLong(_cursorIndexOfExpenseDelta);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReversalOf;
            if (_cursor.isNull(_cursorIndexOfReversalOf)) {
              _tmpReversalOf = null;
            } else {
              _tmpReversalOf = _cursor.getString(_cursorIndexOfReversalOf);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new CashEntry(_tmpId,_tmpIdemKey,_tmpKind,_tmpCashDelta,_tmpExpenseDelta,_tmpCategory,_tmpNote,_tmpReversalOf,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<CashTotals> cashTotals() {
    final String _sql = "SELECT COALESCE(SUM(CASE WHEN kind IN ('CAPITAL_IN','CAPITAL_OUT') THEN cashDelta ELSE 0 END),0) capital,\n"
            + "        COALESCE(SUM(expenseDelta),0) expenses FROM cash_entries";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"cash_entries"}, new Callable<CashTotals>() {
      @Override
      @NonNull
      public CashTotals call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCapital = 0;
          final int _cursorIndexOfExpenses = 1;
          final CashTotals _result;
          if (_cursor.moveToFirst()) {
            final long _tmpCapital;
            _tmpCapital = _cursor.getLong(_cursorIndexOfCapital);
            final long _tmpExpenses;
            _tmpExpenses = _cursor.getLong(_cursorIndexOfExpenses);
            _result = new CashTotals(_tmpCapital,_tmpExpenses);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Entry>> allEntries() {
    final String _sql = "SELECT * FROM ledger";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"ledger"}, new Callable<List<Entry>>() {
      @Override
      @NonNull
      public List<Entry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfIdemKey = CursorUtil.getColumnIndexOrThrow(_cursor, "idemKey");
          final int _cursorIndexOfCustomerId = CursorUtil.getColumnIndexOrThrow(_cursor, "customerId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfPrincipalDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "principalDelta");
          final int _cursorIndexOfFeeDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "feeDelta");
          final int _cursorIndexOfCashDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "cashDelta");
          final int _cursorIndexOfIncomeDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "incomeDelta");
          final int _cursorIndexOfLossDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "lossDelta");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReversalOf = CursorUtil.getColumnIndexOrThrow(_cursor, "reversalOf");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfPlanId = CursorUtil.getColumnIndexOrThrow(_cursor, "planId");
          final int _cursorIndexOfReceiptNo = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptNo");
          final int _cursorIndexOfReference = CursorUtil.getColumnIndexOrThrow(_cursor, "reference");
          final int _cursorIndexOfTax = CursorUtil.getColumnIndexOrThrow(_cursor, "tax");
          final int _cursorIndexOfItems = CursorUtil.getColumnIndexOrThrow(_cursor, "items");
          final List<Entry> _result = new ArrayList<Entry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Entry _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpIdemKey;
            _tmpIdemKey = _cursor.getString(_cursorIndexOfIdemKey);
            final String _tmpCustomerId;
            _tmpCustomerId = _cursor.getString(_cursorIndexOfCustomerId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpPrincipalDelta;
            _tmpPrincipalDelta = _cursor.getLong(_cursorIndexOfPrincipalDelta);
            final long _tmpFeeDelta;
            _tmpFeeDelta = _cursor.getLong(_cursorIndexOfFeeDelta);
            final long _tmpCashDelta;
            _tmpCashDelta = _cursor.getLong(_cursorIndexOfCashDelta);
            final long _tmpIncomeDelta;
            _tmpIncomeDelta = _cursor.getLong(_cursorIndexOfIncomeDelta);
            final long _tmpLossDelta;
            _tmpLossDelta = _cursor.getLong(_cursorIndexOfLossDelta);
            final Long _tmpDueDate;
            if (_cursor.isNull(_cursorIndexOfDueDate)) {
              _tmpDueDate = null;
            } else {
              _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReversalOf;
            if (_cursor.isNull(_cursorIndexOfReversalOf)) {
              _tmpReversalOf = null;
            } else {
              _tmpReversalOf = _cursor.getString(_cursorIndexOfReversalOf);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpPlanId;
            if (_cursor.isNull(_cursorIndexOfPlanId)) {
              _tmpPlanId = null;
            } else {
              _tmpPlanId = _cursor.getString(_cursorIndexOfPlanId);
            }
            final long _tmpReceiptNo;
            _tmpReceiptNo = _cursor.getLong(_cursorIndexOfReceiptNo);
            final String _tmpReference;
            _tmpReference = _cursor.getString(_cursorIndexOfReference);
            final long _tmpTax;
            _tmpTax = _cursor.getLong(_cursorIndexOfTax);
            final String _tmpItems;
            _tmpItems = _cursor.getString(_cursorIndexOfItems);
            _item = new Entry(_tmpId,_tmpIdemKey,_tmpCustomerId,_tmpType,_tmpPrincipalDelta,_tmpFeeDelta,_tmpCashDelta,_tmpIncomeDelta,_tmpLossDelta,_tmpDueDate,_tmpNote,_tmpReversalOf,_tmpCreatedAt,_tmpPlanId,_tmpReceiptNo,_tmpReference,_tmpTax,_tmpItems);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Customer>> customers() {
    final String _sql = "SELECT * FROM customers WHERE status!='ARCHIVED'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"customers"}, new Callable<List<Customer>>() {
      @Override
      @NonNull
      public List<Customer> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfAccountNo = CursorUtil.getColumnIndexOrThrow(_cursor, "accountNo");
          final List<Customer> _result = new ArrayList<Customer>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Customer _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final long _tmpCreditLimit;
            _tmpCreditLimit = _cursor.getLong(_cursorIndexOfCreditLimit);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpAccountNo;
            _tmpAccountNo = _cursor.getString(_cursorIndexOfAccountNo);
            _item = new Customer(_tmpId,_tmpName,_tmpPhone,_tmpAddress,_tmpCreditLimit,_tmpStatus,_tmpCreatedAt,_tmpEmail,_tmpNotes,_tmpAccountNo);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object lastReceipt(final Continuation<? super Long> $completion) {
    final String _sql = "SELECT COALESCE(MAX(receiptNo),0) FROM ledger";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Customer>> archivedCustomers() {
    final String _sql = "SELECT * FROM customers WHERE status='ARCHIVED' ORDER BY name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"customers"}, new Callable<List<Customer>>() {
      @Override
      @NonNull
      public List<Customer> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfAccountNo = CursorUtil.getColumnIndexOrThrow(_cursor, "accountNo");
          final List<Customer> _result = new ArrayList<Customer>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Customer _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final long _tmpCreditLimit;
            _tmpCreditLimit = _cursor.getLong(_cursorIndexOfCreditLimit);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpAccountNo;
            _tmpAccountNo = _cursor.getString(_cursorIndexOfAccountNo);
            _item = new Customer(_tmpId,_tmpName,_tmpPhone,_tmpAddress,_tmpCreditLimit,_tmpStatus,_tmpCreatedAt,_tmpEmail,_tmpNotes,_tmpAccountNo);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object dumpCustomers(final Continuation<? super List<Customer>> $completion) {
    final String _sql = "SELECT * FROM customers";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Customer>>() {
      @Override
      @NonNull
      public List<Customer> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "phone");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfCreditLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "creditLimit");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfAccountNo = CursorUtil.getColumnIndexOrThrow(_cursor, "accountNo");
          final List<Customer> _result = new ArrayList<Customer>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Customer _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhone;
            _tmpPhone = _cursor.getString(_cursorIndexOfPhone);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final long _tmpCreditLimit;
            _tmpCreditLimit = _cursor.getLong(_cursorIndexOfCreditLimit);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpEmail;
            _tmpEmail = _cursor.getString(_cursorIndexOfEmail);
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final String _tmpAccountNo;
            _tmpAccountNo = _cursor.getString(_cursorIndexOfAccountNo);
            _item = new Customer(_tmpId,_tmpName,_tmpPhone,_tmpAddress,_tmpCreditLimit,_tmpStatus,_tmpCreatedAt,_tmpEmail,_tmpNotes,_tmpAccountNo);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object dumpLedger(final Continuation<? super List<Entry>> $completion) {
    final String _sql = "SELECT * FROM ledger";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Entry>>() {
      @Override
      @NonNull
      public List<Entry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfIdemKey = CursorUtil.getColumnIndexOrThrow(_cursor, "idemKey");
          final int _cursorIndexOfCustomerId = CursorUtil.getColumnIndexOrThrow(_cursor, "customerId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfPrincipalDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "principalDelta");
          final int _cursorIndexOfFeeDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "feeDelta");
          final int _cursorIndexOfCashDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "cashDelta");
          final int _cursorIndexOfIncomeDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "incomeDelta");
          final int _cursorIndexOfLossDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "lossDelta");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReversalOf = CursorUtil.getColumnIndexOrThrow(_cursor, "reversalOf");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfPlanId = CursorUtil.getColumnIndexOrThrow(_cursor, "planId");
          final int _cursorIndexOfReceiptNo = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptNo");
          final int _cursorIndexOfReference = CursorUtil.getColumnIndexOrThrow(_cursor, "reference");
          final int _cursorIndexOfTax = CursorUtil.getColumnIndexOrThrow(_cursor, "tax");
          final int _cursorIndexOfItems = CursorUtil.getColumnIndexOrThrow(_cursor, "items");
          final List<Entry> _result = new ArrayList<Entry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Entry _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpIdemKey;
            _tmpIdemKey = _cursor.getString(_cursorIndexOfIdemKey);
            final String _tmpCustomerId;
            _tmpCustomerId = _cursor.getString(_cursorIndexOfCustomerId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpPrincipalDelta;
            _tmpPrincipalDelta = _cursor.getLong(_cursorIndexOfPrincipalDelta);
            final long _tmpFeeDelta;
            _tmpFeeDelta = _cursor.getLong(_cursorIndexOfFeeDelta);
            final long _tmpCashDelta;
            _tmpCashDelta = _cursor.getLong(_cursorIndexOfCashDelta);
            final long _tmpIncomeDelta;
            _tmpIncomeDelta = _cursor.getLong(_cursorIndexOfIncomeDelta);
            final long _tmpLossDelta;
            _tmpLossDelta = _cursor.getLong(_cursorIndexOfLossDelta);
            final Long _tmpDueDate;
            if (_cursor.isNull(_cursorIndexOfDueDate)) {
              _tmpDueDate = null;
            } else {
              _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReversalOf;
            if (_cursor.isNull(_cursorIndexOfReversalOf)) {
              _tmpReversalOf = null;
            } else {
              _tmpReversalOf = _cursor.getString(_cursorIndexOfReversalOf);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpPlanId;
            if (_cursor.isNull(_cursorIndexOfPlanId)) {
              _tmpPlanId = null;
            } else {
              _tmpPlanId = _cursor.getString(_cursorIndexOfPlanId);
            }
            final long _tmpReceiptNo;
            _tmpReceiptNo = _cursor.getLong(_cursorIndexOfReceiptNo);
            final String _tmpReference;
            _tmpReference = _cursor.getString(_cursorIndexOfReference);
            final long _tmpTax;
            _tmpTax = _cursor.getLong(_cursorIndexOfTax);
            final String _tmpItems;
            _tmpItems = _cursor.getString(_cursorIndexOfItems);
            _item = new Entry(_tmpId,_tmpIdemKey,_tmpCustomerId,_tmpType,_tmpPrincipalDelta,_tmpFeeDelta,_tmpCashDelta,_tmpIncomeDelta,_tmpLossDelta,_tmpDueDate,_tmpNote,_tmpReversalOf,_tmpCreatedAt,_tmpPlanId,_tmpReceiptNo,_tmpReference,_tmpTax,_tmpItems);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object dumpCash(final Continuation<? super List<CashEntry>> $completion) {
    final String _sql = "SELECT * FROM cash_entries";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CashEntry>>() {
      @Override
      @NonNull
      public List<CashEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfIdemKey = CursorUtil.getColumnIndexOrThrow(_cursor, "idemKey");
          final int _cursorIndexOfKind = CursorUtil.getColumnIndexOrThrow(_cursor, "kind");
          final int _cursorIndexOfCashDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "cashDelta");
          final int _cursorIndexOfExpenseDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "expenseDelta");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReversalOf = CursorUtil.getColumnIndexOrThrow(_cursor, "reversalOf");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<CashEntry> _result = new ArrayList<CashEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CashEntry _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpIdemKey;
            _tmpIdemKey = _cursor.getString(_cursorIndexOfIdemKey);
            final String _tmpKind;
            _tmpKind = _cursor.getString(_cursorIndexOfKind);
            final long _tmpCashDelta;
            _tmpCashDelta = _cursor.getLong(_cursorIndexOfCashDelta);
            final long _tmpExpenseDelta;
            _tmpExpenseDelta = _cursor.getLong(_cursorIndexOfExpenseDelta);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReversalOf;
            if (_cursor.isNull(_cursorIndexOfReversalOf)) {
              _tmpReversalOf = null;
            } else {
              _tmpReversalOf = _cursor.getString(_cursorIndexOfReversalOf);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new CashEntry(_tmpId,_tmpIdemKey,_tmpKind,_tmpCashDelta,_tmpExpenseDelta,_tmpCategory,_tmpNote,_tmpReversalOf,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object dumpFollow(final Continuation<? super List<FollowUp>> $completion) {
    final String _sql = "SELECT * FROM follow_ups";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<FollowUp>>() {
      @Override
      @NonNull
      public List<FollowUp> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCustomerId = CursorUtil.getColumnIndexOrThrow(_cursor, "customerId");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfPromiseDate = CursorUtil.getColumnIndexOrThrow(_cursor, "promiseDate");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<FollowUp> _result = new ArrayList<FollowUp>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FollowUp _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpCustomerId;
            _tmpCustomerId = _cursor.getString(_cursorIndexOfCustomerId);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final Long _tmpPromiseDate;
            if (_cursor.isNull(_cursorIndexOfPromiseDate)) {
              _tmpPromiseDate = null;
            } else {
              _tmpPromiseDate = _cursor.getLong(_cursorIndexOfPromiseDate);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new FollowUp(_tmpId,_tmpCustomerId,_tmpNote,_tmpPromiseDate,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object dumpAudit(final Continuation<? super List<AuditLog>> $completion) {
    final String _sql = "SELECT * FROM audit_log";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AuditLog>>() {
      @Override
      @NonNull
      public List<AuditLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAction = CursorUtil.getColumnIndexOrThrow(_cursor, "action");
          final int _cursorIndexOfDetail = CursorUtil.getColumnIndexOrThrow(_cursor, "detail");
          final int _cursorIndexOfActor = CursorUtil.getColumnIndexOrThrow(_cursor, "actor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AuditLog> _result = new ArrayList<AuditLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AuditLog _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpAction;
            _tmpAction = _cursor.getString(_cursorIndexOfAction);
            final String _tmpDetail;
            _tmpDetail = _cursor.getString(_cursorIndexOfDetail);
            final String _tmpActor;
            _tmpActor = _cursor.getString(_cursorIndexOfActor);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AuditLog(_tmpId,_tmpAction,_tmpDetail,_tmpActor,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Reconciliation>> recons() {
    final String _sql = "SELECT * FROM reconciliations ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"reconciliations"}, new Callable<List<Reconciliation>>() {
      @Override
      @NonNull
      public List<Reconciliation> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfExpected = CursorUtil.getColumnIndexOrThrow(_cursor, "expected");
          final int _cursorIndexOfCounted = CursorUtil.getColumnIndexOrThrow(_cursor, "counted");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfActor = CursorUtil.getColumnIndexOrThrow(_cursor, "actor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Reconciliation> _result = new ArrayList<Reconciliation>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Reconciliation _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpExpected;
            _tmpExpected = _cursor.getLong(_cursorIndexOfExpected);
            final long _tmpCounted;
            _tmpCounted = _cursor.getLong(_cursorIndexOfCounted);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpActor;
            _tmpActor = _cursor.getString(_cursorIndexOfActor);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Reconciliation(_tmpId,_tmpExpected,_tmpCounted,_tmpNote,_tmpActor,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object dumpRecons(final Continuation<? super List<Reconciliation>> $completion) {
    final String _sql = "SELECT * FROM reconciliations";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Reconciliation>>() {
      @Override
      @NonNull
      public List<Reconciliation> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfExpected = CursorUtil.getColumnIndexOrThrow(_cursor, "expected");
          final int _cursorIndexOfCounted = CursorUtil.getColumnIndexOrThrow(_cursor, "counted");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfActor = CursorUtil.getColumnIndexOrThrow(_cursor, "actor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Reconciliation> _result = new ArrayList<Reconciliation>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Reconciliation _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpExpected;
            _tmpExpected = _cursor.getLong(_cursorIndexOfExpected);
            final long _tmpCounted;
            _tmpCounted = _cursor.getLong(_cursorIndexOfCounted);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpActor;
            _tmpActor = _cursor.getString(_cursorIndexOfActor);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Reconciliation(_tmpId,_tmpExpected,_tmpCounted,_tmpNote,_tmpActor,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object refCount(final String r, final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(DISTINCT COALESCE(planId, id)) FROM ledger WHERE type='CREDIT' AND reference=? AND id NOT IN (SELECT reversalOf FROM ledger WHERE reversalOf IS NOT NULL)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, r);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object cashSum(final Continuation<? super Long> $completion) {
    final String _sql = "SELECT COALESCE(SUM(cashDelta),0) FROM ledger";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object lentSum(final Continuation<? super Long> $completion) {
    final String _sql = "SELECT COALESCE(SUM(CASE WHEN type IN ('CREDIT','ADJUSTMENT') OR (type='REVERSAL' AND reversalOf IN (SELECT id FROM ledger WHERE type IN ('CREDIT','ADJUSTMENT'))) THEN principalDelta - tax ELSE 0 END),0) FROM ledger";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object capitalSum(final Continuation<? super Long> $completion) {
    final String _sql = "SELECT COALESCE(SUM(CASE WHEN kind IN ('CAPITAL_IN','CAPITAL_OUT') THEN cashDelta ELSE 0 END),0) FROM cash_entries";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object expenseSum(final Continuation<? super Long> $completion) {
    final String _sql = "SELECT COALESCE(SUM(expenseDelta),0) FROM cash_entries";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object planEntries(final String p, final Continuation<? super List<Entry>> $completion) {
    final String _sql = "SELECT * FROM ledger WHERE planId=? AND type='CREDIT'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, p);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Entry>>() {
      @Override
      @NonNull
      public List<Entry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfIdemKey = CursorUtil.getColumnIndexOrThrow(_cursor, "idemKey");
          final int _cursorIndexOfCustomerId = CursorUtil.getColumnIndexOrThrow(_cursor, "customerId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfPrincipalDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "principalDelta");
          final int _cursorIndexOfFeeDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "feeDelta");
          final int _cursorIndexOfCashDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "cashDelta");
          final int _cursorIndexOfIncomeDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "incomeDelta");
          final int _cursorIndexOfLossDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "lossDelta");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReversalOf = CursorUtil.getColumnIndexOrThrow(_cursor, "reversalOf");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfPlanId = CursorUtil.getColumnIndexOrThrow(_cursor, "planId");
          final int _cursorIndexOfReceiptNo = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptNo");
          final int _cursorIndexOfReference = CursorUtil.getColumnIndexOrThrow(_cursor, "reference");
          final int _cursorIndexOfTax = CursorUtil.getColumnIndexOrThrow(_cursor, "tax");
          final int _cursorIndexOfItems = CursorUtil.getColumnIndexOrThrow(_cursor, "items");
          final List<Entry> _result = new ArrayList<Entry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Entry _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpIdemKey;
            _tmpIdemKey = _cursor.getString(_cursorIndexOfIdemKey);
            final String _tmpCustomerId;
            _tmpCustomerId = _cursor.getString(_cursorIndexOfCustomerId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpPrincipalDelta;
            _tmpPrincipalDelta = _cursor.getLong(_cursorIndexOfPrincipalDelta);
            final long _tmpFeeDelta;
            _tmpFeeDelta = _cursor.getLong(_cursorIndexOfFeeDelta);
            final long _tmpCashDelta;
            _tmpCashDelta = _cursor.getLong(_cursorIndexOfCashDelta);
            final long _tmpIncomeDelta;
            _tmpIncomeDelta = _cursor.getLong(_cursorIndexOfIncomeDelta);
            final long _tmpLossDelta;
            _tmpLossDelta = _cursor.getLong(_cursorIndexOfLossDelta);
            final Long _tmpDueDate;
            if (_cursor.isNull(_cursorIndexOfDueDate)) {
              _tmpDueDate = null;
            } else {
              _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReversalOf;
            if (_cursor.isNull(_cursorIndexOfReversalOf)) {
              _tmpReversalOf = null;
            } else {
              _tmpReversalOf = _cursor.getString(_cursorIndexOfReversalOf);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpPlanId;
            if (_cursor.isNull(_cursorIndexOfPlanId)) {
              _tmpPlanId = null;
            } else {
              _tmpPlanId = _cursor.getString(_cursorIndexOfPlanId);
            }
            final long _tmpReceiptNo;
            _tmpReceiptNo = _cursor.getLong(_cursorIndexOfReceiptNo);
            final String _tmpReference;
            _tmpReference = _cursor.getString(_cursorIndexOfReference);
            final long _tmpTax;
            _tmpTax = _cursor.getLong(_cursorIndexOfTax);
            final String _tmpItems;
            _tmpItems = _cursor.getString(_cursorIndexOfItems);
            _item = new Entry(_tmpId,_tmpIdemKey,_tmpCustomerId,_tmpType,_tmpPrincipalDelta,_tmpFeeDelta,_tmpCashDelta,_tmpIncomeDelta,_tmpLossDelta,_tmpDueDate,_tmpNote,_tmpReversalOf,_tmpCreatedAt,_tmpPlanId,_tmpReceiptNo,_tmpReference,_tmpTax,_tmpItems);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object entriesOf(final String id, final Continuation<? super List<Entry>> $completion) {
    final String _sql = "SELECT * FROM ledger WHERE customerId=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Entry>>() {
      @Override
      @NonNull
      public List<Entry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfIdemKey = CursorUtil.getColumnIndexOrThrow(_cursor, "idemKey");
          final int _cursorIndexOfCustomerId = CursorUtil.getColumnIndexOrThrow(_cursor, "customerId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfPrincipalDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "principalDelta");
          final int _cursorIndexOfFeeDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "feeDelta");
          final int _cursorIndexOfCashDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "cashDelta");
          final int _cursorIndexOfIncomeDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "incomeDelta");
          final int _cursorIndexOfLossDelta = CursorUtil.getColumnIndexOrThrow(_cursor, "lossDelta");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "dueDate");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfReversalOf = CursorUtil.getColumnIndexOrThrow(_cursor, "reversalOf");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfPlanId = CursorUtil.getColumnIndexOrThrow(_cursor, "planId");
          final int _cursorIndexOfReceiptNo = CursorUtil.getColumnIndexOrThrow(_cursor, "receiptNo");
          final int _cursorIndexOfReference = CursorUtil.getColumnIndexOrThrow(_cursor, "reference");
          final int _cursorIndexOfTax = CursorUtil.getColumnIndexOrThrow(_cursor, "tax");
          final int _cursorIndexOfItems = CursorUtil.getColumnIndexOrThrow(_cursor, "items");
          final List<Entry> _result = new ArrayList<Entry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Entry _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpIdemKey;
            _tmpIdemKey = _cursor.getString(_cursorIndexOfIdemKey);
            final String _tmpCustomerId;
            _tmpCustomerId = _cursor.getString(_cursorIndexOfCustomerId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpPrincipalDelta;
            _tmpPrincipalDelta = _cursor.getLong(_cursorIndexOfPrincipalDelta);
            final long _tmpFeeDelta;
            _tmpFeeDelta = _cursor.getLong(_cursorIndexOfFeeDelta);
            final long _tmpCashDelta;
            _tmpCashDelta = _cursor.getLong(_cursorIndexOfCashDelta);
            final long _tmpIncomeDelta;
            _tmpIncomeDelta = _cursor.getLong(_cursorIndexOfIncomeDelta);
            final long _tmpLossDelta;
            _tmpLossDelta = _cursor.getLong(_cursorIndexOfLossDelta);
            final Long _tmpDueDate;
            if (_cursor.isNull(_cursorIndexOfDueDate)) {
              _tmpDueDate = null;
            } else {
              _tmpDueDate = _cursor.getLong(_cursorIndexOfDueDate);
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpReversalOf;
            if (_cursor.isNull(_cursorIndexOfReversalOf)) {
              _tmpReversalOf = null;
            } else {
              _tmpReversalOf = _cursor.getString(_cursorIndexOfReversalOf);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpPlanId;
            if (_cursor.isNull(_cursorIndexOfPlanId)) {
              _tmpPlanId = null;
            } else {
              _tmpPlanId = _cursor.getString(_cursorIndexOfPlanId);
            }
            final long _tmpReceiptNo;
            _tmpReceiptNo = _cursor.getLong(_cursorIndexOfReceiptNo);
            final String _tmpReference;
            _tmpReference = _cursor.getString(_cursorIndexOfReference);
            final long _tmpTax;
            _tmpTax = _cursor.getLong(_cursorIndexOfTax);
            final String _tmpItems;
            _tmpItems = _cursor.getString(_cursorIndexOfItems);
            _item = new Entry(_tmpId,_tmpIdemKey,_tmpCustomerId,_tmpType,_tmpPrincipalDelta,_tmpFeeDelta,_tmpCashDelta,_tmpIncomeDelta,_tmpLossDelta,_tmpDueDate,_tmpNote,_tmpReversalOf,_tmpCreatedAt,_tmpPlanId,_tmpReceiptNo,_tmpReference,_tmpTax,_tmpItems);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AuditLog>> auditLog() {
    final String _sql = "SELECT * FROM audit_log ORDER BY createdAt DESC LIMIT 500";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"audit_log"}, new Callable<List<AuditLog>>() {
      @Override
      @NonNull
      public List<AuditLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAction = CursorUtil.getColumnIndexOrThrow(_cursor, "action");
          final int _cursorIndexOfDetail = CursorUtil.getColumnIndexOrThrow(_cursor, "detail");
          final int _cursorIndexOfActor = CursorUtil.getColumnIndexOrThrow(_cursor, "actor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AuditLog> _result = new ArrayList<AuditLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AuditLog _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpAction;
            _tmpAction = _cursor.getString(_cursorIndexOfAction);
            final String _tmpDetail;
            _tmpDetail = _cursor.getString(_cursorIndexOfDetail);
            final String _tmpActor;
            _tmpActor = _cursor.getString(_cursorIndexOfActor);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AuditLog(_tmpId,_tmpAction,_tmpDetail,_tmpActor,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<FollowUp>> followUps() {
    final String _sql = "SELECT * FROM follow_ups ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"follow_ups"}, new Callable<List<FollowUp>>() {
      @Override
      @NonNull
      public List<FollowUp> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCustomerId = CursorUtil.getColumnIndexOrThrow(_cursor, "customerId");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfPromiseDate = CursorUtil.getColumnIndexOrThrow(_cursor, "promiseDate");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<FollowUp> _result = new ArrayList<FollowUp>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FollowUp _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpCustomerId;
            _tmpCustomerId = _cursor.getString(_cursorIndexOfCustomerId);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final Long _tmpPromiseDate;
            if (_cursor.isNull(_cursorIndexOfPromiseDate)) {
              _tmpPromiseDate = null;
            } else {
              _tmpPromiseDate = _cursor.getLong(_cursorIndexOfPromiseDate);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new FollowUp(_tmpId,_tmpCustomerId,_tmpNote,_tmpPromiseDate,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
