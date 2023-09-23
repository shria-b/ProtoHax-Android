package dev.sora.protohax.ui.overlay.hud.elements

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import dev.sora.protohax.MyApplication
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.ui.overlay.RenderLayerView
import dev.sora.protohax.ui.overlay.hud.*
import dev.sora.protohax.util.ColorUtils
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.EventModuleToggle
import dev.sora.relay.cheat.value.NamedChoice
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean


class ModuleIndicatorElement : HudElement(HudManager.MODULE_INDICATOR_ELEMENT_IDENTIFIER) {
	private var sortingModeValue by listValue("SortingMode", SortingMode.values(), SortingMode.LENGTH_DESCENDING)
	private var textRTLValue by boolValue("TextRTL", true)
	private var colorModeValue by listValue("ColorMode", ColorMode.values(), ColorMode.HUE)
	private var colorReversedSortValue by boolValue("ColorReversedSort", false)
	private var colorRedValue by intValue(
		"ColorRed",
		255,
		0..255
	).visible { colorModeValue != ColorMode.HUE && colorModeValue != ColorMode.RAINBOW }
	private var colorGreenValue by intValue(
		"ColorGreen",
		255,
		0..255
	).visible { colorModeValue != ColorMode.HUE && colorModeValue != ColorMode.RAINBOW }
	private var colorBlueValue by intValue(
		"ColorBlue",
		255,
		0..255
	).visible { colorModeValue != ColorMode.HUE && colorModeValue != ColorMode.RAINBOW }
	private var textSizeValue by intValue("TextSize", 15, 1..50).listen {
		paint.textSize = it * MyApplication.density
		it
	}
	private var spacingValue by intValue("Spacing", 3, 0..30)
	private var stripeValue by boolValue("Stripe", false)
	private var stripeBlurValue by boolValue("Stripe Blur", true).visible {
		stripeValue
	}
	private var stripeBlurModeValue by listValue("Blur Mode", HudBlurMode.values(), HudBlurMode.NORMAL).visible { stripeBlurValue && stripeValue }
	private var stripeBlurRadiusValue by floatValue("Blur Radius", 1f, 0f..80f).visible { stripeBlurValue && stripeValue }
	private var backgroundValue by boolValue("Background", false)
	private var backgroundBlurValue by boolValue("Blur", true).visible {
		backgroundValue
	}
	private var backgroundBlurModeValue by listValue("Blur Mode", HudBlurMode.values(), HudBlurMode.NORMAL).visible { backgroundBlurValue && backgroundValue }
	private var backgroundBlurRadiusValue by floatValue("Blur Radius", 1f, 0f..80f).visible { backgroundBlurValue && backgroundValue }
	private var backgroundColorModeValue by listValue(
		"Background Mode",
		BackgroundColorMode.values(),
		BackgroundColorMode.CUSTOM
	).visible { backgroundValue }
	private var backgroundColorRedValue by intValue(
		"ColorRed",
		255,
		0..255
	).visible { backgroundColorModeValue != BackgroundColorMode.HUE && backgroundColorModeValue != BackgroundColorMode.RAINBOW && backgroundColorModeValue != BackgroundColorMode.BLACK && backgroundValue }
	private var backgroundColorGreenValue by intValue(
		"ColorGreen",
		255,
		0..255
	).visible { backgroundColorModeValue != BackgroundColorMode.HUE && backgroundColorModeValue != BackgroundColorMode.RAINBOW && backgroundColorModeValue != BackgroundColorMode.BLACK && backgroundValue }
	private var backgroundColorBlueValue by intValue(
		"ColorBlue",
		255,
		0..255
	).visible { backgroundColorModeValue != BackgroundColorMode.HUE && backgroundColorModeValue != BackgroundColorMode.RAINBOW && backgroundColorModeValue != BackgroundColorMode.BLACK && backgroundValue }
	private var backgroundAlphaValue by intValue(
		"Alpha",
		120,
		0..255
	).visible { backgroundColorModeValue != BackgroundColorMode.HUE && backgroundColorModeValue != BackgroundColorMode.RAINBOW && backgroundColorModeValue != BackgroundColorMode.BLACK && backgroundValue }

	private val paint = TextPaint().also {
		it.color = Color.WHITE
		it.isAntiAlias = true
		it.textSize = 20 * MyApplication.density
	}

	private val backgroundPaint = Paint().also {
		it.color = Color.WHITE
		it.isAntiAlias = true
	}

	private val stripePaint = Paint().also {
		it.color = Color.WHITE
		it.isAntiAlias = true
	}

	override var height = 10f
		private set
	override var width = 10f
		private set

	init {
		alignmentValue = HudAlignment.RIGHT_TOP
		posX = 0
		posY = 0
	}

	private fun paintSettings(){
		if (blurValue) {
			paint.maskFilter = BlurMaskFilter(blurRadiusValue, blurModeValue.getBlurMode())
		} else {
			paint.maskFilter = null
		}
		if(backgroundBlurValue){
			backgroundPaint.maskFilter = BlurMaskFilter(backgroundBlurRadiusValue, backgroundBlurModeValue.getBlurMode())
		} else {
			backgroundPaint.maskFilter = null
		}
		if(stripeBlurValue){
			stripePaint.maskFilter = BlurMaskFilter(stripeBlurRadiusValue, stripeBlurModeValue.getBlurMode())
		} else {
			stripePaint.maskFilter = null
		}
		paint.setShadowLayer(shadowRadiusValue, 0f, 0f, Color.argb(shadowAlphaValue, 0, 0, 0))
	}

	override fun onRender(canvas: Canvas, editMode: Boolean, needRefresh: AtomicBoolean, context: Context) {
		val modules = sortingModeValue.getModules(paint)
		val lineHeight = paint.fontMetrics.let { it.descent - it.ascent }
		if (modules.isEmpty()) {
			if (editMode) {
				if (height != lineHeight) {
					needRefresh.set(true)
				}
				height = lineHeight
				val alertNoModules = "No modules has toggled on currently"
				width = paint.measureText(alertNoModules)

				paint.color =
					colorModeValue.getColor(0, 1, colorRedValue, colorGreenValue, colorBlueValue)
				backgroundPaint.color =
					colorModeValue.getColor(0, 1, colorRedValue, colorGreenValue, colorBlueValue)
				canvas.drawText(alertNoModules, 0f, -paint.fontMetrics.ascent, paint)

			}
			return
		}
		var y = 0f
		val lineSpacing = (spacingValue * MyApplication.density)
		val maxWidth = modules.maxOf { paint.measureText(it.name) }

		modules.forEachIndexed { i, module ->
			val textWidth = paint.measureText(module.name)
			val padding = 8 * MyApplication.density
			val bgLeft = if (textRTLValue) maxWidth - textWidth - padding else -padding
			val bgTop = y - padding
			val bgRight = bgLeft + textWidth + 2 * padding
			val bgBottom = bgTop + lineHeight + 2 * padding
			val bgRect = RectF(bgLeft, bgTop, bgRight, bgBottom)
			paintSettings()
			paint.color = colorModeValue.getColor(
				if (colorReversedSortValue) modules.size - i else i,
				modules.size,
				colorRedValue,
				colorGreenValue,
				colorBlueValue
			)
			paint.typeface = fontValue.getFont(context)
			if (backgroundValue) {
				backgroundPaint.color = backgroundColorModeValue.getColor(
					if (colorReversedSortValue) modules.size - i else i,
					modules.size,
					backgroundColorRedValue,
					backgroundColorGreenValue,
					backgroundColorBlueValue,
					backgroundAlphaValue
				)
					canvas.drawRoundRect(
						bgRect,
						4 * MyApplication.density,
						4 * MyApplication.density,
						backgroundPaint
					)
			}
			if (stripeValue) {
				stripePaint.color = ColorUtils.getRainbow()
				if (!textRTLValue) {
					val stripeRight = bgLeft * 2 - 5
					val stripeRect = RectF(bgLeft, bgTop, stripeRight, bgBottom)
						canvas.drawRoundRect(
							stripeRect,
							4 * MyApplication.density,
							4 * MyApplication.density,
							stripePaint
						)
				} else {
					val stripeLeft = maxWidth + 5
					val stripeRect = RectF(stripeLeft, bgTop, bgRight, bgBottom)
						canvas.drawRoundRect(
							stripeRect,
							4 * MyApplication.density,
							4 * MyApplication.density,
							stripePaint
						)
				}
			}
			canvas.drawText(
				module.name,
				if (textRTLValue) maxWidth - textWidth else 0f,
				-paint.fontMetrics.ascent + y,
				paint
			)

			y += lineHeight + lineSpacing
		}
		y -= lineHeight
		if (height != y) {
			needRefresh.set(true)
			height = y
		}
		if (width != maxWidth) {
			needRefresh.set(true)
			width = maxWidth
		}
		if(colorModeValue == ColorMode.RAINBOW || backgroundColorModeValue == BackgroundColorMode.RAINBOW || stripeValue){
			needRefresh.set(true)
		}
	}

	private val onModuleToggle = handle<EventModuleToggle> {
		session.eventManager.emit(RenderLayerView.EventRefreshRender(session))
	}

	enum class SortingMode(override val choiceName: String) : NamedChoice {
		NAME_ASCENDING("NameAscending") {
			override fun getModules(paint: TextPaint): List<CheatModule> {
				return MinecraftRelay.moduleManager.modules
					.filter { it.state }
					.sortedBy { it.name }
			}
		},
		NAME_DESCENDING("NameDescending") {
			override fun getModules(paint: TextPaint): List<CheatModule> {
				return MinecraftRelay.moduleManager.modules
					.filter { it.state }
					.sortedBy { it.name }
					.reversed()
			}
		},
		LENGTH_ASCENDING("LengthAscending") {
			override fun getModules(paint: TextPaint): List<CheatModule> {
				return MinecraftRelay.moduleManager.modules
					.filter { it.state }
					.sortedBy { paint.measureText(it.name) }
			}
		},
		LENGTH_DESCENDING("LengthDescending") {
			override fun getModules(paint: TextPaint): List<CheatModule> {
				return MinecraftRelay.moduleManager.modules
					.filter { it.state }
					.sortedBy { paint.measureText(it.name) }
					.reversed()
			}
		};

		abstract fun getModules(paint: TextPaint): List<CheatModule>
	}


	enum class ColorMode(override val choiceName: String) : NamedChoice {
		CUSTOM("Custom") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int): Int {
				return Color.rgb(r, g, b)
			}
		},
		HUE("Hue") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int): Int {
				return dev.sora.protohax.util.Color.HSBtoRGB(index.toFloat() / size, 0.5f, 1f)
			}
		},
		RAINBOW("RainBow") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int): Int {
				val color = ColorUtils.getRainbow()
				return Color.rgb(color.red, color.green, color.blue)
			}
		},
		SATURATION_SHIFT_ASCENDING("SaturationShift") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int): Int {
				val hsv = floatArrayOf(0f, 0f, 0f)
				Color.colorToHSV(Color.rgb(r, g, b), hsv)
				hsv[2] = index.toFloat() / size
				return Color.HSVToColor(hsv)
			}
		};

		abstract fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int): Int
	}

	enum class BackgroundColorMode(override val choiceName: String) : NamedChoice {
		CUSTOM("Custom") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int, alpha: Int): Int {
				return Color.argb(alpha, r, g, b)
			}
		},
		HUE("Hue") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int, alpha: Int): Int {
				return dev.sora.protohax.util.Color.HSBtoRGB(index.toFloat() / size, 0.5f, 1f)
			}
		},
		RAINBOW("RainBow") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int, alpha: Int): Int {
				return ColorUtils.getRainbow()
			}
		},
		BLACK("Black") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int, alpha: Int): Int {
				return Color.BLACK
			}
		},
		SATURATION_SHIFT_ASCENDING("SaturationShift") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int, alpha: Int): Int {
				val hsv = floatArrayOf(0f, 0f, 0f)
				Color.colorToHSV(Color.rgb(r, g, b), hsv)
				hsv[2] = index.toFloat() / size
				return Color.HSVToColor(hsv)
			}
		};

		abstract fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int, alpha: Int): Int
	}
}
