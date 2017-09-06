package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import slick.jdbc.MySQLProfile.api._

object CompSchedule {
  import SnippetHelpers._

  object populatedMatches extends RequestVar[Seq[Match]](Seq.empty)

  val menu = Menu.param[Competition](
    "Schedule",
    "Schedule",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "schedule" >> validateCompetitionAccess
}

class CompSchedule(competition: Competition) extends Loggable {
  def render = {
    val scheduledTeamIds = CompSchedule.populatedMatches.is.flatMap { m =>
      Seq(m.prosecutionTeamId, m.defenseTeamId)
    }
    val byeTeams = DB.runAwait(Teams.to[List].filterNot(_.id inSet scheduledTeamIds).result) match {
      case Full(actualByeTeams) =>
        actualByeTeams

      case other =>
        logger.warn(s"Got unexpected result when computing bye teams: $other")
        Nil
    }

    ClearClearable andThen
    ".match-row" #> CompSchedule.populatedMatches.is.flatMap { m =>
      for {
        prosecution <- DB.runAwait(Teams.filter(_.id === m.prosecutionTeamId).result.head)
        defense <- DB.runAwait(Teams.filter(_.id === m.defenseTeamId).result.head)
        presidingJudge <- DB.runAwait(Judges.filter(_.id === m.presidingJudgeId).result.head)
        scoringJudge <- DB.runAwait(Judges.filter(_.id === m.scoringJudgeId.getOrElse(-1)).result.head)
      } yield {
        ".prosecution-team [value]" #> prosecution.name &
        ".defense-team [value]" #> defense.name &
        ".presiding-judge [value]" #> presidingJudge.name &
        ".scoring-judge [value]" #> scoringJudge.name
      }
    } &
    ".bye-team" #> byeTeams.map { team =>
      "^ *" #> team.name
    }
  }
}
