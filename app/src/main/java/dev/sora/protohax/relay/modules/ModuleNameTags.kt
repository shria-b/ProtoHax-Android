package dev.sora.protohax.relay.modules

import android.graphics.Canvas
import android.graphics.Point
import dev.sora.protohax.relay.service.AppService
import dev.sora.protohax.ui.overlay.RenderLayerView
import dev.sora.protohax.ui.render.EntityNameTag
import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.entity.EntityPlayer
import org.cloudburstmc.math.matrix.Matrix4f
import org.cloudburstmc.math.vector.Vector2d
import kotlin.math.cos
import kotlin.math.sin


class ModuleNameTags : CheatModule("NameTags" , CheatCategory.VISUAL) {

    private val fovValue by intValue("Fov", 110, 40..110)
    private val botsValue by boolValue("Bots", false)
    val leftStatusBarValue by boolValue("LeftStatusBar", true)
    private val originalSizeValue by boolValue("OriginalSize", true)
    private val avoidScreenValue by boolValue("AvoidScreen", true)

    override fun onEnable() {
        session.eventManager.emit(RenderLayerView.EventRefreshRender(session))
        displayList.clear()
    }
    var displayList = HashMap<EntityPlayer, EntityNameTag>()
	private val handleRender = handle<RenderLayerView.EventRender> {
        this.needRefresh = true
        if (avoidScreenValue && this.session.player.openContainer != null) return@handle
        val map = this.session.level.entityMap.values.filterIsInstance<EntityPlayer>()
        if (map.isEmpty()) return@handle
        val player = this.session.player
        val canvas = this.canvas
        val realSize = Point()
        (this.context as AppService).windowManager.defaultDisplay.getRealSize(realSize)
        val screenWidth = if(originalSizeValue) realSize.x else canvas.width
        val screenHeight = if(originalSizeValue) realSize.y else canvas.height

        val viewProjMatrix =  Matrix4f.createPerspective(fovValue.toFloat()+10, screenWidth.toFloat() / screenHeight, 0.1f, 128f)
            .mul(Matrix4f.createTranslation(player.vec3Position)
                .mul(rotY(-player.rotationYaw-180))
                .mul(rotX(-player.rotationPitch))
                .invert())
//
        map.forEach {
            drawEntityBox(it, viewProjMatrix, screenWidth, screenHeight, canvas)
        }
    }

    private fun drawEntityBox(entity: EntityPlayer, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int, canvas: Canvas) {
        if(displayList[entity]==null){
            displayList[entity]= EntityNameTag()
        }
        displayList[entity]!!.draw(entity,viewProjMatrix,screenWidth,screenHeight,canvas,this)

    }

    fun worldToScreen(posX: Double, posY: Double, posZ: Double, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int): Vector2d? {
        val w = viewProjMatrix.get(3, 0) * posX +
                viewProjMatrix.get(3, 1) * posY +
                viewProjMatrix.get(3, 2) * posZ +
                viewProjMatrix.get(3, 3)
        if (w < 0.01f) return null
        val inverseW = 1 / w

        val screenX = screenWidth / 2f + (0.5f * ((viewProjMatrix.get(0, 0) * posX + viewProjMatrix.get(0, 1) * posY +
                viewProjMatrix.get(0, 2) * posZ + viewProjMatrix.get(0, 3)) * inverseW) * screenWidth + 0.5f)
        val screenY = screenHeight / 2f - (0.5f * ((viewProjMatrix.get(1, 0) * posX + viewProjMatrix.get(1, 1) * posY +
                viewProjMatrix.get(1, 2) * posZ + viewProjMatrix.get(1, 3)) * inverseW) * screenHeight + 0.5f)
        return Vector2d.from(screenX, screenY)
    }

    private fun rotX(angle: Float): Matrix4f {
        val rad = Math.toRadians(angle.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()

        return Matrix4f.from(1f, 0f, 0f, 0f,
            0f, c, -s, 0f,
            0f, s, c, 0f,
            0f, 0f, 0f, 1f)
    }

    private fun rotY(angle: Float): Matrix4f {
        val rad = Math.toRadians(angle.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()

        return Matrix4f.from(c, 0f, s, 0f,
            0f, 1f, 0f, 0f,
            -s, 0f, c, 0f,
            0f, 0f, 0f, 1f)
    }
}
