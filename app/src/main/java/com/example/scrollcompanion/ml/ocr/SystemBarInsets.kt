package com.example.scrollcompanion.ml.ocr

import android.content.Context

/** Heights, in pixels, of the status bar and navigation bar that appear in a full-screen capture. */
data class SystemBarInsets(val top: Int, val bottom: Int) {

    companion object {
        val NONE = SystemBarInsets(top = 0, bottom = 0)

        fun from(context: Context): SystemBarInsets {
            val resources = context.resources
            fun dimen(name: String): Int {
                val id = resources.getIdentifier(name, "dimen", "android")
                return if (id > 0) resources.getDimensionPixelSize(id) else 0
            }

            // Mode 2 is gesture navigation, which has no bar worth cropping.
            val modeId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
            val threeButtonNav = modeId > 0 && resources.getInteger(modeId) != 2
            return SystemBarInsets(
                top = dimen("status_bar_height"),
                bottom = if (threeButtonNav) dimen("navigation_bar_height") else 0,
            )
        }
    }
}
