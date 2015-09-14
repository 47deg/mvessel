package com.fortysevendeg.mvessel.api.impl

import com.fortysevendeg.mvessel.api.CursorType.Value
import com.fortysevendeg.mvessel.api.{CursorType, CursorProxy}

class CursorSeq(
  columnNames: Seq[String],
  rows: Seq[Seq[Any]])
  extends CursorProxy
  with BaseCursor {

  override def getCount: Int = rows.size

  override def getColumnCount: Int = columnNames.size

  override def getColumnIndexOrThrow(columnName: String): Int =
    columnNames.indexOf(columnName) match {
      case i if i < 0 =>
        throw new IllegalArgumentException(s"column '$columnName' does not exist")
      case i => i
    }

  override def getColumnName(columnIndex: Int): String = columnNames(columnIndex)

  override def getCursorType(columnIndex: Int): Value = readType(getValue(columnIndex))

  override def getDouble(columnIndex: Int): Double = getValue(columnIndex) match {
    case null => 0d
    case i: Int => i.toDouble
    case f: Float => f.toDouble
    case a => a.toString.toDouble
  }

  override def getFloat(columnIndex: Int): Float = getValue(columnIndex) match {
    case null => 0f
    case i: Int => i.toFloat
    case f: Float => f.toFloat
    case a => a.toString.toFloat
  }

  override def getLong(columnIndex: Int): Long = getValue(columnIndex) match {
    case null => 0l
    case i: Int => i.toLong
    case f: Float => f.toLong
    case a => a.toString.toLong
  }

  override def getShort(columnIndex: Int): Short = getValue(columnIndex) match {
    case null => 0
    case i: Int => i.toShort
    case f: Float => f.toShort
    case a => a.toString.toShort
  }

  override def getInt(columnIndex: Int): Int = getValue(columnIndex) match {
    case null => 0
    case i: Int => i.toInt
    case f: Float => f.toInt
    case a => a.toString.toInt
  }

  override def getBlob(columnIndex: Int): Array[Byte] = getValue(columnIndex) match {
    case null => null
    case a: Array[Byte] => a
    case a => a.toString.getBytes
  }

  override def getString(columnIndex: Int): String = getValue(columnIndex) match {
    case null => null
    case a => a.toString
  }

  override def isNull(columnIndex: Int): Boolean = getCursorType(columnIndex) == CursorType.Null

  private[this] def getValue(columnIndex: Int): Any =
    (getColumnCount, getPosition) match {
      case (count, _) if columnIndex < 0 || columnIndex >= count =>
        throw new IllegalArgumentException(s"Requested column: $columnIndex, # of columns: $getColumnCount")
      case (_, position) if position < 0 =>
        throw new IllegalStateException("Before first row.")
      case (_, position) if position > getCount =>
        throw new IllegalStateException("After last row.")
      case (_, position) =>
        rows(position)(columnIndex)
    }

  private[this] def readType(anyRef: Any) = anyRef match {
    case null => CursorType.Null
    case i: Int => CursorType.Integer
    case f: Float => CursorType.Float
    case b: Array[Byte] => CursorType.Blob
    case _ => CursorType.String
  }
}
