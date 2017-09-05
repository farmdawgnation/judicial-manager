package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import frmr.scyig.matching
import net.liftweb.actor._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.util.Helpers._
import slick.jdbc.MySQLProfile.api._

object CompSchedulerSetup {
  import SnippetHelpers._

  val menu = Menu.param[Competition](
    "Scheduler Setup",
    "Scheduler Setup",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "scheduler-setup" >> validateCompetitionAccess
}

sealed trait SetupMatchingAlgorithm
case object RandomizedMatching extends SetupMatchingAlgorithm
case object OpportunityMatching extends SetupMatchingAlgorithm
case object ChallengeMatching extends SetupMatchingAlgorithm

class CompSchedulerSetup(competition: Competition) extends Loggable {
  private[this] var numberOfRooms: Box[Int] = Empty
  private[this] var matchingAlgorithm: Box[SetupMatchingAlgorithm] = Empty

  private[this] def hasEnoughJudges: Boolean = {
    numberOfRooms match {
      case Full(numberOfRooms) =>
        val presidingJudgeCount = DB.runAwait(
          Judges.filter(_.kind === PresidingJudge.asInstanceOf[JudgeKind])
            .filter(_.competitionId === competition.id.getOrElse(-1))
            .filter(_.enabled)
            .length
            .result
        )

        val scoringJudgeCount = DB.runAwait(
          Judges.filter(_.kind === ScoringJudge.asInstanceOf[JudgeKind])
            .filter(_.competitionId === competition.id.getOrElse(-1))
            .filter(_.enabled)
            .length
            .result
        )

        (scoringJudgeCount, presidingJudgeCount) match {
          case (Full(scoringCount), Full(presidingCount)) =>
            presidingCount >= numberOfRooms &&
            scoringCount >= numberOfRooms

          case other =>
            logger.error(s"Error retreiving judge counts: $other")
            false
        }


      case _ =>
        false
    }
  }

  private[this] def convertJudgesToParticipants: Seq[matching.models.Participant] = {
    val judges = DB.runAwait(Judges.to[Seq].filter(_.competitionId === competition.id.getOrElse(-1)).result)

    judges match {
      case Full(judges) =>
        judges.map {
          case judge if judge.kind == PresidingJudge =>
            matching.models.PresidingJudge(
              matching.models.ParticipantName(judge.name),
              Some(matching.models.ParticipantOrganization(judge.organization)),
              webappId = judge.id.getOrElse(-1)
            )

          case judge =>
            matching.models.ScoringJudge(
              matching.models.ParticipantName(judge.name),
              Some(matching.models.ParticipantOrganization(judge.organization)),
              webappId = judge.id.getOrElse(-1)
            )
        }

      case _ =>
        throw new RuntimeException("Something went wrong accessing the DB.")
    }
  }

  private[this] def convertTeamsToParticipants: Seq[matching.models.Participant] = {
    val teams = DB.runAwait(Teams.to[Seq].filter(_.competitionId === competition.id.getOrElse(-1)).result)

    teams match {
      case Full(teams) =>
        teams.map { team =>
          matching.models.CompetingTeam(
            matching.models.ParticipantName(team.name),
            matching.models.ParticipantOrganization(team.organization),
            webappId = team.id.getOrElse(-1)
          )
        }

      case _ =>
        throw new RuntimeException("Something went wrong accessing the DB.")
    }
  }

  private[this] def suggester: (Seq[matching.models.Participant]) => matching.ParticipantSuggester = {
    matchingAlgorithm match {
      case Full(ChallengeMatching) =>
        (participants) => matching.CompetitiveParticipantSuggester(participants)
      case Full(OpportunityMatching) =>
        (participants) => matching.OpportunityParticipantSuggester(participants)
      case _ =>
        (participants) => matching.RandomizedParticipantSuggester(participants)
    }
  }

  private[this] def newMatchingEngine: matching.MatchingEngine =
    new matching.MatchingEngine(
      convertTeamsToParticipants ++ convertJudgesToParticipants,
      numberOfRooms.openOr(0),
      suggester = suggester
    )

  private[this] def scheduleRound: Seq[Match] = {
    val matchingEngine = newMatchingEngine

    matchingEngine ! matching.StartMatching

    val resultingStateFuture = new LAFuture[matching.MatchingEngineState]
    matchingEngine ! matching.QueryMatchingState(resultingStateFuture)

    val resultingState = resultingStateFuture.get

    resultingState.scheduledRounds.collect {
      case trial: matching.models.Trial =>
        Match(
          id = None,
          competitionId = competition.id.getOrElse(-1),
          prosecutionTeamId = trial.prosecution.webappId,
          defenseTeamId = trial.defense.webappId,
          presidingJudgeId = trial.presidingJudge.webappId,
          scoringJudgeId = trial.scoringJudge.map(_.webappId),
          round = competition.round + 1,
          order = 0
        )
    }
  }

  private[this] def submitSchedulerForm = {
    (numberOfRooms, matchingAlgorithm) match {
      case (Empty, Empty) => S.error("Please fill out the form before submitting.")
      case (_: Failure, _) => S.error("Please enter a valid number in number of rooms.")
      case (Empty, _) => S.error("Please enter number of rooms before submitting.")
      case (_, Empty) => S.error("Please select a matching algorithm before submitting.")

      case (Full(rooms), Full(algorithm)) if ! hasEnoughJudges =>
        S.error(s"Not enough judges. Make sure there are $rooms presiding and $rooms scoring judges.")

      case (Full(rooms), Full(algorithm)) =>
        println(scheduleRound)
        S.notice("The round was successfully scheduled")

      case (_, _) => S.error("Some unexpected error occurred while processing the form.")
    }
  }

  def render = {
    SHtml.makeFormsAjax andThen
    "#number-of-rooms" #> SHtml.text("", v => numberOfRooms = Full(v).filter(_.nonEmpty).flatMap(asInt(_))) &
    SHtml.radioCssSel[SetupMatchingAlgorithm](Empty, matchingAlgorithm = _)(
      "#randomized-matching" -> RandomizedMatching,
      "#opportunity-matching" -> OpportunityMatching,
      "#challenge-matching" -> ChallengeMatching
    ) &
    "#generate-schedule" #> SHtml.ajaxOnSubmit(submitSchedulerForm _)
  }
}
