package ftl2.sys

import ftl2.rendering.Cursor

interface GameContainer {
    val input: Input
    val width: Int
    val height: Int

    fun exit()

    /**
     * Switch to the given cursor image, or the default image if [cursor]
     * is null.
     */
    fun setCursor(cursor: Cursor?)
}
