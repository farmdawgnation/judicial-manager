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
package frmr.scyig.matching.scoring

import frmr.scyig.matching.models._
import java.util.UUID
import net.liftweb.common._

/**
 * Scoring actions that could be taken on a trial.
 */
sealed trait ScoringActions
case class DenoteScoringError(error: String) extends ScoringActions
case class RecordHistoricalTrial(historicalTrial: HistoricalTrial) extends ScoringActions
case class RecordHistoricalBye(teamId: UUID) extends ScoringActions

case class ScoreByTeam(prosecution: Int, defense: Int)

/**
 * Generates scoring actions based on the activites of a round. These actions are expected to be
 * processed as instructions by whatever persistence layer is in effect.
 */
class ScoreActionGenerator(getMatches: ()=>Box[Seq[ScheduledRoundMatch]]) {
  type TrialIdentifier = UUID
  type ScoreSummary = Map[TrialIdentifier, ScoreByTeam]

  def generateScoringActions(scores: ScoreSummary): Box[Seq[ScoringActions]] = {
    for (scheduledMatches <- getMatches()) yield {
      for (scheduledMatch <- scheduledMatches) yield {
        scheduledMatch match {
          case Bye(team) =>
            RecordHistoricalBye(team.id)

          case Trial(prosecution, defense, presidingJudge, scoringJudge, _, trialId) =>
            scores.get(trialId) match {
              case Some(ScoreByTeam(pScore, dScore)) =>
                RecordHistoricalTrial(HistoricalTrial(
                  prosecutionIdentifier = prosecution.id,
                  prosecutionScore = pScore,
                  defenseIdentifier = defense.id,
                  defenseScore = dScore,
                  presidingJudgeIdentifier = presidingJudge.id,
                  scoringJudgeIdentifier = scoringJudge.map(_.id)
                ))

              case None =>
                DenoteScoringError(s"Scores for $trialId couldn't be located.")
            }
        }
      }
    }
  }
}
