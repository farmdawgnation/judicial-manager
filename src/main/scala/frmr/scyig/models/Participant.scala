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
package frmr.scyig.models

import java.util.UUID

/**
 * Our representation for someone who is participating in the judicial conference.
 * This is the unifying type for all kinds of participant.
 */
sealed trait Participant {
  def id: UUID
  def name: ParticipantName
  def organization: Option[ParticipantOrganization]
}

sealed trait Judge extends Participant
case class PresidingJudge(
  id: UUID,
  name: ParticipantName,
  organization: Option[ParticipantOrganization],
  matchHistory: Seq[HistoricalMatch]
) extends Judge
case class ScoringJudge(
  id: UUID,
  name: ParticipantName,
  organization: Option[ParticipantOrganization],
  matchHistory: Seq[HistoricalMatch]
) extends Judge

case class CompetingTeam(
  id: UUID,
  name: ParticipantName,
  private val _organization: ParticipantOrganization,
  matchHistory: Seq[HistoricalMatch]
) extends Participant {
  override val organization = Some(_organization)

  def hasPlayed_?(opponentIdentifier: UUID): Boolean = {
    matchHistory.find(hmatch =>
      hmatch.prosecutionIdentifier == opponentIdentifier ||
      hmatch.defenseIdentifier == opponentIdentifier
    ).isDefined
  }
}

case class ParticipantName(value: String)
case class ParticipantOrganization(value: String)
