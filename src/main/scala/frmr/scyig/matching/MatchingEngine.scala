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
import net.liftweb.common._
import net.liftweb.actor._

/**
 * The brains of this operation! The matching engine is responsible for generating fully realized
 * rounds using the suggesters and by ensuring the matching policies are enforced.
 *
 * @param participants All of the participants in the competition.
 * @param numberOfRooms The number of rooms available to the matching engine.
 * @param suggester The suggester that should be used for suggesting possible members.
 */
class MatchingEngine(
  initialParticipants: Seq[Participant],
  numberOfRooms: Int,
  matchingPolicy: MatchingPolicy = MatchingPolicy.default,
  suggester: (Seq[Participant])=>ParticipantSuggester = participants=>new RandomizedParticipantSuggester(participants)
) extends LiftActor with Loggable {
  private[matching] val self = this
  private[matching] var finalState: Option[MatchingEngineState] = None

  private[this] var notifyOnFinish: Seq[LAFuture[MatchingEngineState]] = Seq.empty

  val getNumberOfRooms = numberOfRooms
  val getInitialParticipants = initialParticipants

  private[matching] def buildMatch(state: MatchingEngineState): MatchingEngineEvent = {
    val suggestions = state.suggester.suggestParticipants(state.currentlyBuildingRound)

    (state.currentlyBuildingRound, suggestions) match {
      case (None, suggestions) if suggestions.nonEmpty =>
        suggestions.headOption match {
          case Some(team: CompetingTeam) =>
            val updatedState = state.copy(
              currentlyBuildingRound = Some(MatchSeed(team)),
              remainingParticipants = state.remainingParticipants.filterNot(_ == suggestions.head)
            ).withoutParticipant(team)

            BuildMatch(updatedState)

          case unexpected =>
            MatchingError(s"Unexpected suggestion from suggestion engine $unexpected")
        }

      case (None, _) =>
        FinishMatching(state)

      case (Some(seed: MatchSeed), suggestions) if suggestions.nonEmpty =>
        val suggestedMember = suggestions.find(candidate => matchingPolicy.isValid(seed, candidate))

        suggestedMember match {
          case None =>
            BuildMatch(
              state.copy(
                scheduledRounds = state.scheduledRounds ++ seed.toByes,
                currentlyBuildingRound = None
              ).withoutParticipant(seed.team)
            )

          case Some(nextTeam: CompetingTeam) =>
            BuildMatch(
              state.copy(
                currentlyBuildingRound = Some(seed.withOtherTeam(nextTeam))
              ).withoutParticipant(nextTeam)
            )

          case Some(otherParticipant) =>
            MatchingError(s"Received an unexpected participant suggestion. Expected CompetingTeam. Got: $otherParticipant")
        }

      case (Some(matchedTeams: MatchedTeams), suggestions) if suggestions.nonEmpty =>
        val suggestedMember = suggestions.find(candidate => matchingPolicy.isValid(matchedTeams, candidate))

        suggestedMember match {
          case None =>
            BuildMatch(
              state.copy(
                scheduledRounds = state.scheduledRounds ++ matchedTeams.toByes,
                currentlyBuildingRound = None
              )
            )

          case Some(presidingJudge: Judge) if presidingJudge.isPresiding =>
            BuildMatch(
              state.copy(
                currentlyBuildingRound = Some(matchedTeams.withPresidingJudge(presidingJudge))
              ).withoutParticipant(presidingJudge)
            )

          case Some(otherParticipant) =>
            MatchingError(s"Received an unexpected participant suggestion. Expected PresidingJudge. Got: $otherParticipant")
        }

      case (Some(teamsWithPresiding: MatchedTeamsWithPresidingJudge), suggestions) if suggestions.nonEmpty =>
        val suggestedMember = suggestions.find(candidate => matchingPolicy.isValid(teamsWithPresiding, candidate))

        suggestedMember match {
          case None =>
            BuildMatch(
              state.copy(
                fullyMatchedRounds = state.fullyMatchedRounds :+ teamsWithPresiding.withScoringJudge(None),
                currentlyBuildingRound = None
              )
            )

          case scoringJudgeOpt @ Some(scoringJudge: Judge) if scoringJudge.isScoring =>
            BuildMatch(
              state.copy(
                fullyMatchedRounds = state.fullyMatchedRounds :+ teamsWithPresiding.withScoringJudge(
                  scoringJudgeOpt.asInstanceOf[Option[Judge]]
                ),
                currentlyBuildingRound = None
              ).withoutParticipant(scoringJudge)
            )

          case Some(unexpected) =>
            MatchingError(s"Received an unexpected participant suggestion. Expected ScoringJudge. Got: $unexpected")
        }

      case (Some(partialMatch: PartialRoundMatch), _) =>
        BuildMatch(
          state.copy(
            scheduledRounds = state.scheduledRounds ++ partialMatch.toByes,
            currentlyBuildingRound = None
          )
        )
    }
  }

  private[matching] def startMatching(): MatchingEngineEvent = {
    val initialState = MatchingEngineState(
      remainingParticipants = initialParticipants,
      suggester = suggester(initialParticipants)
    )
    BuildMatch(initialState)
  }

  private[matching] def finishMatching(state: MatchingEngineState): MatchingEngineEvent = {
    val updatedState = Range(0, numberOfRooms).foldLeft(state) { (latestState, roomNumber) =>
      latestState.fullyMatchedRounds.headOption.map { roundNeedingRoom =>
        latestState.copy(
          scheduledRounds = latestState.scheduledRounds :+ roundNeedingRoom.withRoom(roomNumber+1),
          fullyMatchedRounds = latestState.fullyMatchedRounds.tail
        )
      } getOrElse {
        latestState
      }
    }

    val byedState = updatedState.copy(
      fullyMatchedRounds = Seq.empty,
      scheduledRounds = updatedState.scheduledRounds ++ updatedState.fullyMatchedRounds.map(_.toByes).flatten
    )

    RecordFinalState(byedState)
  }

  private[matching] def handleMatchingEvent(matchingEvent: MatchingEngineEvent): Unit = {
    matchingEvent match {
      case StartMatching =>
        logger.info("Starting matching.")
        logger.info(s"Initial participants: $initialParticipants")
        logger.info(s"Number of rooms: $numberOfRooms")
        self ! startMatching()

      case BuildMatch(state) =>
        self ! buildMatch(state)

      case FinishMatching(state) =>
        logger.info("Matching finished.")
        self ! finishMatching(state)

      case RecordFinalState(state) =>
        finalState = Some(state)
        notifyOnFinish.foreach(r => r.complete(Full(state)))

      case MatchingError(error) =>
        logger.error(s"Matching halted with error: $error")
    }
  }

  private[matching] def handleMatchingQuery(matchingQuery: MatchingEngineQuery): Unit = {
    matchingQuery match {
      case QueryMatchingState(requester) =>
        if (finalState.isDefined) {
          finalState.foreach(s => requester.complete(Full(s)))
        }  else {
          notifyOnFinish = requester +: notifyOnFinish
        }
    }
  }

  def messageHandler = {
    case matchingEvent: MatchingEngineEvent =>
      handleMatchingEvent(matchingEvent)
    case matchingQuery: MatchingEngineQuery =>
      handleMatchingQuery(matchingQuery)
    case unexpected =>
      logger.error(s"Unexpected message sent to the Matching Engine: $unexpected")
  }
}

/**
 * The full matching engine state.
 */
case class MatchingEngineState(
  suggester: ParticipantSuggester,
  scheduledRounds: Seq[ScheduledRoundMatch] = Seq.empty,
  fullyMatchedRounds: Seq[CompletedRoundMatch] = Seq.empty,
  currentlyBuildingRound: Option[PartialRoundMatch] = None,
  remainingParticipants: Seq[Participant] = Seq.empty,
  eventAuditLog: Seq[MatchingEngineEventEntry] = Seq()
) {
  def withoutParticipant(participant: Participant): MatchingEngineState = {
    copy(
      suggester = suggester.withoutParticipant(participant.id),
      remainingParticipants = remainingParticipants.filterNot(_ == participant)
    )
  }
}

/**
 * An event that the matching engine is expected to respond to.
 */
sealed trait MatchingEngineEvent
case object StartMatching extends MatchingEngineEvent
case class FinishMatching(state: MatchingEngineState) extends MatchingEngineEvent
case class RecordFinalState(state: MatchingEngineState) extends MatchingEngineEvent
case class BuildMatch(state: MatchingEngineState) extends MatchingEngineEvent
case class MatchingError(error: String) extends MatchingEngineEvent

case class MatchingEngineEventEntry(clazz: Class[_], message: String)

sealed trait MatchingEngineQuery
case class QueryMatchingState(requester: LAFuture[MatchingEngineState]) extends MatchingEngineQuery
