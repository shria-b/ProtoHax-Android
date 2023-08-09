package dev.sora.protohax.ui.overlay.hud.elements

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import dev.sora.protohax.MyApplication
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.ui.overlay.RenderLayerView
import dev.sora.protohax.ui.overlay.hud.HudAlignment
import dev.sora.protohax.ui.overlay.hud.HudElement
import dev.sora.protohax.ui.overlay.hud.HudManager
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
	private var fontModeValue by listValue("Font", FontMode.values(), FontMode.DEFAULT)
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
	private var textSizeValue by intValue("TextSize", 15, 10..50).listen {
		paint.textSize = it * MyApplication.density
		it
	}
	private var spacingValue by intValue("Spacing", 3, 0..20)
	private var blurRadiusValue by floatValue("Radius", 1f, 0f..10f)
	private var rainbowDelay by intValue(
		"Delay",
		70,
		10..200
	).visible { colorModeValue == ColorMode.RAINBOW }

	private val paint = TextPaint().also {
		it.color = Color.WHITE
		it.isAntiAlias = true
		it.textSize = 20 * MyApplication.density
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

				paint.color = colorModeValue.getColor(0, 1, colorRedValue, colorGreenValue, colorBlueValue, rainbowDelay)
				canvas.drawText(alertNoModules, 0f, -paint.fontMetrics.ascent, paint)
			}
			return
		}
		when(fontModeValue){
			FontMode.DEFAULT ->{
				paint.typeface = Typeface.DEFAULT
			}
			FontMode.CUSTOM ->{
				val fontFile = File(context.getExternalFilesDir("fonts"), "Custom_Font.ttf")
				val customTypeface = Typeface.createFromFile(fontFile)
				if(fontFile.exists()) {
					paint.typeface = customTypeface
				}else{
					paint.typeface = Typeface.DEFAULT
				}
			}
		}

		var y = 0f
		val lineSpacing = (spacingValue * MyApplication.density)
		val maxWidth = modules.maxOf { paint.measureText(it.name) }

		modules.forEachIndexed { i, module ->
			val textWidth = paint.measureText(module.name)
			val blurMaskFilter = BlurMaskFilter(blurRadiusValue, BlurMaskFilter.Blur.NORMAL)
			paint.maskFilter = blurMaskFilter
			paint.color = colorModeValue.getColor(
				if (colorReversedSortValue) modules.size - i else i,
				modules.size,
				colorRedValue,
				colorGreenValue,
				colorBlueValue,
				rainbowDelay
			)
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
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int, delay:Int): Int {
				return Color.rgb(r, g, b)
			}
		},
		HUE("Hue") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int, delay:Int): Int {
				return dev.sora.protohax.util.Color.HSBtoRGB(index.toFloat() / size, 0.5f, 1f)
			}
		},
		RAINBOW("RainBow") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int, delay: Int): Int {
				return ColorUtils.astolfoRainbow(delay, 5, index)
			}
		},
		SATURATION_SHIFT_ASCENDING("SaturationShift") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int, delay:Int): Int {
				val hsv = floatArrayOf(0f, 0f, 0f)
				Color.colorToHSV(Color.rgb(r, g, b), hsv)
				hsv[2] = index.toFloat() / size
				return Color.HSVToColor(hsv)
			}
		};

		abstract fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int, delay:Int): Int
	}

	enum class FontMode(override val choiceName: String) : NamedChoice{
		DEFAULT("Default"),
		CUSTOM("Custom")
	}
}
