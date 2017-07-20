package frmr.scyig.db

import slick.jdbc.H2Profile.api._

case class Sponsor(
  id: Option[Int],
  name: String,
  location: String
)

class Sponsors(tag: Tag) extends Table[Sponsor](tag, "sponsors") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def location = column[String]("location")

  def * = (id.?, name, location) <> (Sponsor.tupled, Sponsor.unapply)
}

object Sponsors extends TableQuery[Sponsors](new Sponsors(_))
