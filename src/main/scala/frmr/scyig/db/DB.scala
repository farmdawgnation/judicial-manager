package frmr.scyig.db

import scala.concurrent._
import scala.concurrent.duration._
import slick.jdbc.meta._
import slick.jdbc.MySQLProfile.api._
import net.liftweb.common._
import net.liftweb.util._

object DB extends Loggable {
  val database = {
    for {
      url <- Props.get("database.url") ?~! "No database url"
      username <- Props.get("database.username") ?~! "No database username"
      password <- Props.get("database.password") ?~! "No database password"
    } yield {
      Database.forURL(
        url,
        username,
        password,
        driver = "com.mysql.cj.jdbc.Driver"
      )
    }
  } openOrThrowException("Database information is required.")

  def createSchema(): Unit = {
    val tables: Vector[MTable] = Await.result(database.run(MTable.getTables("%")), Duration(30, SECONDS))

    if (tables.length == 0) {
      logger.info("Tables do not already exist. Creating schemas.")
      val setup = DBIO.seq(
        (Competitions.schema ++ Judges.schema ++ Matches.schema ++ Scores.schema ++ Sponsors.schema ++
          Teams.schema ++ Users.schema ++ UsersSponsors.schema).create
      )

      val schemaFuture = database.run(setup)
      Await.result(schemaFuture, Duration(30, SECONDS))
    } else {
      logger.info("Tables already exist. Not creating schemas.")
    }
  }
}
