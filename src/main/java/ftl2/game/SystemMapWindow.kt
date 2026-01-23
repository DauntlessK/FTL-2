package ftl2.game

import ftl2.Constants
import ftl2.game.Window
import ftl2.math.ConstPoint
import ftl2.math.Point
import ftl2.rendering.Colour
import ftl2.rendering.Graphics
import ftl2.system.POI
import ftl2.system.System
import kotlin.math.hypot
import kotlin.math.roundToInt

class SystemMapWindow(private val game: InGameState, displaySystem: ftl2.system.System? = null, private val selectedCallback: (POI?) -> Unit) : Window() {

    override val size = ConstPoint(567, 327)

    // Use a slightly smaller, clearer font for labels
    private val titleFont = game.getFont("HL1", 2.5f)

    // Use provided system or generate a temporary one for display
    // Default to a test system named "Orion"
    private val system = displaySystem ?: System.generate("Orion", game.difficulty)

    // Compute centre dynamically from window position (small visual offset applied)
    private fun centre(): Point {
        // Add a small x/y offset so the system sits slightly to the right and lower
        return Point(position.x + size.x / 2 + 24, position.y + size.y / 2 + 8)
    }

    private var hovered: POI? = null

    override fun draw(g: Graphics) {
        // Background box
        g.colour = Colour(20, 24, 30)
        g.fillRect(position.x, position.y, size.x, size.y)

        // Title
        titleFont.drawString(position.x + 13f, position.y + 25f, "System map", Constants.JUMP_DISABLED_TEXT)

        // Draw centre/star
        val star = system.centre
        val c = centre()

        // Compute scale so all POIs/planets fit inside the window
        val padding = 36f
        val maxDrawable = kotlin.math.min(size.x.toFloat(), size.y.toFloat()) / 2f - padding
        var maxDist = 0f
        for (poi in system.pois) {
            val d = hypot(poi.pos.x.toDouble(), poi.pos.y.toDouble()).toFloat()
            if (d > maxDist) maxDist = d
        }
        for (planet in system.planets) {
            val d = hypot(planet.pos.x.toDouble(), planet.pos.y.toDouble()).toFloat()
            if (d > maxDist) maxDist = d
        }
        // Include star if it isn't at origin
        val starDist = hypot(star.pos.x.toDouble(), star.pos.y.toDouble()).toFloat()
        if (starDist > maxDist) maxDist = starDist
        val scale = if (maxDist > 0f && maxDist > maxDrawable) maxDrawable / maxDist else 1f

        val starPos = Point((c.x + star.pos.x * scale).roundToInt(), (c.y + star.pos.y * scale).roundToInt())
        // Draw the star: prefer an art PNG if available, otherwise draw a circle
        val starImg = if (star.artPath != null) game.getImgIfExists(star.artPath) else null
        if (starImg != null) {
            // Draw at native size, centred
            val w = starImg.width
            val h = starImg.height
            starImg.draw(starPos.x - w / 2f, starPos.y - h / 2f)
        } else {
            g.colour = Colour(255, 215, 80)
            g.fillOval(starPos.x - 16f, starPos.y - 16f, 32f, 32f)
        }

        // Draw POIs (use art if available)
        // Draw planets (decorative)
        for (planet in system.planets) {
            val pp = Point((c.x + planet.pos.x * scale).roundToInt(), (c.y + planet.pos.y * scale).roundToInt())
            val pImg = if (planet.artPath != null) game.getImgIfExists(planet.artPath) else null
            if (pImg != null) {
                val iw = pImg.width
                val ih = pImg.height
                pImg.draw(pp.x - iw / 2f, pp.y - ih / 2f)
            }
        }

        for (poi in system.pois) {
            val p = Point((c.x + poi.pos.x * scale).roundToInt(), (c.y + poi.pos.y * scale).roundToInt())

            val poiImg = if (poi.artPath != null) game.getImgIfExists(poi.artPath) else null
            val isHovered = poi == hovered

            if (poiImg != null) {
                // Draw at native size, centred
                val iw = poiImg.width
                val ih = poiImg.height
                poiImg.draw(p.x - iw / 2f, p.y - ih / 2f)
            } else {
                g.colour = if (isHovered) Constants.SECTOR_BRANCH_HOVER else Colour(180, 200, 220)
                g.fillOval(p.x - 6f, p.y - 6f, 12f, 12f)
            }

            // Name
            titleFont.drawString(p.x + 8f, p.y + 4f, poi.name, Constants.SECTOR_NAME_TEXT)
        }
    }

    override fun updateUI(x: Int, y: Int) {
        super.updateUI(x, y)

        hovered = null
        val mx = x
        val my = y
        val c = centre()

        // Recompute same scale as draw so hover positions match rendering
        val padding = 36f
        val maxDrawable = kotlin.math.min(size.x.toFloat(), size.y.toFloat()) / 2f - padding
        var maxDist = 0f
        for (poi in system.pois) {
            val d = hypot(poi.pos.x.toDouble(), poi.pos.y.toDouble()).toFloat()
            if (d > maxDist) maxDist = d
        }
        val starDist = hypot(system.centre.pos.x.toDouble(), system.centre.pos.y.toDouble()).toFloat()
        if (starDist > maxDist) maxDist = starDist
        val scale = if (maxDist > 0f && maxDist > maxDrawable) maxDrawable / maxDist else 1f

        for (poi in system.pois) {
            val p = Point((c.x + poi.pos.x * scale).roundToInt(), (c.y + poi.pos.y * scale).roundToInt())
            val d = hypot((mx - p.x).toDouble(), (my - p.y).toDouble())
            val img = if (poi.artPath != null) game.getImgIfExists(poi.artPath) else null
            val radius = if (img != null) (kotlin.math.max(img.width, img.height) / 2f + 4f) else 16f
            if (d <= radius) {
                hovered = poi
                break
            }
        }
    }

    override fun mouseClick(button: Int, x: Int, y: Int) {
        super.mouseClick(button, x, y)
        // If clicked outside window, treat as cancel
        if (hovered == null) {
            selectedCallback(null)
            return
        }

        selectedCallback(hovered)
    }

    override fun escapePressed() {
        selectedCallback(null)
    }
}
