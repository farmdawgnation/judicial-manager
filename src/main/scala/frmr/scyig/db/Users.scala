package frmr.scyig.db

import slick.jdbc.H2Profile.api._

case class User(
  id: Option[Int],
  email: String,
  passwordHash: String,
  name: String,
  superuser: Boolean
)

class Users(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email", O.Unique)
  def passwordHash = column[String]("password_hash")
  def name = column[String]("name")
  def superuser = column[Boolean]("superuser")

  def * = (id.?, email, passwordHash, name, superuser) <> (User.tupled, User.unapply)
}

object Users extends TableQuery[Users](new Users(_))
