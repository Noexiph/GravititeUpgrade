package noexiph.gravititeupgrade;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class GravititeInputHandler {

    private static boolean wasJumping = false;
    private static int jumpCooldown = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (jumpCooldown > 0) jumpCooldown--;

            boolean isJumping = client.options.jumpKey.isPressed();

            // Detect "Rising Edge" (Key pushed down this tick)
            if (isJumping && !wasJumping) {
                // Double Jump Condition:
                // 1. Player is in the air (!isOnGround)
                // 2. Player is NOT in Creative/Spectator (Vanilla flight handles those)
                // 3. Player is NOT climbing a ladder/vine
                if (!client.player.isOnGround() && !client.player.getAbilities().creativeMode && !client.player.isClimbing()) {

                    if (jumpCooldown == 0) {
                        // Check if we have the interface
                        if (client.player instanceof IGravititeFlightAccess access) {
                            // Logic:
                            // If we are ALREADY flying, we can always toggle OFF.
                            // If we are NOT flying, we need > 0 fuel to toggle ON.
                            if (access.aether$isGravititeFlying() || access.aether$getFlightTimer() > 0) {
                                ClientPlayNetworking.send(new GravititePayload());
                                jumpCooldown = 4; // Short cooldown to prevent accidental double-toggles
                            }
                        }
                    }
                }
            }
            wasJumping = isJumping;
        });
    }
}