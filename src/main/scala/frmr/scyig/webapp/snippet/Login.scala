package frmr.scyig.webapp.snippet

import frmr.scyig.webapp.auth._
import net.liftweb.http._
import net.liftweb.http.js._
import net.liftweb.http.js.JsCmds._
import net.liftweb.sitemap._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Login {
  val menu = Menu.i("Login") / "login"
}
class Login {
  private[this] var email: String = ""
  private[this] var password: String = ""

  private[this] def authenticate = {
    AuthenticationHelpers.login_!(email, password) match {
      case AuthenticationFailure =>
        Alert("Invalid username or password")

      case AuthenticationInternalError =>
        Alert("An internal error occurred while attempting to authenticate. Try again later.")

      case AuthenticationSuccess(_) =>
        RedirectTo(CompChooser.menu.loc.calcDefaultHref)
    }
  }

  def loginForm = {
    SHtml.makeFormsAjax andThen
    "#email-input" #> SHtml.text("", email = _) &
    "#password-input" #> SHtml.password("", password = _) &
    "#login-action" #> SHtml.ajaxOnSubmit(authenticate _)
  }
}
