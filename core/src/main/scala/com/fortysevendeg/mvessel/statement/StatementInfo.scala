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

trait StatementInfo {

  val selectRegex = "(?m)(?s)(?i)\\s*(SELECT|PRAGMA|EXPLAIN QUERY PLAN).*".r

  val limitRegex = "(?m)(?s)(?i)\\s*.*LIMIT\\s+(\\d+).*".r

  val changeRegex = "(?m)(?s)(?i)\\s*(INSERT|UPDATE|DELETE).*".r

  def isSelect(sql: String): Boolean = selectRegex.pattern.matcher(sql).matches()

  def readLimit(sql: String): Option[Int] = limitRegex.findFirstMatchIn(sql) map (_.group(1).toInt)

  def toLimitedSql(sql: String, defaultLimit: Int): String =
    readLimit(sql) match {
      case Some(_) => sql
      case _ =>
        val pos = sql.lastIndexOf(';')
        val newSql = if (pos > 0) sql.substring(0, pos) else sql
        s"$newSql LIMIT $defaultLimit;"
    }

  def isChange(sql: String): Boolean = changeRegex.pattern.matcher(sql).matches()

}

object StatementInfo extends StatementInfo