package frmr.scyig.webapp.snippet

import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.db._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import slick.jdbc.MySQLProfile.api._

object CompChooser {
  val menu = Menu.i("Competition Chooser") / "competitions" >>
    If(() => S.loggedIn_?, RedirectResponse(Login.menu.loc.calcDefaultHref))


  def competitionsQuery = Users.to[List].filter(_.id === currentUserId.is)
    .join(UsersSponsors).on(_.id === _.userId)
    .map(_._2)
    .join(Competitions).on(_.sponsorId === _.sponsorId)
    .map(_._2)
    .result

  private[this] def listCompetitions: Box[List[Competition]] =
    DB.runAwait(competitionsQuery)

  def render = {
    ClearClearable andThen
    ".competition-entry" #> listCompetitions.openOr(Nil).map { competition =>
      ".competition-selection-title *" #> competition.name &
      ".competition-selection-location *" #> competition.description &
      ".competition-selection-date *" #> competition.dates &
      ".select-competition [href]" #> CompDashboard.menu.toLoc.calcHref(competition)
    }
  }
}
