package ftl2.crew

import ftl2.Animations
import ftl2.layout.Room

class CrewRock(blueprint: CrewBlueprint, animations: Animations, room: Room, mode: SlotType) :
    LivingCrew(blueprint, animations, room, mode) {

    override val maxHealth: Float get() = 150f
    override val movementSpeed: Float get() = BASE_MOVEMENT_SPEED * 0.5f
    override val fireFightingSpeed: Float get() = 1.67f
    override val fireDamageMult: Float get() = 0f
}
