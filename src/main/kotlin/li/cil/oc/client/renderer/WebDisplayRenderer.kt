package li.cil.oc.client.renderer

import com.mojang.blaze3d.vertex.*
import com.mojang.math.Axis
import li.cil.oc.OpenComputers
import li.cil.oc.common.block.WebDisplayBlock
import li.cil.oc.common.blockentity.WebDisplayBlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix4f

/**
 * Renderer for WebDisplay block entities.
 * 
 * Renders web page content as text on the display surface.
 */
class WebDisplayRenderer(
    context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<WebDisplayBlockEntity> {
    
    companion object {
        // Display constants
        const val SCREEN_PADDING = 0.02f
        const val TEXT_SCALE = 0.01f
        const val LINE_HEIGHT = 9f
        const val CHAR_WIDTH = 6f
        
        // Colors
        const val BACKGROUND_COLOR = 0xFF1A1A2E // Dark blue background
        const val TEXT_COLOR = 0xFFE0E0E0 // Light gray text
        const val LINK_COLOR = 0xFF6699FF // Blue for links
        const val TITLE_COLOR = 0xFF00FF99 // Green for title
        const val LOADING_COLOR = 0xFFFFAA00 // Orange for loading
        
        // Textures
        val SCREEN_TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/block/web_display_screen.png")
    }
    
    private val font: Font = Minecraft.getInstance().font
    
    override fun render(
        blockEntity: WebDisplayBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        if (!blockEntity.isOn) return
        
        poseStack.pushPose()
        
        // Position at block center
        poseStack.translate(0.5, 0.5, 0.5)
        
        // Rotate to face the correct direction
        val facing = blockEntity.blockState.getValue(WebDisplayBlock.FACING)
        when (facing) {
            Direction.NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(0f))
            Direction.SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180f))
            Direction.WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90f))
            Direction.EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270f))
            else -> {}
        }
        
        // Move to screen surface (front face)
        poseStack.translate(0.0, 0.0, -0.49)
        
        // Render screen background
        renderScreenBackground(poseStack, bufferSource, packedLight)
        
        // Scale and position text
        poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE)
        poseStack.translate(-45.0, -30.0, -0.01)
        
        val matrix = poseStack.last().pose()
        
        // Render title bar
        renderTitleBar(blockEntity, matrix, bufferSource, packedLight)
        
        // Render content
        renderContent(blockEntity, matrix, bufferSource, packedLight)
        
        poseStack.popPose()
    }
    
    private fun renderScreenBackground(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        light: Int
    ) {
        val matrix = poseStack.last().pose()
        val consumer = bufferSource.getBuffer(RenderType.entityCutout(SCREEN_TEXTURE))
        
        val halfWidth = 0.48f
        val halfHeight = 0.35f
        
        // Background color components
        val r = ((BACKGROUND_COLOR shr 16) and 0xFF) / 255f
        val g = ((BACKGROUND_COLOR shr 8) and 0xFF) / 255f
        val b = (BACKGROUND_COLOR and 0xFF) / 255f
        
        // Render background quad
        consumer.addVertex(matrix, -halfWidth, -halfHeight, 0f)
            .setColor(r, g, b, 0.95f)
            .setUv(0f, 0f)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0f, 0f, 1f)
        
        consumer.addVertex(matrix, halfWidth, -halfHeight, 0f)
            .setColor(r, g, b, 0.95f)
            .setUv(1f, 0f)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0f, 0f, 1f)
        
        consumer.addVertex(matrix, halfWidth, halfHeight, 0f)
            .setColor(r, g, b, 0.95f)
            .setUv(1f, 1f)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0f, 0f, 1f)
        
        consumer.addVertex(matrix, -halfWidth, halfHeight, 0f)
            .setColor(r, g, b, 0.95f)
            .setUv(0f, 1f)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0f, 0f, 1f)
    }
    
    private fun renderTitleBar(
        blockEntity: WebDisplayBlockEntity,
        matrix: Matrix4f,
        bufferSource: MultiBufferSource,
        light: Int
    ) {
        val title = if (blockEntity.isLoading) "Loading..." else blockEntity.pageTitle
        val url = blockEntity.currentUrl
        
        // Title
        val titleColor = if (blockEntity.isLoading) LOADING_COLOR.toInt() else TITLE_COLOR.toInt()
        font.drawInBatch(
            "▸ $title",
            0f, 0f,
            titleColor,
            false,
            matrix,
            bufferSource,
            Font.DisplayMode.NORMAL,
            0,
            light
        )
        
        // URL bar
        val urlDisplay = if (url.length > 35) url.take(32) + "..." else url
        font.drawInBatch(
            "🔗 $urlDisplay",
            0f, 10f,
            TEXT_COLOR.toInt(),
            false,
            matrix,
            bufferSource,
            Font.DisplayMode.NORMAL,
            0,
            light
        )
        
        // Separator line
        font.drawInBatch(
            "─".repeat(15),
            0f, 20f,
            0xFF444444.toInt(),
            false,
            matrix,
            bufferSource,
            Font.DisplayMode.NORMAL,
            0,
            light
        )
    }
    
    private fun renderContent(
        blockEntity: WebDisplayBlockEntity,
        matrix: Matrix4f,
        bufferSource: MultiBufferSource,
        light: Int
    ) {
        val content = blockEntity.renderedContent
        val scrollOffset = blockEntity.scrollOffset
        val maxLines = 5 // Visible lines
        
        var y = 30f
        
        for (i in scrollOffset until minOf(scrollOffset + maxLines, content.size)) {
            val line = content[i]
            
            // Detect links (simple heuristic)
            val color = when {
                line.startsWith("[") && line.contains("]") -> LINK_COLOR.toInt()
                line.startsWith("═") || line.startsWith("─") -> 0xFF666666.toInt()
                line.startsWith("•") -> 0xFF88FF88.toInt()
                else -> TEXT_COLOR.toInt()
            }
            
            val displayLine = if (line.length > 20) line.take(17) + "..." else line
            
            font.drawInBatch(
                displayLine,
                0f, y,
                color,
                false,
                matrix,
                bufferSource,
                Font.DisplayMode.NORMAL,
                0,
                light
            )
            
            y += LINE_HEIGHT
        }
        
        // Show scroll indicator if needed
        if (content.size > maxLines) {
            val scrollPercent = if (content.size - maxLines > 0) 
                scrollOffset * 100 / (content.size - maxLines) 
            else 0
            
            font.drawInBatch(
                "▼ $scrollPercent%",
                70f, 55f,
                0xFF888888.toInt(),
                false,
                matrix,
                bufferSource,
                Font.DisplayMode.NORMAL,
                0,
                light
            )
        }
    }
    
    override fun shouldRenderOffScreen(blockEntity: WebDisplayBlockEntity): Boolean {
        return true
    }
    
    override fun getViewDistance(): Int {
        return 64
    }
}
