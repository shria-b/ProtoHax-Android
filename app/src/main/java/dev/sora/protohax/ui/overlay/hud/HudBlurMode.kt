package dev.sora.protohax.ui.overlay.hud

import android.graphics.BlurMaskFilter
import dev.sora.relay.cheat.value.NamedChoice

enum class HudBlurMode(override val choiceName: String) : NamedChoice {
	INNER("Inner") {
		override fun getBlurMode(): BlurMaskFilter.Blur {
			return BlurMaskFilter.Blur.INNER
		}
	},
	NORMAL("Normal") {
		override fun getBlurMode(): BlurMaskFilter.Blur {
			return BlurMaskFilter.Blur.NORMAL
		}
	},
	OUTER("Outer") {
		override fun getBlurMode(): BlurMaskFilter.Blur {
			return BlurMaskFilter.Blur.OUTER
		}
	},
	SOLID("Solid") {
		override fun getBlurMode(): BlurMaskFilter.Blur {
			return BlurMaskFilter.Blur.SOLID
		}
	};
	abstract fun getBlurMode(): BlurMaskFilter.Blur
}
