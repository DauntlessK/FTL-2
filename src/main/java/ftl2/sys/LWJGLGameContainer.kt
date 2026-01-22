package ftl2.sys

import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.KHRDebug
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.NULL
import org.newdawn.slick.InputListener
import org.newdawn.slick.KeyListener
import org.newdawn.slick.MouseListener
import org.newdawn.slick.opengl.ImageDataFactory
import ftl2.math.Point
import ftl2.rendering.Cursor
import ftl2.rendering.Graphics
import ftl2.rendering.ShaderProgramme
import java.io.BufferedInputStream
import kotlin.math.*


class LWJGLGameContainer(private val game: Game) : GameContainer {
    private lateinit var lwjglInput: LWJGLInput
    override val input: Input get() = lwjglInput

    override var width: Int = 400
        private set
    override var height: Int = 400
        private set
    // Actual framebuffer (pixel) size; may differ from logical width/height on some platforms
    private var framebufferWidth: Int = width
    private var framebufferHeight: Int = height
    override val fbWidth: Int get() = framebufferWidth
    override val fbHeight: Int get() = framebufferHeight
    

    private lateinit var g: Graphics

    /**
     * The GLFW window handle.
     */
    private var window: Long = 0
    // Track logical window size (in window coordinates) for callback enforcement
    private var windowWidth: Int = width
    private var windowHeight: Int = height
    private var resizingFromCallback: Boolean = false

    override fun exit() {
        glfwSetWindowShouldClose(window, true)
    }

    override fun setCursor(cursor: Cursor?) {
        if (cursor != null) {
            cursor.setActive(window)
        } else {
            glfwSetCursor(window, NULL)
        }
    }

    fun start() {
        println("Using LWJGL version ${Version.getVersion()}")
        init()

        try {
            loop()
        } finally {
            // Call shutdown if there was an exception, to avoid a VM crash
            // from not freeing native resources.
            game.shutdown()
        }

        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        // Terminate GLFW and free the error callback
        glfwTerminate()
        glfwSetErrorCallback(null)!!.free()
    }

    private fun init() {
        // Set up an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(glfwInit()) { "Unable to initialize GLFW" }

        // Configure GLFW
        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable

        // Set the OpenGL stuff
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)

        // TODO disable this for end users
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE)

        // Create the window
        window = glfwCreateWindow(width, height, game.title, NULL, NULL)
        if (window == NULL)
            throw RuntimeException("Failed to create the GLFW window")

        stackPush().use { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*

                // Get the window size passed to glfwCreateWindow
                glfwGetWindowSize(window, pWidth, pHeight)
                windowWidth = pWidth[0]
                windowHeight = pHeight[0]

                // Use the framebuffer size for pixel measurements (HiDPI displays may differ)
                val fbw = stack.mallocInt(1)
                val fbh = stack.mallocInt(1)
                glfwGetFramebufferSize(window, fbw, fbh)
                framebufferWidth = fbw[0]
                framebufferHeight = fbh[0]

            // Get the resolution of the primary monitor
            val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())!!

            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth[0]) / 2,
                (vidmode.height() - pHeight[0]) / 2
            )
        }

        // Set the window's icon
        setupWindowIcon()

        // Make the OpenGL context current
        glfwMakeContextCurrent(window)
        // Enable v-sync
        glfwSwapInterval(1)

        // Make the window visible
        glfwShowWindow(window)

        // Enforce the aspect ratio during interactive resizes by correcting
        // the other dimension in the window-size callback. This works even on
        // platforms where `glfwSetWindowAspectRatio` may not be honoured.
        glfwSetWindowSizeCallback(window) { _, newW, newH ->
            if (resizingFromCallback) return@glfwSetWindowSizeCallback
            resizingFromCallback = true
            try {
                val aspect = width.toDouble() / height.toDouble()

                // Decide which dimension the user changed (width or height)
                val desiredW: Int
                val desiredH: Int
                if (newW != windowWidth && newH != windowHeight) {
                    // Both changed: prefer width as driver
                    desiredW = newW
                    desiredH = kotlin.math.max(1, (desiredW / aspect).roundToInt())
                } else if (newW != windowWidth) {
                    desiredW = newW
                    desiredH = kotlin.math.max(1, (desiredW / aspect).roundToInt())
                } else {
                    desiredH = newH
                    desiredW = kotlin.math.max(1, (desiredH * aspect).roundToInt())
                }

                if (desiredW != newW || desiredH != newH) {
                    glfwSetWindowSize(window, desiredW, desiredH)
                }

                windowWidth = desiredW
                windowHeight = desiredH
            } finally {
                resizingFromCallback = false
            }
        }

        lwjglInput = LWJGLInput(window, this)
        g = Graphics()
        g.markCurrentImageTransformSource()

        if (game is KeyListener)
            lwjglInput.keyListeners.add(game)
        if (game is MouseListener)
            lwjglInput.mouseListeners.add(game)

        // Set up OpenGL
        initOpenGL()
        // Enforce the native aspect ratio so users can only resize while preserving it.
        // Compute reduced aspect ratio values (numerator/denominator) and set GLFW constraint.
        run {
            fun gcd(a: Int, b: Int): Int {
                var x = a
                var y = b
                while (y != 0) {
                    val t = x % y
                    x = y
                    y = t
                }
                return x
            }

            val g = gcd(width, height)
            val aw = width / g
            val ah = height / g
            glfwSetWindowAspectRatio(window, aw, ah)
        }
    }

    private fun setupWindowIcon() {
        val path = "assets/img/xftl_icon.png"

        val imageData = ImageDataFactory.getImageDataFor(path)
        javaClass.classLoader.getResourceAsStream(path).use { stream ->
            imageData.loadImage(BufferedInputStream(stream), false, null)
        }

        stackPush().use { stack ->
            val imageArray = GLFWImage.Buffer(stack.malloc(GLFWImage.SIZEOF))
            val image = imageArray[0]

            image.set(imageData.width, imageData.height, imageData.imageBufferData)

            glfwSetWindowIcon(window, imageArray)
        }
    }

    private fun initOpenGL() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()

        // From Slick's ImmediateModeOGLRenderer
        glDisable(GL_DEPTH_TEST)

        glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        glClearDepth(1.0)

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glViewport(0, 0, width, height)

        // Prefer the actual framebuffer size (pixel dimensions) if available
        glViewport(0, 0, framebufferWidth, framebufferHeight)
        // Set up debug output
        glEnable(KHRDebug.GL_DEBUG_OUTPUT)

        // Don't fill the console with errors if there's an issue each frame
        val maxErrorCount = 50
        var errorCount = 0

        // OSX lacks debug capabilities, we'll get an exception if we try to set them
        if (!GL.getCapabilities().GL_KHR_debug) {
            return
        }

        // Disable the flood of notification messages
        KHRDebug.glDebugMessageControl(
            GL_DONT_CARE,
            GL_DONT_CARE,
            KHRDebug.GL_DEBUG_SEVERITY_NOTIFICATION,
            null as IntArray?,
            false
        )

        KHRDebug.glDebugMessageCallback({ source, type, id, severity, length, message, _ ->
            errorCount++
            if (errorCount == maxErrorCount) {
                println("Got too many OpenGL debug messages, stopping output")
            }
            if (errorCount >= maxErrorCount) {
                return@glDebugMessageCallback
            }

            val messageString = MemoryUtil.memUTF8(message, length)

            // Filtering for end users?
            println("Got GL debug message (id=$id src=$source type=$type sev=$severity): $messageString")
        }, 0)
    }

    private fun loop() {
        game.init(this)

        var lastNanos = System.nanoTime()

        // Run the rendering loop until the user has attempted to close
        // the window, or exit is called.
        while (!glfwWindowShouldClose(window)) {
            // Read any changes to the input system
            lwjglInput.update()

            // Find the delta-time, in seconds
            val thisUpdateTime = System.nanoTime()
            val deltaNS = thisUpdateTime - lastNanos
            val deltaSec = deltaNS / 1_000_000_000f
            lastNanos = thisUpdateTime

            val isFocused = glfwGetWindowAttrib(window, GLFW_FOCUSED) == GLFW_TRUE

            // Update the game state
            // If there's an exception, catch it instead of crashing the whole game.
            // If we're in an invalid state this won't help, but on-click/on-keypress
            // stuff should generally be OK.
            try {
                game.update(this, deltaSec)
            } catch (ex: Exception) {
                println("Exception during game update")
                ex.printStackTrace()
            }

            // Check for framebuffer size changes and update viewport
            stackPush().use { stack ->
                val fbw = stack.mallocInt(1)
                val fbh = stack.mallocInt(1)
                glfwGetFramebufferSize(window, fbw, fbh)
                val newW = fbw[0]
                val newH = fbh[0]
                if (newW != framebufferWidth || newH != framebufferHeight) {
                    framebufferWidth = newW
                    framebufferHeight = newH
                    glViewport(0, 0, framebufferWidth, framebufferHeight)
                }
            }

            // Only render if the user can see the result
            if (!isFocused) {
                // Internally run at ~20 updates/sec when unfocused to save CPU.
                glfwWaitEventsTimeout(1.0 / 20)
                continue
            }

            // Ensure shaders receive the actual framebuffer (pixel) size
            ShaderProgramme.SHADER_SCREEN_SIZE.set(framebufferWidth, framebufferHeight)

            // Render out the game, applying a scaling transform so the game's logical
            // `width`/`height` coordinate space maps to the actual framebuffer while
            // preserving aspect ratio.
            g.loadIdentityMatrix()
            // Compute integer-preserving scale to map logical -> pixel coordinates
            val scaleX = framebufferWidth.toFloat() / width.toFloat()
            val scaleY = framebufferHeight.toFloat() / height.toFloat()
            val scale = kotlin.math.min(scaleX, scaleY)
            val outW = (width * scale).toInt()
            val outH = (height * scale).toInt()
            val offsetX = ((framebufferWidth - outW) / 2).toFloat()
            val offsetY = ((framebufferHeight - outH) / 2).toFloat()

            g.translate(offsetX, offsetY)
            g.scale(scale, scale)

            game.render(this, g)

            // Reset transform for safety; shaders/GL state expect pixel-space elsewhere
            g.loadIdentityMatrix()

            // Swap the colour buffers, presenting the newly-drawn one
            // to the screen.
            glfwSwapBuffers(window)

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents()
        }
    }

    fun setDisplayMode(width: Int, height: Int, fullScreen: Boolean) {
        this.width = width
        this.height = height
        if (window != 0L) {
            glfwSetWindowSize(window, width, height)
            // After changing the window size, query the resulting framebuffer
            // size (may differ on HiDPI displays) and update viewport.
            stackPush().use { stack ->
                val fbw = stack.mallocInt(1)
                val fbh = stack.mallocInt(1)
                glfwGetFramebufferSize(window, fbw, fbh)
                framebufferWidth = fbw[0]
                framebufferHeight = fbh[0]
                glViewport(0, 0, framebufferWidth, framebufferHeight)
            }
        }
    }
}

private class LWJGLInput(val window: Long, val container: LWJGLGameContainer) : Input {
    override var mouseX: Int = 0
        private set
    override var mouseY: Int = 0
        private set

    private val mouseClickPos = Point(0, 0)

    private var scrollLeftover: Double = 0.0

    // Note that GLFW_KEY_LAST is the maximum possible ID, not one past it.
    private val pendingKeyPresses = BooleanArray(GLFW_KEY_LAST + 1)

    val keyListeners = ArrayList<KeyListener>()
    val mouseListeners = ArrayList<MouseListener>()

    init {
        // Set up a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(
            window
        ) { _: Long, key: Int, scancode: Int, action: Int, mods: Int ->
            keyCallback(key, scancode, action, mods)
        }
        glfwSetCharCallback(window) { _, codepoint -> charCallback(codepoint.toChar()) }
        glfwSetMouseButtonCallback(window) { _, button, action, mods -> mouseButtonCallback(button, action, mods) }
        glfwSetScrollCallback(window) { _, xOffset, yOffset -> mouseWheelCallback(xOffset, yOffset) }
    }

    fun update() {
        // Block updates while unfocused, so the player doesn't hear
        // the mouse-over sounds for buttons through another window.
        // This happens on Linux/X11 without this check.
        if (glfwGetWindowAttrib(window, GLFW_FOCUSED) == GLFW_FALSE)
            return

        val lastX = mouseX
        val lastY = mouseY

        stackPush().use { stack ->
            val x = stack.mallocDouble(1)
            val y = stack.mallocDouble(1)
            glfwGetCursorPos(window, x, y)
            // Convert from framebuffer pixel coordinates into the game's logical
            // coordinate space (the `width`/`height` set via setDisplayMode). We
            // also account for centered letterboxing introduced when aspect ratio
            // preserves a different scale.
            val pixelX = x[0].toFloat()
            val pixelY = y[0].toFloat()

            val scaleX = container.fbWidth.toFloat() / container.width.toFloat()
            val scaleY = container.fbHeight.toFloat() / container.height.toFloat()
            val scale = min(scaleX, scaleY)
            val outW = (container.width * scale)
            val outH = (container.height * scale)
            val offsetX = (container.fbWidth - outW) / 2f
            val offsetY = (container.fbHeight - outH) / 2f

            val logicalXf = (pixelX - offsetX) / scale
            val logicalYf = (pixelY - offsetY) / scale

            // Clamp to logical window bounds so UI logic doesn't see negative values
            var lx = logicalXf.toInt()
            var ly = logicalYf.toInt()
            if (lx < 0) lx = 0
            if (ly < 0) ly = 0
            if (lx > container.width) lx = container.width
            if (ly > container.height) ly = container.height

            mouseX = lx
            mouseY = ly
        }

        if (lastX == mouseX && lastY == mouseY)
            return

        val dragging = isMouseButtonDown(Input.MOUSE_LEFT_BUTTON) || isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON)

        iterateAllowingMutation(mouseListeners) { listener ->
            when (dragging) {
                true -> listener.mouseDragged(lastX, lastY, mouseX, mouseY)
                false -> listener.mouseMoved(lastX, lastY, mouseX, mouseY)
            }
        }
    }

    private fun keyCallback(rawKey: Int, scancode: Int, action: Int, mods: Int) {
        // Convert between the different codes used by the "|" UK/ISO key on different platforms
        val key = remapKey(rawKey)

        // Ignore unsupported keys
        if (key == GLFW_KEY_UNKNOWN) {
            return
        }

        if (action == GLFW_PRESS) {
            pendingKeyPresses[key] = true
        }

        // Keys don't map 1-1 with characters (most characters on most European
        // keyboards do, but you can't rely on that!) so send 0 for the character
        // now, and then send the character by itself as a second press.
        iterateAllowingMutation(keyListeners) { listener ->
            when (action) {
                GLFW_PRESS -> listener.keyPressed(key, 0.toChar())
                GLFW_RELEASE -> listener.keyReleased(key, 0.toChar())
            }
        }
    }

    private fun charCallback(codepoint: Char) {
        iterateAllowingMutation(keyListeners) { listener ->
            listener.keyPressed(-1, codepoint)
        }
    }

    private fun mouseButtonCallback(button: Int, action: Int, mods: Int) {
        iterateAllowingMutation(mouseListeners) { listener ->
            when (action) {
                GLFW_PRESS -> listener.mousePressed(button, mouseX, mouseY)
                GLFW_RELEASE -> listener.mouseReleased(button, mouseX, mouseY)
            }
        }

        if (action == GLFW_PRESS) {
            mouseClickPos.set(mouseX, mouseY)
        }

        // Check if this is a 'click' event, with little to no dragging.
        // We don't support double-clicking, but that's not used anywhere.
        if (action == GLFW_RELEASE) {
            val dist = mouseClickPos.distToSq(mouseX, mouseY)
            if (dist <= CLICK_DISTANCE * CLICK_DISTANCE) {
                iterateAllowingMutation(mouseListeners) { listener ->
                    listener.mouseClicked(button, mouseX, mouseY, 1)
                }
            }
        }
    }

    private fun mouseWheelCallback(xOffset: Double, yOffset: Double) {
        val input = yOffset + scrollLeftover
        val change = (input * SCROLL_SCALE).toInt()

        // Store any integer round-off away, to be added onto the next scroll.
        scrollLeftover = input - change / SCROLL_SCALE

        iterateAllowingMutation(mouseListeners) { listener ->
            listener.mouseWheelMoved(change)
        }
    }

    /**
     * Iterate through a list, while accepting that it might be mutated while doing so.
     *
     * If this happens an input might be delivered twice or missed, but in
     * the case this is dealing with - clicking a button or pressing a key
     * that causes the game's state to switch - it's not a problem.
     */
    private inline fun <T> iterateAllowingMutation(list: List<T>, callback: (T) -> Unit) {
        var i = 0
        while (i < list.size) {
            callback(list[i])
            i++
        }
    }

    override fun isMouseButtonDown(button: Int): Boolean {
        return glfwGetMouseButton(window, button) == GLFW_PRESS
    }

    override fun isKeyPressed(key: Int): Boolean {
        val oldValue = pendingKeyPresses[key]
        pendingKeyPresses[key] = false
        return oldValue
    }

    override fun isKeyDown(key: Int): Boolean {
        return glfwGetKey(window, remapKey(key)) == GLFW_PRESS
    }

    override fun addListener(listener: InputListener) {
        keyListeners.add(listener)
        mouseListeners.add(listener)
    }

    override fun removeAllListeners() {
        keyListeners.clear()
        mouseListeners.clear()
    }

    override fun clearInputPressedRecord() {
        for (i in pendingKeyPresses.indices) {
            pendingKeyPresses[i] = false
        }
    }

    private fun remapKey(original: Int): Int {
        // See the documentation for KEY_BAR.
        if (original == GLFW_KEY_WORLD_2) {
            return Input.KEY_BAR
        }

        return original
    }

    companion object {
        // The maximum movement in pixels that doesn't count as dragging
        private const val CLICK_DISTANCE = 4

        // How much one movement of the scroll wheel equates to in Slick's units
        private const val SCROLL_SCALE = 120
    }
}
