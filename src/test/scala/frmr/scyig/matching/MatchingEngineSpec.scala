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
package frmr.scyig
package matching

import org.scalatest._
import org.scalatest.prop._
import frmr.scyig.matching.models._
import frmr.scyig.Generators._
import net.liftweb.actor._

import scala.concurrent._

class MatchingEngineSpec extends WordSpec with GeneratorDrivenPropertyChecks with Matchers {
  LAScheduler.onSameThread = true

  "The Matching Engine" should {
    "end with a fully scheduled final state" in {
      forAll(matchingEngineGen) { matchingEngine =>
        matchingEngine ! StartMatching

        matchingEngine.finalState.isDefined should equal(true)

        matchingEngine.finalState.get.scheduledRounds.collect {
          case trial: Trial => trial
        }.length should be <= matchingEngine.getNumberOfRooms
        matchingEngine.finalState.get.fullyMatchedRounds should equal(Seq())
        matchingEngine.finalState.get.remainingParticipants.collect {
          case team: CompetingTeam => team
        } should equal(Seq())
        matchingEngine.finalState.get.currentlyBuildingRound should equal(None)
        assertEachTeamIsSeenOnce(matchingEngine.finalState.get, matchingEngine)
      }
    }
  }

  def assertEachTeamIsSeenOnce(finalState: MatchingEngineState, engine: MatchingEngine) = {
    var participantsSet = engine.getInitialParticipants.map(_.id).toSet

    val participants = finalState.scheduledRounds.flatMap { round =>
      round match {
        case Trial(team1, team2, presidingJudge, Some(schedulingJudge), _, _) =>
          Seq(team1, team2, presidingJudge, schedulingJudge)

        case Trial(team1, team2, presidingJudge, _, _, _) =>
          Seq(team1, team2, presidingJudge)

        case Bye(team) =>
          Seq(team)
      }
    }

    val keyedParticipants = participants.groupBy(_.id)
    val scheduledParticipants = keyedParticipants.keySet

    keyedParticipants.foreach {
      case (id, matchingEntries) =>
        withClue("Found a participant multiple times:") {
          matchingEntries.length should equal(1)
        }
        withClue("Found a participant not in initial set: ") {
          participantsSet.contains(id) should equal(true)
        }
    }

    val unscheduledParticipants = participantsSet -- scheduledParticipants
    val remainingParticipantsIds = finalState.remainingParticipants.map(_.id).toSet

    remainingParticipantsIds == unscheduledParticipants
  }
}
