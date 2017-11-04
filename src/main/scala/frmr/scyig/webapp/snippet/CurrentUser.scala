package frmr.scyig.webapp.snippet

import frmr.scyig.webapp.auth.AuthenticationHelpers._
import net.liftweb.util._
import net.liftweb.util.Helpers._

object CurrentUser {
  def rootLink =
    "^ [href]" #> CompChooser.menu.loc.calcDefaultHref

  def userName =
    "^ *" #> currentUser.is.map(_.name).getOrElse("A Ghost")

  def logoutLink =
    "^ [href]" #> Logout.menu.loc.calcDefaultHref

  def adminLink = {
    val isSuperuser = currentUser.is.map(_.superuser).getOrElse(false)

    if (! isSuperuser) {
      ClearNodes
    } else {
      "#administration-link [href]" #> admin.Dashboard.menu.loc.calcDefaultHref
    }
  }
}
