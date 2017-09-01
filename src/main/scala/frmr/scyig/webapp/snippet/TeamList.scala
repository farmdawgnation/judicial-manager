package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import slick.jdbc.MySQLProfile.api._

object TeamList {
  import SnippetHelpers._

  val menu = Menu.param[Competition](
    "Competition Teams",
    "Manage Teams",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "teams" >>
    validateCompetitionAccess >>
    TemplateBox( () => Templates("competition" :: "star" :: "teams" :: "manage" :: Nil))

  def render = {
    val teams: List[Team] = DB.runAwait(Teams.to[List].result).openOrThrowException("Teams couldn't be found")

    if (teams.isEmpty) {
      ".team-row" #> ClearNodes
    } else {
      ".no-team-rows" #> ClearNodes &
      ".team-row" #> teams.map { team =>
        ".team-id *" #> team.id &
        ".team-name *" #> team.name &
        ".team-org *" #> team.organization
      }
    }
  }
}
