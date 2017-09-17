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

object JudgeUpload {
  import SnippetHelpers._

  val menu = Menu.param[Competition](
    "Upload Judges",
    "Upload Judges",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "judges" / "upload" >>
    validateCompetitionAccess
}

class JudgeUpload(competition: Competition) {
  val competitionId = competition.id.getOrElse {
    throw new IllegalStateException("Running with a competition lacking an ID")
  }
  var csvContent: Option[String] = None

  def csvUploadHandler(file: FileParamHolder) = {
    println("got file")
    csvContent = Some(new String(file.file, "UTF-8"))
  }

  def doUpload = {
    println(csvContent)
    val judges = for {
      csv <- csvContent.toSeq
      csvRow <- csv.lines if csvRow.nonEmpty
      _ = println(csvRow)
      Array(judgeName, judgeOrg, judgeKind) = csvRow.split(",")
      actualKind = judgeKind.toLowerCase.trim match {
        case "scoring" => ScoringJudge
        case _ => PresidingJudge
      }
      judge = Judge(None, competitionId, judgeName.trim, judgeOrg.trim, actualKind)
      _ <- DB.runAwait(Judges.insertOrUpdate(judge)).logFailure("Something went wrong talking to the db")
    } yield {
      judge
    }

    S.redirectTo(JudgeList.menu.toLoc.calcHref(competition), () => S.notice(s"Uploaded ${judges.length} judges"))
  }

  def render = {
    "#judge-csv-file" #> SHtml.fileUpload(csvUploadHandler _) &
    ".upload" #> SHtml.onSubmitUnit(doUpload _)
  }
}
