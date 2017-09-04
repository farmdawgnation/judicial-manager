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

object JudgeForm {
  import SnippetHelpers._

  val createMenu = Menu.param[Competition](
    "Create Judge",
    "Create Judge",
    idToCompetition,
    _.id.getOrElse("").toString,
  ) / "competition" / * / "judge" / "create" >>
    validateCompetitionAccess >>
    TemplateBox( () => Templates("competition" :: "star" :: "judges" :: "form" :: Nil))

  val editMenu = Menu.params[(Competition, Judge)](
    "Edit Judge",
    "Edit Judge",
    (params) => {
      for {
        competition <- idToCompetition(params.head)
        judge <- idToJudge(params.last)
      } yield {
        (competition, judge)
      }
    },
    (params) => params._1.id.getOrElse("").toString :: params._2.id.getOrElse("").toString :: Nil
  ) / "competition" / * / "judge" / "edit" / * >>
    validateCompetitionResourceAccess >>
    TemplateBox( () => Templates("competition" :: "star" :: "judges" :: "form" :: Nil))
}

class JudgeForm(compAndJudge: (Competition, Judge)) {
  def this(competition: Competition) = this(competition, Judge(None, competition.id.getOrElse(0), "", "", PresidingJudge))

  var (competition, judge) = compAndJudge

  private[this] def save() = {
    DB.runAwait(Judges.insertOrUpdate(judge))
  }

  private[this] def saveAndReturn = {
    save()
    RedirectTo(JudgeList.menu.toLoc.calcHref(competition))
  }

  private[this] def saveAndCreateAnother = {
    save()
    RedirectTo(JudgeForm.createMenu.toLoc.calcHref(competition))
  }

  def render = {
    SHtml.makeFormsAjax andThen
    "#judge-name" #> SHtml.text(judge.name, v => judge = judge.copy(name = v)) &
    "#judge-org-name" #> SHtml.text(judge.organization, v => judge = judge.copy(organization = v)) &
    SHtml.radioCssSel[JudgeKind](Full(judge.kind), v => v.foreach(v => judge = judge.copy(kind = v)))(
      "#presiding-judge-kind" -> PresidingJudge,
      "#scoring-judge-kind" -> ScoringJudge
    ) &
    ".save-and-create" #> (judge.id.isDefined ? ClearNodes | PassThru) andThen
    ".save-and-create" #> SHtml.ajaxOnSubmit(saveAndCreateAnother _) &
    ".save" #> SHtml.ajaxOnSubmit(saveAndReturn _)
  }
}
