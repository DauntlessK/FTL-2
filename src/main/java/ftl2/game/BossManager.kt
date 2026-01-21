package ftl2.game

import org.jdom2.Element
import ftl2.Ship
import ftl2.math.IPoint
import ftl2.rendering.Graphics
import ftl2.savegame.ISerialReferencable
import ftl2.savegame.ObjectRefs
import ftl2.savegame.RefLoader
import ftl2.sector.Beacon
import ftl2.sector.Sector

/**
 * Manages a boss ship for a sector.
 *
 * Such a ship may occupy a given beacon, and be drawn on the map as jumping.
 *
 * This is used to implement the flagship, but mods can use it for custom bosses.
 */
interface BossManager : ISerialReferencable {
    /**
     * A unique string identifying this boss. This is saved so the correct boss
     * can be deserialised.
     */
    val serialisationType: String

    /**
     * If this boss is drawn as occupying a beacon, this is non-null.
     */
    val beacon: Beacon?

    /**
     * The beacon the boss will be at next time it jumps. This can be set to
     * null if it's stationary.
     */
    val nextBeacon: Beacon?

    /**
     * True if the boss will move on the next jump.
     */
    val jumping: Boolean

    /**
     * True if the ship spawned by this manager should use the larger flagship UI elements,
     * instead of the regular enemy UI elements.
     */
    val useBossShipUI: Boolean

    /**
     * Create the boss ship.
     *
     * This is called whenever the player jumps to the beacon that contains this ship.
     *
     * The returned ship is not serialised if the player jumps away.
     */
    fun createShip(): Ship

    /**
     * Draw the icon for this boss on the map.
     *
     * [centre] is the centre position of the beacon around which the boss should be drawn.
     */
    fun drawMapIcon(g: Graphics, centre: IPoint)

    /**
     * Draw the arrow indicating the boss will jump to the next beacon at the same time
     * as the player next jumps.
     */
    fun drawJumpArc(g: Graphics, from: IPoint, to: IPoint)

    /**
     * Called every time the player jumps.
     */
    fun advanceFleet()

    /**
     * Called when the player kills a ship spawned by this boss.
     */
    fun bossShipKilled(enemy: Ship)

    // Serialisation
    fun saveToXML(elem: Element, refs: ObjectRefs)
    fun loadFromXML(elem: Element, refs: RefLoader)

    companion object {
        fun createBySerialisationType(type: String, sector: Sector, game: InGameState): BossManager {
            return when (type) {
                FlagshipBoss.SERIALISATION_TYPE -> FlagshipBoss.createForDeserialisation(sector, game)
                else -> error("Unknown boss serialisation type '$type'")
            }
        }
    }
}
