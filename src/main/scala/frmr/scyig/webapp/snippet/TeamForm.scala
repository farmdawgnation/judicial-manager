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

object TeamForm {
  import SnippetHelpers._

  val createMenu = Menu.param[Competition](
    "Create Team",
    "Create Team",
    idToCompetition,
    _.id.getOrElse("").toString,
  ) / "competition" / * / "team" / "create" >>
    validateCompetitionAccess >>
    validateCompetitionStatus(NotStarted) >>
    TemplateBox( () => Templates("competition" :: "star" :: "teams" :: "form" :: Nil))

  val editMenu = Menu.params[(Competition, Team)](
    "Edit Team",
    "Edit Team",
    (params) => {
      for {
        competition <- idToCompetition(params.head)
        team <- idToTeam(params.last)
      } yield {
        (competition, team)
      }
    },
    (params) => params._1.id.getOrElse("").toString :: params._2.id.getOrElse("").toString :: Nil
  ) / "competition" / * / "team" / "edit" / * >>
    validateCompetitionResourceAccess >>
    validateCompetitionStatus(NotStarted) >>
    TemplateBox( () => Templates("competition" :: "star" :: "teams" :: "form" :: Nil))
}

class TeamForm(compAndTeam: (Competition, Team)) {
  def this(competition: Competition) = this(competition, Team(None, competition.id.getOrElse(0), "", ""))

  var (competition, team) = compAndTeam

  private[this] def save() = {
    DB.runAwait(Teams.insertOrUpdate(team))
  }

  private[this] def saveAndReturn = {
    save()
    RedirectTo(TeamList.menu.toLoc.calcHref(competition))
  }

  private[this] def saveAndCreateAnother = {
    save()
    RedirectTo(TeamForm.createMenu.toLoc.calcHref(competition))
  }

  def render = {
    SHtml.makeFormsAjax andThen
    "#team-name" #> SHtml.text(team.name, v => team = team.copy(name = v)) &
    "#org-name" #> SHtml.text(team.organization, v => team = team.copy(organization = v)) &
    ".save-and-create" #> (team.id.isDefined ? ClearNodes | PassThru) andThen
    ".save-and-create" #> SHtml.ajaxOnSubmit(saveAndCreateAnother _) &
    ".save" #> SHtml.ajaxOnSubmit(saveAndReturn _)
  }
}
