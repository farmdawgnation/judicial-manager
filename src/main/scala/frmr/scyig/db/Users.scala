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
  def checkpw(candidatePassword: String): Boolean =
    BCrypt.checkpw(candidatePassword, passwordHash)
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
