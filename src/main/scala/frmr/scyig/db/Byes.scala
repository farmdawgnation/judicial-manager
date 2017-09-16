package frmr.scyig.db

import java.util.UUID
import slick.jdbc.MySQLProfile.api._

case class Bye(
  id: Option[Int],
  competitionId: Int,
  teamId: Int,
  round: Int,
  uuid: UUID = UUID.randomUUID()
)

class Byes(tag: Tag) extends Table[Bye](tag, "byes") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def competitionId = column[Int]("competition_id")
  def teamId = column[Int]("team_id")
  def round = column[Int]("round")
  def uuid = column[UUID]("uuid")

  def * = (id.?, competitionId, teamId, round, uuid) <> (Bye.tupled, Bye.unapply)

  def cidFK = foreignKey("b_competition_id_fk", competitionId, Competitions)(_.id, onDelete = ForeignKeyAction.Cascade)
  def tidFK = foreignKey("b_team_id_fk", teamId, Teams)(_.id, onDelete = ForeignKeyAction.Cascade)
}

object Byes extends TableQuery[Byes](new Byes(_))
