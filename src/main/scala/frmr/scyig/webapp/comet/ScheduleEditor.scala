package frmr.scyig.webapp.comet

import frmr.scyig.db._
import frmr.scyig.webapp.snippet._
import net.liftweb.common._
import net.liftweb.common.BoxLogging._
import net.liftweb.http._
import net.liftweb.http.js._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JE._
import net.liftweb.json._
import net.liftweb.json.Extraction._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import slick._
import slick.jdbc.MySQLProfile.api._

case class MatchViewModel(
  prosecutionTeamId: Int,
  prosecutionTeamName: String,
  defenseTeamId: Int,
  defenseTeamName: String,
  presidingJudgeId: Int,
  presidingJudgeName: String,
  scoringJudgeId: Int,
  scoringJudgeName: String
) {
  def toModel(competitionId: Int, round: Int, order: Int): Match = {
    Match(
      None,
      competitionId,
      prosecutionTeamId,
      defenseTeamId,
      presidingJudgeId,
      scoringJudgeId,
      round,
      order
    )
  }
}
object MatchViewModel {
  def apply(model: Match): MatchViewModel = {
    MatchViewModel(
      model.prosecutionTeamId,
      model.prosecutionTeam.map(_.name).openOr(""),
      model.defenseTeamId,
      model.defenseTeam.map(_.name).openOr(""),
      model.presidingJudgeId,
      model.presidingJudge.map(_.name).openOr(""),
      model.scoringJudgeId,
      model.scoringJudge.map(_.name).openOr("")
    )
  }
}

case class TeamViewModel(
  id: Int,
  name: String
)
object TeamViewModel {
  def apply(team: Team): TeamViewModel = {
    TeamViewModel(
      team.id.getOrElse(0),
      team.name
    )
  }
}

object scheduleEditorPopulatedMatches extends RequestVar[Box[Seq[Match]]](Empty)

class ScheduleEditor() extends CometActor with Loggable {
  implicit val formats = DefaultFormats

  private[this] val competition: Competition = S.request.flatMap(_.location).flatMap(_.currentValue).collect {
    case competition: Competition => competition
    case (item1: Competition, _) => item1
  } openOrThrowException("No competition?")

  private[this] val matchesQuery = Matches.to[Seq]
    .filter(_.competitionId === competition.id.getOrElse(0))
    .filter(_.round === competition.round)
    .result

  private[this] val isCreatingNewRound: Boolean = scheduleEditorPopulatedMatches.is.isDefined

  private[this] def saveSchedule(serializedJson: String): JsCmd = {
    val viewModelCollection = parse(serializedJson).extract[List[MatchViewModel]]

    val actions = if (isCreatingNewRound) {
      val updateCompetition = Competitions.insertOrUpdate(competition.copy(round = competition.round+1, status = InProgress))

      val matches = for {
        (viewModel, index) <- viewModelCollection.zipWithIndex
        dbModel = viewModel.toModel(
          competition.id.getOrElse(0),
          competition.round + 1,
          index
        )
      } yield {
        dbModel
      }
      val inserts = matches.map(Matches.insertOrUpdate)
      val insertByes = calculatedByes(matches).map({ team => Bye(
        None,
        competition.id.getOrElse(0),
        team.id.getOrElse(0),
        competition.round + 1
      )}).map(Byes += _)

      val allQueries = inserts ++ insertByes :+ updateCompetition

      DBIO.seq(allQueries: _*).transactionally
    } else {
      val clearMatches = Matches
        .filter(_.competitionId === competition.id.getOrElse(0))
        .filter(_.round === competition.round)
        .delete
      val matches = for {
        (viewModel, index) <- viewModelCollection.zipWithIndex
        dbModel = viewModel.toModel(
          competition.id.getOrElse(0),
          competition.round,
          index
        )
      } yield {
        dbModel
      }
      val insertMatches = matches.map(Matches.insertOrUpdate)


      val clearByes = Byes
        .filter(_.competitionId === competition.id.getOrElse(0))
        .filter(_.round === competition.round)
        .delete
      val insertByes = calculatedByes(matches).map({ team => Bye(
        None,
        competition.id.getOrElse(0),
        team.id.getOrElse(0),
        competition.round
      )}).map(Byes += _)

      val allQueries = Seq(clearMatches, clearByes) ++ insertMatches ++ insertByes

      DBIO.seq(allQueries: _*).transactionally
    }

    DB.runAwait(actions).logEmptyBox("Error saving schedule") match {
      case Full(_) if isCreatingNewRound =>
        RedirectTo(
          CompDashboard.menu.toLoc.calcHref(competition),
          () => S.notice(s"Congratulations! You've started round ${competition.round+1} of the competition!")
         )

      case Full(_) =>
        RedirectTo(
          CompDashboard.menu.toLoc.calcHref(competition),
          () => S.notice(s"The schedule for round ${competition.round} has been updated.")
        )

      case Failure(message, Full(ex), _) =>
        S.error("Something went wrong")
    }
  }

  private[this] def calculatedByes(codedMatches: Seq[Match]) = {
    val scheduledTeamIds = codedMatches.flatMap { m =>
      Seq(m.prosecutionTeamId, m.defenseTeamId)
    }

    val teamsQuery = Teams.to[List]
      .filter(_.competitionId === competition.id.getOrElse(0))
      .filterNot(_.id inSet scheduledTeamIds)
      .result

    DB.runAwait(teamsQuery) match {
      case Full(actualByeTeams) =>
        actualByeTeams

      case other =>
        logger.warn(s"Got unexpected result when computing bye teams: $other")
        Nil
    }
  }

  def render = {
    val initialMatches: Seq[Match] = scheduleEditorPopulatedMatches.is or
      DB.runAwait(matchesQuery) openOr
      Seq.empty

    val viewModelMatches = initialMatches.map(MatchViewModel(_))
    val viewModelMatchesJson = decompose(viewModelMatches)
    S.appendJs(JE.Call("judicialManager.setSchedule", viewModelMatchesJson))

    ".save-schedule [onclick]" #> SHtml.ajaxCall(JE.Call("judicialManager.serializeSchedule"), saveSchedule _)
  }
}
