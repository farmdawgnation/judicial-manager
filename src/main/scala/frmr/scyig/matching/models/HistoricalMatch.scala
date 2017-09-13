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

sealed trait HistoricalMatch

case class HistoricalTrial(
  prosecutionIdentifier: UUID,
  prosecutionScores: Seq[Int],
  defenseIdentifier: UUID,
  defenseScores: Seq[Int],
  presidingJudgeIdentifier: UUID,
  scoringJudgeIdentifier: Option[UUID]
) extends HistoricalMatch {
  def scoreFor(identifier: UUID): Seq[Int] = {
    if (prosecutionIdentifier == identifier) {
      prosecutionScores
    } else if (defenseIdentifier == identifier) {
      defenseScores
    } else {
      Seq.empty
    }
  }
}

case class HistoricalBye(teamIdentifier: UUID) extends HistoricalMatch
