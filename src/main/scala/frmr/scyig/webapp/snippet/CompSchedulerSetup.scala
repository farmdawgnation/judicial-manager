package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import net.liftweb.common._
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
