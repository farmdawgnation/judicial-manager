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
  def this(tuple: (Competition, Team)) = this(tuple._1)

  def name =
    "a *" #> competition.name &
    "a [href]" #> CompDashboard.menu.toLoc.calcHref(competition)

  def status =
    ".competition-status-value *" #> competition.status.value &
    ".competition-round-numeral *" #> competition.round
}

class CompDashboard(competition: Competition) {
  private[this] def hideIfNotInProgress = {
    if (competition.status == InProgress) {
     PassThru
    } else {
      ClearNodes
    }
  }
  def render = {
    ClearClearable andThen
    ".in-progress-group" #> hideIfNotInProgress &
    ".view-teams-entry" #> hideIfNotInProgress andThen
    ".view-teams-entry" #> {
      "a [href]" #> TeamList.menu.toLoc.calcHref(competition)
    } &
    ".manage-teams-entry" #> {
      "a [href]" #> TeamList.menu.toLoc.calcHref(competition)
    } &
    ".manage-judges-entry" #> {
      "a [href]" #> JudgeList.menu.toLoc.calcHref(competition)
    }
  }
}
