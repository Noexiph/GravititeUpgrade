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

    // Animation constants
    private static final int FRAMES = 6;
    private static final int FRAME_DURATION_TICKS = 2; // Speed of animation

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;

        if (player == null) return;

        // Only render if flying
        boolean isFlying = false;
        if (player instanceof IGravititeFlightAccess access) {
            isFlying = access.aether$isGravititeFlying();
        }

        if (!isFlying) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d camPos = context.camera().getPos();

        // FIX 1: Get tickDelta from the TickCounter
        float tickDelta = context.tickCounter().getTickDelta(false);

        // Interpolate player position
        double x = MathHelper.lerp(tickDelta, player.prevX, player.getX()) - camPos.x;
        double y = MathHelper.lerp(tickDelta, player.prevY, player.getY()) - camPos.y;
        double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ()) - camPos.z;

        matrices.push();
        matrices.translate(x, y, z);

        long time = client.world.getTime();
        int frameIndex = (int) ((time / FRAME_DURATION_TICKS) % FRAMES);

        // FIX 2: Vertical Strip Logic
        // Image is 1 frame wide, 6 frames tall.
        // U is constant (0 to 1)
        // V slices the height
        float uMin = 0.0f;
        float uMax = 1.0f;
        float vMin = (float) frameIndex / FRAMES;
        float vMax = (float) (frameIndex + 1) / FRAMES;

        // Render Setup
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, PLANE_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        // Plane size radius (approx 1 block radius = 2 block width)
        float size = 1.0f;

        // Rotate to be flat on the ground
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Draw Quad
        // Note: With vertical strips, check if you need to rotate the UVs.
        // Standard "Top to Bottom" usually maps normally here.
        buffer.vertex(matrix, -size, -size, 0).texture(uMin, vMin).color(255, 255, 255, 255);
        buffer.vertex(matrix, -size, size, 0).texture(uMin, vMax).color(255, 255, 255, 255);
        buffer.vertex(matrix, size, size, 0).texture(uMax, vMax).color(255, 255, 255, 255);
        buffer.vertex(matrix, size, -size, 0).texture(uMax, vMin).color(255, 255, 255, 255);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        matrices.pop();
    }
}