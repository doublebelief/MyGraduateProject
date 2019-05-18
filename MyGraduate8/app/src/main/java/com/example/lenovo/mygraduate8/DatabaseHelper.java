package com.example.lenovo.mygraduate8;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;


public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;
    private static final String HELLO="HELLO";
    //三个不同参数的构造函数
    //带全部参数的构造函数，此构造函数必不可少
    public DatabaseHelper(Context context, String name,CursorFactory factory,
                          int version) {
        super(context, name, factory, version);

    }
    //带两个参数的构造函数，调用的其实是带三个参数的构造函数
    public DatabaseHelper(Context context, String name){
        this(context,name,VERSION);
    }
    //带三个参数的构造函数，调用的是带所有参数的构造函数
    public DatabaseHelper(Context context,String name,int version){
        this(context, name,null,version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.i(HELLO,"create a Database");
        //创建数据库sql语句
        String sql = "create table user(name varchar(255),facePath varchar(255),voicePath varchar(255))";
        //执行创建数据库操作
        sqLiteDatabase.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.i(HELLO,"update a Database");
    }
}
