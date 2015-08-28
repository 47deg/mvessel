package com.fortysevendeg.mvessel

import java.sql.SQLException
import java.util.Properties

import com.fortysevendeg.mvessel.util.ConnectionValues
import org.specs2.matcher.Scope
import org.specs2.mutable.Specification

trait DriverSpecification
  extends Specification {

  val name = "database.db"

  val minorVersion = 0

  val majorVersion = 1

  val validUrl = s"jdbc:sqlite:$name"

  val invalidPrefixUrl = s"jdbc:oracle:$name"

  val invalidUrl = s"urlNotValid"

  val properties = new Properties()

  trait DriverScope extends Scope

  trait WithConnectionValues extends DriverScope {

    val timeout = 1

    val retry = 5

    val connectionValues = ConnectionValues(name, Map("timeout" -> timeout.toString, "retry" -> retry.toString))

    val driver = new AndroidDriver {

      override def parseConnectionString(connectionString: String): Option[ConnectionValues] = Some(connectionValues)
    }

  }

  trait WithoutConnectionValues extends DriverScope {

    val driver = new AndroidDriver {

      override def parseConnectionString(connectionString: String): Option[ConnectionValues] = None
    }

  }

}

class DriverSpec
  extends DriverSpecification {

  "connect" should {

    "create a Connection with the params obtained by the ConnectionStringParser" in
      new WithConnectionValues {
        driver.connect(validUrl, properties) must beLike {
          case c: Connection =>
            c.databaseName shouldEqual name
            c.timeout shouldEqual timeout
            c.retryInterval shouldEqual retry
        }
      }

    "throws a SQLException when the URL can't be parsed" in
      new WithoutConnectionValues {
        driver.connect(validUrl, properties) must throwA[SQLException]
      }
  }

}
