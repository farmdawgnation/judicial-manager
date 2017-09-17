package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.js.JsCmds._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import slick.jdbc.MySQLProfile.api._

object TeamUpload {
  import SnippetHelpers._

  val menu = Menu.param[Competition](
    "Upload Teams",
    "Upload Teams",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "teams" / "upload" >>
    validateCompetitionAccess
}
