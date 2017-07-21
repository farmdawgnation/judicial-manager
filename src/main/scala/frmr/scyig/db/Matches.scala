package frmr.scyig.db

import slick.jdbc.MySQLProfile.api._

case class Match(
  id: Option[Int],
  competitionId: Int,
  prosecutionTeamId: Int,
  defenseTeamId: Int,
  presidingJudgeId: Int,
  scoringJudgeId: Option[Int],
  round: Int,
  order: Int
)

class Matches(tag: Tag) extends Table[Match](tag, "matches") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def competitionId = column[Int]("competition_id")
  def prosecutionTeamId = column[Int]("prosecution_team_id")
  def defenseTeamId = column[Int]("defense_team_id")
  def presidingJudgeId = column[Int]("presiding_judge_id")
  def scoringJudgeId = column[Option[Int]]("scoring_judge_id")
  def round = column[Int]("round")
  def order = column[Int]("order")

  def * = (id.?, competitionId, prosecutionTeamId, defenseTeamId, presidingJudgeId, scoringJudgeId, round, order) <> (Match.tupled, Match.unapply)

  def cidFK = foreignKey("m_competition_id_fk", competitionId, Competitions)(_.id, onDelete = ForeignKeyAction.Cascade)
  def ptidFK = foreignKey("m_prosecution_team_id_fk", prosecutionTeamId, Teams)(_.id, onDelete = ForeignKeyAction.Restrict)
  def dtidFK = foreignKey("m_defense_team_id_fk", defenseTeamId, Teams)(_.id, onDelete = ForeignKeyAction.Restrict)
  def pjidFK = foreignKey("m_presiding_judge_id_fk", presidingJudgeId, Judges)(_.id, onDelete = ForeignKeyAction.Restrict)
  def sjidFK = foreignKey("m_scoring_judge_id_fk", scoringJudgeId, Judges)(_.id.?, onDelete = ForeignKeyAction.Restrict)
}

object Matches extends TableQuery[Matches](new Matches(_))
