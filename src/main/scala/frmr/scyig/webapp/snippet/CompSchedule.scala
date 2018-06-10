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

  val menu = Menu.param[Competition](
    "Current Round Schedule",
    "Current Round Schedule",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "schedule" >> validateCompetitionAccess
}
class CompSchedule(competition: Competition) {
  def attachCompetitionId =
    "^ [data-competition-id]" #> competition.competitionId
}
