package frmr.scyig.webapp.snippet
package admin

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.webapp.snippet.SnippetHelpers._
import net.liftweb.common._
import net.liftweb.common.BoxLogging._
import net.liftweb.http._
import net.liftweb.http.js._
import net.liftweb.http.js.JsCmds._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import slick.jdbc.MySQLProfile.api._

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

class UserForm(user: User) {
  def this() = this(User(None, "", "", "", false))

  var currentUser = user
  var specifiedSponsorIds = Seq[Int]()

  private[this] def setSponsorsForCreatedOrUpdatedUser(createdUserId: Option[Int]) = {
    createdUserId match {
      case Some(newUserId) =>
        specifiedSponsorIds.map { sponsorId =>
          UsersSponsors += ("", newUserId, sponsorId)
        }

      case None =>
        user.setSponsors(specifiedSponsorIds)
    }
  }

  private[this] def saveUser(): JsCmd = {
    if (currentUser.id.isDefined || currentUser.passwordHash.nonEmpty) {
      DB.runAwait((Users returning Users.map(_.id)).insertOrUpdate(currentUser))
        .map(createdUserId => setSponsorsForCreatedOrUpdatedUser(createdUserId))
        .map(_ => RedirectTo(UserList.menu.loc.calcDefaultHref, () => S.notice("User was saved")))
        .logEmptyBox("Failed to save user")
        .openOr(Alert("Failed to save user. Please see log."))
    } else {
      S.error("New users are requied to have a password.")
    }
  }

  private[this] def setUserPassword(newPassword: String) = {
    Option(newPassword).filter(_.nonEmpty) match {
      case Some(newPassword) =>
        currentUser = currentUser.copy(passwordHash = Users.hashpw(newPassword))

      case _ =>
    }
  }

  private[this] def setSuperuserStatus(status: Box[Boolean]) = {
    val newStatus = status.openOr(false)
    currentUser = currentUser.copy(superuser = newStatus)
  }

  def render = {
    val superuserStatuses = Seq(
      ("#enabled-superuser-status" -> true),
      ("#disabled-superuser-status" -> false)
    )

    val sponsors = {
      DB.runAwait(Sponsors.to[Seq].result)
        .logFailure("Couldn't get sponsors")
        .openOr(Seq())
    }

    val sponsorOptions = {
      for {
        sponsor <- sponsors
        sponsorId <- sponsor.id
      } yield {
        SHtml.SelectableOption(sponsorId, sponsor.name)
      }
    }

    SHtml.makeFormsAjax andThen
    "#user-name" #> SHtml.text(user.name, v => currentUser = currentUser.copy(name = v)) &
    "#user-email" #> SHtml.text(user.email, v => currentUser = currentUser.copy(email = v)) &
    "#user-new-password" #> SHtml.password("", setUserPassword) &
    SHtml.radioCssSel[Boolean](Full(user.superuser), setSuperuserStatus _)(superuserStatuses: _*) &
    "#sponsors" #> SHtml.multiSelectObj[Int](sponsorOptions, user.sponsorIds, specifiedSponsorIds = _) &
    ".save" #> SHtml.ajaxOnSubmit(saveUser _)
  }
}
