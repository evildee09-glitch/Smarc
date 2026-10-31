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
public final class UserDao_Impl implements UserDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<User> __insertionAdapterOfUser;

  private final EntityInsertionAdapter<User> __insertionAdapterOfUser_1;

  private final EntityDeletionOrUpdateAdapter<User> __updateAdapterOfUser;

  private final SharedSQLiteStatement __preparedStmtOfClear;

  public UserDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUser = new EntityInsertionAdapter<User>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `users` (`id`,`username`,`displayName`,`role`,`pwHash`,`pwSalt`,`recHash`,`recSalt`,`active`,`mustChange`,`bio`,`createdAt`,`totp`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final User entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUsername());
        statement.bindString(3, entity.getDisplayName());
        statement.bindString(4, entity.getRole());
        statement.bindString(5, entity.getPwHash());
        statement.bindString(6, entity.getPwSalt());
        statement.bindString(7, entity.getRecHash());
        statement.bindString(8, entity.getRecSalt());
        final int _tmp = entity.getActive() ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.getMustChange() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        final int _tmp_2 = entity.getBio() ? 1 : 0;
        statement.bindLong(11, _tmp_2);
        statement.bindLong(12, entity.getCreatedAt());
        statement.bindString(13, entity.getTotp());
      }
    };
    this.__insertionAdapterOfUser_1 = new EntityInsertionAdapter<User>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `users` (`id`,`username`,`displayName`,`role`,`pwHash`,`pwSalt`,`recHash`,`recSalt`,`active`,`mustChange`,`bio`,`createdAt`,`totp`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final User entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUsername());
        statement.bindString(3, entity.getDisplayName());
        statement.bindString(4, entity.getRole());
        statement.bindString(5, entity.getPwHash());
        statement.bindString(6, entity.getPwSalt());
        statement.bindString(7, entity.getRecHash());
        statement.bindString(8, entity.getRecSalt());
        final int _tmp = entity.getActive() ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.getMustChange() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        final int _tmp_2 = entity.getBio() ? 1 : 0;
        statement.bindLong(11, _tmp_2);
        statement.bindLong(12, entity.getCreatedAt());
        statement.bindString(13, entity.getTotp());
      }
    };
    this.__updateAdapterOfUser = new EntityDeletionOrUpdateAdapter<User>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `users` SET `id` = ?,`username` = ?,`displayName` = ?,`role` = ?,`pwHash` = ?,`pwSalt` = ?,`recHash` = ?,`recSalt` = ?,`active` = ?,`mustChange` = ?,`bio` = ?,`createdAt` = ?,`totp` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final User entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUsername());
        statement.bindString(3, entity.getDisplayName());
        statement.bindString(4, entity.getRole());
        statement.bindString(5, entity.getPwHash());
        statement.bindString(6, entity.getPwSalt());
        statement.bindString(7, entity.getRecHash());
        statement.bindString(8, entity.getRecSalt());
        final int _tmp = entity.getActive() ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.getMustChange() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        final int _tmp_2 = entity.getBio() ? 1 : 0;
        statement.bindLong(11, _tmp_2);
        statement.bindLong(12, entity.getCreatedAt());
        statement.bindString(13, entity.getTotp());
        statement.bindString(14, entity.getId());
      }
    };
    this.__preparedStmtOfClear = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM users";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final User u, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUser.insert(u);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object putAll(final List<User> l, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUser_1.insert(l);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final User u, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfUser.handle(u);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clear(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClear.acquire();
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
          __preparedStmtOfClear.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object byName(final String n, final Continuation<? super User> $completion) {
    final String _sql = "SELECT * FROM users WHERE username=? COLLATE NOCASE";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, n);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<User>() {
      @Override
      @Nullable
      public User call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfPwHash = CursorUtil.getColumnIndexOrThrow(_cursor, "pwHash");
          final int _cursorIndexOfPwSalt = CursorUtil.getColumnIndexOrThrow(_cursor, "pwSalt");
          final int _cursorIndexOfRecHash = CursorUtil.getColumnIndexOrThrow(_cursor, "recHash");
          final int _cursorIndexOfRecSalt = CursorUtil.getColumnIndexOrThrow(_cursor, "recSalt");
          final int _cursorIndexOfActive = CursorUtil.getColumnIndexOrThrow(_cursor, "active");
          final int _cursorIndexOfMustChange = CursorUtil.getColumnIndexOrThrow(_cursor, "mustChange");
          final int _cursorIndexOfBio = CursorUtil.getColumnIndexOrThrow(_cursor, "bio");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTotp = CursorUtil.getColumnIndexOrThrow(_cursor, "totp");
          final User _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpPwHash;
            _tmpPwHash = _cursor.getString(_cursorIndexOfPwHash);
            final String _tmpPwSalt;
            _tmpPwSalt = _cursor.getString(_cursorIndexOfPwSalt);
            final String _tmpRecHash;
            _tmpRecHash = _cursor.getString(_cursorIndexOfRecHash);
            final String _tmpRecSalt;
            _tmpRecSalt = _cursor.getString(_cursorIndexOfRecSalt);
            final boolean _tmpActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfActive);
            _tmpActive = _tmp != 0;
            final boolean _tmpMustChange;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfMustChange);
            _tmpMustChange = _tmp_1 != 0;
            final boolean _tmpBio;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfBio);
            _tmpBio = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpTotp;
            _tmpTotp = _cursor.getString(_cursorIndexOfTotp);
            _result = new User(_tmpId,_tmpUsername,_tmpDisplayName,_tmpRole,_tmpPwHash,_tmpPwSalt,_tmpRecHash,_tmpRecSalt,_tmpActive,_tmpMustChange,_tmpBio,_tmpCreatedAt,_tmpTotp);
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
  public Object byId(final String id, final Continuation<? super User> $completion) {
    final String _sql = "SELECT * FROM users WHERE id=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<User>() {
      @Override
      @Nullable
      public User call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfPwHash = CursorUtil.getColumnIndexOrThrow(_cursor, "pwHash");
          final int _cursorIndexOfPwSalt = CursorUtil.getColumnIndexOrThrow(_cursor, "pwSalt");
          final int _cursorIndexOfRecHash = CursorUtil.getColumnIndexOrThrow(_cursor, "recHash");
          final int _cursorIndexOfRecSalt = CursorUtil.getColumnIndexOrThrow(_cursor, "recSalt");
          final int _cursorIndexOfActive = CursorUtil.getColumnIndexOrThrow(_cursor, "active");
          final int _cursorIndexOfMustChange = CursorUtil.getColumnIndexOrThrow(_cursor, "mustChange");
          final int _cursorIndexOfBio = CursorUtil.getColumnIndexOrThrow(_cursor, "bio");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTotp = CursorUtil.getColumnIndexOrThrow(_cursor, "totp");
          final User _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpPwHash;
            _tmpPwHash = _cursor.getString(_cursorIndexOfPwHash);
            final String _tmpPwSalt;
            _tmpPwSalt = _cursor.getString(_cursorIndexOfPwSalt);
            final String _tmpRecHash;
            _tmpRecHash = _cursor.getString(_cursorIndexOfRecHash);
            final String _tmpRecSalt;
            _tmpRecSalt = _cursor.getString(_cursorIndexOfRecSalt);
            final boolean _tmpActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfActive);
            _tmpActive = _tmp != 0;
            final boolean _tmpMustChange;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfMustChange);
            _tmpMustChange = _tmp_1 != 0;
            final boolean _tmpBio;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfBio);
            _tmpBio = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpTotp;
            _tmpTotp = _cursor.getString(_cursorIndexOfTotp);
            _result = new User(_tmpId,_tmpUsername,_tmpDisplayName,_tmpRole,_tmpPwHash,_tmpPwSalt,_tmpRecHash,_tmpRecSalt,_tmpActive,_tmpMustChange,_tmpBio,_tmpCreatedAt,_tmpTotp);
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
  public Flow<List<User>> all() {
    final String _sql = "SELECT * FROM users ORDER BY createdAt";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"users"}, new Callable<List<User>>() {
      @Override
      @NonNull
      public List<User> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfPwHash = CursorUtil.getColumnIndexOrThrow(_cursor, "pwHash");
          final int _cursorIndexOfPwSalt = CursorUtil.getColumnIndexOrThrow(_cursor, "pwSalt");
          final int _cursorIndexOfRecHash = CursorUtil.getColumnIndexOrThrow(_cursor, "recHash");
          final int _cursorIndexOfRecSalt = CursorUtil.getColumnIndexOrThrow(_cursor, "recSalt");
          final int _cursorIndexOfActive = CursorUtil.getColumnIndexOrThrow(_cursor, "active");
          final int _cursorIndexOfMustChange = CursorUtil.getColumnIndexOrThrow(_cursor, "mustChange");
          final int _cursorIndexOfBio = CursorUtil.getColumnIndexOrThrow(_cursor, "bio");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTotp = CursorUtil.getColumnIndexOrThrow(_cursor, "totp");
          final List<User> _result = new ArrayList<User>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final User _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpPwHash;
            _tmpPwHash = _cursor.getString(_cursorIndexOfPwHash);
            final String _tmpPwSalt;
            _tmpPwSalt = _cursor.getString(_cursorIndexOfPwSalt);
            final String _tmpRecHash;
            _tmpRecHash = _cursor.getString(_cursorIndexOfRecHash);
            final String _tmpRecSalt;
            _tmpRecSalt = _cursor.getString(_cursorIndexOfRecSalt);
            final boolean _tmpActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfActive);
            _tmpActive = _tmp != 0;
            final boolean _tmpMustChange;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfMustChange);
            _tmpMustChange = _tmp_1 != 0;
            final boolean _tmpBio;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfBio);
            _tmpBio = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpTotp;
            _tmpTotp = _cursor.getString(_cursorIndexOfTotp);
            _item = new User(_tmpId,_tmpUsername,_tmpDisplayName,_tmpRole,_tmpPwHash,_tmpPwSalt,_tmpRecHash,_tmpRecSalt,_tmpActive,_tmpMustChange,_tmpBio,_tmpCreatedAt,_tmpTotp);
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
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM users";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
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
  public Object activeOwners(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM users WHERE role='OWNER' AND active=1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
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
  public Object dump(final Continuation<? super List<User>> $completion) {
    final String _sql = "SELECT * FROM users";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<User>>() {
      @Override
      @NonNull
      public List<User> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfPwHash = CursorUtil.getColumnIndexOrThrow(_cursor, "pwHash");
          final int _cursorIndexOfPwSalt = CursorUtil.getColumnIndexOrThrow(_cursor, "pwSalt");
          final int _cursorIndexOfRecHash = CursorUtil.getColumnIndexOrThrow(_cursor, "recHash");
          final int _cursorIndexOfRecSalt = CursorUtil.getColumnIndexOrThrow(_cursor, "recSalt");
          final int _cursorIndexOfActive = CursorUtil.getColumnIndexOrThrow(_cursor, "active");
          final int _cursorIndexOfMustChange = CursorUtil.getColumnIndexOrThrow(_cursor, "mustChange");
          final int _cursorIndexOfBio = CursorUtil.getColumnIndexOrThrow(_cursor, "bio");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTotp = CursorUtil.getColumnIndexOrThrow(_cursor, "totp");
          final List<User> _result = new ArrayList<User>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final User _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpPwHash;
            _tmpPwHash = _cursor.getString(_cursorIndexOfPwHash);
            final String _tmpPwSalt;
            _tmpPwSalt = _cursor.getString(_cursorIndexOfPwSalt);
            final String _tmpRecHash;
            _tmpRecHash = _cursor.getString(_cursorIndexOfRecHash);
            final String _tmpRecSalt;
            _tmpRecSalt = _cursor.getString(_cursorIndexOfRecSalt);
            final boolean _tmpActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfActive);
            _tmpActive = _tmp != 0;
            final boolean _tmpMustChange;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfMustChange);
            _tmpMustChange = _tmp_1 != 0;
            final boolean _tmpBio;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfBio);
            _tmpBio = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final String _tmpTotp;
            _tmpTotp = _cursor.getString(_cursorIndexOfTotp);
            _item = new User(_tmpId,_tmpUsername,_tmpDisplayName,_tmpRole,_tmpPwHash,_tmpPwSalt,_tmpRecHash,_tmpRecSalt,_tmpActive,_tmpMustChange,_tmpBio,_tmpCreatedAt,_tmpTotp);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
