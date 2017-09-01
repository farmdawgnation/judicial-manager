package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import net.liftweb.common._
import net.liftweb.util.Helpers._
import slick.jdbc.MySQLProfile.api._

object SnippetHelpers {
  def idToCompetition(idStr: String): Box[Competition] = {
    for {
      id <- (tryo(idStr.toInt) or Empty)
      query = Competitions.filter(_.id === id).result.head
      competition <- DB.runAwait(query)
    } yield {
      competition
    }
  }
}
