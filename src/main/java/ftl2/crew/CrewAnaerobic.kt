package ftl2.crew

import ftl2.Animations
import ftl2.layout.Room

class CrewAnaerobic(blueprint: CrewBlueprint, animations: Animations, room: Room, mode: SlotType) :
    LivingCrew(blueprint, animations, room, mode) {

    override val movementSpeed: Float get() = BASE_MOVEMENT_SPEED * 0.85f
    override val suffocationMultiplier: Float get() = 0f
    override val anaerobicOxygenDrainRate: Float get() = 0.08f
}
