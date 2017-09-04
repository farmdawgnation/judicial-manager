package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import frmr.scyig.matching._
import frmr.scyig.matching.models._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.util.Helpers._

object CompSchedulerSetup {
  import SnippetHelpers._

  val menu = Menu.param[Competition](
    "Scheduler Setup",
    "Scheduler Setup",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "scheduler-setup" >> validateCompetitionAccess
}

sealed trait SetupMatchingAlgorithm
case object RandomizedMatching extends SetupMatchingAlgorithm
case object OpportunityMatching extends SetupMatchingAlgorithm
case object ChallengeMatching extends SetupMatchingAlgorithm

class CompSchedulerSetup(competition: Competition) {
  private[this] var numberOfRooms: Box[Int] = Empty
  private[this] var matchingAlgorithm: Box[SetupMatchingAlgorithm] = Empty

  private[this] def convertJudgesToParticipants: Seq[Participant] = ???

  private[this] def convertTeamsToParticipants: Seq[Participant] = ???

  private[this] def submitSchedulerForm = {
    (numberOfRooms, matchingAlgorithm) match {
      case (Empty, Empty) => S.error("Please fill out the form before submitting.")
      case (_: Failure, _) => S.error("Please enter a valid number in number of rooms.")
      case (Empty, _) => S.error("Please enter number of rooms before submitting.")
      case (_, Empty) => S.error("Please select a matching algorithm before submitting.")
      case (Full(rooms), Full(algorithm)) => S.notice("Good job you filled out a form.")
      case (_, _) => S.error("Some unexpected error occurred while processing the form.")
    }
  }

  def render = {
    SHtml.makeFormsAjax andThen
    "#number-of-rooms" #> SHtml.text("", v => numberOfRooms = Full(v).filter(_.nonEmpty).flatMap(asInt(_))) &
    SHtml.radioCssSel[SetupMatchingAlgorithm](Empty, matchingAlgorithm = _)(
      "#randomized-matching" -> RandomizedMatching,
      "#opportunity-matching" -> OpportunityMatching,
      "#challenge-matching" -> ChallengeMatching
    ) &
    "#generate-schedule" #> SHtml.ajaxOnSubmit(submitSchedulerForm _)
  }
}
