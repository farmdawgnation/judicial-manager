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

object CompetitionForm {
  import SnippetHelpers._

  val createMenu = Menu.i("Create Competition") / "admin" / "competitions" / "create" >>
    validateSuperuser >>
    TemplateBox( () => Templates("admin" :: "competitions" :: "form" :: Nil) )

  val editMenu = Menu.param[Competition](
    "Edit Competition",
    "Edit Competition",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "admin" / "competitions" / "edit" / * >>
    validateSuperuser >>
    TemplateBox( () => Templates("admin" :: "competitions" :: "form" :: Nil) )
}
