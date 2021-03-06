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

import java.sql.SQLException
import TypeTransformers._
import com.fortysevendeg.mvessel._

import scala.collection.mutable

trait StatementArguments {

  def toStringArray: scala.Array[String] = scala.Array.empty

  def toArray: scala.Array[AnyRef] = scala.Array.empty

}

class PreparedStatementArguments extends StatementArguments {

  private[this] var argumentsList: NonEmptyList[mutable.Map[Int, AnyRef]] = NonEmptyList(mutable.Map.empty)

  // Prepared statements count from 1
  private[this] var maxIndex: Int = 0

  val invalidArgumentIndexErrorMessage = (i: Int) => s"Invalid parameter index: $i"

  val classNotSupportedErrorMessage = (c: String) => s"Class $c not supported for bind args"

  def map[U](f: scala.Array[AnyRef] => U): Seq[U] =
    argumentsList map (argumentsMap => f(toArray(argumentsMap, maxIndex)))

  def addNewEntry(): Unit =
    argumentsList = NonEmptyList(first = mutable.Map.empty, tail = argumentsList.tail :+ argumentsList.first)

  def clearArguments(): Unit = {
    argumentsList = NonEmptyList(mutable.Map.empty)
    maxIndex = 0
  }

  override def toStringArray: Array[String] = toArray map { value =>
    Option(value) match {
      case Some(v: String) => v
      case Some(v) => v.toString
      case None => nullString
    }
  }

  override def toArray: Array[AnyRef] = toArray(argumentsList.first, maxIndex)

  // In android only byte[], String, Long and Double are supported in bindArgs.

  def setObjectArgument(position: Int, arg: Any): Unit = withValidation(position, arg) {
    case a: scala.Array[Byte] => setArgument(position, a)
    case a: Byte => setArgument(position, a)
    case a: Boolean => setArgument(position, a)
    case a: Double => setArgument(position, a)
    case a: Short => setArgument(position, a)
    case a: Float => setArgument(position, a)
    case a: Long => setArgument(position, a)
    case a: String => setArgument(position, a)
    case a: java.sql.Date => setArgument(position, a)
    case a: java.sql.Time => setArgument(position, a)
    case a: java.sql.Timestamp => setArgument(position, a)
    case `javaNull` => setNullArgument(position)
    case _ => throw new SQLException(classNotSupportedErrorMessage(arg.getClass.getName))
  }

  def setArgument(position: Int, arg: scala.Array[Byte]): Unit =
    withValidation(position, arg)(addToHead(position, _))

  def setArgument(position: Int, arg: Double): Unit =
    withValidation(position, arg) { d =>
      addToHead(position, java.lang.Double.valueOf(d))
    }

  def setArgument(position: Int, arg: Long): Unit =
    withValidation(position, arg) { l =>
      addToHead(position, java.lang.Long.valueOf(l))
    }

  def setArgument(position: Int, arg: String): Unit =
    withValidation(position, arg)(addToHead(position, _))

  def setArgument(position: Int, arg: Byte): Unit =
    withValidation(position, arg) { a =>
      setArgument(position, a.toLong)
    }

  def setArgument(position: Int, arg: Boolean): Unit =
    withValidation(position, arg) { a =>
      setArgument(position, a.toLong)
    }

  def setArgument(position: Int, arg: Short): Unit =
    withValidation(position, arg) { a =>
      setArgument(position, a.toLong)
    }

  def setArgument(position: Int, arg: Float): Unit =
    withValidation(position, arg) { a =>
      setArgument(position, a.toDouble)
    }

  def setArgument(position: Int, arg: java.sql.Date): Unit =
    withValidation(position, arg) { a =>
      setArgument(position, a.toLong)
    }

  def setArgument(position: Int, arg: java.sql.Time): Unit =
    withValidation(position, arg) { a =>
      setArgument(position, a.toLong)
    }

  def setArgument(position: Int, arg: java.sql.Timestamp): Unit =
    withValidation(position, arg) { a =>
      setArgument(position, a.toLong)
    }

  private[this] def addToHead(position: Int, arg: AnyRef) = {
    argumentsList.first += (position -> arg)
    if (position > maxIndex) maxIndex = position
  }

  private[this] def setNullArgument(position: Int): Unit = {
    argumentsList.first.remove(position)
    if (position > maxIndex) maxIndex = position
  }

  private[this] def withValidation[T](position: Int, arg: T)(f: T => Unit) = (position, Option(arg)) match {
    case (p, _) if p <= 0 => throw new SQLException(invalidArgumentIndexErrorMessage(p))
    case (_, Some(a)) => f(a)
    case (_, None) => setNullArgument(position)
  }

  private[this] def toArray(
    map: mutable.Map[Int, AnyRef],
    maxIndex: Int): scala.Array[AnyRef] =
    (1 to maxIndex map {
      map get _ match {
        case Some(v) => v
        case None => javaNull
      }
    }).toArray

}

case class NonEmptyList[+A](first: A, tail: Seq[A] = Seq.empty) {

  def map[U](f: (A) => U): Seq[U] = tail :+ first map f

}

object TypeTransformers {

  implicit class BooleanTransformer(arg: Boolean) {
    def toLong: Long = if (arg) 1l else 0l
  }

  implicit class DateTransformer(arg: java.sql.Date) {
    def toLong: Long = arg.getTime
  }

  implicit class TimeTransformer(arg: java.sql.Time) {
    def toLong: Long = arg.getTime
  }

  implicit class TimestampTransformer(arg: java.sql.Timestamp) {
    def toLong: Long = arg.getTime
  }

}