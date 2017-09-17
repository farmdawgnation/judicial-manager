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
  /*
  val competitionId = competition.getOrElse {
    throw new IllegalStateException("This snippet somehow got a competition without an id")
  }

  val currentRoundMatches = DB.run(
    Matches.to[Seq]
      .filter(_.competitionId == competitionId)
  )

  def render = {
    "^" #> currentRoundMatches.map { matches =>
      ".match-row" #> matches.zipWithIndex.map {
        case (individualMatch, index) =>
          ".match-number *" #> (index + 1) &
          ".prosecution-name *" #>
      }
    }
  }*/
}
