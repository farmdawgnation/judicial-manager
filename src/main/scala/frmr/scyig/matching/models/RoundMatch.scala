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
  def teams: Seq[CompetingTeam] = Seq()

  def toByes: Seq[Bye] = {
    teams.map(Bye(_))
  }
}

/**
 * A completely built match that hasn't been scheduled yet.
 */
sealed trait CompletedRoundMatch extends RoundMatch {
  def withRoom(roomNumber: Int): Trial
  def teams: Seq[CompetingTeam] = Seq()

  def toByes: Seq[Bye] = {
    teams.map(Bye(_))
  }
}

/**
 * A match that is completely scheduled with room and all.
 */
sealed trait ScheduledRoundMatch extends RoundMatch

/**
 * A seed for the matching algorithm that only consists of a single competing team without a role
 * assigned.
 */
case class MatchSeed(
  team: CompetingTeam
) extends PartialRoundMatch {
  override val teams = Seq(team)

  def withOtherTeam(otherTeam: CompetingTeam, otherTeamIsProsecution: Boolean = false) = {
    if (otherTeamIsProsecution) {
      MatchedTeams(otherTeam, team)
    } else {
      MatchedTeams(team, otherTeam)
    }
  }
}

/**
 * A partial match in which the teams have been matched.
 */
case class MatchedTeams(
  prosecution: CompetingTeam,
  defense: CompetingTeam
) extends PartialRoundMatch {
  override val teams = Seq(prosecution, defense)

  def withPresidingJudge(presidingJudge: Judge): MatchedTeamsWithPresidingJudge = {
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
  prosecution: CompetingTeam,
  defense: CompetingTeam,
  presidingJudge: Judge
) extends PartialRoundMatch {
  override val teams = Seq(prosecution, defense)

  def withScoringJudge(scoringJudge: Option[Judge]): ScheduleableTrial = {
    ScheduleableTrial(
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
case class ScheduleableTrial(
  prosecution: CompetingTeam,
  defense: CompetingTeam,
  presidingJudge: Judge,
  scoringJudge: Option[Judge]
) extends CompletedRoundMatch {
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
  presidingJudge: Judge,
  scoringJudge: Option[Judge],
  roomNumber: Int,
  id: UUID = UUID.randomUUID()
) extends ScheduledRoundMatch

/**
 * A Bye match, representing where a team is not playing another team.
 */
case class Bye(
  team: CompetingTeam,
  id: UUID = UUID.randomUUID()
) extends ScheduledRoundMatch
