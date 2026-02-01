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
import net.minecraft.util.math.MathHelper;

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

        // 1. Check Gravitite Armor
        float maxCrystals = 0;
        for (ItemStack stack : client.player.getArmorItems()) {
            if (stack.getItem().toString().toLowerCase().contains("gravitite")) {
                maxCrystals += 1.5f;
            }
        }
        if (maxCrystals <= 0) return;

        // 2. Get Data
        float currentTimer = 0f;
        if (client.player instanceof IGravititeFlightAccess access) {
            currentTimer = access.aether$getFlightTimer() / 20.0f;
        }

        // 3. Dynamic Position Calculation
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // Base Vanilla Left-Side Stack Calculation:
        // 1. Bottom: Hotbar (Height)
        // 2. Health Bar: Defaults to height - 39.
        //    If health > 20 (absorption/boost), it adds rows. 1 row = 10 pixels.
        //    Formula: ceil(health / 20) rows.

        float health = client.player.getHealth();
        float maxHealth = client.player.getMaxHealth();
        float absorption = client.player.getAbsorptionAmount();

        // Effective health for rendering rows (Vanilla logic roughly)
        // Vanilla renders Max Health rows, then puts Absorption on top (or overlays? It depends on version).
        // Usually, Absorption adds MORE rows if it exceeds 20.
        // Let's count "Visual Rows".
        int healthRows = MathHelper.ceil((maxHealth + absorption) / 20.0f);
        int rowHeight = 10;

        // Vanilla Logic:
        // Health starts at: height - 39
        // Armor starts at:  height - 39 - ((healthRows - 1) * rowHeight) - 10
        // (Because Armor is ABOVE health).

        // Let's calculate the TOP of the Armor Bar.
        int armorBaseY = height - 39;
        int healthOffset = (healthRows - 1) * rowHeight; // Extra health push
        int armorStart = armorBaseY - healthOffset - 10; // 10 pixels above health

        // We want to be 1 pixel above that.
        // Note: The crystal is 9 pixels tall.
        int hudY = armorStart - 10; // 10 pixels space for our bar (9 texture + 1 padding)

        // Sanity Check: If underwater, Air bar is on the RIGHT, so we are safe on the LEFT.

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();

        int totalIcons = (int) Math.ceil(maxCrystals);

        for (int i = 0; i < totalIcons; i++) {
            int state = 0;
            if (currentTimer >= i + 1) state = 2; // Full
            else if (currentTimer > i) state = 1; // Half

            int u = 0;
            if (state == 1) u = 9;
            if (state == 0) u = 18;

            // Align Left (Start at -91, move right by 8 pixels per icon)
            int xPos = (width / 2) - 91 + (i * 8);

            context.drawTexture(GRAVITY_BAR_TEXTURE, xPos, hudY, u, 0, 9, 9, 27, 9);
        }
        RenderSystem.disableBlend();
    }
}