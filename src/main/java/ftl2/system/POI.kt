package ftl2.system

import org.jdom2.Element
import ftl2.game.InGameState
import ftl2.math.ConstPoint
import ftl2.savegame.ISerialReferencable
import ftl2.savegame.ObjectRefs
import ftl2.savegame.RefLoader
import ftl2.savegame.SaveUtil
import kotlin.random.Random

/** A point-of-interest inside a `System`. */
enum class SpawnLocation { NEAR_PLANET, FREE_SPACE, OUTER_EDGE }

enum class StationType { MINING, MANUFACTURING, DOCKYARD, DEFENSE, RESEARCH, HABITAT, TRADE_HUB }

enum class DerelictType { SHIP, STATION, DEBRIS }

class POI(
    val id: Int,
    val name: String,
    val pos: ConstPoint,
    val type: POIType,
    private val seed: Int,
    var artPath: String? = null,
    var spawnLocation: SpawnLocation = SpawnLocation.FREE_SPACE,
    var stationType: StationType? = null,
    var derelictType: DerelictType? = null,
    var anomalyType: String? = null,
    var isJumpBeacon: Boolean = false
) : ISerialReferencable {
    var visited: Boolean = false

    fun saveToXML(elem: Element, refs: ObjectRefs) {
        SaveUtil.addObjectId(elem, refs, this)
        SaveUtil.addAttr(elem, "name", name)
        SaveUtil.addAttrInt(elem, "x", pos.x)
        SaveUtil.addAttrInt(elem, "y", pos.y)
        SaveUtil.addAttr(elem, "type", type.name)
        SaveUtil.addAttrInt(elem, "seed", seed)
        val ap = artPath
        if (ap != null) {
            SaveUtil.addAttr(elem, "art", ap)
        }
        SaveUtil.addAttr(elem, "spawn", spawnLocation.name)
        if (stationType != null) SaveUtil.addAttr(elem, "stationType", stationType!!.name)
        if (derelictType != null) SaveUtil.addAttr(elem, "derelictType", derelictType!!.name)
        if (anomalyType != null) SaveUtil.addAttr(elem, "anomalyType", anomalyType!!)
        if (isJumpBeacon) SaveUtil.addAttrBool(elem, "jumpBeacon", true)
        SaveUtil.addAttrBool(elem, "visited", visited)
    }

    companion object {
        fun loadFromXML(elem: Element, refs: RefLoader, game: InGameState): POI {
            val name = SaveUtil.getAttr(elem, "name")
            val x = SaveUtil.getAttrInt(elem, "x")
            val y = SaveUtil.getAttrInt(elem, "y")
            val type = POIType.valueOf(SaveUtil.getAttr(elem, "type"))
            val seed = SaveUtil.getAttrInt(elem, "seed")
            val art = elem.getAttributeValue("art")
            val spawnStr = elem.getAttributeValue("spawn")
            val spawn = try { SpawnLocation.valueOf(spawnStr ?: "FREE_SPACE") } catch (e: Exception) { SpawnLocation.FREE_SPACE }
            val stationStr = elem.getAttributeValue("stationType")
            val derelictStr = elem.getAttributeValue("derelictType")
            val anomaly = elem.getAttributeValue("anomalyType")
            val jumpBeacon = SaveUtil.getAttrBool(elem, "jumpBeacon")

            val poi = POI(Random(seed).nextInt(), name, ConstPoint(x, y), type, seed, art, spawn,
                if (stationStr != null) try { StationType.valueOf(stationStr) } catch (e: Exception) { null } else null,
                if (derelictStr != null) try { DerelictType.valueOf(derelictStr) } catch (e: Exception) { null } else null,
                anomaly,
                jumpBeacon
            )
            poi.visited = SaveUtil.getAttrBool(elem, "visited")
            SaveUtil.registerObjectId(elem, refs, poi)
            return poi
        }
    }
}

enum class POIType {
    CENTRE,
    JUMP_BEACON,
    CITY,
    OUTPOST,
    ASTEROID_FIELD,
    STATION,
    ANOMALY,
    DERELICT;

    fun defaultName(index: Int): String = when (this) {
        CENTRE -> "Star"
        CITY -> "City $index"
        OUTPOST -> "Outpost $index"
        ASTEROID_FIELD -> "Asteroid Field $index"
        STATION -> "Station $index"
        ANOMALY -> "Anomaly $index"
        JUMP_BEACON -> "Jump Beacon"
        DERELICT -> "Derelict $index"
    }
}
