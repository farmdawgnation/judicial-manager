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

import frmr.scyig.models._
import java.util.UUID
import scala.math._
import scala.util.Random

/**
 * Determines a list of participants that would be good to fill the next open
 * slot in the partial match.
 */
trait ParticipantSuggester {
  def suggestParticipants(partialMatch: Option[PartialRoundMatch]): Seq[Participant]
  def withoutParticipant(participantId: UUID): ParticipantSuggester

  val participants: Seq[Participant]

  lazy val teams: Seq[CompetingTeam] = participants.collect {
    case team: CompetingTeam => team
  }
  lazy val presidingJudges: Seq[PresidingJudge] = participants.collect {
    case judge: PresidingJudge => judge
  }
  lazy val scoringJudges: Seq[ScoringJudge] = participants.collect {
    case judge: ScoringJudge => judge
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
        teams.sortBy(_.averageScore)

      case Some(MatchSeed(initialTeam)) if ! initialTeam.hasScores_? =>
        teams.sortBy(_.hasScores_?)

      case Some(MatchSeed(initialTeam)) =>
        val sortedByScore = teams.sortBy(_.averageScore)
        val listLength = sortedByScore.length

        val tier1 = sortedByScore.slice(0, listLength/2)
        val tier2 = sortedByScore.slice(listLength/2, listLength)

        val initialTeamWasLikelyTier1 = tier2
          .headOption
          .map(_.averageScore < initialTeam.averageScore)
          .getOrElse(false)

        if (initialTeamWasLikelyTier1) {
          tier2 ++
          tier1
        } else {
          tier1 ++
          tier2
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
case class ByePrioritizingParticipantSuggester[T <: ParticipantSuggester](
  innerSuggester: T
)(mf: Manifest[T]) extends ParticipantSuggester {
  override val participants = innerSuggester.participants

  def suggestParticipants(partialMatch: Option[PartialRoundMatch]): Seq[Participant] = {
    //TODO!
    innerSuggester.suggestParticipants(partialMatch)
  }

  def withoutParticipant(participantId: UUID): ParticipantSuggester = {
    // TODO!
    ???
  }
}
