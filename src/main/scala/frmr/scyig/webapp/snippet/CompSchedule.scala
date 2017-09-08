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
  import CompSchedule._
  private[this] def updateMatch(index: Int, updater: (Match)=>Match): Unit = {
    val currentMatches = populatedMatches.is
    val matchInQuestion = currentMatches(index)
    val updatedMatches = currentMatches.patch(index, updater(matchInQuestion) :: Nil, 1)
    populatedMatches(updatedMatches)
  }

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
    "^ [data-competition-id]" #> competition.id.getOrElse(0).toString &
    ".match-row" #> CompSchedule.populatedMatches.is.zipWithIndex.flatMap {
      case (m, idx) =>
        for {
          prosecution <- DB.runAwait(Teams.filter(_.id === m.prosecutionTeamId).result.head)
          defense <- DB.runAwait(Teams.filter(_.id === m.defenseTeamId).result.head)
          presidingJudge <- DB.runAwait(Judges.filter(_.id === m.presidingJudgeId).result.head)
          scoringJudge <- DB.runAwait(Judges.filter(_.id === m.scoringJudgeId.getOrElse(-1)).result.head)
        } yield {
          ".prosecution-team-id" #> SHtml.hidden(v => updateMatch(idx, _.copy(prosecutionTeamId = v.toInt)), m.prosecutionTeamId.toString) &
          ".prosecution-team [value]" #> prosecution.name &
          ".defense-team-id" #> SHtml.hidden(v => updateMatch(idx, _.copy(defenseTeamId = v.toInt)), m.defenseTeamId.toString) &
          ".defense-team [value]" #> defense.name &
          ".presiding-judge-id" #> SHtml.hidden(v => updateMatch(idx, _.copy(presidingJudgeId = v.toInt)), m.presidingJudgeId.toString) &
          ".presiding-judge [value]" #> presidingJudge.name &
          ".scoring-judge-id" #> SHtml.hidden(v => updateMatch(idx, _.copy(scoringJudgeId = Some(v.toInt))), m.scoringJudgeId.toString) &
          ".scoring-judge [value]" #> scoringJudge.name
        }
    } &
    ".bye-team" #> byeTeams.map { team =>
      "^ *" #> team.name
    }
  }
}
