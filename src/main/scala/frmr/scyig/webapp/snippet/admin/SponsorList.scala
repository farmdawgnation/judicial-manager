package frmr.scyig.webapp.snippet
package admin

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.webapp.snippet.SnippetHelpers._
import slick.jdbc.MySQLProfile.api._
import net.liftweb.http._
import net.liftweb.http.js.JsCmds._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import net.liftweb.util.Helpers._

object SponsorList {
  val menu = Menu.i("Sponsors Administration") / "admin" / "sponsors" >> validateSuperuser >>
    TemplateBox( () => Templates("admin" :: "sponsors" :: "manage" :: Nil) )

  def addLink = {
    "^ [href]" #> SponsorForm.createMenu.loc.calcDefaultHref
  }

  def deleteSponsor(sponsor: Sponsor)(s: String) = {
    DB.runAwait(Sponsors.filter(_.id === sponsor.id).delete)
    Reload
  }

  def render = {
    val sponsors: List[Sponsor] =
      DB.runAwait(Sponsors.to[List].result).openOrThrowException("Error finding sponsors")

    if (sponsors.isEmpty) {
      ".sponsor-row" #> ClearNodes
    } else {
      ".no-sponsor-rows" #> ClearNodes andThen
      ".sponsor-row" #> sponsors.map { sponsor =>
        ".sponsor-id *" #> sponsor.id &
        ".sponsor-name *" #> sponsor.name &
        ".sponsor-location *" #> sponsor.location &
        ".edit-sponsor [href]" #> SponsorForm.editMenu.toLoc.calcHref(sponsor) &
        ".delete-sponsor [onclick]" #> SHtml.onEventIf(
          "Delete this sponsor?",
          deleteSponsor(sponsor)
        )
      }
    }
  }
}
