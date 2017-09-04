package frmr.scyig.webapp.js

import net.liftweb.http.js._
import net.liftweb.http.js.JE._
import net.liftweb.json._

/**
 * Trigger a DOM event on the client with an arbitrary name and
 * parameters.
 */
class TriggerEvent(eventName:String, parameters:JObject = JObject(Nil)) extends JsCmd {
  def toJsCmd = {
    Call("judicialManager.event", eventName, parameters).cmd.toJsCmd
  }
}
object TriggerEvent {
  def apply(eventName: String) = new TriggerEvent(eventName)
}

/**
 * An abstract class that can be extended to make the extending
 * class a DOM event that can be triggered on the client.
 */
abstract class DomEvent(eventName:String) extends JsCmd {
  import Extraction._

  implicit def typeHints = Serialization.formats(NoTypeHints)

  def toJsCmd = {
    Call("judicialManager.event", eventName, decompose(this)).cmd.toJsCmd
  }
}
