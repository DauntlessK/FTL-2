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
class POI(
    val id: Int,
    val name: String,
    val pos: ConstPoint,
    val type: POIType,
    private val seed: Int,
    var artPath: String? = null
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

            val poi = POI(Random(seed).nextInt(), name, ConstPoint(x, y), type, seed, art)
            poi.visited = SaveUtil.getAttrBool(elem, "visited")
            SaveUtil.registerObjectId(elem, refs, poi)
            return poi
        }
    }
}

enum class POIType {
    CENTRE,
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
        DERELICT -> "Derelict $index"
    }
}
