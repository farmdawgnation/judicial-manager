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

object UserForm {
  import SnippetHelpers._

  val createMenu = Menu.i("Create User") / "admin" / "users" / "create" >>
    validateSuperuser >>
    TemplateBox( () => Templates("admin" :: "users" :: "form" :: Nil) )

  val editMenu = Menu.param[User](
    "Edit User",
    "Edit User",
    idToUser,
    _.id.getOrElse("").toString
  ) / "admin" / "users" / "edit" / * >>
    validateSuperuser >>
    TemplateBox( () => Templates("admin" :: "users" :: "form" :: Nil) )
}
