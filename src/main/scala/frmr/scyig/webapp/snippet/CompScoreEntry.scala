package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.util._
import net.liftweb.util.CanResolveAsync._
import net.liftweb.util.Helpers._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml._
import slick._
import slick.jdbc.MySQLProfile.api._

object CompScoreEntry {
  import SnippetHelpers._

  val menu = Menu.param[Competition](
    "Scores",
    "Scores",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "scores" >> validateCompetitionAccess
}

class CompScoreEntry(competition: Competition) {
  def render = {
    val matchesQuery = {
      Matches.to[Seq]
        .filter(_.competitionId === competition.id.getOrElse(0))
        .filter(_.round === competition.round)
        .result
    }

    val matchResults = DB.runAwait(matchesQuery)

    ClearClearable andThen
    SHtml.makeFormsAjax andThen
    "tbody" #> matchResults.map { roundMatches =>
      ".match-row" #> roundMatches.map { m =>
        val prosecution = DB.runAwait(
          Teams.filter(_.id === m.prosecutionTeamId).result.head
        )

        val defense = DB.runAwait(
          Teams.filter(_.id === m.defenseTeamId).result.head
        )

        val presidingJudge = DB.runAwait(
          Judges.filter(_.id === m.presidingJudgeId).result.head
        )

        val scoringJudge = DB.runAwait(
          Judges.filter(_.id === m.scoringJudgeId).result.head
        )

        val presidingProsecutionScore = DB.runAwait(
          Scores.filter(_.matchId === m.id.getOrElse(0))
            .filter(_.scorerId === m.presidingJudgeId)
            .result
        )

        val scoringJudgeScore = DB.runAwait(
          Scores.filter(_.matchId === m.id.getOrElse(0))
            .filter(_.scorerId === m.scoringJudgeId)
            .result
        )

        ".prosecution *" #> prosecution.map(_.name) &
        ".defense *" #> defense.map(_.name) &
        ".judges" #> {
          ".presiding *" #> presidingJudge.map(_.name) &
          ".scoring *" #> scoringJudge.map(_.name)
        }
      }
    }
  }
}
