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

trait MatchingPolicy {
  def isAcceptableTeamMatch: PartialFunction[(CompetingTeam, CompetingTeam), Boolean]
}

object DefaultPolicy extends MatchingPolicy {
  override def isAcceptableTeamMatch = {
    case _ =>
      true
  }
}

object NotFromSameOrganizationPolicy extends MatchingPolicy {
  override def isAcceptableTeamMatch = {
    case (team1, team2) if team1.organization == team2.organization =>
      false
  }
}

object NotAPreviousOpponentPolicy extends MatchingPolicy {
  override def isAcceptableTeamMatch = {
    // In theory, we shouldn't need the "OR" here, but team names aren't really a unique identifier
    // yet... more to come in this area, I expect.
    case (team1, team2) if team1.hasPlayed_?(team2.name) || team2.hasPlayed_?(team1.name) =>
      false
  }
}
