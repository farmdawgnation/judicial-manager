package frmr.scyig.webapp.snippet

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import java.io.StringWriter
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import org.apache.commons.csv._
import slick.jdbc.MySQLProfile.api._

object ScorecardExport {
  import SnippetHelpers._

  def menu = Menu.param[Competition](
    "Scorecard Export",
    "Scorecard Export",
    idToCompetition,
    _.id.getOrElse("").toString
  ) / "competition" / * / "scorecard-export" >> validateCompetitionAccess >>
    TestValueAccess( (competition) => Full(exportScorecardToCsv(competition)) )

  private[this] def generateTeamRow(team: Team): Seq[String] = {
    // This is incredibly lame code. I am sorry. :(
    val fullSchedule = (team.matches ++ team.byes).flatten.toSeq.sortBy(_.round)

    val stringifiedScores = fullSchedule.flatMap {
      case scheduledMatch: Match =>
        scheduledMatch.scoresFor(team.teamId)
          .openOrThrowException(s"Error retreiving scores for $team")
          .map(_.score.toString)
      case scheduledBye: Bye =>
        Seq("BYE", "BYE")
    }

    Seq(team.name) ++ stringifiedScores
  }

  def exportScorecardToCsv(competition: Box[Competition]): LiftResponse = {
    val writer = new StringWriter()
    val csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)

    for {
      competition <- competition ?~! "Could not determine competition"
      competitionId <- competition.id
      teams <- DB.runAwait(Teams.to[Seq].filter(_.competitionId === competitionId).result) ?~!
        "Could not load teams"
    } {
      teams.foreach { team =>
        val teamFields = generateTeamRow(team)
        csvPrinter.printRecord(teamFields:_*)
      }
    }

    val csvBytes = writer.toString().getBytes("UTF-8")
    csvPrinter.close()
    new InMemoryResponse(
      data = csvBytes,
      headers = List(
        ("Content-Disposition", "attachment; filename=score-export.csv"),
        ("Content-Type", "text/csv")
      ),
      cookies = Nil,
      code = 200
    )
  }
}
