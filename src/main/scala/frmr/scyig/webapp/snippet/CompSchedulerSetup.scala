package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import frmr.scyig.matching
import frmr.scyig.matching._
import frmr.scyig.matching.models.{HistoricalMatch, HistoricalTrial, HistoricalBye, CompetingTeam, Judge}
import frmr.scyig.webapp.comet._
import net.liftweb.actor._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util.Helpers._
import scala.collection.immutable
import slick.jdbc.MySQLProfile.api._

object CompSchedulerSetup {
  import SnippetHelpers._

  val setupMenu = Menu.param[Competition](
    "Scheduler Setup",
    "Scheduler Setup",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "scheduler-setup" >> validateCompetitionAccess

  val scheduleMenu = Menu.param[Competition](
    "Next Round Scheduler",
    "Next Round Scheduler",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "scheduler" >>
    validateCompetitionAccess >>
    TemplateBox( () => Templates("competition" :: "star" :: "schedule" :: Nil) )
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
        judges.map { judge =>
          Judge(
            matching.models.ParticipantName(judge.name),
            Some(matching.models.ParticipantOrganization(judge.organization)),
            isPresiding = judge.enabled && judge.kind == PresidingJudge,
            isScoring = judge.enabled && judge.kind == ScoringJudge,
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

  private[this] def convertMatchesToHistoricalMatches(participants: Seq[matching.models.Participant]): Seq[(Int, Seq[HistoricalMatch])] = {
    val teams = participants.collect {
      case team: matching.models.CompetingTeam => team
    }

    val matchesHeld: Seq[Match] = DB.runAwait(
      Matches.to[Seq]
        .filter(_.competitionId === competition.id.getOrElse(-1))
        .result
    ).openOrThrowException("Couldn't retrieve matches")
    val matchesByRound: immutable.Map[Int, Seq[Match]] = matchesHeld.sortBy(_.round).groupBy(_.round)

    val trialsByRound: Map[Int, Seq[HistoricalTrial]] = matchesByRound.map {
      case (round, matches) =>
        val trials = matchesHeld.map { matchHeld =>
          val scoresForRound: Seq[Score] = DB.runAwait(Scores.to[Seq].filter(_.matchId === matchHeld.id.getOrElse(0)).result).openOrThrowException(
            "Scores for round could not be retrieved"
          )

          val prosecutionUUID = participants.find(_.webappId == matchHeld.prosecutionTeamId).map(_.id).getOrElse {
            throw new RuntimeException("Could not determine prosecution UUID")
          }
          val prosecutionScores = scoresForRound.filter(_.teamId == matchHeld.prosecutionTeamId).map(_.score)

          val defenseUUID = participants.find(_.webappId == matchHeld.defenseTeamId).map(_.id).getOrElse {
            throw new RuntimeException("Could not determine defense UUID")
          }
          val defenseScores = scoresForRound.filter(_.teamId == matchHeld.defenseTeamId).map(_.score)

          val presidingUUID = participants.find(_.webappId == matchHeld.presidingJudgeId).map(_.id).getOrElse {
            throw new RuntimeException("Could not determine presiding judge UUID")
          }
          val scoringUUID = participants.find(_.webappId == matchHeld.scoringJudgeId.getOrElse(0)).map(_.id)

          matching.models.HistoricalTrial(
            prosecutionUUID,
            prosecutionScores,
            defenseUUID,
            defenseScores,
            presidingUUID,
            scoringUUID
          )
        }

        round -> trials
    }

    val byesByRound: Map[Int, Seq[HistoricalBye]] = trialsByRound.map {
      case (round, trials) =>
        val byeTeams = teams.filter { team =>
          trials.find(trial => trial.prosecutionIdentifier == team.id || trial.defenseIdentifier == team.id).isEmpty
        }

        round -> byeTeams.map(team => HistoricalBye(team.id))
    }

    trialsByRound.map({
      case (round, trials) =>
        (round, trials ++ byesByRound.get(round).getOrElse(Seq.empty))
    }).toSeq.sortBy(_._1)
  }

  private[this] def decorateParticipantHistory(participants: Seq[matching.models.Participant]): Seq[matching.models.Participant] = {
    val allMatchesByRound: Seq[(Int, Seq[HistoricalMatch])] = convertMatchesToHistoricalMatches(participants)
    var participantsByUuid = participants.map(p => (p.id, p)).toMap

    allMatchesByRound.foreach {
      case (_, historicalMatch) => historicalMatch match {
        case trial: HistoricalTrial =>
          for {
            pParticipant <- participantsByUuid.get(trial.prosecutionIdentifier)
            pTeam <- Box.asA[CompetingTeam](pParticipant)
          } {
            val updatedTeam = pTeam.copy(matchHistory = pTeam.matchHistory :+ trial)
            participantsByUuid = participantsByUuid + (pTeam.id -> updatedTeam)
          }

          for {
            dParticipant <- participantsByUuid.get(trial.defenseIdentifier)
            dTeam <- Box.asA[CompetingTeam](dParticipant)
          } {
            val updatedTeam = dTeam.copy(matchHistory = dTeam.matchHistory :+ trial)
            participantsByUuid = participantsByUuid + (dTeam.id -> updatedTeam)
          }

          for {
            pjParticipant <- participantsByUuid.get(trial.presidingJudgeIdentifier)
            pjJudge <- Box.asA[models.Judge](pjParticipant)
          } {
            val updatedJudge = pjJudge.copy(matchHistory = pjJudge.matchHistory :+ trial)
            participantsByUuid = participantsByUuid + (pjJudge.id -> updatedJudge)
          }

          for {
            scoringJudgeIdentifier <- trial.scoringJudgeIdentifier
            sjParticipant <- participantsByUuid.get(scoringJudgeIdentifier)
            sjJudge <- Box.asA[models.Judge](sjParticipant)
          } {
            val updatedJudge = sjJudge.copy(matchHistory = sjJudge.matchHistory :+ trial)
            participantsByUuid = participantsByUuid + (sjJudge.id -> updatedJudge)
          }

        case bye: HistoricalBye =>
          for {
            byeParticipant <- participantsByUuid.get(bye.teamIdentifier)
            byeTeam <- Box.asA[CompetingTeam](byeParticipant)
          } {
            val updatedTeam = byeTeam.copy(matchHistory = byeTeam.matchHistory :+ bye)
            participantsByUuid = participantsByUuid + (byeTeam.id -> updatedTeam)
          }
      }
    }

    participantsByUuid.values.toSeq
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
      decorateParticipantHistory(convertTeamsToParticipants ++ convertJudgesToParticipants),
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
        val computedSchedule = scheduleRound
        S.redirectTo(CompSchedulerSetup.scheduleMenu.toLoc.calcHref(competition), () => scheduleEditorPopulatedMatches(Full(computedSchedule)))

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
