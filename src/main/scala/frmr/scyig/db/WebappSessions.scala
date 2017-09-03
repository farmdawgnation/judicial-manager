package frmr.scyig.db

import slick.jdbc.MySQLProfile.api._

case class WebappSession(
  id: String,
  userId: Int
)

class WebappSessions(tag: Tag) extends Table[WebappSession](tag, "webapp_sessions") {
  def id = column[String]("id", O.PrimaryKey, O.SqlType("CHAR(64)"))
  def userId = column[Int]("user_id")

  def * = (id, userId) <> (WebappSession.tupled, WebappSession.unapply)

  def uidFK = foreignKey("ws_user_id_fk", userId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)
}

object WebappSessions extends TableQuery[WebappSessions](new WebappSessions(_))
