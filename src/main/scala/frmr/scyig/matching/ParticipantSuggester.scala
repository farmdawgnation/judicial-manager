/**
 * Copyright 2016 Matthew Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
**/
package frmr.scyig.matching

import frmr.scyig.matching.models._
import java.util.UUID
import scala.math._
import scala.util.Random
import net.liftweb.common._

/**
 * Determines a list of participants that would be good to fill the next open
 * slot in the partial match.
 */
trait ParticipantSuggester extends Loggable {
  def suggestParticipants(partialMatch: Option[PartialRoundMatch]): Seq[Participant]
  def withoutParticipant(participantId: UUID): ParticipantSuggester

  val participants: Seq[Participant]

  lazy val teams: Seq[CompetingTeam] = participants.collect {
    case team: CompetingTeam => team
  }
  lazy val presidingJudges: Seq[Judge] = participants.collect {
    case judge: Judge if judge.isPresiding => judge
  }
  lazy val scoringJudges: Seq[Judge] = participants.collect {
    case judge: Judge if judge.isScoring => judge
  }

  def suggestJudges(partialMatch: Option[PartialRoundMatch]): Seq[Participant] = {
    partialMatch match {
      case Some(_: MatchedTeams) =>
        presidingJudges

      case Some(_: MatchedTeamsWithPresidingJudge) =>
        scoringJudges

      case _ =>
        Seq()
    }
  }
}

/**
 * Suggests teams in a randomized fashion. Presiding and scoring judges are suggested in the order
 * they appeared in the original participants sequence.
 */
case class RandomizedParticipantSuggester(
  override val participants: Seq[Participant]
) extends ParticipantSuggester {
  val randomizedTeams = Random.shuffle(teams)

  def suggestParticipants(partialMatch: Option[PartialRoundMatch]): Seq[Participant] = {
    partialMatch match {
      case None | Some(MatchSeed(_)) =>
        randomizedTeams

      case _ =>
        suggestJudges(partialMatch)
    }
  }

  def withoutParticipant(participantId: UUID): ParticipantSuggester = {
    new RandomizedParticipantSuggester(
      participants.filterNot(_.id == participantId)
    )
  }
}

/**
 * Suggests teams in the order of least absolute value difference in overall score. Presiding and
 * scoring judges are suggested in the order they appeared in the original participants sequence.
 */
case class CompetitiveParticipantSuggester(
  override val participants: Seq[Participant]
) extends ParticipantSuggester {
  def suggestParticipants(partialMatch: Option[PartialRoundMatch]): Seq[Participant] = {
    partialMatch match {
      case None =>
        teams.sortBy(_.averageScore).reverse

      case Some(MatchSeed(initialTeam)) =>
        teams.sortBy { team =>
          abs(team.averageScore - initialTeam.averageScore)
        }

      case _ =>
        suggestJudges(partialMatch)
    }
  }

  def withoutParticipant(participantId: UUID): ParticipantSuggester = {
    new CompetitiveParticipantSuggester(
      participants.filterNot(_.id == participantId)
    )
  }
}

/**
 * Suggests teams using an "opportunity power match" algorithm. For a new round of matching, teams
 * with the lowest scores are attempted to be scheduled first. If the seed team lacks a score, the
 * algorithm will suggest all other teams lacking a score first. If the seed team does have a score,
 * then the list of other teams is split in half into two tiers. It will then guess which tier the
 * seed team is in. It will then suggest all the members of the *other* tier first.
 */
case class OpportunityParticipantSuggester(
  override val participants: Seq[Participant]
) extends ParticipantSuggester {
  def suggestParticipants(partialMatch: Option[PartialRoundMatch]): Seq[Participant] = {
    partialMatch match {
      case None =>
        teams.sortBy(_.averageScore).reverse

      case Some(MatchSeed(initialTeam)) if ! initialTeam.hasScores_? =>
        teams.sortBy(_.hasScores_?)

      case Some(MatchSeed(initialTeam)) =>
        // highest teams first
        val sortedByScore = teams.sortBy(_.averageScore).reverse
        val listLength = sortedByScore.length

        val windowedGropings = sortedByScore.sliding(3, 3)
        val (intermediate1, intermediate2) = windowedGropings
          .zipWithIndex
          .partition(_._2 % 2 == 0) // group by even and odd indexes

        val teamListA: Seq[CompetingTeam] = intermediate1.flatMap(_._1).toSeq
        val teamListB: Seq[CompetingTeam] = intermediate2.flatMap(_._1).toSeq

        val initialTeamIsListA = teamListB.find(_.id == initialTeam.id).isEmpty

        if (initialTeamIsListA) {
          teamListB ++
          teamListA
        } else {
          teamListA ++
          teamListB
        }

      case _ =>
        suggestJudges(partialMatch)
    }
  }

  def withoutParticipant(participantId: UUID): ParticipantSuggester = {
    new OpportunityParticipantSuggester(
      participants.filterNot(_.id == participantId)
    )
  }
}

/**
 * A suggester that takes in another suggester as its sole argument. When starting a new matching
 * round, the ByePrioritizingParticipantSuggester will initially suggest teams who have a bye count
 * higher than the mean of all the bye counts of all the competing teams.
 */
case class ByePrioritizingParticipantSuggester(
  innerSuggester: ParticipantSuggester
) extends ParticipantSuggester {
  override val participants = innerSuggester.participants

  private[this] def possiblySuggestOnBye(partialMatch: Option[PartialRoundMatch]): Seq[Participant] = {
    val byeBuckets = teams.groupBy(_.byeCount)
    val byeCounts = byeBuckets.keySet.toSeq

    if (byeCounts.length > 1) {
      logger.info(s"Suggesting teams based on bye counts: ${byeBuckets.mapValues(_.map(_.name.value))}")
      teams.sortBy(_.byeCount).reverse
    } else {
      logger.info("Byes are balanced. Delegating suggestions.")
      innerSuggester.suggestParticipants(partialMatch)
    }
  }

  def suggestParticipants(partialMatch: Option[PartialRoundMatch]): Seq[Participant] = {
    logger.info("Suggesting participants")
    partialMatch match {
      case value @ None =>
        logger.info("Suggesting prosecution")
        possiblySuggestOnBye(value)

      case value @ Some(MatchSeed(_)) =>
        logger.info("Suggesting defense")
        possiblySuggestOnBye(value)

      case other =>
        logger.info("Delegating suggestions")
        innerSuggester.suggestParticipants(other)
    }
  }

  def withoutParticipant(participantId: UUID): ParticipantSuggester = {
    new ByePrioritizingParticipantSuggester(
      innerSuggester.withoutParticipant(participantId)
    )
  }
}

/**
 * A suggester that will prioritize those participants that need to fill the role its currently
 * looking at (be that prosecution or defense).
 */
case class RolePrioritizingParticipantSuggester(
  innerSuggester: ParticipantSuggester
) extends ParticipantSuggester {
  override val participants = innerSuggester.participants

  private[this] def ifHasOpinion(opinionTest: (Seq[Int])=>Boolean, opinionHandler: => Seq[Participant], noOpinionHandler: => Seq[Participant]) = {
    val roleCountBuckets = teams.groupBy(t => t.prosecutionCount - t.defenseCount)
    val bucketCount = roleCountBuckets.keySet.toSeq

    if (opinionTest(bucketCount)) {
      logger.info("This suggestor has an opinion")
      opinionHandler
    } else {
      logger.info("This suffestor has no opinion, delegating suggestions")
      noOpinionHandler
    }
  }

  def suggestParticipants(partialMatch: Option[PartialRoundMatch]): Seq[Participant] = {
    logger.info("Suggesting participants")
    partialMatch match {
      case value @ None =>
        logger.info("Suggesting prosecution")
        ifHasOpinion(
          opinionTest = (counts) => counts.find(_ < 0).isDefined,
          opinionHandler = teams.sortBy(t => t.prosecutionCount - t.defenseCount),
          noOpinionHandler = innerSuggester.suggestParticipants(value)
        )

      case value @ Some(MatchSeed(_)) =>
        logger.info("Suggesting defense")
        ifHasOpinion(
          opinionTest = (counts) => counts.find(_ > 0).isDefined,
          opinionHandler = teams.sortBy(t => t.prosecutionCount - t.defenseCount).reverse,
          noOpinionHandler = innerSuggester.suggestParticipants(value)
        )

      case other =>
        logger.info("Delegating suggestions")
        innerSuggester.suggestParticipants(other)
    }
  }

  def withoutParticipant(participantId: UUID): ParticipantSuggester = {
    new RolePrioritizingParticipantSuggester(
      innerSuggester.withoutParticipant(participantId)
    )
  }
}
