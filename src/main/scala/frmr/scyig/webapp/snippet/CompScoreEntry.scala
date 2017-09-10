package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.js.JsCmds._
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
  private[this] def recordScore(matchId: Int, teamId: Int, scorerId: Int, score: Int) = {
    val deleteExisting = Scores
      .filter(_.matchId === matchId)
      .filter(_.teamId === teamId)
      .filter(_.scorerId === scorerId)
      .delete

    val addNew = Scores += Score(matchId, teamId, scorerId, score)

    DB.runAwait(DBIO.seq(deleteExisting, addNew)).openOrThrowException("Error recording scores")
  }

  private[this] def submitScores() = {
    RedirectTo(
      CompDashboard.menu.toLoc.calcHref(competition),
      () => S.notice("Scores have been saved")
    )
  }

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
            .filter(_.teamId === m.prosecutionTeamId)
            .filter(_.scorerId === m.presidingJudgeId)
            .map(_.score)
            .result
            .head
        )

        val presidingDefenseScore = DB.runAwait(
          Scores.filter(_.matchId === m.id.getOrElse(0))
            .filter(_.teamId === m.defenseTeamId)
            .filter(_.scorerId === m.presidingJudgeId)
            .map(_.score)
            .result
            .head
        )

        val scoringProsecutionScore = DB.runAwait(
          Scores.filter(_.matchId === m.id.getOrElse(0))
            .filter(_.teamId === m.prosecutionTeamId)
            .filter(_.scorerId === m.scoringJudgeId)
            .map(_.score)
            .result
            .head
        )

        val scoringDefenseScore = DB.runAwait(
          Scores.filter(_.matchId === m.id.getOrElse(0))
            .filter(_.teamId === m.defenseTeamId)
            .filter(_.scorerId === m.scoringJudgeId)
            .map(_.score)
            .result
            .head
        )

        ".prosecution *" #> prosecution.map(_.name) &
        ".defense *" #> defense.map(_.name) &
        ".judges" #> {
          ".presiding *" #> presidingJudge.map(_.name) &
          ".scoring *" #> scoringJudge.map(_.name)
        } &
        ".presiding-prosecution-score" #> SHtml.text(
          presidingProsecutionScore.map(_.toString).openOr(""),
          (v) => asInt(v).map(score => recordScore(
            m.id.getOrElse(0),
            m.prosecutionTeamId,
            m.presidingJudgeId,
            score
          ))
        ) &
        ".presiding-defense-score" #> SHtml.text(
          presidingDefenseScore.map(_.toString).openOr(""),
          (v) => asInt(v).map(score => recordScore(
            m.id.getOrElse(0),
            m.defenseTeamId,
            m.presidingJudgeId,
            score
          ))
        ) &
        ".scoring-prosecution-score" #> SHtml.text(
          scoringProsecutionScore.map(_.toString).openOr(""),
          (v) => asInt(v).map(score => recordScore(
            m.id.getOrElse(0),
            m.prosecutionTeamId,
            m.scoringJudgeId.getOrElse(0),
            score
          ))
        ) &
        ".scoring-defense-score" #> SHtml.text(
          scoringDefenseScore.map(_.toString).openOr(""),
          (v) => asInt(v).map(score => recordScore(
            m.id.getOrElse(0),
            m.defenseTeamId,
            m.scoringJudgeId.getOrElse(0),
            score
          ))
        )

      }
    } &
    ".save-scores" #> SHtml.ajaxOnSubmit(submitScores _)
  }
}
