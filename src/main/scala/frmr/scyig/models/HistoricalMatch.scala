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

sealed trait HistoricalMatch

case class HistoricalTrial(
  prosecutionIdentifier: UUID,
  prosecutionScore: Int,
  defenseIdentifier: UUID,
  defenseScore: Int,
  presidingJudgeIdentifier: UUID,
  scoringJudgeIdentifier: Option[UUID]
) extends HistoricalMatch {
  def scoreFor(identifier: UUID): Int = {
    if (prosecutionIdentifier == identifier) {
      prosecutionScore
    } else if (defenseIdentifier == identifier) {
      defenseScore
    } else {
      0
    }
  }
}

case object HistoricalBye extends HistoricalMatch
