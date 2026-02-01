package noexiph.gravititeupgrade;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class GravititeInputHandler {

    private static boolean wasJumping = false;
    private static boolean hasReleasedJumpInAir = false;

    // Timer to track Double-Tap speed
    private static long lastJumpPressTime = 0;
    private static final long DOUBLE_TAP_THRESHOLD_MS = 300; // 0.3 Seconds

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean isJumping = client.options.jumpKey.isPressed();
            boolean onGround = client.player.isOnGround();
            long currentTime = System.currentTimeMillis();

            // 1. Reset "Released" tracker on ground
            if (onGround) {
                hasReleasedJumpInAir = false;
            } else if (!isJumping) {
                hasReleasedJumpInAir = true;
            }

            // 2. Input Detection (Rising Edge)
            if (isJumping && !wasJumping) {

                if (client.player instanceof IGravititeFlightAccess access) {
                    boolean isFlying = access.aether$isGravititeFlying();
                    boolean shouldToggle = false;

                    // --- LOGIC A: WE ARE NOT FLYING (Start Flight) ---
                    if (!isFlying) {
                        // Condition: In Air, released space at least once, not creative, not climbing
                        if (!onGround && hasReleasedJumpInAir && !client.player.getAbilities().creativeMode && !client.player.isClimbing()) {
                            // Check Fuel
                            if (access.aether$getFlightTimer() > 0) {
                                shouldToggle = true;
                            }
                        }
                    }

                    // --- LOGIC B: WE ARE FLYING (Stop Flight) ---
                    else {
                        // Condition: FAST Double Tap detected
                        if ((currentTime - lastJumpPressTime) <= DOUBLE_TAP_THRESHOLD_MS) {
                            shouldToggle = true;
                        } else {
                            // This is just a single press (Ascend). Do NOT toggle off.
                        }
                    }

                    // 3. Execution
                    if (shouldToggle) {
                        ClientPlayNetworking.send(new GravititePayload());
                        access.aether$setGravititeFlying(!isFlying); // Client Prediction (Fixes rendering lag)

                        // Reset timer so we don't trigger again immediately
                        lastJumpPressTime = 0;
                    } else {
                        // Record this press time for the next check
                        lastJumpPressTime = currentTime;
                    }
                }
            }
            wasJumping = isJumping;
        });
    }
}