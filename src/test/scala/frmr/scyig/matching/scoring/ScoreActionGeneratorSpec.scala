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
import frmr.scyig.matching.scoring

import frmr.scyig.Generators._
import frmr.scyig.matching.models._
import frmr.scyig.matching.scoring._
import java.util.UUID
import org.scalatest._
import org.scalatest.prop._
import net.liftweb.common._
import scala.util.Random

class ScoreActionGeneratorSpec extends WordSpec with GeneratorDrivenPropertyChecks with Matchers {
  "ScoreActionGenerator" should {
    "generate correct scoring actions" in {
      forAll(multipleScheduledRoundMatchesGen) { scheduledRounds =>
        val scoresByTrial: Map[UUID, ScoresByTeam] = scheduledRounds.collect({
          case trial: Trial =>
            val pScore = Random.nextInt(100)
            val dScore = Random.nextInt(100)
            (trial.id, ScoresByTeam(Seq(pScore, pScore), Seq(dScore, dScore)))
        }).toMap

        val actionGenerator = new ScoreActionGenerator(() => Full(scheduledRounds))
        val scoringActions = actionGenerator.generateScoringActions(scoresByTrial).openOrThrowException("error in scoring action generator")

        scoringActions.foreach { scoringAction =>
          scoringAction match {
            case RecordHistoricalBye(teamId) =>
              val locatedBye = scheduledRounds.collectFirst {
                case bye: Bye if bye.team.id == teamId =>
                  bye
              }

              locatedBye.isDefined should equal(true)

            case RecordHistoricalTrial(histTrial) =>
              val locatedTrial = scheduledRounds.collectFirst {
                case trial @ Trial(p, d, pj, sj, _, _) if p.id == histTrial.prosecutionIdentifier &&
                                                          d.id == histTrial.defenseIdentifier &&
                                                          pj.id == histTrial.presidingJudgeIdentifier &&
                                                          sj.map(_.id) == histTrial.scoringJudgeIdentifier =>
                  trial
              }

              locatedTrial.isDefined should equal(true)

              val openedTrial = locatedTrial.get
              scoresByTrial.get(openedTrial.id).isDefined should equal(true)

              val openedScores = scoresByTrial.get(openedTrial.id).get
              openedScores.prosecution should equal(histTrial.prosecutionScores)
              openedScores.defense should equal(histTrial.defenseScores)

            case DenoteScoringError(error) =>
              fail(s"Scoring error was surfaced: $error")
          }
        }
      }
    }
  }
}
