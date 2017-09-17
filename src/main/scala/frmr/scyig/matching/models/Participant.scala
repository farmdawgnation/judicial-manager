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
package frmr.scyig.matching.models

import java.util.UUID

/**
 * Our representation for someone who is participating in the judicial conference.
 * This is the unifying type for all kinds of participant.
 */
sealed trait Participant {
  def id: UUID
  def name: ParticipantName
  def organization: Option[ParticipantOrganization]
  def webappId: Int
}

case class Judge(
  name: ParticipantName,
  organization: Option[ParticipantOrganization],
  matchHistory: Seq[HistoricalTrial] = Seq.empty,
  isPresiding: Boolean = false,
  isScoring: Boolean = false,
  id: UUID = UUID.randomUUID(),
  webappId: Int = -1
) extends Participant {
  def hasJudged_?(participantIdentifier: UUID): Boolean = {
    matchHistory.find(hmatch =>
      hmatch.prosecutionIdentifier == participantIdentifier ||
      hmatch.defenseIdentifier == participantIdentifier ||
      hmatch.presidingJudgeIdentifier == participantIdentifier
    ).isDefined
  }
}

case class CompetingTeam(
  name: ParticipantName,
  private val _organization: ParticipantOrganization,
  matchHistory: Seq[HistoricalMatch] = Seq.empty,
  id: UUID = UUID.randomUUID(),
  webappId: Int = -1
) extends Participant {
  override val organization = Some(_organization)

  lazy val playedMatches = matchHistory.collect {
    case trial: HistoricalTrial => trial
  }

  def hasPlayed_?(opponentIdentifier: UUID): Boolean = {
    playedMatches.find(hmatch =>
      hmatch.prosecutionIdentifier == opponentIdentifier ||
      hmatch.defenseIdentifier == opponentIdentifier
    ).isDefined
  }

  lazy val hasScores_? = playedMatches.nonEmpty
  lazy val averageScore: Double = playedMatches.flatMap(_.scoreFor(id)).foldLeft(0D)(_ + _) / playedMatches.length
  lazy val byeCount = matchHistory.count(_.isInstanceOf[HistoricalBye])
  lazy val prosecutionCount = playedMatches.count(_.prosecutionIdentifier == id)
  lazy val defenseCount = playedMatches.count(_.defenseIdentifier == id)
}

case class ParticipantName(value: String)
case class ParticipantOrganization(value: String)
