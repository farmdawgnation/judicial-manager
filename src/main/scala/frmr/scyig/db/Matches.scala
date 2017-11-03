package frmr.scyig.db

import java.util.UUID
import net.liftweb.common._
import slick.jdbc.MySQLProfile.api._

trait Scheduleable {
  def round: Int
}

case class Match(
  id: Option[Int],
  competitionId: Int,
  prosecutionTeamId: Int,
  defenseTeamId: Int,
  presidingJudgeId: Int,
  scoringJudgeId: Option[Int],
  round: Int,
  order: Int,
  uuid: UUID = UUID.randomUUID()
) extends Scheduleable {
  def scoresFor(teamId: Int): Box[Seq[Score]] = {
    val matchId = id.getOrElse(0)
    DB.runAwait(
      Scores.to[Seq].filter(_.matchId === matchId).filter(_.teamId === teamId).result
    )
  }

  def prosecutionScores = scoresFor(prosecutionTeamId)
  def defenseScores = scoresFor(defenseTeamId)
}

class Matches(tag: Tag) extends Table[Match](tag, "matches") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def competitionId = column[Int]("competition_id")
  def prosecutionTeamId = column[Int]("prosecution_team_id")
  def defenseTeamId = column[Int]("defense_team_id")
  def presidingJudgeId = column[Int]("presiding_judge_id")
  def scoringJudgeId = column[Option[Int]]("scoring_judge_id")
  def round = column[Int]("round")
  def order = column[Int]("order")
  def uuid = column[UUID]("uuid")

  def * = (id.?, competitionId, prosecutionTeamId, defenseTeamId, presidingJudgeId, scoringJudgeId,
    round, order, uuid) <> (Match.tupled, Match.unapply)

  def cidFK = foreignKey("m_competition_id_fk", competitionId, Competitions)(_.id, onDelete = ForeignKeyAction.Cascade)
  def ptidFK = foreignKey("m_prosecution_team_id_fk", prosecutionTeamId, Teams)(_.id, onDelete = ForeignKeyAction.Restrict)
  def dtidFK = foreignKey("m_defense_team_id_fk", defenseTeamId, Teams)(_.id, onDelete = ForeignKeyAction.Restrict)
  def pjidFK = foreignKey("m_presiding_judge_id_fk", presidingJudgeId, Judges)(_.id, onDelete = ForeignKeyAction.Restrict)
  def sjidFK = foreignKey("m_scoring_judge_id_fk", scoringJudgeId, Judges)(_.id.?, onDelete = ForeignKeyAction.Restrict)
}

object Matches extends TableQuery[Matches](new Matches(_)) {
  def countDefenseOccurrences(team: Team): Box[Int] = {
    DB.runAwait(
      Matches.filter(_.defenseTeamId === team.id.getOrElse(0)).length.result
    )
  }

  def countProsecutionOccurrences(team: Team): Box[Int] = {
    DB.runAwait(
      Matches.filter(_.prosecutionTeamId === team.id.getOrElse(0)).length.result
    )
  }
}
