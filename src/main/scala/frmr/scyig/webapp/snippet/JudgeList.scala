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

  def render = {
    val judges: List[Judge] = DB.runAwait(Judges.to[List].result).openOrThrowException("Judges couldn't be found")

    if (judges.isEmpty) {
      ".judge-row" #> ClearNodes
    } else {
      ".no-judge-rows" #> ClearNodes &
      ".judge-row" #> judges.map { judge =>
        ".judge-id *" #> judge.id &
        ".judge-name *" #> judge.name &
        ".judge-org *" #> judge.organization &
        ".judge-kind *" #> judge.kind.value &
        ".edit-judge [href]" #> JudgeForm.editMenu.toLoc.calcHref(competition, judge) &
        ".delete-judge [onclick]" #> SHtml.ajaxInvoke( () => deleteJudge(judge) )
      }
    }
  }
}
