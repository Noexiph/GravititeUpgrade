package noexiph.gravititeupgrade;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class GravititePlaneRenderer {

    private static final Identifier PLANE_TEXTURE = Identifier.of("gravititeupgrade", "textures/particles/gravitite_aura/gravity_pulse.png");
    private static final int FRAMES = 6;
    private static final int FRAME_DURATION_TICKS = 2;

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;

        if (player == null) return;

        // 1. Check Flight State
        boolean isFlying = false;
        if (player instanceof IGravititeFlightAccess access) {
            isFlying = access.aether$isGravititeFlying();
        }
        if (!isFlying) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d camPos = context.camera().getPos();
        float tickDelta = context.tickCounter().getTickDelta(false);

        double x = MathHelper.lerp(tickDelta, player.prevX, player.getX()) - camPos.x;
        double y = MathHelper.lerp(tickDelta, player.prevY, player.getY()) - camPos.y - 0.1;
        double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ()) - camPos.z;

        matrices.push();
        matrices.translate(x, y, z);

        long time = client.world.getTime();
        int frameIndex = (int) ((time / FRAME_DURATION_TICKS) % FRAMES);

        float uMin = 0.0f;
        float uMax = 1.0f;
        float vMin = (float) frameIndex / FRAMES;
        float vMax = (float) (frameIndex + 1) / FRAMES;

        // --- RENDER SETUP FIX ---
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, PLANE_TEXTURE);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // FIX: Enable Depth Test so it hides behind the player's legs
        RenderSystem.enableDepthTest();

        // FIX: Enable Depth Write (Mask=True) if you want it to sort properly with other things,
        // or False if it's a ghost-like particle. "True" usually fixes "seeing through things".
        // However, for transparent textures, 'false' is common to avoid cutting holes.
        // If it was appearing "on top", it's because DepthTest was likely OFF or Func was ALWAYS.
        RenderSystem.depthMask(false);
        RenderSystem.depthFunc(515); // GL_LEQUAL (Standard Minecraft Depth Func)

        RenderSystem.disableCull();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        float size = 1.0f;

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        buffer.vertex(matrix, -size, -size, 0).texture(uMin, vMin).color(255, 255, 255, 255);
        buffer.vertex(matrix, -size, size, 0).texture(uMin, vMax).color(255, 255, 255, 255);
        buffer.vertex(matrix, size, size, 0).texture(uMax, vMax).color(255, 255, 255, 255);
        buffer.vertex(matrix, size, -size, 0).texture(uMax, vMin).color(255, 255, 255, 255);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        matrices.pop();
    }
}