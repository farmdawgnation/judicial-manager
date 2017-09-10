package frmr.scyig.db

import slick.jdbc.MySQLProfile.api._

case class Score(
  matchId: Int,
  teamId: Int,
  scorerId: Int,
  score: Int
)

class Scores(tag: Tag) extends Table[Score](tag, "scores") {
  def matchId = column[Int]("match_id")
  def teamId = column[Int]("team_id")
  def scorerId = column[Int]("scorer_id")
  def score = column[Int]("score")

  def * = (matchId, teamId, scorerId, score) <> (Score.tupled, Score.unapply)
}

object Scores extends TableQuery[Scores](new Scores(_))
