package frmr.scyig.webapp.snippet
package admin

import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.webapp.snippet.SnippetHelpers._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._

object CompetitionList {
  val menu = Menu.i("Competitions Administration") / "admin" / "competitions" >> validateSuperuser >>
    TemplateBox( () => Templates("admin" :: "competitions" :: "manage" :: Nil) )
}
