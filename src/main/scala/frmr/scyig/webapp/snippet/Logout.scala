package frmr.scyig.webapp.snippet

import frmr.scyig.webapp.auth.AuthenticationHelpers._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._

object Logout {
  def menu = Menu.i("Logout") / "logout" >>
    EarlyResponse( () => {
      val logoutCookie = logout_!()
      Full(RedirectResponse(
        Login.menu.loc.calcDefaultHref,
        logoutCookie
      ))
    })
}
