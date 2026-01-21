package ftl2.crew

import ftl2.Animations
import ftl2.layout.Room

class CrewSlug(blueprint: CrewBlueprint, animations: Animations, room: Room, mode: SlotType) :
    LivingCrew(blueprint, animations, room, mode) {

    override val isMindControlResistant: Boolean get() = true

    // TODO implement
}
