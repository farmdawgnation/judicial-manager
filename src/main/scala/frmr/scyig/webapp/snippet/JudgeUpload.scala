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

class JudgeUpload(competition: Competition) extends Loggable {
  val competitionId = competition.id.getOrElse {
    throw new IllegalStateException("Running with a competition lacking an ID")
  }
  var csvContent: Option[String] = None

  def csvUploadHandler(file: FileParamHolder) = {
    println("got file")
    csvContent = Some(new String(file.file, "UTF-8"))
  }

  def doUpload = {
    def parseRow(row: String): Option[(String, String, String)] = {
      row.split(",").toList match {
        case f1 :: f2 :: f3 :: rest =>
          Some((f1, f2, f3))
        case _ =>
          logger.warn(s"Ignoring incorrectly formatted CSV row during import: $row")
          None
      }
    }

    val judges = for {
      csv <- csvContent.toSeq
      csvRow <- csv.lines if csvRow.nonEmpty
      (judgeName, judgeOrg, judgeKind) <- parseRow(csvRow)
      actualKind = judgeKind.toLowerCase.trim match {
        case "scoring" => ScoringJudge
        case _ => PresidingJudge
      }
    } yield {
      val judge = Judge(None, competitionId, judgeName.trim, judgeOrg.trim, actualKind)
      Judges.insertOrUpdate(judge)
    }

    DB.runAwait(DBIO.seq(judges: _*)).logFailure("Database error processing upload") match {
      case fail: Failure =>
        S.error("A database failure occurred. Please see the log for more information.")

      case _ =>
        S.redirectTo(JudgeList.menu.toLoc.calcHref(competition), () => S.notice(s"Uploaded ${judges.length} judges"))
    }
  }

  def render = {
    "#judge-csv-file" #> SHtml.fileUpload(csvUploadHandler _) &
    ".upload" #> SHtml.onSubmitUnit(doUpload _)
  }
}
