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
import frmr.scyig.models._
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
      }
    }
  }
}
