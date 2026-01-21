package ftl2.crew

import ftl2.Animations
import ftl2.layout.Room

class CrewMantis(blueprint: CrewBlueprint, animations: Animations, room: Room, mode: SlotType) :
    LivingCrew(blueprint, animations, room, mode) {

    override val repairSpeed: Float get() = super.repairSpeed * 0.5f
    override val attackDamageMult: Float get() = super.attackDamageMult * 1.5f
    override val movementSpeed: Float get() = BASE_MOVEMENT_SPEED * 1.2f
}
