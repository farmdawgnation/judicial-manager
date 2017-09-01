package frmr.scyig.webapp.snippet

import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.db._
import net.liftweb.common._
import net.liftweb.sitemap._
import slick.jdbc.MySQLProfile.api._

object CompChooser {
  val menu = Menu.i("Competition Chooser") / "competitions"

  val competitionsQuery = Users.to[List].filter(_.id === currentUserId.is)
    .join(UsersSponsors).on(_.id === _.userId)
    .map(_._2)
    .join(Competitions).on(_.sponsorId === _.sponsorId)
    .map(_._2)
    .result

  def listCompetitions: Box[List[Competition]] =
    DB.runAwait(competitionsQuery)
}
