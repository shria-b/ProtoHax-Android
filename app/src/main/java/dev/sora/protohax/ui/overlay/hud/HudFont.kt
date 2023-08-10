package dev.sora.protohax.ui.overlay.hud

import dev.sora.relay.cheat.value.NamedChoice

enum class HudFont(override val choiceName: String) : NamedChoice {
	DEFAULT("Default"),
	BOLD("Bold"),
	CUSTOM("Custom")
}
