package frmr.scyig.webapp.snippet

import frmr.scyig.webapp.auth._
import net.liftweb.http._
import net.liftweb.http.js._
import net.liftweb.http.js.JsCmds._
import net.liftweb.util._
import net.liftweb.util.Helpers._

class Login {
  private[this] var email: String = ""
  private[this] var password: String = ""

  private[this] def authenticate = {
    Alert("bacon")
  }

  def loginForm = {
    SHtml.makeFormsAjax andThen
    "#email-input" #> SHtml.text("", email = _) &
    "#password-input" #> SHtml.password("", password = _) &
    "#login-action" #> SHtml.ajaxOnSubmit(authenticate _)
  }
}
