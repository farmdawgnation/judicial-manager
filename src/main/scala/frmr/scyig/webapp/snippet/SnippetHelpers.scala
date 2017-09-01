package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap.Loc._
import net.liftweb.util.Helpers._
import slick.jdbc.MySQLProfile.api._

object SnippetHelpers {
  def idToCompetition(idStr: String): Box[Competition] = {
    for {
      id <- (tryo(idStr.toInt) or Empty)
      query = Competitions.filter(_.id === id).result.head
      competition <- DB.runAwait(query)
    } yield {
      competition
    }
  }

  val validateCompetitionAccess: TestValueAccess[Competition] = {
    TestValueAccess((competiton) => competiton.flatMap { comp =>
      if (currentUser.is.toSeq.flatMap(_.sponsorIds).contains(comp.sponsorId)) {
        Empty
      } else {
        Full(RedirectResponse(CompChooser.menu.loc.calcDefaultHref))
      }
    })
  }
}
