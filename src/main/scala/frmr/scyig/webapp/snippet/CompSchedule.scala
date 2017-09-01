package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import net.liftweb.common._
import net.liftweb.sitemap._
import net.liftweb.util.Helpers._

object CompSchedule {
  import SnippetHelpers._

  val menu = Menu.param[Competition](
    "Competition Dashboard",
    "Dashboard",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competitions" / * / "schedule"
}
