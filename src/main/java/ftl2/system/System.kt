package ftl2.system

import org.jdom2.Element
import ftl2.game.Difficulty
import ftl2.game.InGameState
import ftl2.math.ConstPoint
import ftl2.savegame.ObjectRefs
import ftl2.savegame.RefLoader
import ftl2.savegame.SaveUtil
import java.util.*
import kotlin.random.Random

enum class StarType { G, B, M, BINARY }

/**
 * Represents a traditional solar-style system containing a central star and a
 * collection of points-of-interest (POI) such as planets, stations and anomalies.
 * Movement is free between POIs (no explicit graph of connections).
 */
class System {
    val name: String

    /** Star spectral/type for this system (influences planet placement). */
    val starType: StarType

    /** The central object of the system (usually a star). */
    val centre: POI

    /** Background image path chosen for this system (optional). */
    val bgStarArtPath: String?

    /** The collection of POIs in this system (does not include the centre). */
    val pois = ArrayList<POI>()

    /** Planets (decorative) - not clickable POIs. */
    val planets = ArrayList<Planet>()

    constructor(name: String, centre: POI, planets: List<Planet>, pois: List<POI>, starType: StarType = StarType.G, bgStarArtPath: String? = null) {
        this.name = name
        this.centre = centre
        this.starType = starType
        this.planets.addAll(planets)
        this.pois.addAll(pois)
        this.bgStarArtPath = bgStarArtPath
    }

    /** Procedurally generate a system around a central star. */
    companion object {
        /** Generate a system with a pseudo-random layout.
         *  @param seed optional seed for reproducibility.
         */
        fun generate(name: String, difficulty: Difficulty, seed: Int? = null): System {
            val rand = if (seed != null) Random(seed) else Random.Default
            // Choose a star type for this system
            val starType = StarType.values()[rand.nextInt(StarType.values().size)]

            // Pick sun art depending on star type
            val sunVariants = when (starType) {
                StarType.G -> listOf("assets/img/system/suns/sun1.png")
                StarType.B -> listOf("assets/img/system/suns/sun2.png")
                StarType.M -> listOf("assets/img/system/suns/sun1.png")
                StarType.BINARY -> listOf("assets/img/system/suns/sun2.png")
            }
            val chosenSun = sunVariants[rand.nextInt(sunVariants.size)]
            val star = POI(
                id = rand.nextInt(),
                name = "Star",
                pos = ConstPoint(0, 0),
                type = POIType.CENTRE,
                seed = rand.nextInt(),
                artPath = chosenSun
            )

            // Choose and store a background for this system (persistent)
            val bgChoices = listOf("img/stars/bg_dullstars.png", "img/stars/bg_dullstars2.png")
            val chosenBg = bgChoices[rand.nextInt(bgChoices.size)]

            // We'll consider 8 possible orbits and decide per-orbit whether to place a planet
            val ORBITS = 8

            // Minimum separation (pixels) between POIs to avoid overlap
            // Increased to keep POIs further from planets
            val MIN_SEPARATION = 160

            // Extra inner gap from the star so the first planet/orbit isn't too close
            val INNER_GAP = 80

            // Total target POIs per system (aim 3..7)
            var totalTarget = rand.nextInt(3, 8)

            val pois = ArrayList<POI>()
            val STANDARD_POI_ART = "img/map/map_icon_diamond_yellow.png"

            // Place planets on concentric orbits
            // Slightly larger spacing so POIs don't sit on top of planets
            val orbitSpacing = 210
            val planetList = ArrayList<Planet>()
            // Loop through each orbit and decide whether to place a planet
            for (i in 1..ORBITS) {
                // Base placement chance; can be adjusted by star type and orbit index
                var placeChance = 0.5f
                when (starType) {
                    StarType.G -> placeChance += 0.0f
                    StarType.B -> placeChance += 0.05f
                    StarType.M -> placeChance -= 0.1f
                    StarType.BINARY -> placeChance -= 0.4f
                }

                // Make inner orbits slightly more/less likely
                placeChance -= (1 - (i.toFloat() / ORBITS)) * 0.05f

                val roll = rand.nextFloat()
                if (roll > placeChance) continue // skip placing a planet on this orbit

                var placed = false
                var attempts = 0
                var x = 0
                var y = 0
                while (!placed && attempts < 64) {
                    val radius = INNER_GAP + i * orbitSpacing + rand.nextInt(-40, 41)
                    val angle = rand.nextDouble(0.0, Math.PI * 2)
                    x = (radius * kotlin.math.cos(angle)).toInt()
                    y = (radius * kotlin.math.sin(angle)).toInt()

                    // Ensure separation from existing planets
                    var ok = true
                    for (other in planetList) {
                        val dx = other.pos.x - x
                        val dy = other.pos.y - y
                        if (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) < MIN_SEPARATION) {
                            ok = false
                            break
                        }
                    }
                    if (ok) placed = true
                    attempts++
                }

                // Decide planet type allowed by orbit
                val planetType = when (i) {
                    1, 2 -> if (starType == StarType.BINARY) null else listOf(PlanetType.MAGMA, PlanetType.LIFELESS).let { it[rand.nextInt(it.size)] }
                    in 2..4 -> listOf(PlanetType.LIFELESS, PlanetType.TERRESTRIAL).let { it[rand.nextInt(it.size)] }
                    in 3..6 -> listOf(PlanetType.GAS_GIANT, PlanetType.TERRESTRIAL).let { it[rand.nextInt(it.size)] }
                    6, 7 -> listOf(PlanetType.ICE, PlanetType.GAS_GIANT).let { it[rand.nextInt(it.size)] }
                    else -> listOf(PlanetType.TERRESTRIAL, PlanetType.LIFELESS).let { it[rand.nextInt(it.size)] }
                }

                if (planetType == null) continue

                // Pick art for planet type
                val artPath = when (planetType) {
                    PlanetType.TERRESTRIAL -> listOf(
                        "assets/img/system/poi/planet_terrestrial1.png",
                        "assets/img/system/poi/planet_terrestrial2.png",
                        "assets/img/system/poi/planet_terrestrial3.png"
                    ).let { it[rand.nextInt(it.size)] }
                    PlanetType.LIFELESS -> listOf(
                        "assets/img/system/poi/planet_lifeless1.png",
                        "assets/img/system/poi/planet_lifeless2.png",
                        "assets/img/system/poi/planet_lifeless3.png"
                    ).let { it[rand.nextInt(it.size)] }
                    PlanetType.MAGMA -> listOf(
                        "assets/img/system/poi/planet_magma1.png",
                        "assets/img/system/poi/planet_magma2.png"
                    ).let { it[rand.nextInt(it.size)] }
                    PlanetType.ICE -> listOf(
                        "assets/img/system/poi/planet_ice1.png",
                        "assets/img/system/poi/planet_ice2.png"
                    ).let { it[rand.nextInt(it.size)] }
                    PlanetType.GAS_GIANT -> listOf(
                        "assets/img/system/poi/planet_gas1.png",
                        "assets/img/system/poi/planet_gas2.png",
                        "assets/img/system/poi/planet_gas3.png"
                    ).let { it[rand.nextInt(it.size)] }
                }

                val planet = Planet(
                    id = rand.nextInt(),
                    name = planetType.defaultName(i),
                    pos = ConstPoint(x, y),
                    type = planetType,
                    seed = rand.nextInt(),
                    artPath = artPath
                )
                planetList += planet
            }

            // Ensure at least one planet exists so the system isn't empty of planets
            if (planetList.isEmpty()) {
                val fallbackRadius = INNER_GAP + 3 * orbitSpacing
                val fx = (fallbackRadius * kotlin.math.cos(0.0)).toInt()
                val fy = (fallbackRadius * kotlin.math.sin(0.0)).toInt()
                val pType = PlanetType.TERRESTRIAL
                val artPath = "assets/img/system/poi/planet_terrestrial1.png"
                val planet = Planet(rand.nextInt(), pType.defaultName(1), ConstPoint(fx, fy), pType, rand.nextInt(), artPath)
                planetList += planet
            }

            // Map to track how many close POIs (satellites) each planet has
            val satelliteCounts = HashMap<Planet, Int>()

            // Ensure totalTarget respects actual planets placed
            if (totalTarget < planetList.size) totalTarget = planetList.size

            // First pass: attempt to place a POI on/near each planet according to planet type
            for (parent in planetList) {
                if (pois.size >= totalTarget) break

                val baseChance = 50
                val chance = when (parent.type) {
                    PlanetType.TERRESTRIAL -> baseChance + 20
                    PlanetType.GAS_GIANT -> baseChance + 10
                    PlanetType.MAGMA -> baseChance - 10
                    PlanetType.ICE -> baseChance - 5
                    PlanetType.LIFELESS -> baseChance - 5
                }

                if (rand.nextInt(100) >= chance) continue

                val index = satelliteCounts.getOrDefault(parent, 0)
                var sx = 0
                var sy = 0
                var satAttempts = 0
                var satPlaced = false
                val baseSatRadius = 40 + index * 18
                while (!satPlaced && satAttempts < 32) {
                    val sRadius = baseSatRadius + rand.nextInt(-12, 13)
                    val sAngle = rand.nextDouble(0.0, Math.PI * 2)
                    sx = parent.pos.x + (sRadius * kotlin.math.cos(sAngle)).toInt()
                    sy = parent.pos.y + (sRadius * kotlin.math.sin(sAngle)).toInt()
                    var ok = true
                    for (existing in pois) {
                        val dx = existing.pos.x - sx
                        val dy = existing.pos.y - sy
                        if (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) < MIN_SEPARATION) { ok = false; break }
                    }
                    if (ok) {
                        for (existing in planetList) {
                            val dx = existing.pos.x - sx
                            val dy = existing.pos.y - sy
                            if (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) < MIN_SEPARATION) { ok = false; break }
                        }
                    }
                    if (ok) satPlaced = true
                    satAttempts++
                }
                if (!satPlaced) {
                    val fallbackAngle = rand.nextDouble(0.0, Math.PI * 2)
                    val fallbackR = baseSatRadius
                    sx = parent.pos.x + (fallbackR * kotlin.math.cos(fallbackAngle)).toInt()
                    sy = parent.pos.y + (fallbackR * kotlin.math.sin(fallbackAngle)).toInt()
                }

                val type = when (parent.type) {
                    PlanetType.TERRESTRIAL -> when (rand.nextInt(100)) {
                        in 0..39 -> POIType.CITY
                        in 40..79 -> POIType.STATION
                        in 80..89 -> POIType.OUTPOST
                        in 90..94 -> POIType.ASTEROID_FIELD
                        else -> POIType.DERELICT
                    }
                    PlanetType.GAS_GIANT -> when (rand.nextInt(100)) {
                        in 0..49 -> POIType.STATION
                        in 50..79 -> POIType.OUTPOST
                        in 80..89 -> POIType.ASTEROID_FIELD
                        else -> POIType.DERELICT
                    }
                    else -> when (rand.nextInt(100)) {
                        in 0..39 -> POIType.OUTPOST
                        in 40..69 -> POIType.ASTEROID_FIELD
                        in 70..89 -> POIType.DERELICT
                        else -> POIType.STATION
                    }
                }

                val poiArt = STANDARD_POI_ART

                // subtype assignment
                var stationSubtype: ftl2.system.StationType? = null
                var derelictSubtype: ftl2.system.DerelictType? = null
                var anomalySubtype: String? = null
                if (type == POIType.STATION) {
                    val st = ftl2.system.StationType.values()[rand.nextInt(ftl2.system.StationType.values().size)]
                    stationSubtype = st
                }
                if (type == POIType.DERELICT) {
                    val dt = ftl2.system.DerelictType.values()[rand.nextInt(ftl2.system.DerelictType.values().size)]
                    derelictSubtype = dt
                }

                val poi = POI(
                    id = rand.nextInt(),
                    name = type.defaultName(pois.size + 1),
                    pos = ConstPoint(sx, sy),
                    type = type,
                    seed = rand.nextInt(),
                    artPath = poiArt,
                    spawnLocation = ftl2.system.SpawnLocation.NEAR_PLANET,
                    stationType = stationSubtype,
                    derelictType = derelictSubtype,
                    anomalyType = anomalySubtype,
                    isJumpBeacon = false
                )
                pois += poi
                satelliteCounts[parent] = index + 1
            }

            // Place any remaining POIs away from planets (free-floating)
            while (pois.size < totalTarget) {
                var placed = false
                var attempts = 0
                var fx = 0
                var fy = 0
                var chosenOrbit = 0
                while (!placed && attempts < 128) {
                    chosenOrbit = rand.nextInt(1, planetList.size + 4)
                    val radius = chosenOrbit * orbitSpacing + rand.nextInt(-120, 121)
                    val angle = rand.nextDouble(0.0, Math.PI * 2)
                    fx = (radius * kotlin.math.cos(angle)).toInt()
                    fy = (radius * kotlin.math.sin(angle)).toInt()

                    var ok = true
                    for (existing in pois) {
                        val dx = existing.pos.x - fx
                        val dy = existing.pos.y - fy
                        if (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) < MIN_SEPARATION) { ok = false; break }
                    }
                    if (ok) {
                        for (existing in planetList) {
                            val dx = existing.pos.x - fx
                            val dy = existing.pos.y - fy
                            if (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) < MIN_SEPARATION) { ok = false; break }
                        }
                    }
                    if (ok) placed = true
                    attempts++
                }
                if (!placed) {
                    var safe = false
                    var fallbackAttempts = 0
                    while (!safe && fallbackAttempts < 16) {
                        val r = (planetList.size + 2) * orbitSpacing + rand.nextInt(0, 300)
                        val a = rand.nextDouble(0.0, Math.PI * 2)
                        fx = (r * kotlin.math.cos(a)).toInt()
                        fy = (r * kotlin.math.sin(a)).toInt()
                        safe = true
                        for (existing in pois) {
                            val dx = existing.pos.x - fx
                            val dy = existing.pos.y - fy
                            if (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) < MIN_SEPARATION) { safe = false; break }
                        }
                        if (safe) {
                            for (existing in planetList) {
                                val dx = existing.pos.x - fx
                                val dy = existing.pos.y - fy
                                if (kotlin.math.hypot(dx.toDouble(), dy.toDouble()) < MIN_SEPARATION) { safe = false; break }
                            }
                        }
                        fallbackAttempts++
                    }
                }

                // Allow outer-edge jump beacons
                var type: POIType
                var poiArt = STANDARD_POI_ART
                val isOuter = (chosenOrbit >= ORBITS - 1)
                if (isOuter && rand.nextInt(100) < 15) {
                    type = POIType.JUMP_BEACON
                    poiArt = STANDARD_POI_ART
                } else {
                    type = when (rand.nextInt(100)) {
                        in 0..25 -> POIType.ASTEROID_FIELD
                        in 26..50 -> POIType.STATION
                        in 51..75 -> POIType.ANOMALY
                        else -> POIType.DERELICT
                    }
                    poiArt = STANDARD_POI_ART
                }

                var stationSubtype: ftl2.system.StationType? = null
                var derelictSubtype: ftl2.system.DerelictType? = null
                var anomalySubtype: String? = null
                if (type == POIType.STATION) {
                    stationSubtype = ftl2.system.StationType.values()[rand.nextInt(ftl2.system.StationType.values().size)]
                }
                if (type == POIType.DERELICT) {
                    derelictSubtype = ftl2.system.DerelictType.values()[rand.nextInt(ftl2.system.DerelictType.values().size)]
                }

                val poi = POI(
                    id = rand.nextInt(),
                    name = type.defaultName(pois.size + 1),
                    pos = ConstPoint(fx, fy),
                    type = type,
                    seed = rand.nextInt(),
                    artPath = poiArt,
                    spawnLocation = if (type == POIType.JUMP_BEACON) ftl2.system.SpawnLocation.OUTER_EDGE else ftl2.system.SpawnLocation.FREE_SPACE,
                    stationType = stationSubtype,
                    derelictType = derelictSubtype,
                    anomalyType = anomalySubtype,
                    isJumpBeacon = (type == POIType.JUMP_BEACON)
                )
                pois += poi
            }

            return System(name, star, planetList, pois, starType, chosenBg)
        }
    }

    /** Save this system to XML. */
    fun saveToXML(elem: Element, globalRefs: ObjectRefs): ObjectRefs {
        val refs = ObjectRefs(globalRefs)

        SaveUtil.addAttr(elem, "name", name)
        SaveUtil.addAttr(elem, "starType", starType.name)
        if (bgStarArtPath != null) SaveUtil.addAttr(elem, "bgStar", bgStarArtPath)

        // Register centre, planets and POIs so they can be referenced if needed
        refs.register(centre, "poi")
        for (p in planets) refs.register(p, "poi")
        for (p in pois) refs.register(p, "poi")

        // Save centre
        val centreElem = Element("centre")
        centre.saveToXML(centreElem, refs)
        elem.addContent(centreElem)

        // Save planets
        for (p in planets) {
            val planetElem = Element("planet")
            p.saveToXML(planetElem, refs)
            elem.addContent(planetElem)
        }

        // Save POIs
        for (p in pois) {
            val poiElem = Element("poi")
            p.saveToXML(poiElem, refs)
            elem.addContent(poiElem)
        }

        return refs
    }

    /** Load a system from XML. */
    constructor(elem: Element, refs: RefLoader, game: InGameState) {
        name = SaveUtil.getAttr(elem, "name")
        val starTypeStr = elem.getAttributeValue("starType") ?: "G"
        val st = try { StarType.valueOf(starTypeStr) } catch (e: Exception) { StarType.G }
        starType = st

        // Use a temporary RefLoader for POIs
        val poiRefs = RefLoader()

        // Centre
        val centreElem = elem.getChild("centre")
            ?: error("Missing centre element when loading System")
        val centrePoi = POI.loadFromXML(centreElem, poiRefs, game)
        centre = centrePoi

        // Planets
        for (planetElem in elem.getChildren("planet")) {
            val planet = Planet.loadFromXML(planetElem, poiRefs, game)
            planets += planet
        }

        // POIs
        for (poiElem in elem.getChildren("poi")) {
            val poi = POI.loadFromXML(poiElem, poiRefs, game)
            pois += poi
        }

        // Register loaded POIs with refs so external objects may reference them
        for (p in listOf(centre) + planets + pois) {
            SaveUtil.registerObjectId(Element("dummy"), refs, p)
        }
        // Load saved background if present
        val bg = elem.getAttributeValue("bgStar")
        bgStarArtPath = if (bg != null && bg.isNotBlank()) bg else null
    }
}

