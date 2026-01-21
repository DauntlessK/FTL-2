package ftl2.environment

import ftl2.game.InGameState
import ftl2.sector.Beacon

/**
 * The environment used at a regular beacon.
 */
class NormalEnvironment(game: InGameState, beacon: Beacon) : AbstractEnvironment(game, beacon) {
    override val type: Beacon.EnvironmentType get() = Beacon.EnvironmentType.NORMAL
}
