package com.fortysevendeg.mvessel.util

import java.sql.ResultSet

import android.database.{MatrixCursor, Cursor}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import StructureControlOps._

import scala.util.Random

trait StructureControlOpsSpecification
  extends Specification
  with Mockito {

  val total = 10

  val allValues: Seq[Array[AnyRef]] = (1 to total map { i =>
    Array(i.asInstanceOf[AnyRef], s"name $i", new java.lang.Integer(Random.nextInt(99)))
  }).toSeq

  trait StructureControlOps
    extends Scope

  trait WithCursors
    extends StructureControlOps {

    val columnNames = Array("id", "name", "age")

    val cursor = buildCursor(allValues.slice(0, total))

    val oneRowCursor = buildCursor(allValues.slice(0, 1))

    val emptyCursor = buildCursor(Seq.empty)

    def buildCursor(values: Seq[Array[AnyRef]]): Cursor = {
      val cursor = new MatrixCursor(columnNames)
      values foreach cursor.addRow
      cursor
    }

  }

  trait WithResultSets
    extends StructureControlOps {

    val resultSet = mock[ResultSet]
    resultSet.next() returns (true, Seq.fill(total - 1)(true) :+ false :_*)
    resultSet.getString(1) returns (allValues.head(1).toString, allValues.slice(1, total) map (_(1).toString) :_*)

    val oneRowResultSet = mock[ResultSet]
    oneRowResultSet.next() returns (true, false)
    oneRowResultSet.getString(1) returns allValues.head(1).toString

    val emptyResultSet = mock[ResultSet]
    emptyResultSet.next() returns false

  }

}

class StructureControlOpsSpec
  extends StructureControlOpsSpecification {

  "process for Cursor" should {

    "return a sequence with all elements when until param is not specified" in new WithCursors {
      cursor.process { c =>
        c.getString(1)
      } shouldEqual allValues.map(_(1))
    }

    "return a sequence with one element for a one element cursor when until param is not specified" in new WithCursors {
      oneRowCursor.process { c =>
        c.getString(1)
      } shouldEqual Seq(allValues.head(1))
    }

    "return an empty sequence for a cursor without rows" in new WithCursors {
      emptyCursor.process { c =>
        c.getString(1)
      } shouldEqual Seq.empty
    }

    "return a sequence with 1 elements when pass 1 as until param" in new WithCursors {
      cursor.process (c => c.getString(1), Some(1)) shouldEqual Seq(allValues.head(1))
    }

    "return an empty sequence when pass 0 as until param" in new WithCursors {
      cursor.process (c => c.getString(1), Some(0)) shouldEqual Seq.empty
    }

  }

  "processOne for Cursor" should {

    "return a sequence with one element" in new WithCursors {
      cursor.processOne { c =>
        c.getString(1)
      } shouldEqual allValues.headOption.map(_(1))
    }

    "return a sequence with one element for a one element cursor" in new WithCursors {
      oneRowCursor.processOne { c =>
        c.getString(1)
      } shouldEqual allValues.headOption.map(_(1))
    }

    "return an empty sequence for a cursor without rows" in new WithCursors {
      emptyCursor.processOne { c =>
        c.getString(1)
      } shouldEqual None
    }

  }

  "process for ResultSet" should {

    "return a sequence with all elements when until param is not specified" in new WithResultSets {
      resultSet.process { c =>
        c.getString(1)
      } shouldEqual allValues.map(_(1))
    }

    "return a sequence with one element for a one element cursor when until param is not specified" in new WithResultSets {
      oneRowResultSet.process { c =>
        c.getString(1)
      } shouldEqual Seq(allValues.head(1))
    }

    "return an empty sequence for a cursor without rows" in new WithResultSets {
      emptyResultSet.process { c =>
        c.getString(1)
      } shouldEqual Seq.empty
    }

    "return a sequence with 1 elements when pass 1 as until param" in new WithResultSets {
      resultSet.process (c => c.getString(1), Some(1)) shouldEqual Seq(allValues.head(1))
    }

    "return an empty sequence when pass 0 as until param" in new WithResultSets {
      resultSet.process (c => c.getString(1), Some(0)) shouldEqual Seq.empty
    }

  }

  "processOne for ResultSet" should {

    "return a sequence with one element" in new WithResultSets {
      resultSet.processOne { c =>
        c.getString(1)
      } shouldEqual allValues.headOption.map(_(1))
    }

    "return a sequence with one element for a one element cursor" in new WithResultSets {
      oneRowResultSet.processOne { c =>
        c.getString(1)
      } shouldEqual allValues.headOption.map(_(1))
    }

    "return an empty sequence for a cursor without rows" in new WithResultSets {
      emptyResultSet.processOne { c =>
        c.getString(1)
      } shouldEqual None
    }

  }


}
