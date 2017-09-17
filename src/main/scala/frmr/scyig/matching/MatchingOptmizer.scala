package frmr.scyig.matching

import frmr.scyig.matching.models._

object MatchingOptimizer {
  def noOpOptmizer(rounds: Seq[ScheduledRoundMatch]) = rounds

  /**
   * Attempts to even out the number of times a particular team plays prosecution or defense.
   */
  def roleOpimizer(rounds: Seq[ScheduledRoundMatch]) = rounds.map {
    case byeRound: Bye =>
      byeRound

    case trialRound: Trial =>
      val prosecutionRoleScore =
        trialRound.prosecution.prosecutionCount - trialRound.prosecution.defenseCount
      val defenseRoleScore =
        trialRound.defense.prosecutionCount - trialRound.defense.defenseCount

      val swappingRolesWouldHelp =
        prosecutionRoleScore > 0 && defenseRoleScore < 0

      if (swappingRolesWouldHelp) {
        trialRound.copy(
          prosecution = trialRound.defense,
          defense = trialRound.prosecution
        )
      } else {
        trialRound
      }
  }
}
