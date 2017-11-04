package frmr.scyig.webapp.snippet
package admin

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.webapp.snippet.SnippetHelpers._
import slick.jdbc.MySQLProfile.api._
import net.liftweb.common.BoxLogging._
import net.liftweb.http._
import net.liftweb.http.js.JsCmds._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import net.liftweb.util.Helpers._

object UserList {
  val menu = Menu.i("Users Administration") / "admin" / "users" >> validateSuperuser >>
    TemplateBox( () => Templates("admin" :: "users" :: "manage" :: Nil) )

  def addLink = {
    "^ [href]" #> UserForm.createMenu.loc.calcDefaultHref
  }

  def deleteUser(user: User)(s: String) = {
    DB.runAwait(Users.filter(_.id === user.id).delete)
      .map(_ => Reload)
      .logFailure("Failed to delete user")
      .openOr(Alert("Failed to delete ueser. Please see log."))
  }

  def render = {
    val users: List[User] =
      DB.runAwait(Users.to[List].result).openOrThrowException("Error finding users")

    if (users.isEmpty) {
      ".user-row" #> ClearNodes
    } else {
      ".no-user-rows" #> ClearNodes andThen
      ".user-row" #> users.map { user =>
        ".user-id *" #> user.id &
        ".user-name *" #> user.name &
        ".user-email *" #> user.email &
        ".user-is-superuser *" #> (user.superuser ? "Yes" | "No") &
        ".edit-user [href]" #> UserForm.editMenu.toLoc.calcHref(user) &
        ".delete-user [onclick]" #> SHtml.onEventIf(
          "Delete this user?",
          deleteUser(user)
        )
      }
    }
  }
}
