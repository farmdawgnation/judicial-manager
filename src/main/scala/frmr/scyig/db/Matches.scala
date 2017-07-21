package frmr.scyig.db

import slick.jdbc.H2Profile.api._

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

  def cidFK = foreignKey("competition_id_fk", competitionId, Competitions)(_.id, onDelete = ForeignKeyAction.Cascade)
  def ptidFK = foreignKey("prosecution_team_id_fk", prosecutionTeamId, Teams)(_.id, onDelete = ForeignKeyAction.Restrict)
  def dtidFK = foreignKey("defense_team_id_fk", defenseTeamId, Teams)(_.id, onDelete = ForeignKeyAction.Restrict)
  def pjidFK = foreignKey("presiding_judge_id_fk", presidingJudgeId, Judges)(_.id, onDelete = ForeignKeyAction.Restrict)
  def sjidFK = foreignKey("scoring_judge_id_fk", scoringJudgeId, Judges)(_.id.?, onDelete = ForeignKeyAction.Restrict)
}

object Matches extends TableQuery[Matches](new Matches(_))
