package frmr.scyig.webapp.snippet
package admin

import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.webapp.snippet.SnippetHelpers._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._

object SponsorList {
  val menu = Menu.i("Sponsors Administration") / "admin" / "sponsors" >> validateSuperuser >>
    TemplateBox( () => Templates("admin" :: "sponsors" :: "manage" :: Nil) )
}
