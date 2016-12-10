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

package object pprint {
  implicit class ParticipantRenderer(participant: Participant) {
    def asStr = {
      participant.organization match {
        case Some(org) =>
          s"${participant.name.value} from ${org.value}"
        case None =>
          s"${participant.name.value}"
      }
    }
  }

  implicit class SchedulePrettyPrinter(schedule: Seq[ScheduledRoundMatch]) {
    def pprint = {
      val trials = schedule.collect {
        case trial: Trial => trial
      }

      val byes = schedule.collect {
        case bye: Bye => bye
      }

      println("=== BEGIN SCHEDULE ===\n")

      for ((trial, index) <- trials.zipWithIndex) {
        val number = index + 1
        println(s"$number.\t${trial.prosecution.asStr}")
        println("\tvs")
        println(s"\t${trial.defense.asStr}")
        println(s"\tPresiding Judge: ${trial.presidingJudge.asStr}")
        if (trial.scoringJudge.isDefined) {
          println(s"\tScoring Judge: ${trial.scoringJudge.get.asStr}")
        }
        println("")
      }

      println("=== BYES ===\n")

      println(byes.map(_.team.asStr).mkString("\n"))

      println("=== END SCHEDULE ===")
    }
  }
}
