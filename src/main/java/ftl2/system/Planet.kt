package ftl2.system

import org.jdom2.Element
import ftl2.game.InGameState
import ftl2.math.ConstPoint
import ftl2.savegame.ISerialReferencable
import ftl2.savegame.ObjectRefs
import ftl2.savegame.RefLoader
import ftl2.savegame.SaveUtil
import kotlin.random.Random

/** A decorative planet in a `System`. */
class Planet(
    val id: Int,
    val name: String,
    val pos: ConstPoint,
    val type: PlanetType,
    private val seed: Int,
    val artPath: String? = null
) : ISerialReferencable {
    fun saveToXML(elem: Element, refs: ObjectRefs) {
        SaveUtil.addObjectId(elem, refs, this)
        SaveUtil.addAttr(elem, "name", name)
        SaveUtil.addAttrInt(elem, "x", pos.x)
        SaveUtil.addAttrInt(elem, "y", pos.y)
        SaveUtil.addAttr(elem, "type", type.name)
        SaveUtil.addAttrInt(elem, "seed", seed)
        if (artPath != null) SaveUtil.addAttr(elem, "art", artPath)
    }

    companion object {
        fun loadFromXML(elem: Element, refs: RefLoader, game: InGameState): Planet {
            val name = SaveUtil.getAttr(elem, "name")
            val x = SaveUtil.getAttrInt(elem, "x")
            val y = SaveUtil.getAttrInt(elem, "y")
            val type = PlanetType.valueOf(SaveUtil.getAttr(elem, "type"))
            val seed = SaveUtil.getAttrInt(elem, "seed")
            val art = elem.getAttributeValue("art")

            val planet = Planet(Random(seed).nextInt(), name, ConstPoint(x, y), type, seed, art)
            SaveUtil.registerObjectId(elem, refs, planet)
            return planet
        }
    }
}

enum class PlanetType {
    LIFELESS,
    MAGMA,
    TERRESTRIAL,
    GAS_GIANT,
    ICE;

    fun defaultName(index: Int): String = when (this) {
        LIFELESS -> "Lifeless Planet $index"
        MAGMA -> "Magma Planet $index"
        TERRESTRIAL -> "Planet $index"
        GAS_GIANT -> "Gas Giant $index"
        ICE -> "Ice Planet $index"
    }
}
