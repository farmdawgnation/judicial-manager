package frmr.scyig.webapp.snippet
package admin

import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.webapp.snippet.SnippetHelpers._
import net.liftweb.sitemap._

object Dashboard {
  val menu = Menu.i("Admin Dashboard") / "admin" / "dashboard" >> validateSuperuser
}
