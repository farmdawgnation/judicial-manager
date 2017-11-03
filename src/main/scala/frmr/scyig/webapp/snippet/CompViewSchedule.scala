package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import net.liftweb.util.Helpers._

object CompViewSchedule {
  import SnippetHelpers._

  val menu = Menu.param[Competition](
    "View Current Schedule",
    "View Current Schedule",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "view-schedule" >> validateCompetitionAccess
}

class CompViewSchedule(competition: Competition) {
  def render = {
    "^" #> competition.currentRoundMatches.map { matches =>
      ".match-row" #> matches.zipWithIndex.map {
        case (individualMatch, index) =>
          ".match-number *" #> (index + 1) &
          ".prosecution-name *" #> individualMatch.prosecutionTeam.map(_.name) &
          ".defense-name *" #> individualMatch.defenseTeam.map(_.name) &
          ".presiding-name *" #> individualMatch.presidingJudge.map(_.name) &
          ".scoring-name *" #> individualMatch.scoringJudge.map(_.name)
      }
    }
  }
}
