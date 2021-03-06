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

package com.fortysevendeg

package object mvessel {

  val javaNull = null

  val nullString = "NULL"

  implicit class IndexOps(columnIndex: Int) {
    def index: Int = columnIndex - 1
  }

  implicit class SQLStringOps(string: String) {
    def escape: String = string.replace("'", "''")
  }

}
