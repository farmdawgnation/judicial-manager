package frmr.scyig.db

import slick.jdbc.H2Profile.api._

case class Judge(
  id: Option[Int],
  competitionId: Int,
  name: String,
  organization: String
)

class Judges(tag: Tag) extends Table[Judge](tag, "judges") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def competitionId = column[Int]("competition_id")
  def name = column[String]("name")
  def organization = column[String]("organization")

  def * = (id.?, competitionId, name, organization) <> (Judge.tupled, Judge.unapply)

  def cidFK = foreignKey("competition_id_fk", competitionId, Judges)(_.id, onDelete = ForeignKeyAction.Cascade)
}

object Judges extends TableQuery[Judges](new Judges(_))
