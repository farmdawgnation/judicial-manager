package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import net.liftweb.util.Helpers._

object CompDashboard {
  import SnippetHelpers._

  val menu = Menu.param[Competition](
    "Competition Dashboard",
    "Dashboard",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "dashboard" >> validateCompetitionAccess
}

class CompMeta(competition: Competition) {
  def name =
    "^ *" #> competition.name

  def status =
    ".competition-status-value *" #> competition.status &
    ".competition-round-numeral *" #> competition.round
}

class CompDashboard(competition: Competition) {
  private[this] def hideIfNotInProgress = {
    if (competition.status == "In Progress") {
     PassThru
    } else {
      ClearNodes
    }
  }
  def render = {
    ClearClearable andThen
    ".in-progress-group" #> hideIfNotInProgress &
    ".view-teams-entry" #> hideIfNotInProgress
  }
}
