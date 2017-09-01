package frmr.scyig.db

import slick.jdbc.MySQLProfile.api._

case class Competition(
  id: Option[Int],
  name: String,
  sponsorId: Int,
  dates: String,
  description: String,
  location: String,
  status: String,
  round: Int
)

class Competitions(tag: Tag) extends Table[Competition](tag, "competitions") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def sponsorId = column[Int]("sponsor_id")
  def dates = column[String]("dates")
  def description = column[String]("description")
  def location = column[String]("location")
  def status = column[String]("status")
  def round = column[Int]("round")

  def * = (id.?, name, sponsorId, dates, description, location, status, round) <> (Competition.tupled, Competition.unapply)

  def sidFK = foreignKey("c_sponsor_id_fk", sponsorId, Sponsors)(_.id, onDelete = ForeignKeyAction.Cascade)
}

object Competitions extends TableQuery[Competitions](new Competitions(_))
