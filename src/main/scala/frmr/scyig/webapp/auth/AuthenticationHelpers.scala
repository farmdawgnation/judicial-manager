package frmr.scyig.webapp.auth

import frmr.scyig.db._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.provider._
import net.liftweb.http.ContainerSerializer._
import net.liftweb.util.Helpers
import scala.concurrent._
import scala.concurrent.duration._
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure => TryFailure, _}

/**
 * The authentication helpers wrap some boilerplate authentication
 * responsibilities such as making the user accessible during the life
 * of a request and sticking the user id in the container session.
 */
object AuthenticationHelpers extends Loggable {
  val extendedSessionCookieName = "Judicial-Manager-Session-ID"

  object currentUserId extends SessionVar[Int](0)

  private[this] def retrieveUser: Option[User] = {
    for {
      actualId <- Option(currentUserId.is) if actualId > 0
      user <- DB.runAwait(Users.filter(_.id === actualId).take(1).result.head)
    } yield {
      user
    }
  }

  object currentUser extends RequestVar[Option[User]](retrieveUser)

  def login_!(email: String, password: String): AuthenticationResult = {
    val authFuture = DB.run(Users.filter(_.email === email).result.head).transform {
      case Success(user) if user.id.isDefined && user.checkpw(password) =>
        logger.trace(s"Authentication for $email successful")

        val sessionId = Helpers.hash256(
          Random.alphanumeric.take(256).mkString
        )
        val sessionCookie = HTTPCookie(
          extendedSessionCookieName,
          sessionId
        ).copy(
          maxAge = Full(604800),
          httpOnly = Full(true)
        )

        Success(AuthenticationSuccess(
          user.id.getOrElse(-1),
          sessionCookie
         ))

      case Success(user) if user.id.isEmpty =>
        logger.error(s"Auth attempt for $email yielded a user without an id.")
        Success(AuthenticationInternalError)

      case Success(_) | TryFailure(_: NoSuchElementException) =>
        Success(AuthenticationFailure)

      case TryFailure(ex) if ex.isInstanceOf[SlickException] =>
        logger.debug(s"Slick exeception during authentication", ex)
        Success(AuthenticationInternalError)

      case TryFailure(ex) =>
        logger.error(s"Unexpected exception during authentication", ex)
        Success(AuthenticationInternalError)
    }

    Await.result(authFuture, 30.seconds) match {
      case succ @ AuthenticationSuccess(userId, cookie) =>
        currentUserId(userId)

        DB.run(
          WebappSessions.insertOrUpdate(
            WebappSession(cookie.value.getOrElse(""), userId)
          )
        )

        succ

      case other =>
        other
    }
  }

  def logout_!(): HTTPCookie = {
    S.session.foreach(_.destroySession())
    HTTPCookie(
      extendedSessionCookieName,
      "deleted"
    ).copy(
      httpOnly = Full(true),
      maxAge = Full(604800)
    )
  }

  def authenticateFromSessionCookie_!(request: Box[Req]) = {
    for (request <- request) {
      val possibleAuthCookie = request.cookies.find(_.name == extendedSessionCookieName)

      if (currentUserId.is == 0 && possibleAuthCookie.isDefined) {
        logger.info("Found session id cookie")
        val Some(authCookie) = possibleAuthCookie
        val existingSessions: Box[WebappSession] = DB.runAwait(
          WebappSessions.to[List]
            .filter(_.id === authCookie.value.openOr(""))
            .result
            .head
        )

        existingSessions match {
          case Full(matchingSession) =>
            currentUserId(matchingSession.userId)

          case _ =>
            S.deleteCookie(extendedSessionCookieName)
        }
      }
    }
  }
}

sealed trait AuthenticationResult
case class AuthenticationSuccess(userId: Int, cookie: HTTPCookie) extends AuthenticationResult
case object AuthenticationFailure extends AuthenticationResult
case object AuthenticationInternalError extends AuthenticationResult
