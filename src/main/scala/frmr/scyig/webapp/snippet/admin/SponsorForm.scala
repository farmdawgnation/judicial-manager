package frmr.scyig.webapp.snippet
package admin

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.webapp.snippet.SnippetHelpers._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import net.liftweb.util.Helpers._

object SponsorForm {
  import SnippetHelpers._

  val createMenu = Menu.i("Create Sponsor") / "admin" / "sponsors" / "create" >>
    validateSuperuser >>
    TemplateBox( () => Templates("admin" :: "sponsors" :: "form" :: Nil) )

  val editMenu = Menu.param[Sponsor](
    "Edit Sponsor",
    "Edit Sponsor",
    idToSponsor,
    _.id.getOrElse("").toString
  ) / "admin" / "sponsors" / "edit" / * >>
    validateSuperuser >>
    TemplateBox( () => Templates("admin" :: "sponsors" :: "form" :: Nil) )
}
