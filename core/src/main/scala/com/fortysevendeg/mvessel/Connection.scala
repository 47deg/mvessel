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

package com.fortysevendeg.mvessel

import java.sql.{Connection => SQLConnection, DatabaseMetaData => SQLDatabaseMetaData, _}
import java.util.Properties
import java.util.concurrent.Executor

import com.fortysevendeg.mvessel.api.{CursorProxy, DatabaseProxyFactory}
import com.fortysevendeg.mvessel.logging.LogWrapper
import com.fortysevendeg.mvessel.metadata.DatabaseMetaData
import com.fortysevendeg.mvessel.statement.{PreparedStatement, Statement}
import com.fortysevendeg.mvessel.util.DatabaseUtils.WrapSQLException

class Connection[T <: CursorProxy](
  databaseWrapperFactory: DatabaseProxyFactory[T],
  val databaseName: String,
  val timeout: Long = 0,
  val retryInterval: Int = 50,
  val flags: Int = 0,
  val logWrapper: LogWrapper)
  extends SQLConnection
  with WrapperNotSupported {

  protected def defaultAutoCommit(): Boolean = false

  private[this] def createPreparedStatement(
    sql: String,
    columnName: Option[String] = None) =
    new PreparedStatement(
      sql = sql,
      connection = this,
      columnGenerated = columnName,
      logWrapper = logWrapper)

  val rollbackSql = "rollback;"

  val closingErrorMessage = "Error closing database"

  val autoCommitErrorMessage = "Database is in auto-commit mode"

  val alreadyClosedErrorMessage = "Database connection closed"

  private[this] var database: Option[Database[T]] = Some(new Database(databaseWrapperFactory, databaseName, timeout, retryInterval, flags))

  private[this] var autoCommit: Boolean = defaultAutoCommit()

  override def close(): Unit = synchronized {
    database match {
      case Some(db) =>
        logWrapper.logOnError(closingErrorMessage, db.close())
        database = None
      case _ =>
    }
  }

  override def commit(): Unit = withOpenDatabase { db =>
    autoCommit match {
      case true => throw new SQLException(autoCommitErrorMessage)
      case _ =>
        db.setTransactionSuccessful()
        db.endTransaction()
        db.beginTransaction()
    }
  }

  override def createStatement(): Statement[T] = new Statement(
    sqlConnection = this,
    logWrapper = logWrapper)

  override def getAutoCommit: Boolean = autoCommit

  override def setAutoCommit(autoCommit: Boolean): Unit = withOpenDatabase { db =>
    (this.autoCommit, autoCommit) match {
      case (false, true) =>
        db.setTransactionSuccessful()
        db.endTransaction()
        this.autoCommit = true
      case (true, false) =>
        db.beginTransaction()
        this.autoCommit = false
      case _ =>
    }
  }

  override def getMetaData: SQLDatabaseMetaData = new DatabaseMetaData(this, logWrapper)

  override def isClosed: Boolean = database match {
    case Some(db) => !db.database.isOpen
    case _ => true
  }

  override def nativeSQL(sql: String): String = sql

  override def prepareStatement(sql: String): PreparedStatement[T] = createPreparedStatement(sql)

  override def prepareStatement(sql: String, columnNames: scala.Array[String]): PreparedStatement[T] =
    Option(columnNames) match {
      case Some(scala.Array()) => createPreparedStatement(sql)
      case Some(scala.Array(columnName)) => createPreparedStatement(sql, Some(columnName))
      case None => createPreparedStatement(sql)
      case _ => logWrapper.notImplemented(javaNull)
    }

  override def rollback(): Unit = withOpenDatabase { db =>
    autoCommit match {
      case true => throw new SQLException(autoCommitErrorMessage)
      case _ =>
        db.execSQL(rollbackSql)
        db.endTransaction()
        db.beginTransaction()
    }
  }

  override def finalize() {
    logWrapper.i("Finalize Connection")
    if (!isClosed) close()
    super.finalize()
  }

  def withOpenDatabase[R](f: (Database[T]) => R) = WrapSQLException(database, alreadyClosedErrorMessage)(f)

  override def clearWarnings(): Unit = logWrapper.notImplemented(Unit)

  override def createStatement(resultSetType: Int, resultSetConcurrency: Int): Statement[T] =
    logWrapper.notImplemented(javaNull)

  override def createStatement(resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int): Statement[T] =
    logWrapper.notImplemented(javaNull)

  override def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int): PreparedStatement[T] =
    logWrapper.notImplemented(prepareStatement(sql))

  override def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int): PreparedStatement[T] =
    logWrapper.notImplemented(prepareStatement(sql))

  override def prepareStatement(sql: String, autoGeneratedKeys: Int): PreparedStatement[T] =
    logWrapper.notImplemented(prepareStatement(sql))

  override def prepareStatement(sql: String, columnIndexes: scala.Array[Int]): PreparedStatement[T] =
    logWrapper.notImplemented(prepareStatement(sql))

  override def getCatalog: String = logWrapper.notImplemented(javaNull)

  override def getHoldability: Int = logWrapper.notImplemented(0)

  override def getTransactionIsolation: Int = logWrapper.notImplemented(0)

  override def getTypeMap: java.util.Map[String, Class[_]] = logWrapper.notImplemented(javaNull)

  override def getWarnings: SQLWarning = logWrapper.notImplemented(javaNull)

  override def isReadOnly: Boolean = logWrapper.notImplemented(false)

  override def prepareCall(sql: String): CallableStatement = logWrapper.notImplemented(javaNull)

  override def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int): CallableStatement =
    logWrapper.notImplemented(javaNull)

  override def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int): CallableStatement =
    logWrapper.notImplemented(javaNull)

  override def releaseSavepoint(savepoint: Savepoint): Unit = logWrapper.notImplemented(Unit)

  override def rollback(savepoint: Savepoint): Unit = logWrapper.notImplemented(Unit)

  override def setCatalog(catalog: String): Unit = logWrapper.notImplemented(Unit)

  override def setHoldability(holdability: Int): Unit = logWrapper.notImplemented(Unit)

  override def setReadOnly(readOnly: Boolean): Unit = logWrapper.notImplemented(Unit)

  override def setSavepoint(): Savepoint = logWrapper.notImplemented(javaNull)

  override def setSavepoint(name: String): Savepoint = logWrapper.notImplemented(javaNull)

  override def setTransactionIsolation(level: Int): Unit = logWrapper.notImplemented(Unit)

  override def setTypeMap(map: java.util.Map[String, Class[_]]): Unit = logWrapper.notImplemented(Unit)

  override def getNetworkTimeout: Int = logWrapper.notImplemented(0)

  override def createBlob(): Blob = logWrapper.notImplemented(javaNull)

  override def createSQLXML(): SQLXML = logWrapper.notImplemented(javaNull)

  override def createNClob(): NClob = logWrapper.notImplemented(javaNull)

  override def getClientInfo(name: String): String = logWrapper.notImplemented(javaNull)

  override def getClientInfo: Properties = logWrapper.notImplemented(javaNull)

  override def getSchema: String = logWrapper.notImplemented(javaNull)

  override def setNetworkTimeout(executor: Executor, milliseconds: Int): Unit = logWrapper.notImplemented(Unit)

  override def setClientInfo(name: String, value: String): Unit = logWrapper.notImplemented(Unit)

  override def setClientInfo(properties: Properties): Unit = logWrapper.notImplemented(Unit)

  override def createClob(): Clob = logWrapper.notImplemented(javaNull)

  override def createArrayOf(typeName: String, elements: scala.Array[AnyRef]): Array = logWrapper.notImplemented(javaNull)

  override def abort(executor: Executor): Unit = logWrapper.notImplemented(Unit)

  override def isValid(timeout: Int): Boolean = logWrapper.notImplemented(false)

  override def createStruct(typeName: String, attributes: scala.Array[AnyRef]): Struct = logWrapper.notImplemented(javaNull)

  override def setSchema(schema: String): Unit = logWrapper.notImplemented(Unit)
}

object Connection {

  val defaultTimeout = 0

  val defaultRetryInterval = 50

}