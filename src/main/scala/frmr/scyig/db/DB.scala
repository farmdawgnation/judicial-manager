package frmr.scyig.db

import scala.concurrent._
import scala.concurrent.duration._
import slick.jdbc.MySQLProfile.api._
import net.liftweb.util._

object DB {
  val database = {
    for {
      url <- Props.get("database.url") ?~! "No database url"
      username <- Props.get("database.username") ?~! "No database username"
      password <- Props.get("database.password") ?~! "No database password"
    } yield {
      Database.forURL(
        url,
        username,
        password
      )
    }
  } openOrThrowException("Database information is required.")

  def createSchema(): Unit = {
    val setup = DBIO.seq(
      (Competitions.schema ++ Judges.schema ++ Matches.schema ++ Scores.schema ++ Sponsors.schema ++
        Teams.schema ++ Users.schema ++ UsersSponsors.schema).create
    )

    val schemaFuture = database.run(setup)
    Await.result(schemaFuture, Duration(30, SECONDS))
  }
}
