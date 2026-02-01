package noexiph.gravititeupgrade;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class GravititeInputHandler {

    private static boolean wasJumping = false;
    private static int jumpCooldown = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (jumpCooldown > 0) jumpCooldown--;

            boolean isJumping = client.options.jumpKey.isPressed();

            // Rising Edge (Pressed Space this tick)
            if (isJumping && !wasJumping) {
                // Logic: Must be in air, not in creative, and not on cooldown
                if (!client.player.isOnGround() && !client.player.getAbilities().creativeMode) {
                    if (jumpCooldown == 0) {

                        // Check if we have fuel before trying to toggle ON
                        // We can read our own synced data on the client!
                        if (client.player instanceof IGravititeFlightAccess access) {
                            // If we are flying, we can always toggle OFF.
                            // If we are NOT flying, we need fuel > 0 to toggle ON.
                            boolean isFlying = access.aether$isGravititeFlying();
                            if (isFlying || access.aether$getFlightTimer() > 0) {
                                ClientPlayNetworking.send(new GravititePayload());
                                jumpCooldown = 5;
                            }
                        }
                    }
                }
            }
            wasJumping = isJumping;
        });
    }
}