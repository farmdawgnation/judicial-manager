package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
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

  def idToJudge(idStr: String): Box[Judge] = {
    for {
      id <- (tryo(idStr.toInt) or Empty)
      query = Judges.filter(_.id === id).result.head
      judge <- DB.runAwait(query)
    } yield {
      judge
    }
  }

  def hideIfCompetitionIs(competition: Competition, status: CompetitionStatus) = {
    (competition.status == status) ? ClearNodes | PassThru
  }

  def hideIfCompetitionIsnt(competition: Competition, status: CompetitionStatus) = {
    (competition.status != status) ? ClearNodes | PassThru
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

  def validateCompetitionStatus(status: CompetitionStatus): TestValueAccess[Competition] = {
    TestValueAccess((competition) =>
      competition.filter(_.status != status)
        .map(competition =>
          RedirectResponse(CompDashboard.menu.toLoc.calcHref(competition))
        )
    )
  }

  def validateCompetitionStatus(status: CompetitionStatus, dummyVal: Boolean = true): TestValueAccess[(Competition, _)] = {
    TestValueAccess((resources) =>
      resources.filter(_._1.status != status)
        .map(_._1)
        .map(competition =>
          RedirectResponse(CompDashboard.menu.toLoc.calcHref(competition))
        )
    )
  }
}
