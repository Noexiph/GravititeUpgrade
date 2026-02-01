package noexiph.gravititeupgrade;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

public class GravititeSoundController {

    private static SoundInstance currentSound = null;

    // Cycle index instead of Random
    private static int currentSoundIndex = -1; // Start at -1 so first one is 0

    // --- SEAMLESS LOOP CONFIG ---
    private static int ticksPlayed = 0;
    private static int currentSoundDuration = 0;

    // Based on your files being 40 ticks (2s) long with a fade out.
    // 32 ticks creates an 8-tick (0.4s) cross-fade overlap.
    private static final int[] SOUND_LENGTH_TICKS = {
            32, // levitate0
            32, // levitate1
            32  // levitate2
    };

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean isFlying = false;
            if (client.player instanceof IGravititeFlightAccess access) {
                isFlying = access.aether$isGravititeFlying();
            }

            if (!isFlying) {
                // Not flying: Stop sound and reset cycle
                if (currentSound != null) {
                    client.getSoundManager().stop(currentSound);
                    currentSound = null;
                    ticksPlayed = 0;
                    currentSoundIndex = -1; // Optional: Reset to start from 0 next time
                }
            } else {
                // Flying: Manage Loop
                ticksPlayed++;

                // Trigger new sound if null OR if we passed the overlap point
                if (currentSound == null || ticksPlayed >= currentSoundDuration) {

                    // Increment and Cycle (0 -> 1 -> 2 -> 0)
                    currentSoundIndex = (currentSoundIndex + 1) % 3;

                    SoundEvent nextSound = switch (currentSoundIndex) {
                        case 0 -> GravititeUpgrade.LEVITATE_0;
                        case 1 -> GravititeUpgrade.LEVITATE_1;
                        default -> GravititeUpgrade.LEVITATE_2;
                    };

                    currentSoundDuration = SOUND_LENGTH_TICKS[currentSoundIndex];
                    ticksPlayed = 0;

                    // Play at player position
                    double x = client.player.getX();
                    double y = client.player.getY();
                    double z = client.player.getZ();

                    currentSound = new PositionedSoundInstance(
                            nextSound.getId(),
                            SoundCategory.PLAYERS,
                            1.0f, 1.0f,
                            Random.create(),
                            false, 0,
                            SoundInstance.AttenuationType.LINEAR,
                            x, y, z,
                            false
                    );

                    client.getSoundManager().play(currentSound);
                }
            }
        });
    }
}