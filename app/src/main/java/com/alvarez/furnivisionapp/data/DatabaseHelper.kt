package com.alvarez.furnivisionapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "MyDatabase"

        // Table names
        private const val TABLE_USERS = "Users"
        private const val TABLE_SHOPS = "Shops"
        private const val TABLE_FURNITURE = "Furniture"
        private const val TABLE_FAVORITE_FURNITURE = "FavoriteFurniture"

        // Common column names
        private const val COLUMN_ID = "_id"

        // Users table columns
        private const val COLUMN_USER_NAME = "name"
        private const val COLUMN_USER_EMAIL = "email"
        private const val COLUMN_USER_PASSWORD = "password"

        // Shops table columns
        private const val COLUMN_SHOP_NAME = "name"
        private const val COLUMN_SHOP_LOCATION = "location"

        // Furniture table columns
        private const val COLUMN_FURNITURE_NAME = "name"
        private const val COLUMN_FURNITURE_IMAGES = "images"
        private const val COLUMN_FURNITURE_CATEGORY = "category"
        private const val COLUMN_FURNITURE_PRICE = "price"
        private const val COLUMN_FURNITURE_RATING = "rating"
        private const val COLUMN_FURNITURE_SOLD_COUNT = "sold_count"
        private const val COLUMN_FURNITURE_COLORS = "colors"
        private const val COLUMN_FURNITURE_DIMENSIONS = "dimensions"
        private const val COLUMN_FURNITURE_MATERIALS = "materials"

        // Favorite Furniture table columns
        private const val COLUMN_FAVORITE_FURNITURE_FURNITURE_ID = "furniture_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUserTable = ("CREATE TABLE $TABLE_USERS ("
                + "$COLUMN_ID INTEGER PRIMARY KEY,"
                + "$COLUMN_USER_NAME TEXT,"
                + "$COLUMN_USER_EMAIL TEXT,"
                + "$COLUMN_USER_PASSWORD TEXT)")
        db.execSQL(createUserTable)

        val createShopTable = ("CREATE TABLE $TABLE_SHOPS ("
                + "$COLUMN_ID INTEGER PRIMARY KEY,"
                + "$COLUMN_SHOP_NAME TEXT,"
                + "$COLUMN_SHOP_LOCATION TEXT)")
        db.execSQL(createShopTable)

        val createFurnitureTable = ("CREATE TABLE $TABLE_FURNITURE ("
                + "$COLUMN_ID INTEGER PRIMARY KEY,"
                + "$COLUMN_FURNITURE_NAME TEXT,"
                + "$COLUMN_FURNITURE_IMAGES TEXT,"
                + "$COLUMN_FURNITURE_CATEGORY TEXT,"
                + "$COLUMN_FURNITURE_PRICE REAL,"
                + "$COLUMN_FURNITURE_RATING REAL,"
                + "$COLUMN_FURNITURE_SOLD_COUNT INTEGER,"
                + "$COLUMN_FURNITURE_COLORS TEXT,"
                + "$COLUMN_FURNITURE_DIMENSIONS TEXT,"
                + "$COLUMN_FURNITURE_MATERIALS TEXT)")
        db.execSQL(createFurnitureTable)

        val createFavoriteFurnitureTable = ("CREATE TABLE $TABLE_FAVORITE_FURNITURE ("
                + "$COLUMN_ID INTEGER PRIMARY KEY,"
                + "$COLUMN_FAVORITE_FURNITURE_FURNITURE_ID INTEGER)")
        db.execSQL(createFavoriteFurnitureTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older tables if existed
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SHOPS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FURNITURE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITE_FURNITURE")

        // Create tables again
        onCreate(db)
    }

    // CRUD operations for the Users table
    fun addUser(name: String, email: String, password: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_NAME, name)
            put(COLUMN_USER_EMAIL, email)
            put(COLUMN_USER_PASSWORD, password)
        }
        val id = db.insert(TABLE_USERS, null, values)
        db.close()
        return id
    }

//    fun getUserByEmail(email: String): User? {
//        val db = this.readableDatabase
//        val cursor = db.query(
//            TABLE_USERS,
//            null,
//            "$COLUMN_USER_EMAIL = ?",
//            arrayOf(email),
//            null,
//            null,
//            null
//        )
//        var user: User? = null
//        if (cursor.moveToFirst()) {
//            val userId = cursor.getLong(cursor.getColumnIndex(COLUMN_ID))
//            val userName = cursor.getString(cursor.getColumnIndex(COLUMN_USER_NAME))
//            val userEmail = cursor.getString(cursor.getColumnIndex(COLUMN_USER_EMAIL))
//            val userPassword = cursor.getString(cursor.getColumnIndex(COLUMN_USER_PASSWORD))
//            user = User(userId, userName, userEmail, userPassword)
//        }
//        cursor.close()
//        db.close()
//        return user
//    }

    fun updateUser(userId: Long, name: String, email: String, password: String): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_NAME, name)
            put(COLUMN_USER_EMAIL, email)
            put(COLUMN_USER_PASSWORD, password)
        }
        val rowsAffected = db.update(
            TABLE_USERS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(userId.toString())
        )
        db.close()
        return rowsAffected
    }

    fun deleteUser(userId: Long): Int {
        val db = this.writableDatabase
        val rowsAffected = db.delete(
            TABLE_USERS,
            "$COLUMN_ID = ?",
            arrayOf(userId.toString())
        )
        db.close()
        return rowsAffected
    }
}

data class User( val id: Long, val name: String, val email: String, val password: String )