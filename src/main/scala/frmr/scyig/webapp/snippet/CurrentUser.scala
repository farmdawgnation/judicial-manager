package frmr.scyig.webapp.snippet

import frmr.scyig.webapp.auth.AuthenticationHelpers._
import net.liftweb.util.Helpers._

object CurrentUser {
  def rootLink =
    "^ [href]" #> CompChooser.menu.loc.calcDefaultHref

  def userName =
    "^ *" #> currentUser.is.map(_.name).getOrElse("A Ghost")

  def logoutLink =
    "^ [href]" #> Logout.menu.loc.calcDefaultHref
}
