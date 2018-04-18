package frmr.scyig.db

import java.util.UUID
import net.liftweb.common._
import slick.jdbc.MySQLProfile.api._

case class Team(
  id: Option[Int],
  competitionId: Int,
  name: String,
  organization: String,
  uuid: UUID = UUID.randomUUID()
) {
  def teamId = id getOrElse 0

  def byes: Box[Seq[Bye]] = {
    DB.runAwait(
      Byes.to[Seq].filter(_.teamId === teamId).result
    )
  }

  def matches: Box[Seq[Match]] = {
    DB.runAwait(
      Matches.to[Seq].filter(m => m.prosecutionTeamId === id || m.defenseTeamId === id).result
    )
  }

  def prosecutionOccurrences: Box[Int] =
    Matches.countProsecutionOccurrences(this)

  def defenseOccurrences: Box[Int] =
    Matches.countDefenseOccurrences(this)
}

class Teams(tag: Tag) extends Table[Team](tag, "teams") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def competitionId = column[Int]("competition_id")
  def name = column[String]("name")
  def organization = column[String]("organization")
  def uuid = column[UUID]("uuid")

  def * = (id.?, competitionId, name, organization, uuid) <> (Team.tupled, Team.unapply)

  def cidFK = foreignKey("t_competition_id_fk", competitionId, Competitions)(_.id, onDelete = ForeignKeyAction.Cascade)
}

object Teams extends TableQuery[Teams](new Teams(_))
