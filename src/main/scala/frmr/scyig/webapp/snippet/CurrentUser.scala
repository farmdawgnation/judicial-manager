package frmr.scyig.webapp.snippet

import frmr.scyig.webapp.auth.AuthenticationHelpers._
import net.liftweb.util.Helpers._

object CurrentUser {
  def userName =
    "^ *" #> currentUser.is.map(_.name).getOrElse("A Ghost")

  def logoutLink =
    "^ [href]" #> "/logout"
}
