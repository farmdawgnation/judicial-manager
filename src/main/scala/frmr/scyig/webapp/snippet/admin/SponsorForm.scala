package frmr.scyig.webapp.snippet
package admin

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.webapp.snippet.SnippetHelpers._
import net.liftweb.http._
import net.liftweb.http.js.JsCmds._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import slick.jdbc.MySQLProfile.api._

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

class SponsorForm(sponsor: Sponsor) {
  def this() = this(Sponsor(None, "", ""))

  var currentSponsor = sponsor

  private[this] def saveSponsor() = {
    DB.runAwait(Sponsors.insertOrUpdate(currentSponsor))
    RedirectTo(SponsorList.menu.loc.calcDefaultHref, () => S.notice("Sponsor was saved"))
  }

  def render = {
    SHtml.makeFormsAjax andThen
    "#sponsor-name" #> SHtml.text(sponsor.name, v => currentSponsor = currentSponsor.copy(name = v)) &
    "#sponsor-location" #> SHtml.text(sponsor.location, v => currentSponsor = currentSponsor.copy(location = v)) &
    ".save" #> SHtml.ajaxOnSubmit(saveSponsor _)
  }
}
