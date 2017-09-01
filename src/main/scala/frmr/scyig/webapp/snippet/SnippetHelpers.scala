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

  def idToTeam(idStr: String): Box[Team] = {
    for {
      id <- (tryo(idStr.toInt) or Empty)
      query = Teams.filter(_.id === id).result.head
      team <- DB.runAwait(query)
    } yield {
      team 
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

  val validateCompetitionResourceAccess: TestValueAccess[(Competition, _)] = {
    TestValueAccess((resources) => resources.flatMap { resource =>
      if (currentUser.is.toSeq.flatMap(_.sponsorIds).contains(resource._1.sponsorId)) {
        Empty
      } else {
        Full(RedirectResponse(CompChooser.menu.loc.calcDefaultHref))
      }
    })
  }
}
