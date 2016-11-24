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

case class MatchingResult(
  matches: Seq[RoundMatch],
  remainingTeams: Seq[CompetingTeam],
  remainingJudges: Seq[PresidingJudge],
  renamingSoringJudges: Seq[ScoringJudge]
)

trait MatchingStrategy {
  def matchTeams(
    orderedTeams: Seq[CompetingTeam],
    presidingJudges: Seq[PresidingJudge],
    scoringJudges: Seq[ScoringJudge]
  ): Seq[RoundMatch]
}

object CompetitivePowerMatchingStrategy {
  def matchTeams(
    orderedTeams: Seq[CompetingTeam],
    presidingJudges: Seq[PresidingJudge],
    scoringJudges: Seq[ScoringJudge]
  ): Seq[RoundMatch] = {
    Seq()
  }
}

object OpportunityPowerMatchingStrategy {
  def matchTeams(
    orderedTeams: Seq[CompetingTeam],
    presidingJudges: Seq[PresidingJudge],
    scoringJudges: Seq[ScoringJudge]
  ): Seq[RoundMatch] = {
    Seq()
  }
}
