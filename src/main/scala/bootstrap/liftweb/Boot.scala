/**
 * Copyright 2016 Matthew Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
**/
package bootstrap.liftweb

import java.sql._
import net.liftweb.http._
import net.liftweb.squerylrecord._
import net.liftweb.squerylrecord.RecordTypeMode._
import net.liftweb.util._
import org.squeryl._
import org.squeryl.adapters._

class Boot {
  def boot(): Unit = {
    Class.forName("org.postgresql.Driver")

    def connection = DriverManager.getConnection("jdbc:postgresql://localhost/scyig", "postgres", "yigisgreat")

    SquerylRecord.initWithSquerylSession( Session.create(connection, new PostgreSqlAdapter) )

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // Base package for the webapp
    LiftRules.addToPackages("frmr.scyig.httpd")

    S.addAround(new LoanWrapper {
      override def apply[T](f: => T): T = {
        val result = inTransaction {
          try {
            Right(f)
          } catch {
            case e: LiftFlowOfControlException => Left(e)
          }
        }

        result match {
          case Right(r) => r
          case Left(exception) => throw exception
        }
      }
    })
  }
}