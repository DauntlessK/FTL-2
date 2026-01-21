package ftl2.crew

import ftl2.Animations
import ftl2.layout.Room

class CrewGhost(blueprint: CrewBlueprint, animations: Animations, room: Room, mode: SlotType) :
    LivingCrew(blueprint, animations, room, mode) {

    override val maxHealth: Float get() = 50f
    override val suffocationMultiplier: Float get() = 0f

    // TODO somehow make them use humans sheets and animations but with
    //   reduced opacity! (otherwise crash if encountered since no animations defined)
}
