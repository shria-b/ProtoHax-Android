package dev.sora.protohax.ui.overlay.hud

import android.content.Context
import android.graphics.Typeface
import dev.sora.relay.cheat.value.NamedChoice
import java.io.File

enum class HudFont(override val choiceName: String) : NamedChoice {
	DEFAULT("Default") {
		override fun getFont(context: Context): Typeface {
			return Typeface.DEFAULT
		}
	},
	BOLD("Bold") {
		override fun getFont(context: Context): Typeface {
			return Typeface.DEFAULT_BOLD
		}
	},
	SERIF("Serif") {
		override fun getFont(context: Context): Typeface {
			return Typeface.SERIF
		}
	},
	MONOSPACE("MonoSpace") {
		override fun getFont(context: Context): Typeface {
			return Typeface.MONOSPACE
		}
	},
	CUSTOM("Custom") {
		override fun getFont(context: Context): Typeface {
			val fontFile = File(context.getExternalFilesDir("fonts"), "Custom_Font.ttf")
			val customTypeface = Typeface.createFromFile(fontFile)
			if (fontFile.exists()) {
				return customTypeface
			} else {
				return Typeface.DEFAULT
			}
		}
	};

	abstract fun getFont(context: Context): Typeface
}
