package frmr.scyig.webapp.snippet
package admin

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.webapp.snippet.SnippetHelpers._
import slick.jdbc.MySQLProfile.api._
import net.liftweb.common.BoxLogging._
import net.liftweb.http._
import net.liftweb.http.js.JsCmds._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import net.liftweb.util.Helpers._

object CompetitionList {
  val menu = Menu.i("Competitions Administration") / "admin" / "competitions" >> validateSuperuser >>
    TemplateBox( () => Templates("admin" :: "competitions" :: "manage" :: Nil) )

  def addLink = {
    "^ [href]" #> CompetitionForm.createMenu.loc.calcDefaultHref
  }

  def deleteCompetition(competition: Competition)(s: String) = {
    DB.runAwait(Competitions.filter(_.id === competition.id).delete)
      .map(_ => Reload)
      .logFailure("Failed to delete competition")
      .openOr(Alert("Failed to delete competition. Please see log."))
  }

  def render = {
    val competitions: List[Competition] =
      DB.runAwait(Competitions.to[List].result).openOrThrowException("Error finding competitions")

    if (competitions.isEmpty) {
      ".competition-row" #> ClearNodes
    } else {
      ".no-competition-rows" #> ClearNodes andThen
      ".competition-row" #> competitions.map { competition =>
        ".competition-id *" #> competition.id &
        ".competition-name *" #> competition.name &
        ".competition-sponsor *" #> competition.sponsor.map(_.name) &
        ".edit-competition [href]" #> CompetitionForm.editMenu.toLoc.calcHref(competition) &
        ".delete-competition [onclick]" #> SHtml.onEventIf(
          "Delete this competition?",
          deleteCompetition(competition)
        )
      }
    }
  }
}
