package frmr.scyig.webapp.auth

import frmr.scyig.db._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.ContainerSerializer._
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
        Success(AuthenticationSuccess(user.id.getOrElse(-1)))

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
      case succ @ AuthenticationSuccess(userId) =>
        currentUserId(userId)
        succ

      case other =>
        other
    }
  }

  def logout_!(): Unit = {
    currentUserId(0)
  }
}

sealed trait AuthenticationResult
case class AuthenticationSuccess(userId: Int) extends AuthenticationResult
case object AuthenticationFailure extends AuthenticationResult
case object AuthenticationInternalError extends AuthenticationResult
