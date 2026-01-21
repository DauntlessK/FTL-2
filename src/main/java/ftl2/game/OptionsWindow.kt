package ftl2.game

import ftl2.f
import ftl2.math.ConstPoint
import ftl2.math.IPoint
import ftl2.rendering.Graphics
import ftl2.ui.Slider
import ftl2.ui.WidgetContainer

class OptionsWindow(val game: InGameState, val close: () -> Unit) : Window() {
    private val widgetTree: WidgetContainer = game.uiLoader.load("options").mainWidget

    private val soundSlider = widgetTree.byId["sound"] as Slider
    private val musicSlider = widgetTree.byId["music"] as Slider

    override val size: IPoint get() = widgetTree.root.size

    init {
        buttons += widgetTree.buildButtons(game, this, ConstPoint.ZERO)

        soundSlider.value = game.mainGame.profile.soundVolume
        musicSlider.value = game.mainGame.profile.musicVolume

        soundSlider.changeListener = this::updateVolumes
        musicSlider.changeListener = this::updateVolumes

        widgetTree.addButtonListener("keybinds") {
            game.shipUI.showKeybindWindow()
        }
    }

    override fun draw(g: Graphics) {
        g.pushTransform()
        g.translate(position.x.f, position.y.f)

        widgetTree.draw(g)

        g.popTransform()
    }

    override fun escapePressed() {
        updateVolumes()
        game.mainGame.profile.markDirty()

        close()
    }

    private fun updateVolumes() {
        game.mainGame.profile.soundVolume = soundSlider.value
        game.mainGame.profile.musicVolume = musicSlider.value
    }
}
