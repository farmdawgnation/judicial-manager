package frmr.scyig.webapp.comet

import frmr.scyig.db._
import frmr.scyig.webapp.snippet._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.js._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.json._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import slick._
import slick.jdbc.MySQLProfile.api._

object scheduleEditorPopulatedMatches extends RequestVar[Box[Seq[Match]]](Empty)

class ScheduleEditor() extends CometActor with Loggable {
  private[this] val competition: Competition = S.request.flatMap(_.location).flatMap(_.currentValue).collect {
    case competition: Competition => competition
    case (item1: Competition, _) => item1
  } openOrThrowException("No competition?")

  private[this] val matchesQuery = Matches.to[Seq]
    .filter(_.competitionId === competition.id.getOrElse(0))
    .filter(_.round === competition.round)
    .result

  private[this] var currentEditorMatches: Seq[Match] = scheduleEditorPopulatedMatches.is or
    DB.runAwait(matchesQuery) openOr
    Seq.empty

  private[this] def updateMatch(index: Int, updater: (Match)=>Match): Unit = {
    val matchInQuestion = currentEditorMatches(index)
    currentEditorMatches = currentEditorMatches.patch(index, updater(matchInQuestion) :: Nil, 1)
  }

  private[this] def ajaxUpdateMatch(index: Int, updater: (Match)=>Match): Unit = {
    updateMatch(index, updater)
    reRender()
  }

  private[this] def ajaxRemoveMatch(index: Int): Unit = {
    currentEditorMatches = currentEditorMatches.patch(index, Nil, 1);
    reRender()
  }

  private[this] def ajaxAddMatch(): Unit = {
    currentEditorMatches = currentEditorMatches :+ Match(None, competition.id.getOrElse(0), 0, 0, 0, None, competition.round + 1, 0)
    reRender()
  }

  private[this] def saveSchedule(): JsCmd = {
    val inserts = currentEditorMatches.map { cem => Matches.insertOrUpdate(cem) }

    val actions = if (scheduleEditorPopulatedMatches.is.isDefined) {
      val updateCompetition = Competitions.insertOrUpdate(competition.copy(round = competition.round+1, status = InProgress))
      val allQueries = inserts :+ updateCompetition

      DBIO.seq(allQueries: _*)
    } else {
      DBIO.seq(inserts: _*)
    }

    DB.runAwait(actions) match {
      case Full(_) if scheduleEditorPopulatedMatches.is.isDefined =>
        RedirectTo(
          CompDashboard.menu.toLoc.calcHref(competition),
          () => S.notice(s"Congratulations! You've started round ${competition.round+1} of the competition!")
         )

      case Full(_) =>
        RedirectTo(
          CompDashboard.menu.toLoc.calcHref(competition),
          () => S.notice(s"The schedule for round ${competition.round} has been updated.")
        )

      case _ =>
        S.error("Something went wrong")
    }
  }

  def render = {
    val scheduledTeamIds = currentEditorMatches.flatMap { m =>
      Seq(m.prosecutionTeamId, m.defenseTeamId)
    }

    val byeTeams = DB.runAwait(Teams.to[List].filterNot(_.id inSet scheduledTeamIds).result) match {
      case Full(actualByeTeams) =>
        actualByeTeams

      case other =>
        logger.warn(s"Got unexpected result when computing bye teams: $other")
        Nil
    }

    S.appendJs(Call("window.bindSuggestions").cmd)

    SHtml.makeFormsAjax andThen
    ClearClearable andThen
    "^ [data-competition-id]" #> competition.id.getOrElse(0).toString &
    ".match-row" #> currentEditorMatches.zipWithIndex.flatMap {
      case (m, idx) =>
        for {
          prosecution <- DB.runAwait(Teams.filter(_.id === m.prosecutionTeamId).result.head) or Full(Team(None, competition.id.getOrElse(0), "", ""))
          defense <- DB.runAwait(Teams.filter(_.id === m.defenseTeamId).result.head) or Full(Team(None, competition.id.getOrElse(0), "", ""))
          presidingJudge <- DB.runAwait(Judges.filter(_.id === m.presidingJudgeId).result.head) or Full(Judge(None, competition.id.getOrElse(0), "", ""))
          scoringJudge <- DB.runAwait(Judges.filter(_.id === m.scoringJudgeId.getOrElse(-1)).result.head) or Full(Judge(None, competition.id.getOrElse(0), "", ""))
        } yield {
          ".prosecution-team-id" #> SHtml.hidden(
            v => updateMatch(idx, _.copy(prosecutionTeamId = v.toInt)),
            m.prosecutionTeamId.toString
          ) andThen
          ".prosecution-team-id [data-ajax-update-id]" #> SHtml.ajaxCall(
            "",
            v => ajaxUpdateMatch(idx, _.copy(prosecutionTeamId = v.toInt))
          ).guid &
          ".prosecution-team [value]" #> prosecution.name &
          ".defense-team-id" #> SHtml.hidden(v => updateMatch(idx, _.copy(defenseTeamId = v.toInt)), m.defenseTeamId.toString) andThen
          ".defense-team-id [data-ajax-update-id]" #> SHtml.ajaxCall(
            "",
            v => ajaxUpdateMatch(idx, _.copy(defenseTeamId = v.toInt))
          ).guid &
          ".defense-team [value]" #> defense.name &
          ".presiding-judge-id" #> SHtml.hidden(v => updateMatch(idx, _.copy(presidingJudgeId = v.toInt)), m.presidingJudgeId.toString) andThen
          ".presiding-judge-id [data-ajax-update-id]" #> SHtml.ajaxCall(
            "",
            v => ajaxUpdateMatch(idx, _.copy(presidingJudgeId = v.toInt))
          ).guid &
          ".presiding-judge [value]" #> presidingJudge.name &
          ".scoring-judge-id" #> SHtml.hidden(v => updateMatch(idx, _.copy(scoringJudgeId = Some(v.toInt))), m.scoringJudgeId.getOrElse(0).toString) andThen
          ".scoring-judge-id [data-ajax-update-id]" #> SHtml.ajaxCall(
            "",
            v => ajaxUpdateMatch(idx, _.copy(scoringJudgeId = Some(v.toInt)))
          ).guid &
          ".scoring-judge [value]" #> scoringJudge.name &
          ".remove-match [onclick]" #> SHtml.ajaxInvoke( () => ajaxRemoveMatch(idx) )
        }
    } &
    ".bye-team" #> byeTeams.map { team =>
      "^ *" #> team.name
    } &
    ".add-match [onclick]" #> SHtml.ajaxInvoke( () => ajaxAddMatch() ) &
    ".save-schedule" #> SHtml.ajaxOnSubmit( () => saveSchedule() )
  }
}
