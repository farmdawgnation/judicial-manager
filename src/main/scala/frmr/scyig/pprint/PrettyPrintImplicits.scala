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

import frmr.scyig.models._

package object pprint {
  implicit class SchedulePrettyPrinter(schedule: Seq[ScheduledRoundMatch]) {
    def pprint = {
      val trials = schedule.collect {
        case trial: Trial => trial
      }

      val byes = schedule.collect {
        case bye: Bye => bye
      }

      println("=== SCHEDULE ===\n")

      for ((trial, index) <- trials.zipWithIndex) {
        val number = index + 1
        println(s"$number.\t${trial.prosecution.name.value} from ${trial.prosecution.organization.map(_.value).getOrElse("none")}")
        println("\tvs")
        println(s"\t${trial.defense.name.value} from ${trial.defense.organization.map(_.value).getOrElse("none")}")
        println(s"\tPresiding Judge: ${trial.presidingJudge.name.value} from ${trial.presidingJudge.organization.map(_.value).getOrElse("none")}")
        if (trial.scoringJudge.isDefined) {
          println(s"\tScoring Judge: ${trial.scoringJudge.get.name.value} from ${trial.scoringJudge.get.organization.map(_.value).getOrElse("none")}")
        }
        println("")
      }

      println("=== BYES ===\n")

      println(byes.mkString(","))
    }
  }
}
