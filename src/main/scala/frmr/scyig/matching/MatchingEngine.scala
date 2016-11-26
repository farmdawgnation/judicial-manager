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
import net.liftweb.common._
import net.liftweb.actor._

/**
 * The brains of this operation! The matching engine is responsible for generating fully realized
 * rounds using the suggesters and by ensuring the matching policies are enforced.
 *
 * @param participants All of the participants in the competition.
 * @param roundNumber The round number the matching engine is initilized for.
 * @param numberOfRooms The number of rooms available to the matching engine.
 * @param suggester The suggester that should be used for suggesting possible members.
 */
class MatchingEngine(
  initialParticipants: Seq[Participant],
  roundNumber: Int,
  numberOfRooms: Int,
  matchingPolicy: MatchingPolicy,
  suggester: (Seq[Participant])=>ParticipantSuggester = participants=>new RandomizedParticipantSuggester(participants)
) extends LiftActor with Loggable {
  val self = this

  def buildMatch(state: MatchingEngineState): Unit = {
    val suggestions = state.suggester.suggestParticipants(state.currentlyBuildingRound)

    (state.currentlyBuildingRound, suggestions) match {
      case (None, suggestions) if suggestions.nonEmpty =>
        suggestions.headOption match {
          case Some(team: CompetingTeam) =>
            val updatedState = state.copy(
              currentlyBuildingRound = Some(MatchSeed(team)),
              remainingParticipants = state.remainingParticipants.filterNot(_ == suggestions.head)
            )

            self ! BuildMatch(updatedState)

          case unexpected =>
            logger.error(s"Unexpected suggestion from suggestion engine $unexpected")
        }

      case (Some(seed: MatchSeed), suggestions) if suggestions.nonEmpty =>
        val suggestedMember = suggestions.find(candidate => matchingPolicy.isValid(seed, candidate))

        suggestedMember match {
          case None =>
            self ! BuildMatch(
              state.copy(
                scheduledRounds = state.scheduledRounds ++ seed.toByes,
                currentlyBuildingRound = None
              ).withoutParticipant(seed.team)
            )

          case Some(nextTeam: CompetingTeam) =>
            self ! BuildMatch(
              state.copy(
                currentlyBuildingRound = Some(seed.withOtherTeam(nextTeam))
              ).withoutParticipant(nextTeam)
            )

          case Some(otherParticipant) =>
            self ! MatchingError(s"Received an unexpected participant suggestion. Expected CompetingTeam. Got: $otherParticipant")
        }

      case _ =>
        self ! FinishMatching
    }
  }

  def startMatching(): Unit = {
    val initialState = MatchingEngineState(
      remainingParticipants = initialParticipants,
      suggester = suggester(initialParticipants)
    )
    self ! BuildMatch(initialState)
  }

  def handleMatchingEvent(matchingEvent: MatchingEngineEvent): Unit = {
    matchingEvent match {
      case StartMatching =>
        startMatching()

      case BuildMatch(state) =>
        buildMatch(state)

      case FinishMatching =>

      case MatchingError(error) =>
    }
  }

  def messageHandler = {
    case matchingEvent: MatchingEngineEvent =>
      handleMatchingEvent(matchingEvent)
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
  eventAuditLog: Seq[MatchingEngineEvent] = Seq(StartMatching)
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
case object FinishMatching extends MatchingEngineEvent
case class BuildMatch(state: MatchingEngineState) extends MatchingEngineEvent
case class MatchingError(error: String) extends MatchingEngineEvent
