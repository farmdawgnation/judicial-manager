package frmr.scyig.db

import scala.concurrent._
import scala.concurrent.duration._
import slick.jdbc.meta._
import slick.jdbc.MySQLProfile.api._
import net.liftweb.common._
import net.liftweb.util._
import net.liftweb.util.Helpers.tryo
import org.flywaydb.core.Flyway

object DB extends Loggable {
  def withConnectionInfo[T](handler: (String, String, String)=>T): T = {
    for {
      url <- Props.get("database.url") ?~! "No database url"
      username <- Props.get("database.username") ?~! "No database username"
      password <- Props.get("database.password") ?~! "No database password"
    } yield {
      handler(url, username, password)
    }
  } openOrThrowException("Database information is required.")

  lazy val database = withConnectionInfo { (url, username, password) =>
    Database.forURL(
      url,
      username,
      password,
      driver = "com.mysql.cj.jdbc.Driver"
    )
  }

  def run[T](action: DBIOAction[T, NoStream, _]): Future[T] = {
    database.run(action)
  }

  def runAwait[T](action: DBIOAction[T, NoStream, _], timeout: Duration = 10.seconds): Box[T] = {
    tryo(Await.result(run(action), timeout))
  }

  /**
   * Execute the flyway migrations against the database.
   */
  def migrate(): Unit = {
    logger.info("Invoking flyway migrations")

    withConnectionInfo { (url, username, password) =>
      val flyway = new Flyway()
      flyway.setDataSource(
        url,
        username,
        password
      )
      flyway.migrate()
    }

    logger.info("Flyway migrations complete")
  }
}
