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

package com.fortysevendeg.mvessel.statement

import java.sql.{ResultSet => SQLResultSet, SQLException, SQLWarning, Statement => SQLStatement}

import com.fortysevendeg.mvessel.api.{CursorType, CursorProxy}
import com.fortysevendeg.mvessel.logging.LogWrapper
import com.fortysevendeg.mvessel.resultset.ResultSet
import com.fortysevendeg.mvessel.statement.StatementInfo._
import com.fortysevendeg.mvessel.{Connection, Database, WrapperNotSupported, javaNull}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class Statement[T <: CursorProxy](
  sqlConnection: Connection[T],
  columnGenerated: Option[String] = None,
  logWrapper: LogWrapper)
  extends SQLStatement
  with WrapperNotSupported {

  val connectionClosedErrorMessage = "Connection is closed"

  val statementClosedErrorMessage = "Statement is closed"

  val maxNegativeErrorMessage = "Max rows must be zero or positive"

  val selectLastRowId = "SELECT last_insert_rowid()"

  val defaultUpdateCount = -1

  protected val batchList = new mutable.MutableList[String]

  protected var connection: Option[Connection[T]] = Option(sqlConnection)

  protected var maxRows: Option[Int] = None

  protected var resultSet: Option[SQLResultSet] = None

  protected var updateCount: Option[Int] = None

  def getBatchList: Seq[String] = batchList.toList

  override def addBatch(sql: String): Unit = batchList += sql

  override def clearBatch(): Unit = batchList.clear()

  override def close(): Unit = {
    connection = None
    closeResultSet()
  }

  override def isClosed: Boolean = connection map (_.isClosed) getOrElse true

  protected def executeWithArgs(
    sql: String,
    arguments: Option[StatementArguments] = None): Boolean = withOpenConnection { db =>
    resultSet = isSelect(sql) match {
      case true =>
        updateCount = None
        val limitedSql = maxRows map (m => toLimitedSql(sql, m)) getOrElse sql
        Some(new ResultSet(rawQuery(db, limitedSql, arguments), logWrapper))
      case false =>
        execSQL(db, sql, arguments)
        updateCount = Option(db.changedRowCount())
        None
    }
    resultSet.isDefined
  }

  override def execute(sql: String): Boolean = executeWithArgs(sql)

  override def executeBatch(): scala.Array[Int] = withOpenConnection { db =>
    val updateArray = batchList map { sql =>
      Try(db.execSQL(sql)) match {
        case Success(_) if isChange(sql) => db.changedRowCount()
        case Success(_) => SQLStatement.SUCCESS_NO_INFO
        case Failure(e) =>
          logWrapper.e(s"Error executing statement $sql", Some(e))
          SQLStatement.EXECUTE_FAILED
      }
    }
    updateCount = updateArray filter (_ > 0) reduceOption (_ + _)
    updateArray.toArray
  }

  protected def executeQueryWithArgs(
    sql: String,
    arguments: Option[StatementArguments] = None): SQLResultSet =
    withOpenConnection(newQueryResultSet(_, sql, arguments))

  override def executeQuery(sql: String): SQLResultSet = executeQueryWithArgs(sql)

  protected def executeUpdateWithArgs(
    sql: String,
    arguments: Option[StatementArguments] = None): Int = withOpenConnection { db =>
    execSQL(db, sql, arguments)
    val count = db.changedRowCount()
    updateCount = Some(count)
    count
  }

  override def executeUpdate(sql: String): Int = executeUpdateWithArgs(sql)

  override def getConnection: Connection[T] =
    connection match {
      case Some(c) if !c.isClosed => c
      case _ => throw new SQLException(connectionClosedErrorMessage)
    }

  override def getGeneratedKeys: SQLResultSet =
    withOpenConnection(newQueryResultSet(_, s"$selectLastRowId ${columnGenerated getOrElse ""}"))

  override def getMaxRows: Int = maxRows getOrElse 0

  override def setMaxRows(max: Int): Unit =
    max match {
      case _ if isClosed => throw new SQLException(statementClosedErrorMessage)
      case n if n < 0 => throw new SQLException(s"$maxNegativeErrorMessage Got $n")
      case n if n == 0 => maxRows = None
      case _ => maxRows = Some(max)
    }

  override def getMoreResults: Boolean = getMoreResults(SQLStatement.CLOSE_CURRENT_RESULT)

  override def getMoreResults(current: Int): Boolean = withOpenConnection { _ =>
    if (current == SQLStatement.CLOSE_CURRENT_RESULT) closeResultSet()
    false
  }

  override def getUpdateCount: Int = {
    val count = updateCount getOrElse defaultUpdateCount
    updateCount = None
    count
  }

  override def execute(sql: String, autoGeneratedKeys: Int): Boolean = logWrapper.notImplemented(false)

  override def execute(sql: String, columnIndexes: scala.Array[Int]): Boolean = logWrapper.notImplemented(false)

  override def execute(sql: String, columnNames: scala.Array[String]): Boolean = logWrapper.notImplemented(false)

  override def executeUpdate(sql: String, autoGeneratedKeys: Int): Int = logWrapper.notImplemented(0)

  override def executeUpdate(sql: String, columnIndexes: scala.Array[Int]): Int = logWrapper.notImplemented(0)

  override def executeUpdate(sql: String, columnNames: scala.Array[String]): Int = logWrapper.notImplemented(0)

  override def cancel(): Unit = logWrapper.notImplemented(Unit)

  override def clearWarnings(): Unit = logWrapper.notImplemented(Unit)

  override def getFetchDirection: Int = logWrapper.notImplemented(0)

  override def setFetchDirection(direction: Int): Unit = logWrapper.notImplemented(Unit)

  override def getFetchSize: Int = logWrapper.notImplemented(0)

  override def setFetchSize(rows: Int): Unit = logWrapper.notImplemented(Unit)

  override def getMaxFieldSize: Int = logWrapper.notImplemented(0)

  override def setMaxFieldSize(max: Int): Unit = logWrapper.notImplemented(Unit)

  override def getQueryTimeout: Int = logWrapper.notImplemented(0)

  override def setQueryTimeout(seconds: Int): Unit = logWrapper.notImplemented(Unit)

  override def getResultSet: SQLResultSet = resultSet getOrElse javaNull

  override def getResultSetConcurrency: Int = logWrapper.notImplemented(0)

  override def getResultSetHoldability: Int = logWrapper.notImplemented(0)

  override def getResultSetType: Int = logWrapper.notImplemented(0)

  override def getWarnings: SQLWarning = logWrapper.notImplemented(javaNull)

  override def setCursorName(name: String): Unit = logWrapper.notImplemented(Unit)

  override def setEscapeProcessing(enable: Boolean): Unit = logWrapper.notImplemented(Unit)

  override def setPoolable(poolable: Boolean): Unit = logWrapper.notImplemented(Unit)

  override def isPoolable: Boolean = logWrapper.notImplemented(false)

  override def isCloseOnCompletion: Boolean = logWrapper.notImplemented(false)

  override def closeOnCompletion(): Unit = logWrapper.notImplemented(Unit)

  protected def closeResultSet() = {
    resultSet foreach { rs =>
      if (!rs.isClosed) rs.close()
    }
    resultSet = None
  }

  protected def withOpenConnection[R](f: (Database[T]) => R) =
    connection match {
      case Some(c) if !c.isClosed =>
        closeResultSet()
        c.withOpenDatabase[R](f)
      case _ =>
        throw new SQLException(connectionClosedErrorMessage)
    }

  private[this] def newQueryResultSet(
    db: Database[T],
    sql: String,
    arguments: Option[StatementArguments] = None): SQLResultSet = {
    val rs = new ResultSet(rawQuery(db, sql, arguments), logWrapper)
    resultSet = Some(rs)
    rs
  }

  private[this] def rawQuery(
    db: Database[T],
    sql: String,
    arguments: Option[StatementArguments] = None): T =
    arguments match {
      case Some(a) => db.rawQuery(sql, a.toStringArray)
      case _ => db.rawQuery(sql)
    }

  private[this] def execSQL(
    db: Database[T],
    sql: String,
    arguments: Option[StatementArguments] = None): Unit =
    arguments match {
      case Some(a) => db.execSQL(sql, a.toArray)
      case _ => db.execSQL(sql)
    }
}
