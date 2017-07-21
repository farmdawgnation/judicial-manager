package frmr.scyig.db

import slick.jdbc.MySQLProfile.api._

case class Team(
  id: Option[Int],
  competitionId: Int,
  name: String,
  organization: String
)

class Teams(tag: Tag) extends Table[Team](tag, "teams") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def competitionId = column[Int]("competition_id")
  def name = column[String]("name")
  def organization = column[String]("organization")

  def * = (id.?, competitionId, name, organization) <> (Team.tupled, Team.unapply)

  def cidFK = foreignKey("competition_id_fk", competitionId, Competitions)(_.id, onDelete = ForeignKeyAction.Cascade)
}

object Teams extends TableQuery[Teams](new Teams(_))
