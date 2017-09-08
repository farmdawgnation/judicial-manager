package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import slick.jdbc.MySQLProfile.api._

object CompSchedule {
  import SnippetHelpers._

  object populatedMatches extends RequestVar[Seq[Match]](Seq.empty)

  val menu = Menu.param[Competition](
    "Schedule",
    "Schedule",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "schedule" >> validateCompetitionAccess
}
