package frmr.scyig.webapp.snippet
package admin

import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.webapp.snippet.SnippetHelpers._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._

object UserList {
  val menu = Menu.i("Users Administration") / "admin" / "users" >> validateSuperuser >>
    TemplateBox( () => Templates("admin" :: "users" :: "manage" :: Nil) )
}
