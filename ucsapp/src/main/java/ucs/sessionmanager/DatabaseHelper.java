package ucs.sessionmanager;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.util.List;

/**
 * Helper class for creating and managing an SQLite database on Android. It is also used for obtaining a database
 * connection directly in the session manager
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String DATABASE_NAME = "session_manager";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME,null,DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, Session.class);
            TableUtils.createTable(connectionSource, OnGoingAttribute.class);
        }catch (SQLException | java.sql.SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            if(checkTableExist(database,"sessions")){
                TableUtils.dropTable(connectionSource,Session.class,false);
            }
            if(checkTableExist(database,"on_going_attributes")){
                TableUtils.dropTable(connectionSource,OnGoingAttribute.class,false);
            }

            onCreate(database,connectionSource);
        }catch (SQLException | java.sql.SQLException e){
            e.printStackTrace();
        }
    }

    private boolean checkTableExist(SQLiteDatabase database, String tableName){
        Cursor c = null;
        boolean tableExist = false;
        try {
            c = database.query(tableName, null,null,null,null,null,null);
            tableExist = true;
            c.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return tableExist;
    }

    public void resetDatabase(ConnectionSource connectionSource) throws java.sql.SQLException {
        SQLiteDatabase database = this.getReadableDatabase();
        System.out.println("ResettingDatabase");
        if(checkTableExist(database,"sessions")){
            TableUtils.dropTable(connectionSource,Session.class,false);
        }
        if(checkTableExist(database,"on_going_attributes")){
            TableUtils.dropTable(connectionSource,OnGoingAttribute.class,false);
        }

        onCreate(database,connectionSource);
    }

    /**
     * For debugging purposes to keep track of Session DB content easily
     * @param connectionSource
     * @throws java.sql.SQLException
     */
    public void getSessions(ConnectionSource connectionSource) throws java.sql.SQLException {
        Dao<Session, String> sessionDao = DaoManager.createDao( connectionSource, Session.class );
        List<Session> sessionList = sessionDao.queryForAll();
        for(Session session: sessionList){
            System.out.println(session.toString());
        }
    }
}
