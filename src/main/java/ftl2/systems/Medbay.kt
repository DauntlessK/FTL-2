package ftl2.systems

import org.jdom2.Element
import ftl2.SystemInfo
import ftl2.Translator
import ftl2.crew.AbstractCrew
import ftl2.crew.LivingCrew
import ftl2.crew.MedbayHealing
import ftl2.game.UIUtils
import ftl2.replaceArg
import ftl2.savegame.ObjectRefs
import ftl2.savegame.RefLoader
import kotlin.math.roundToInt

class Medbay(blueprint: SystemBlueprint) : MainSystem(blueprint) {
    override val sortingType: SortingType get() = SortingType.MEDBAY

    override fun update(dt: Float) {
        super.update(dt)

        var healthPerSec = when (powerSelected) {
            0 -> 0f
            else -> 6.4f * speedForLevel(powerSelected - 1)
        }

        // Hacking hurts the friendly crew, and rather quickly.
        if (isHackActive) {
            healthPerSec = -13f
        }

        val healing = healthPerSec * dt

        for (crew in room!!.crew) {
            // Don't heal drones
            if (crew !is LivingCrew)
                continue

            // Only heal friendly crew
            if (crew.mode == AbstractCrew.SlotType.INTRUDER)
                continue

            // Don't revive dying crewmembers
            if (crew.currentAction == AbstractCrew.Action.DYING)
                continue

            crew.dealDamage(MedbayHealing(-healing))
        }
    }

    // Nothing to serialise
    override fun saveSystem(elem: Element, refs: ObjectRefs) = Unit
    override fun loadSystem(elem: Element, refs: RefLoader) = Unit

    companion object {
        fun speedForLevel(level: Int): Float {
            return when (level) {
                0 -> 1f
                else -> level * 1.5f
            }
        }

        val INFO: SystemInfo = MedbayInfo
    }
}

private object MedbayInfo : SystemInfo("medbay") {
    override val canBeManned: Boolean get() = false
    override val isComputerObstruction: Boolean get() = true

    override fun create(blueprint: SystemBlueprint) = Medbay(blueprint)

    override fun getLevelName(level: Int, translator: Translator): String {
        val fixedPoint = (Medbay.speedForLevel(level) * 100).roundToInt()
        val speedStr = UIUtils.formatStringFTL(fixedPoint)
        return translator["medbay_healing"].replaceArg(speedStr)
    }
}
