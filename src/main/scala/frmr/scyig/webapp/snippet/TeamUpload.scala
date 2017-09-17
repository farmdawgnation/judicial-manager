package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import net.liftweb.common._
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
    val teams = for {
      csv <- csvContent.toSeq
      csvRow <- csv.lines
      Array(teamName, teamOrg) = csvRow.split(",")
      team = Team(None, competitionId, teamName, teamOrg)
      _ <- DB.runAwait(Teams.insertOrUpdate(team))
    } yield {
      team
    }

    S.redirectTo(TeamList.menu.toLoc.calcHref(competition), () => S.notice(s"Uploaded ${teams.length} teams"))
  }

  def render = {
    "#team-csv-file" #> SHtml.fileUpload(csvUploadHandler _) &
    ".upload" #> SHtml.onSubmitUnit(doUpload _)
  }
}
