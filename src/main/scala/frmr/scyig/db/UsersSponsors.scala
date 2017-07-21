package frmr.scyig.db

import slick.jdbc.MySQLProfile.api._

class UsersSponsors(tag: Tag) extends Table[(String, Int, Int)](tag, "users_sponsors") {
  def role = column[String]("role")
  def userId = column[Int]("user_id")
  def sponsorId = column[Int]("sponsor_id")

  def * = (role, userId, sponsorId)

  def uidFK = foreignKey("user_id_fk", userId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)
  def sidFK = foreignKey("sponsor_id_fk", sponsorId, Sponsors)(_.id, onDelete = ForeignKeyAction.Cascade)
}
