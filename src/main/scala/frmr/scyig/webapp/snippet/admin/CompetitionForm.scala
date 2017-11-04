package frmr.scyig.webapp.snippet
package admin

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.webapp.snippet.SnippetHelpers._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.js._
import net.liftweb.http.js.JsCmds._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import slick.jdbc.MySQLProfile.api._

object CompetitionForm {
  import SnippetHelpers._

  val createMenu = Menu.i("Create Competition") / "admin" / "competitions" / "create" >>
    validateSuperuser >>
    TemplateBox( () => Templates("admin" :: "competitions" :: "form" :: Nil) )

  val editMenu = Menu.param[Competition](
    "Edit Competition",
    "Edit Competition",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "admin" / "competitions" / "edit" / * >>
    validateSuperuser >>
    TemplateBox( () => Templates("admin" :: "competitions" :: "form" :: Nil) )
}

class CompetitionForm(competition: Competition) {
  def this() = this(Competition(None, "", 0, "", "", ""))

  var currentCompetition = competition

  private[this] def saveCompetition(): JsCmd = {
    DB.runAwait(Competitions.insertOrUpdate(currentCompetition)) match {
      case Failure(message, _, _) =>
        S.error(message)

      case _ =>
        RedirectTo(CompetitionList.menu.loc.calcDefaultHref, () => S.notice("Competition was saved"))
    }
  }

  private[this] val statusOptions = Seq(
    SHtml.SelectableOption(NotStarted, "Not Started"),
    SHtml.SelectableOption(InProgress, "In Progress"),
    SHtml.SelectableOption(Finished, "Finished")
  )

  private[this] val sponsorOptions =
    for {
      sponsor <- DB.runAwait(Sponsors.to[Seq].result).openOr(Seq())
      sponsorId <- sponsor.id
    } yield {
      SHtml.SelectableOption(sponsorId, sponsor.name)
    }

  def render = {
    SHtml.makeFormsAjax andThen
    "#competition-name" #> SHtml.text(competition.name, v => currentCompetition = currentCompetition.copy(name = v)) &
    "#competition-dates" #> SHtml.text(competition.dates, v => currentCompetition = currentCompetition.copy(dates = v)) &
    "#competition-description" #> SHtml.text(competition.description, v => currentCompetition = currentCompetition.copy(description = v)) &
    "#competition-location" #> SHtml.text(competition.location, v => currentCompetition = currentCompetition.copy(location = v)) &
    "#competition-status" #> SHtml.selectObj[CompetitionStatus](statusOptions, Full(competition.status), v => currentCompetition = currentCompetition.copy(status = v)) &
    "#sponsor" #> SHtml.selectObj[Int](sponsorOptions, Full(competition.sponsorId), v => currentCompetition = currentCompetition.copy(sponsorId = v)) &
    ".save" #> SHtml.ajaxOnSubmit(saveCompetition _)
  }
}
