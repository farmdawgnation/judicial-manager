package frmr.scyig.db

import java.util.UUID
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

  implicit val judgeKindStatusColumnType = MappedColumnType.base[JudgeKind, String](
    _.value,
    forValue(_)
  )
}

case class Judge(
  id: Option[Int],
  competitionId: Int,
  name: String,
  organization: String,
  kind: JudgeKind = PresidingJudge,
  enabled: Boolean = true,
  priority: Int = 0,
  uuid: UUID = UUID.randomUUID()
)

class Judges(tag: Tag) extends Table[Judge](tag, "judges") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def competitionId = column[Int]("competition_id")
  def name = column[String]("name")
  def organization = column[String]("organization")
  def kind = column[JudgeKind]("kind")
  def enabled = column[Boolean]("enabled")
  def priority = column[Int]("priority")
  def uuid = column[UUID]("uuid")

  def * = (id.?, competitionId, name, organization, kind, enabled, priority, uuid) <> (Judge.tupled, Judge.unapply)

  def cidFK = foreignKey("j_competition_id_fk", competitionId, Judges)(_.id, onDelete = ForeignKeyAction.Cascade)
}

object Judges extends TableQuery[Judges](new Judges(_))
