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
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.util._
import frmr.scyig.db._
import frmr.scyig.webapp.api._
import frmr.scyig.webapp.auth.AuthenticationHelpers._
import frmr.scyig.webapp.auth._
import frmr.scyig.webapp.js._
import frmr.scyig.webapp.snippet._
import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta._

class Boot extends Loggable {
  def boot(): Unit = {
    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // Base package for the webapp
    LiftRules.addToPackages("frmr.scyig.webapp")

    // Know how to check logged in status
    LiftRules.loggedInTest = Full( () => {
      currentUserId.is > 0
    })

    // Define our site map
    LiftRules.setSiteMap(SiteMap(
      Menu.i("root") / "index" >>
        EarlyResponse(() => Full(RedirectResponse(CompChooser.menu.loc.calcDefaultHref))),
      Login.menu,
      Logout.menu,

      CompChooser.menu,

      CompDashboard.menu,

      CompScoreEntry.menu,

      CompSchedulerSetup.setupMenu,
      CompSchedulerSetup.scheduleMenu,

      CompSchedule.menu,

      TeamList.menu,
      TeamForm.createMenu,
      TeamForm.editMenu,

      JudgeList.menu,
      JudgeForm.createMenu,
      JudgeForm.editMenu
    ))

    // Add the stateful rest api
    LiftRules.dispatch.append(SuggestionApi)

    // Set security rules
    LiftRules.securityRules = () => {
      SecurityRules(
        content = Some(ContentSecurityPolicy(
          styleSources = List(
            ContentSourceRestriction.Self,
            ContentSourceRestriction.UnsafeInline,
            ContentSourceRestriction.Host("https://*.bootstrapcdn.com"),
            ContentSourceRestriction.Host("https://fonts.googleapis.com")
          ),
          scriptSources = List(
            ContentSourceRestriction.Self,
            ContentSourceRestriction.UnsafeInline,
            ContentSourceRestriction.Host("https://*.bootstrapcdn.com"),
            ContentSourceRestriction.Host("https://*.cloudflare.com"),
            ContentSourceRestriction.Host("https://code.jquery.com")
          ),
          fontSources = List(
            ContentSourceRestriction.Self,
            ContentSourceRestriction.Host("https://fonts.gstatic.com"),
            ContentSourceRestriction.Host("https://fonts.googleapis.com")
          )
        ))
      )
    }

    // Disable caching in development mode
    if (! Props.productionMode) {
      LiftRules.supplementalHeaders.default.set(
        LiftRules.securityRules().headers ++
        List("Cache-Control" -> "no-cache, no-store, must-revalidate")
      )
    }

    // Trigger the event for the start of AJAX servicing
    LiftRules.ajaxStart = Full( () => TriggerEvent("ajax-servicing-started") )
    LiftRules.ajaxEnd = Full( () => TriggerEvent("ajax-servicing-ended") )

    // Attach the extended session management
    LiftRules.earlyInStateful.append(AuthenticationHelpers.authenticateFromSessionCookie_! _)

    // Run our migrations
    DB.migrate()

    // Create admin user if it doesn't already exist
    DB.run(Users.size.result).map { userCount =>
      if (userCount == 0) {
        logger.info("No users found in db. Attempting admin user creation.")
        val userCreation = DB.run(DBIO.seq(
          Users += User(
            None,
            "admin@admin.com",
            Users.hashpw("admin"),
            "Admin",
            true
          ),
          Sponsors += Sponsor(
            None,
            "Judicial Manager Developers",
            "Atlanta, GA, USA"
          ),
          UsersSponsors += ("admin", 1, 1),
          Competitions += Competition(
            None,
            "JM Testing",
            1,
            "Nov 2017",
            "Testing competition",
            "Atlanta, GA"
          )
        ))

        userCreation.map { _ => logger.info("Admin user successfully created.") }
      }
    }
  }
}
