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
  participants: Seq[Participant],
  roundNumber: Int,
  numberOfRooms: Int,
  matchingPolicies: Seq[MatchingPolicy]
) extends LiftActor {
  def messageHandler = {
    case _ =>
  }
}
