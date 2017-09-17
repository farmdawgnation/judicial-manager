package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import net.liftweb.common._
import net.liftweb.common.BoxLogging._
import net.liftweb.http._
import net.liftweb.http.js.JsCmds._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import slick.jdbc.MySQLProfile.api._

object TeamUpload {
  import SnippetHelpers._

  val menu = Menu.param[Competition](
    "Upload Teams",
    "Upload Teams",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "teams" / "upload" >>
    validateCompetitionAccess
}

class TeamUpload(competition: Competition) {
  val competitionId = competition.id.getOrElse {
    throw new IllegalStateException("Running with a competition lacking an ID")
  }
  var csvContent: Option[String] = None

  def csvUploadHandler(file: FileParamHolder) = {
    println("saw file upload")
    csvContent = Some(new String(file.file, "UTF-8"))
  }

  def doUpload = {
    def parseRow(row: String): Option[(String, String)] = {
      row.split(",").toList match {
        case f1 :: f2 :: rest => Some((f1, f2))
        case _ => None
      }
    }

    val teams = for {
      csv <- csvContent.toSeq
      csvRow <- csv.lines if csvRow.nonEmpty
      (teamName, teamOrg) <- parseRow(csvRow)
    } yield {
      val team = Team(None, competitionId, teamName, teamOrg)
      Teams.insertOrUpdate(team)
    }

    DB.runAwait(DBIO.seq(teams: _*)).logFailure("Database error processing upload") match {
      case fail: Failure =>
        S.error("A database failure occurred. Please see the log for more information.")

      case _ =>
        S.redirectTo(TeamList.menu.toLoc.calcHref(competition), () => S.notice(s"Uploaded ${teams.length} teams"))
    }
  }

  def render = {
    "#team-csv-file" #> SHtml.fileUpload(csvUploadHandler _) &
    ".upload" #> SHtml.onSubmitUnit(doUpload _)
  }
}
