package ftl2.sys

import ftl2.rendering.Cursor

interface GameContainer {
    val input: Input
    val width: Int
    val height: Int
    /** The actual framebuffer (pixel) width of the GLFW window. */
    val fbWidth: Int
    /** The actual framebuffer (pixel) height of the GLFW window. */
    val fbHeight: Int

    fun exit()

    /**
     * Switch to the given cursor image, or the default image if [cursor]
     * is null.
     */
    fun setCursor(cursor: Cursor?)
}
