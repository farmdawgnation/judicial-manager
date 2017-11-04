package frmr.scyig.db

import slick.jdbc.MySQLProfile.api._
import net.liftweb.util._

case class User(
  id: Option[Int],
  email: String,
  passwordHash: String,
  name: String,
  superuser: Boolean
) {
  val userId = id.getOrElse(0)

  def checkpw(candidatePassword: String): Boolean =
    BCrypt.checkpw(candidatePassword, passwordHash)

  lazy val sponsorIds: Seq[Int] = {
    DB.runAwait(Users.filter(_.id === id).join(UsersSponsors).on(_.id === _.userId)
      .map(_._2.sponsorId).result).openOr(Seq())
  }

  // FIXME: Make atomic
  def setSponsors(sponsorIds: Seq[Int]) = {
    DB.runAwait(UsersSponsors.filter(_.userId === id).delete)

    val sponsorInserts = sponsorIds.map { sponsorId =>
      DB.runAwait(UsersSponsors += ("", userId, sponsorId))
    }
  }
}

class Users(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email", O.Unique, O.Length(255))
  def passwordHash = column[String]("password_hash")
  def name = column[String]("name")
  def superuser = column[Boolean]("superuser")

  def * = (id.?, email, passwordHash, name, superuser) <> (User.tupled, User.unapply)
}

object Users extends TableQuery[Users](new Users(_)) {
  def hashpw(password: String): String =
    BCrypt.hashpw(password, BCrypt.gensalt())
}
