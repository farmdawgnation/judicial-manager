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
 * Representation of a kind of match in a round at the Judicial conference. Matches may be partial
 * or complete. Partial matches are largely used during the matching algorithm to progressively
 * develop the matches.
 */
sealed trait RoundMatch

/**
 * Parent trait for any kind of match that is incomplete and therefore not capable of being
 * scheduled.
 */
sealed trait PartialRoundMatch extends RoundMatch {
  def prosecution: CompetingTeam
  def defense: CompetingTeam

  def toByes: Seq[Bye] = {
    Seq(Bye(prosecution), Bye(defense))
  }
}

/**
 * Parent trait for any completely scheduled match. This could be an actual [[Trial]] or it could be
 * a [[Bye]].
 */
sealed trait CompletedRoundMatch extends RoundMatch

/**
 * A partial match in which the teams have been matched.
 */
case class MatchedTeams(
  prosecution: CompetingTeam,
  defense: CompetingTeam
) extends PartialRoundMatch {
  def withPresidingJudge(presidingJudge: PresidingJudge): MatchedTeamsWithPresidingJudge = {
    MatchedTeamsWithPresidingJudge(
      prosecution,
      defense,
      presidingJudge
    )
  }
}

/**
 * A partial match in which the teams have a presiding judge.
 */
case class MatchedTeamsWithPresidingJudge(
  override val prosecution: CompetingTeam,
  override val defense: CompetingTeam,
  presidingJudge: PresidingJudge
) extends PartialRoundMatch {
  def withScoringJudge(scoringJudge: Option[ScoringJudge]): MatchedTeamsWithPresidingAndScoringJudge = {
    MatchedTeamsWithPresidingAndScoringJudge(
      prosecution,
      defense,
      presidingJudge,
      scoringJudge
    )
  }
}

/**
 * A parital match in which the teams have a presiding judge and a scoring judge.
 */
case class MatchedTeamsWithPresidingAndScoringJudge(
  override val prosecution: CompetingTeam,
  override val defense: CompetingTeam,
  presidingJudge: PresidingJudge,
  scoringJudge: Option[ScoringJudge]
) extends PartialRoundMatch {
  def withRoom(roomNumber: Int): Trial = {
    Trial(
      prosecution,
      defense,
      presidingJudge,
      scoringJudge,
      roomNumber
    )
  }
}

/**
 * A completely scheduled trial match.
 */
case class Trial(
  prosecution: CompetingTeam,
  defense: CompetingTeam,
  presidingJudge: PresidingJudge,
  scoringJudge: Option[ScoringJudge],
  roomNumber: Int
) extends CompletedRoundMatch

/**
 * A Bye match, representing where a team is not playing another team.
 */
case class Bye(
  team: CompetingTeam
) extends CompletedRoundMatch
