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

/**
 * Our representation for someone who is participating in the judicial conference.
 * This is the unifying type for all kinds of participant.
 */
sealed trait Participant {
  def name: ParticipantName

  def organization: Option[ParticipantOrganization]
}

sealed trait Judge extends Participant
case class PresidingJudge(name: ParticipantName) extends Judge {
  override val organization = None
}
case class ScoringJudge(name: ParticipantName, _organization: ParticipantOrganization) extends Judge {
  override val organization = Some(_organization)
}

case class CompetingTeam(
  name: ParticipantName,
  _organization: ParticipantOrganization,
  roleHistory: Seq[HistoricalRole]
) extends Participant {
  override val organization = Some(_organization)
}

case class ParticipantName(name: String)
case class ParticipantOrganization(name: String)

sealed trait HistoricalRole
case object Procescution extends HistoricalRole
case object Defense extends HisotricalRole
