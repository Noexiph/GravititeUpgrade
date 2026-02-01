package noexiph.gravititeupgrade;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class GravititeUpgradeClient implements ClientModInitializer {

    // Texture: 27x9 pixels (Full, Half, Empty side-by-side)
    private static final Identifier GRAVITY_BAR_TEXTURE = Identifier.of("gravititeupgrade", "textures/gui/gravity_flight.png");

    @Override
    public void onInitializeClient() {
        // 1. Register HUD Rendering (Crystals)
        // Fixed: Method reference now matches (DrawContext, RenderTickCounter)
        HudRenderCallback.EVENT.register(this::renderGravititeHud);

        // 2. Register World Rendering (Flight Plane)
        WorldRenderEvents.AFTER_ENTITIES.register(GravititePlaneRenderer::render);
        GravititeInputHandler.register();
    }

    private void renderGravititeHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.isCreative() || client.player.isSpectator()) return;

        // Calculate Max Capacity (1.5 crystals per Gravitite item)
        float maxCrystals = 0;
        for (ItemStack stack : client.player.getArmorItems()) {
            // Adjust this check to match your specific item IDs
            if (stack.getItem().toString().contains("gravitite")) {
                maxCrystals += 1.5f;
            }
        }

        if (maxCrystals <= 0) return;

        // Get current flight timer from your interface
        float currentTimer = 0f;
        if (client.player instanceof IGravititeFlightAccess access) {
            currentTimer = access.aether$getFlightTimer();
        }

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // Position: Above Armor Bar.
        int left = width / 2 + 10;
        int top = height - 49;

        int totalIcons = (int) Math.ceil(maxCrystals);

        for (int i = 0; i < totalIcons; i++) {
            int state = 0; // 0=Empty

            if (currentTimer >= i + 1) {
                state = 2; // Full
            } else if (currentTimer > i) {
                state = 1; // Half
            }

            // Texture offsets (assuming 9x9 icons side by side: Full | Half | Empty)
            int u = 0;
            if (state == 1) u = 9;
            if (state == 0) u = 18;

            // Draw Right-to-Left
            int xPos = (width / 2) + 82 - (i * 8);

            context.drawTexture(GRAVITY_BAR_TEXTURE, xPos, top, u, 0, 9, 9, 27, 9);
        }

        RenderSystem.disableBlend();
    }
}