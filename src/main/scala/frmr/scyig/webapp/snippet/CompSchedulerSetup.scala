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

  private[this] def convertJudgesToParticipants: Seq[matching.models.Judge] = {
    val judges = DB.runAwait(Judges.to[Seq].filter(_.competitionId === competition.id.getOrElse(-1)).result)

    judges match {
      case Full(judges) =>
        judges.map { judge =>
          Judge(
            matching.models.ParticipantName(judge.name),
            Some(matching.models.ParticipantOrganization(
              judge.organization.toLowerCase.replace(" ", "")
            )),
            isPresiding = judge.enabled && judge.kind == PresidingJudge,
            isScoring = judge.enabled && judge.kind == ScoringJudge,
            webappId = judge.id.getOrElse(-1),
            id = judge.uuid
          )
        }

      case _ =>
        throw new RuntimeException("Something went wrong accessing the DB.")
    }
  }

  private[this] def convertTeamsToParticipants: Seq[matching.models.CompetingTeam] = {
    val teams = DB.runAwait(Teams.to[Seq].filter(_.competitionId === competition.id.getOrElse(-1)).result)

    teams match {
      case Full(teams) =>
        teams.map { team =>
          matching.models.CompetingTeam(
            matching.models.ParticipantName(team.name),
            matching.models.ParticipantOrganization(
              team.organization.toLowerCase.replace(" ", "")
            ),
            webappId = team.id.getOrElse(-1),
            id = team.uuid
          )
        }

      case _ =>
        throw new RuntimeException("Something went wrong accessing the DB.")
    }
  }

  private[this] def dbByeToBye(dbBye: Bye): Box[HistoricalBye] = {
    for {
      teamUUID <- DB.runAwait(
        Teams.filter(_.id === dbBye.teamId).result.head
      ).map(_.uuid)
    } yield {
      HistoricalBye(teamUUID)
    }
  }

  private[this] def dbMatchToTrial(dbMatch: Match): Box[HistoricalTrial] = {
    for {
      matchId <- dbMatch.id
      prosecutionUUID <- DB.runAwait(
        Teams.filter(_.id === dbMatch.prosecutionTeamId).result.head
      ).map(_.uuid)
      defenseUUID <- DB.runAwait(
        Teams.filter(_.id === dbMatch.defenseTeamId).result.head
      ).map(_.uuid)
      presidingJudgeUUID <- DB.runAwait(
        Judges.filter(_.id === dbMatch.presidingJudgeId).result.head
      ).map(_.uuid)
      scoringJudgeUUID <- DB.runAwait(
        Judges.filter(_.id === dbMatch.scoringJudgeId).result.head
      ).map(_.uuid)
      prosecutionScores <- DB.runAwait(
        Scores.to[Seq].filter(_.matchId === matchId).filter(_.teamId === dbMatch.prosecutionTeamId).result
      )
      defenseScores <- DB.runAwait(
        Scores.to[Seq].filter(_.matchId === matchId).filter(_.teamId === dbMatch.defenseTeamId).result
      )
    } yield {
      HistoricalTrial(
        prosecutionUUID,
        prosecutionScores.map(_.score),
        defenseUUID,
        defenseScores.map(_.score),
        presidingJudgeUUID,
        scoringJudgeUUID
      )
    }
  }

  private[this] def decorateTeamHistory(teams: Seq[matching.models.CompetingTeam]): Seq[matching.models.CompetingTeam] = {
    for {
      team <- teams
      teamId = team.webappId

      dbMatches <- DB.runAwait(
        Matches.to[Seq].filter(f => f.prosecutionTeamId === teamId || f.defenseTeamId === teamId).result
      )
      dbByes <- DB.runAwait(
        Byes.to[Seq].filter(_.teamId === teamId).result
      )

      dbMatchesByRound = dbMatches.groupBy(_.round)
      historicalMatchesByRound = dbMatchesByRound.mapValues(_.flatMap(dbMatchToTrial))

      dbByesByRound = dbByes.groupBy(_.round)
      historicalByesByRound = dbByesByRound.mapValues(_.flatMap(dbByeToBye))
    } yield {
      val allHistory: Seq[HistoricalMatch] =
        (historicalMatchesByRound.toList ++ historicalByesByRound.toList)
          .sortBy(_._1)
          .flatMap(_._2)

      team.copy(matchHistory = allHistory)
    }
  }

  private[this] def decorateJudgeHistory(judges: Seq[matching.models.Judge]): Seq[matching.models.Judge] = {
    for {
      judge <- judges
      judgeId = judge.webappId

      dbMatches <- DB.runAwait(
        Matches.to[Seq].filter(f => f.presidingJudgeId === judgeId || f.scoringJudgeId === judgeId).result
      )

      historicalMatchesByRound = dbMatches.groupBy(_.round).mapValues(_.flatMap(dbMatchToTrial))
    } yield {
      val orderedHistory: Seq[HistoricalTrial] =
        historicalMatchesByRound.toList.sortBy(_._1).flatMap(_._2)
      judge.copy(matchHistory = orderedHistory)
    }
  }

  private[this] def suggester: (Seq[matching.models.Participant]) => matching.ParticipantSuggester = {
    def vendSuggester(innermostSuggester: matching.ParticipantSuggester) =
        matching.ByePrioritizingParticipantSuggester(innermostSuggester)

    matchingAlgorithm match {
      case Full(ChallengeMatching) =>
        (participants) => vendSuggester(matching.CompetitiveParticipantSuggester(participants))
      case Full(OpportunityMatching) =>
        (participants) => vendSuggester(matching.OpportunityParticipantSuggester(participants))
      case _ =>
        (participants) => vendSuggester(matching.RandomizedParticipantSuggester(participants))
    }
  }

  private[this] def newMatchingEngine: matching.MatchingEngine = {
    val participants =
      decorateTeamHistory(convertTeamsToParticipants) ++
      decorateJudgeHistory(convertJudgesToParticipants)
    new matching.MatchingEngine(
      participants,
      numberOfRooms.openOr(0),
      suggester = suggester,
      optimizer = matching.MatchingOptimizer.roleOpimizer _
    )
  }

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
          scoringJudgeId = trial.scoringJudge.webappId,
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
