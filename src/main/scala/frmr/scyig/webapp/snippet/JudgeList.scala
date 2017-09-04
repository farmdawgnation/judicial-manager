package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.js._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.jquery.JqJE._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import slick.jdbc.MySQLProfile.api._

object JudgeList {
  import SnippetHelpers._

  val menu = Menu.param[Competition](
    "Competition Judges",
    "Manage Judges",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "judges" >>
    validateCompetitionAccess >>
    TemplateBox( () => Templates("competition" :: "star" :: "judges" :: "manage" :: Nil))
}

class JudgeList(competition: Competition) {
  def addLink =
    "^ [href]" #> JudgeForm.createMenu.toLoc.calcHref(competition)

  private[this] def deleteJudge(judge: Judge) = {
    DB.runAwait(Judges.filter(_.id === judge.id).delete)
    Reload
  }

  def doStatusQuickFlip(judgeId: Int)(): JsCmd = {
    val updatedJudge = for {
      judge <- DB.runAwait(Judges.filter(_.id === judgeId).result.head)
      updatedJudge = judge.copy(enabled = !judge.enabled)
      savedJudge <- DB.runAwait(Judges.insertOrUpdate(updatedJudge))
    } yield {
      updatedJudge
    }

    updatedJudge match {
      case Full(updatedJudge) =>
        val newText = (updatedJudge.enabled ? "Yes" | "No")
        Jq(s"[data-judge-id=$judgeId] .judge-enabled a") ~> JqText(newText)

      case _ =>
        Alert("Something unexpected occurred while quick-flipping status.")
    }
  }

  def doKindQuickFlip(judgeId: Int)(): JsCmd = {
    val updatedJudge = for {
      judge <- DB.runAwait(Judges.filter(_.id === judgeId).result.head)
      newKind = (judge.kind == PresidingJudge) ? ScoringJudge | PresidingJudge
      updatedJudge = judge.copy(kind = newKind)
      savedJudge <- DB.runAwait(Judges.insertOrUpdate(updatedJudge))
    } yield {
      updatedJudge
    }

    updatedJudge match {
      case Full(updatedJudge) =>
        Jq(s"[data-judge-id=$judgeId] .judge-kind a") ~> JqText(updatedJudge.kind.value)

      case _ =>
        Alert("Something unexpected occurred while quick-flipping status.")
    }
  }

  def render = {
    val judges: List[Judge] = DB.runAwait(Judges.to[List].result).openOrThrowException("Judges couldn't be found")

    if (judges.isEmpty) {
      ".judge-row" #> ClearNodes
    } else {
      ".no-judge-rows" #> ClearNodes &
      ".judge-row" #> judges.map { judge =>
        "^ [data-judge-id]" #> judge.id &
        ".judge-id *" #> judge.id &
        ".judge-name *" #> judge.name &
        ".judge-org *" #> judge.organization &
        ".judge-kind *" #> {
          "a *" #> judge.kind.value &
          "a [onclick]" #> SHtml.ajaxInvoke(doKindQuickFlip(judge.id.getOrElse(-1)) _)
        } &
        ".judge-enabled *" #> {
          "a *" #> (judge.enabled ? "Yes" | "No")  &
          "a [onclick]" #> SHtml.ajaxInvoke(doStatusQuickFlip(judge.id.getOrElse(-1)) _)
        } &
        ".judge-priority *" #> judge.priority.toString &
        ".edit-judge [href]" #> JudgeForm.editMenu.toLoc.calcHref(competition, judge) &
        ".delete-judge [onclick]" #> SHtml.onEventIf(s"Delete ${judge.name}?", (s: String) => deleteJudge(judge) )
      }
    }
  }
}
