package li.cil.oc.client.renderer

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import com.mojang.math.Axis
import li.cil.oc.OpenComputers
import li.cil.oc.common.blockentity.HologramBlockEntity
import li.cil.oc.common.blockentity.ScreenBlockEntity
import li.cil.oc.common.entity.DroneEntity
import li.cil.oc.common.entity.RobotEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix4f

/**
 * Renderer for Screen block entities.
 * 
 * Renders the text buffer contents onto the screen surface.
 */
class ScreenRenderer(
    context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<ScreenBlockEntity> {
    
    companion object {
        // Character dimensions
        const val CHAR_WIDTH = 6
        const val CHAR_HEIGHT = 9
        const val SCALE = 1f / 256f
        
        // Font texture
        val FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/font/font.png")
    }
    
    override fun render(
        blockEntity: ScreenBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val buffer = blockEntity.textBuffer ?: return
        
        poseStack.pushPose()
        
        // Position at screen face
        val facing = blockEntity.facing
        poseStack.translate(0.5, 0.5, 0.5)
        
        // Rotate to face the correct direction
        when (facing) {
            net.minecraft.core.Direction.NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(0f))
            net.minecraft.core.Direction.SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180f))
            net.minecraft.core.Direction.WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90f))
            net.minecraft.core.Direction.EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270f))
            else -> {}
        }
        
        // Move to screen surface
        poseStack.translate(0.0, 0.0, -0.501)
        
        // Scale to fit screen
        val screenWidth = buffer.width
        val screenHeight = buffer.height
        val blockWidth = 1.0f
        val blockHeight = 1.0f
        val charScaleX = blockWidth / (screenWidth * CHAR_WIDTH)
        val charScaleY = blockHeight / (screenHeight * CHAR_HEIGHT)
        val charScale = minOf(charScaleX, charScaleY) * 0.9f
        
        poseStack.scale(charScale, -charScale, charScale)
        poseStack.translate(-screenWidth * CHAR_WIDTH / 2.0, -screenHeight * CHAR_HEIGHT / 2.0, 0.0)
        
        // Render each character
        val vertexConsumer = bufferSource.getBuffer(RenderType.text(FONT_TEXTURE))
        val matrix = poseStack.last().pose()
        
        for (y in 0 until screenHeight) {
            for (x in 0 until screenWidth) {
                val char = buffer.getChar(x, y)
                val fg = buffer.getForegroundColor(x, y)
                val bg = buffer.getBackgroundColor(x, y)
                
                val screenX = x * CHAR_WIDTH
                val screenY = y * CHAR_HEIGHT
                
                // Draw background
                if (bg and 0xFFFFFF != 0) {
                    renderColoredQuad(
                        matrix, vertexConsumer,
                        screenX.toFloat(), screenY.toFloat(),
                        CHAR_WIDTH.toFloat(), CHAR_HEIGHT.toFloat(),
                        bg, packedLight
                    )
                }
                
                // Draw character
                if (char != ' ' && char.code > 0) {
                    renderCharacter(
                        matrix, vertexConsumer,
                        screenX.toFloat(), screenY.toFloat(),
                        char, fg, packedLight
                    )
                }
            }
        }
        
        poseStack.popPose()
    }
    
    private fun renderCharacter(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        x: Float,
        y: Float,
        char: Char,
        color: Int,
        light: Int
    ) {
        // Calculate texture coordinates from character code
        val charIndex = char.code and 0xFF
        val texX = (charIndex % 16) * 8
        val texY = (charIndex / 16) * 16
        
        val u0 = texX * SCALE
        val v0 = texY * SCALE
        val u1 = (texX + 6) * SCALE
        val v1 = (texY + 9) * SCALE
        
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        
        consumer.addVertex(matrix, x, y + CHAR_HEIGHT, 0f)
            .setColor(r, g, b, 1f)
            .setUv(u0, v1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0f, 0f, 1f)
        
        consumer.addVertex(matrix, x + CHAR_WIDTH, y + CHAR_HEIGHT, 0f)
            .setColor(r, g, b, 1f)
            .setUv(u1, v1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0f, 0f, 1f)
        
        consumer.addVertex(matrix, x + CHAR_WIDTH, y, 0f)
            .setColor(r, g, b, 1f)
            .setUv(u1, v0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0f, 0f, 1f)
        
        consumer.addVertex(matrix, x, y, 0f)
            .setColor(r, g, b, 1f)
            .setUv(u0, v0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0f, 0f, 1f)
    }
    
    private fun renderColoredQuad(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Int,
        light: Int
    ) {
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        
        // Use a white pixel from the font texture for solid colors
        val u = 0.99f
        val v = 0.99f
        
        consumer.addVertex(matrix, x, y + height, -0.001f)
            .setColor(r, g, b, 1f)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0f, 0f, 1f)
        
        consumer.addVertex(matrix, x + width, y + height, -0.001f)
            .setColor(r, g, b, 1f)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0f, 0f, 1f)
        
        consumer.addVertex(matrix, x + width, y, -0.001f)
            .setColor(r, g, b, 1f)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0f, 0f, 1f)
        
        consumer.addVertex(matrix, x, y, -0.001f)
            .setColor(r, g, b, 1f)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0f, 0f, 1f)
    }
    
    override fun getViewDistance(): Int = 64
}

/**
 * Renderer for Hologram block entities.
 * 
 * Renders a 48x32x48 voxel projection above the hologram projector.
 */
class HologramRenderer(
    context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<HologramBlockEntity> {
    
    companion object {
        const val VOXEL_SIZE = 1f / 48f // Size of each voxel in blocks
        const val MAX_HEIGHT = 2f // Maximum projection height in blocks
    }
    
    override fun render(
        blockEntity: HologramBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        if (!blockEntity.hasPower()) return
        
        poseStack.pushPose()
        
        // Position above the hologram projector
        poseStack.translate(0.5, 1.0, 0.5)
        
        // Apply rotation
        poseStack.mulPose(Axis.YP.rotationDegrees(blockEntity.rotation * 360f / 256f))
        
        // Apply scale
        val scale = blockEntity.scale
        poseStack.scale(scale, scale, scale)
        
        // Offset to center
        poseStack.translate(-0.5, 0.0, -0.5)
        
        // Get render type for translucent rendering
        val consumer = bufferSource.getBuffer(
            RenderType.translucentMovingBlock()
        )
        val matrix = poseStack.last().pose()
        
        // Get colors
        val colors = blockEntity.getColors()
        
        // Render each set voxel
        for (x in 0 until 48) {
            for (z in 0 until 48) {
                for (y in 0 until 32) {
                    val colorIndex = blockEntity.getVoxel(x, y, z)
                    if (colorIndex == 0) continue
                    
                    val color = colors.getOrElse(colorIndex - 1) { 0xFFFFFF }
                    
                    renderVoxel(
                        matrix, consumer,
                        x * VOXEL_SIZE,
                        y * VOXEL_SIZE * (MAX_HEIGHT / 32f),
                        z * VOXEL_SIZE,
                        VOXEL_SIZE, VOXEL_SIZE * (MAX_HEIGHT / 32f), VOXEL_SIZE,
                        color,
                        0.8f // Alpha
                    )
                }
            }
        }
        
        poseStack.popPose()
    }
    
    private fun renderVoxel(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        color: Int,
        alpha: Float
    ) {
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        
        // Top face
        addQuad(matrix, consumer, 
            x, y + sy, z,
            x + sx, y + sy, z,
            x + sx, y + sy, z + sz,
            x, y + sy, z + sz,
            r, g, b, alpha, 0f, 1f, 0f)
        
        // Bottom face
        addQuad(matrix, consumer,
            x, y, z + sz,
            x + sx, y, z + sz,
            x + sx, y, z,
            x, y, z,
            r, g, b, alpha, 0f, -1f, 0f)
        
        // Front face (z+)
        addQuad(matrix, consumer,
            x, y, z + sz,
            x, y + sy, z + sz,
            x + sx, y + sy, z + sz,
            x + sx, y, z + sz,
            r, g, b, alpha, 0f, 0f, 1f)
        
        // Back face (z-)
        addQuad(matrix, consumer,
            x + sx, y, z,
            x + sx, y + sy, z,
            x, y + sy, z,
            x, y, z,
            r, g, b, alpha, 0f, 0f, -1f)
        
        // Right face (x+)
        addQuad(matrix, consumer,
            x + sx, y, z + sz,
            x + sx, y + sy, z + sz,
            x + sx, y + sy, z,
            x + sx, y, z,
            r, g, b, alpha, 1f, 0f, 0f)
        
        // Left face (x-)
        addQuad(matrix, consumer,
            x, y, z,
            x, y + sy, z,
            x, y + sy, z + sz,
            x, y, z + sz,
            r, g, b, alpha, -1f, 0f, 0f)
    }
    
    private fun addQuad(
        matrix: Matrix4f,
        consumer: VertexConsumer,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        r: Float, g: Float, b: Float, a: Float,
        nx: Float, ny: Float, nz: Float
    ) {
        consumer.addVertex(matrix, x1, y1, z1)
            .setColor(r, g, b, a)
            .setUv(0f, 0f)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(nx, ny, nz)
        
        consumer.addVertex(matrix, x2, y2, z2)
            .setColor(r, g, b, a)
            .setUv(0f, 1f)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(nx, ny, nz)
        
        consumer.addVertex(matrix, x3, y3, z3)
            .setColor(r, g, b, a)
            .setUv(1f, 1f)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(nx, ny, nz)
        
        consumer.addVertex(matrix, x4, y4, z4)
            .setColor(r, g, b, a)
            .setUv(1f, 0f)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(nx, ny, nz)
    }
    
    override fun shouldRenderOffScreen(blockEntity: HologramBlockEntity): Boolean = true
    
    override fun getViewDistance(): Int = 256
}

/**
 * Renderer for Robot entities.
 */
class RobotRenderer(
    context: EntityRendererProvider.Context
) : EntityRenderer<RobotEntity>(context) {
    
    companion object {
        val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/entity/robot.png")
    }
    
    private val robotModel = RobotModel(context.bakeLayer(RobotModel.LAYER_LOCATION))
    
    override fun render(
        entity: RobotEntity,
        entityYaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int
    ) {
        poseStack.pushPose()
        
        // Translate to correct position
        poseStack.translate(0.0, 1.5, 0.0)
        poseStack.mulPose(Axis.ZP.rotationDegrees(180f))
        
        // Apply entity rotation
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw))
        
        // Render model
        val consumer = bufferSource.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)))
        robotModel.renderToBuffer(
            poseStack, consumer,
            packedLight, OverlayTexture.NO_OVERLAY,
            0xFFFFFFFF.toInt()
        )
        
        // Render status light
        renderStatusLight(entity, poseStack, bufferSource)
        
        poseStack.popPose()
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight)
    }
    
    private fun renderStatusLight(
        entity: RobotEntity,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource
    ) {
        val lightColor = entity.getLightColor()
        if (lightColor == 0) return
        
        poseStack.pushPose()
        poseStack.translate(0.0, -0.6, -0.35)
        poseStack.scale(0.1f, 0.1f, 0.1f)
        
        val consumer = bufferSource.getBuffer(RenderType.beaconBeam(TEXTURE, true))
        val matrix = poseStack.last().pose()
        
        val r = (lightColor shr 16 and 0xFF) / 255f
        val g = (lightColor shr 8 and 0xFF) / 255f
        val b = (lightColor and 0xFF) / 255f
        
        // Small glowing cube
        renderGlowingCube(matrix, consumer, r, g, b)
        
        poseStack.popPose()
    }
    
    private fun renderGlowingCube(matrix: Matrix4f, consumer: VertexConsumer, r: Float, g: Float, b: Float) {
        val size = 0.5f
        
        // Simple cube vertices
        val vertices = listOf(
            // Front
            -size to (-size to size), size to (-size to size), size to (size to size), -size to (size to size),
            // Back
            size to (-size to -size), -size to (-size to -size), -size to (size to -size), size to (size to -size),
            // Top
            -size to (size to -size), -size to (size to size), size to (size to size), size to (size to -size),
            // Bottom
            -size to (-size to size), -size to (-size to -size), size to (-size to -size), size to (-size to size),
            // Right
            size to (-size to size), size to (-size to -size), size to (size to -size), size to (size to size),
            // Left
            -size to (-size to -size), -size to (-size to size), -size to (size to size), -size to (size to -size)
        )
    }
    
    override fun getTextureLocation(entity: RobotEntity): ResourceLocation = TEXTURE
}

/**
 * Robot model definition.
 */
class RobotModel(root: net.minecraft.client.model.geom.ModelPart) : net.minecraft.client.model.EntityModel<RobotEntity>() {
    
    companion object {
        val LAYER_LOCATION = net.minecraft.client.model.geom.ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "robot"),
            "main"
        )
        
        fun createBodyLayer(): net.minecraft.client.model.geom.builders.LayerDefinition {
            val meshDefinition = net.minecraft.client.model.geom.builders.MeshDefinition()
            val partDefinition = meshDefinition.root
            
            // Main body - a cube
            partDefinition.addOrReplaceChild(
                "body",
                net.minecraft.client.model.geom.builders.CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(-7f, -14f, -7f, 14f, 14f, 14f),
                net.minecraft.client.model.geom.builders.PartPose.offset(0f, 24f, 0f)
            )
            
            return net.minecraft.client.model.geom.builders.LayerDefinition.create(meshDefinition, 64, 64)
        }
    }
    
    private val body: net.minecraft.client.model.geom.ModelPart = root.getChild("body")
    
    override fun setupAnim(
        entity: RobotEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        // Animation logic
    }
    
    override fun renderToBuffer(
        poseStack: PoseStack,
        buffer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int,
        color: Int
    ) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color)
    }
}

/**
 * Renderer for Drone entities.
 */
class DroneRenderer(
    context: EntityRendererProvider.Context
) : EntityRenderer<DroneEntity>(context) {
    
    companion object {
        val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/entity/drone.png")
    }
    
    private val droneModel = DroneModel(context.bakeLayer(DroneModel.LAYER_LOCATION))
    
    override fun render(
        entity: DroneEntity,
        entityYaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int
    ) {
        poseStack.pushPose()
        
        poseStack.translate(0.0, 0.5, 0.0)
        
        // Hover bobbing effect
        val bob = kotlin.math.sin((entity.tickCount + partialTick) * 0.15f) * 0.05f
        poseStack.translate(0.0, bob.toDouble(), 0.0)
        
        // Propeller spin
        val spin = (entity.tickCount + partialTick) * 30f
        
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw))
        
        // Render
        val consumer = bufferSource.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)))
        droneModel.propellerSpin = spin
        droneModel.renderToBuffer(
            poseStack, consumer,
            packedLight, OverlayTexture.NO_OVERLAY,
            0xFFFFFFFF.toInt()
        )
        
        // Render status light
        val lightColor = entity.getLightColor()
        if (lightColor != 0) {
            poseStack.pushPose()
            poseStack.translate(0.0, -0.1, 0.0)
            poseStack.scale(0.05f, 0.05f, 0.05f)
            // Light rendering
            poseStack.popPose()
        }
        
        poseStack.popPose()
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight)
    }
    
    override fun getTextureLocation(entity: DroneEntity): ResourceLocation = TEXTURE
}

/**
 * Drone model definition.
 */
class DroneModel(root: net.minecraft.client.model.geom.ModelPart) : net.minecraft.client.model.EntityModel<DroneEntity>() {
    
    companion object {
        val LAYER_LOCATION = net.minecraft.client.model.geom.ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "drone"),
            "main"
        )
        
        fun createBodyLayer(): net.minecraft.client.model.geom.builders.LayerDefinition {
            val meshDefinition = net.minecraft.client.model.geom.builders.MeshDefinition()
            val partDefinition = meshDefinition.root
            
            // Central body
            partDefinition.addOrReplaceChild(
                "body",
                net.minecraft.client.model.geom.builders.CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(-2f, -1f, -2f, 4f, 2f, 4f),
                net.minecraft.client.model.geom.builders.PartPose.ZERO
            )
            
            // Propeller arms (4)
            for (i in 0 until 4) {
                partDefinition.addOrReplaceChild(
                    "arm$i",
                    net.minecraft.client.model.geom.builders.CubeListBuilder.create()
                        .texOffs(0, 6)
                        .addBox(-0.5f, 0f, -0.5f, 1f, 0.5f, 4f),
                    net.minecraft.client.model.geom.builders.PartPose.offsetAndRotation(
                        0f, -1f, 0f,
                        0f, (i * 90f) * (Math.PI.toFloat() / 180f), 0f
                    )
                )
            }
            
            // Propellers
            for (i in 0 until 4) {
                partDefinition.addOrReplaceChild(
                    "prop$i",
                    net.minecraft.client.model.geom.builders.CubeListBuilder.create()
                        .texOffs(16, 0)
                        .addBox(-2f, -0.1f, -0.25f, 4f, 0.2f, 0.5f),
                    net.minecraft.client.model.geom.builders.PartPose.offset(
                        kotlin.math.sin(i * 90f * Math.PI.toFloat() / 180f) * 3f,
                        -1.2f,
                        kotlin.math.cos(i * 90f * Math.PI.toFloat() / 180f) * 3f
                    )
                )
            }
            
            return net.minecraft.client.model.geom.builders.LayerDefinition.create(meshDefinition, 32, 16)
        }
    }
    
    private val body: net.minecraft.client.model.geom.ModelPart = root.getChild("body")
    private val propellers: List<net.minecraft.client.model.geom.ModelPart> = (0 until 4).map { root.getChild("prop$it") }
    
    var propellerSpin: Float = 0f
    
    override fun setupAnim(
        entity: DroneEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        // Spin propellers
        propellers.forEachIndexed { index, prop ->
            prop.yRot = (propellerSpin + index * 90f) * (Math.PI.toFloat() / 180f)
        }
    }
    
    override fun renderToBuffer(
        poseStack: PoseStack,
        buffer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int,
        color: Int
    ) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color)
        propellers.forEach { it.render(poseStack, buffer, packedLight, packedOverlay, color) }
    }
}
