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

import frmr.scyig.matching._
import frmr.scyig.matching.models._
import java.util.UUID
import org.scalacheck._
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary.arbitrary

object Generators {
  val participantNameGen = for (name <- Gen.alphaStr) yield ParticipantName(name)
  val participantOrgGen = for (name <- Gen.alphaStr) yield ParticipantOrganization(name)

  val teamGen = for {
    name <- participantNameGen
    org <- participantOrgGen
  } yield {
    CompetingTeam(name, org)
  }

  val presidingJudgeGen = for {
    name <- participantNameGen
    org <- Gen.option(participantOrgGen)
  } yield {
    PresidingJudge(name, org)
  }

  val scoringJudgeGen = for {
    name <- participantNameGen
    org <- Gen.option(participantOrgGen)
  } yield {
    ScoringJudge(name, org)
  }

  val participantGen: Gen[Participant] = for {
    participant <- Gen.oneOf(teamGen, presidingJudgeGen, scoringJudgeGen)
  } yield participant

  val participantsGen: Gen[Seq[Participant]] = for {
    participants <- Gen.listOf(participantGen)
  } yield participants

  val matchingEngineGen = for {
    participants <- participantsGen
    roundNumber <- Gen.choose[Int](1, 10)
    numberOfRooms <- Gen.choose[Int](1, 100)
    matchingPolicy = MatchingPolicy.default
    suggester = (participants)=>new RandomizedParticipantSuggester(participants)
  } yield {
    new MatchingEngine(
      participants,
      roundNumber,
      numberOfRooms,
      matchingPolicy,
      suggester
    )
  }

  val byeGen = for (team <- teamGen) yield {
    Bye(team)
  }

  val trialGen = for {
    prosecution <- teamGen
    defense <- teamGen
    presidingJudge <- presidingJudgeGen
    scoringJudge <- Gen.option(scoringJudgeGen)
    roomNumber <- Gen.choose(1, 100)
  } yield {
    Trial(
      prosecution,
      defense,
      presidingJudge,
      scoringJudge,
      roomNumber
    )
  }
}
