package frmr.scyig.webapp.api

import frmr.scyig.db._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.rest._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.liftweb.util.Helpers._
import net.liftweb.util.CanResolveAsync.resolveFuture
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.MySQLProfile.api._

object TeamSuggestionApi extends RestHelper {
  serve {
    case JsonPost("api" :: "v1" :: "competition" :: AsInt(competitionId) :: "team-suggestions" :: Nil, req) if currentUserId.is > 0 =>
      val nameQuery = S.param("q").openOr("")
      val query = Teams.to[List]
        .filter(_.name startsWith nameQuery)
        .filter(_.competitionId === competitionId)
        .result

      DB.run(query).map { teams =>
        val teamObjs = teams.map { team =>
          ("display" -> team.name) ~
          ("value" -> team.id)
        }

        JsonResponse(teamObjs.foldLeft(JObject(Nil))(_ ~ _))
      }
  }
}
