package frmr.scyig.db

import slick.jdbc.MySQLProfile.api._

sealed trait JudgeKind {
  def value: String
}
case object PresidingJudge extends JudgeKind {
  val value = "Presiding"
}
case object ScoringJudge extends JudgeKind {
  val value = "Scoring"
}

object JudgeKind {
  def forValue(input: String): JudgeKind = input match {
    case PresidingJudge.value => PresidingJudge
    case ScoringJudge.value => ScoringJudge
    case s => throw new IllegalStateException(s"Illegal judge kind: $s")
  }

  implicit val judeKindStatusColumnType = MappedColumnType.base[JudgeKind, String](
    _.value,
    forValue(_)
  )
}

case class Judge(
  id: Option[Int],
  competitionId: Int,
  name: String,
  organization: String,
  kind: JudgeKind
)

class Judges(tag: Tag) extends Table[Judge](tag, "judges") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def competitionId = column[Int]("competition_id")
  def name = column[String]("name")
  def organization = column[String]("organization")
  def kind = column[JudgeKind]("kind")

  def * = (id.?, competitionId, name, organization, kind) <> (Judge.tupled, Judge.unapply)

  def cidFK = foreignKey("j_competition_id_fk", competitionId, Judges)(_.id, onDelete = ForeignKeyAction.Cascade)
}

object Judges extends TableQuery[Judges](new Judges(_))
