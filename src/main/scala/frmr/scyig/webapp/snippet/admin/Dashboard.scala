package frmr.scyig.webapp.snippet
package admin

import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.webapp.snippet.SnippetHelpers._
import net.liftweb.sitemap._
import net.liftweb.util.Helpers._

object Dashboard {
  val menu = Menu.i("Admin Dashboard") / "admin" / "dashboard" >> validateSuperuser

  def render = {
    "#users-management-link [href]" #> admin.UserList.menu.loc.calcDefaultHref &
    "#sponsors-management-link [href]" #> admin.SponsorList.menu.loc.calcDefaultHref &
    "#competitions-management-link [href]" #> admin.CompetitionList.menu.loc.calcDefaultHref
  }
}
