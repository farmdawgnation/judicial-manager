package frmr.scyig.db

import slick.jdbc.MySQLProfile.api._

case class Competition(
  id: Option[Int],
  name: String,
  sponsorId: Int,
  dates: String,
  description: String
)

class Competitions(tag: Tag) extends Table[Competition](tag, "competitions") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def sponsorId = column[Int]("sponsor_id")
  def dates = column[String]("dates")
  def description = column[String]("description")

  def * = (id.?, name, sponsorId, dates, description) <> (Competition.tupled, Competition.unapply)

  def sidFK = foreignKey("sponsor_id_fk", sponsorId, Sponsors)(_.id, onDelete = ForeignKeyAction.Cascade)
}

object Competitions extends TableQuery[Competitions](new Competitions(_))
