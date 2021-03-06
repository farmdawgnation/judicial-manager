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
import scala.util.Random
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

    // This is lame but we have some DB queries / transforms that execute in the
    // AJAX cycle that we want to give time to complete
    LiftRules.ajaxPostTimeout = 60000

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

      CompViewSchedule.menu,

      TeamList.menu,
      TeamUpload.menu,
      TeamForm.createMenu,
      TeamForm.editMenu,

      JudgeList.menu,
      JudgeUpload.menu,
      JudgeForm.createMenu,
      JudgeForm.editMenu,

      ScorecardExport.menu,

      admin.Dashboard.menu,

      admin.UserList.menu,
      admin.UserForm.createMenu,
      admin.UserForm.editMenu,

      admin.SponsorList.menu,
      admin.SponsorForm.createMenu,
      admin.SponsorForm.editMenu,

      admin.CompetitionList.menu,
      admin.CompetitionForm.createMenu,
      admin.CompetitionForm.editMenu
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

    // Create seed data if there are no users
    DB.run(Users.size.result).map { userCount =>
      if (userCount == 0) {
        logger.info("No users found in db. Attempting seed data creation.")

        val adminpw = if (Props.productionMode) {
          val productionAdminPassword = Random.alphanumeric.take(16).mkString
          logger.info(s"Admin password: $productionAdminPassword")
          Users.hashpw(productionAdminPassword)
        } else {
          logger.info("Admin password: admin")
          Users.hashpw("admin")
        }

        val seedCreation = DB.run(DBIO.seq(
          Users += User(
            None,
            "admin@admin.com",
            adminpw,
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

        seedCreation.map { _ => logger.info("Seed data successfully created.") }
      }
    }
  }
}
