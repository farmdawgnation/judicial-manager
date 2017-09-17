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

/**
 * Matching policies define the "hard rules" that exist at the judicial competition. Essentially,
 * once the matching engine has a match that it would like to suggest, it needs to validate that
 * potential match with the enabled matching policies.
 */
trait MatchingPolicy {
  def isValid(partialMatch: PartialRoundMatch, proposedParticipant: Participant): Boolean
}
object MatchingPolicy {
  val default = AndMatchingPolicy(NotFromSameOrganizationPolicy, NotAPreviousPolicy)
}

/**
 * A matching policy that unifies two or more other matching policies.
 */
case class AndMatchingPolicy(policy1: MatchingPolicy, policy2: MatchingPolicy, policyn: MatchingPolicy*) extends MatchingPolicy {
  override def isValid(partialMatch: PartialRoundMatch, proposedParticipant: Participant): Boolean = {
    val allPolicies = Seq(policy1, policy2) ++ policyn

    allPolicies.foldLeft(true)(_ && _.isValid(partialMatch, proposedParticipant))
  }
}

/**
 * Ensures no participants in the potential match are from the same organization.
 */
object NotFromSameOrganizationPolicy extends MatchingPolicy {
  override def isValid(partialMatch: PartialRoundMatch, proposedParticipant: Participant): Boolean = {
    (partialMatch, proposedParticipant) match {
      case (MatchSeed(team1), team2: CompetingTeam) =>
        team1.organization != team2.organization

      case (MatchedTeams(team1, team2), presidingJudge: Judge) =>
        presidingJudge.organization.isEmpty ||
        team1.organization != presidingJudge.organization &&
        team2.organization != presidingJudge.organization

      case (MatchedTeamsWithPresidingJudge(team1, team2, presidingJudge), scoringJudge: Judge) =>
        scoringJudge.organization.isEmpty ||
        team1.organization != scoringJudge.organization &&
        team2.organization != scoringJudge.organization &&
        presidingJudge.organization != scoringJudge.organization

      case (_: ScheduleableTrial, _) =>
        true

      case _ =>
        false
    }
  }
}

/**
 * Ensure no participants in a match have seen each other before.
 */
object NotAPreviousPolicy extends MatchingPolicy {
  override def isValid(partialMatch: PartialRoundMatch, proposedParticipant: Participant): Boolean = {
    (partialMatch, proposedParticipant) match {
      case (MatchSeed(team1), team2: CompetingTeam) =>
        ! team1.hasPlayed_?(team2.id)

      case (MatchedTeams(team1, team2), presidingJudge: Judge) =>
        presidingJudge.hasJudged_?(team1.id) == false &&
        presidingJudge.hasJudged_?(team2.id) == false

      case (MatchedTeamsWithPresidingJudge(team1, team2, presidingJudge), scoringJudge: Judge) =>
        scoringJudge.hasJudged_?(team1.id) == false &&
        scoringJudge.hasJudged_?(team2.id) == false &&
        scoringJudge.hasJudged_?(presidingJudge.id) == false

      case (_: ScheduleableTrial, _) =>
        true

      case _ =>
        false
    }
  }
}
