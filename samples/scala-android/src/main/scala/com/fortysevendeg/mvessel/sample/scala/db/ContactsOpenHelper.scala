/*
 * The MIT License (MIT)
 *
 * Copyright (C) 2012 47 Degrees, LLC http://47deg.com hello@47deg.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

package com.fortysevendeg.mvessel.sample.scala.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.fortysevendeg.mvessel._
import com.fortysevendeg.mvessel.Database
import com.fortysevendeg.mvessel.api.impl.AndroidCursor
import com.fortysevendeg.mvessel.api.impl.AndroidDatabaseFactory
import ContactsOpenHelper._

import scala.util.{Failure, Try}

class ContactsOpenHelper(context: Context)
  extends SQLiteOpenHelper(context, database, javaNull, 1) {

  val database: Database[AndroidCursor] = {
    val database: SQLiteDatabase = getReadableDatabase
    new Database[AndroidCursor](
      databaseFactory = new AndroidDatabaseFactory,
      name = database.getPath,
      timeout = 50,
      retry = 0,
      flags = 0)
  }

  def onCreate(db: SQLiteDatabase) {
    db.beginTransaction()

    db.execSQL(
      s"""CREATE TABLE IF NOT EXISTS ${ContactsOpenHelper.TABLE} (
         |${ContactsOpenHelper.C_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
         |${ContactsOpenHelper.C_NAME} TEXT,
         |${ContactsOpenHelper.C_AGE} INTEGER)""".stripMargin)

    1 to 100 foreach { i =>
      db.execSQL(
        s"""INSERT INTO ${ContactsOpenHelper.TABLE}(
           |${ContactsOpenHelper.C_NAME},
           |${ContactsOpenHelper.C_AGE}
           |) VALUES('Contact $i', ${i + 20})""".stripMargin)
    }

    db.setTransactionSuccessful()
    db.endTransaction()
  }

  def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = {}

  override def close() {
    Try(database.close()) match {
      case Failure(e) => e.printStackTrace()
      case _ =>
    }
    super.close()
  }

}

object ContactsOpenHelper {
  val database = "contacts.db"
  val TABLE: String = "contacts"
  val C_ID: String = "_id"
  val C_NAME: String = "name"
  val C_AGE: String = "age"
}