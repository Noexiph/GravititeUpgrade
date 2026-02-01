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

            // Simple cooldown to prevent spamming the packet
            if (jumpCooldown > 0) {
                jumpCooldown--;
            }

            boolean isJumping = client.options.jumpKey.isPressed();

            // Logic:
            // 1. Must be pressing jump (Rising edge: was not jumping last tick)
            // 2. Must NOT be on ground (Double Jump)
            // 3. Not in Creative/Spectator (Vanilla flight takes priority)
            // 4. Cooldown is 0
            if (isJumping && !wasJumping && !client.player.isOnGround() && !client.player.getAbilities().creativeMode) {
                if (jumpCooldown == 0) {
                    // Send the toggle packet
                    if (ClientPlayNetworking.canSend(GravititePayload.ID)) {
                        ClientPlayNetworking.send(new GravititePayload());
                        jumpCooldown = 10; // 0.5 second cooldown
                    }
                }
            }

            wasJumping = isJumping;
        });
    }
}