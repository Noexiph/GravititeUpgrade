package noexiph.gravititeupgrade;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

public class GravititeSoundController {

    private static SoundInstance currentSound = null;
    private static final java.util.Random javaRandom = new java.util.Random();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean isFlying = false;
            if (client.player instanceof IGravititeFlightAccess access) {
                isFlying = access.aether$isGravititeFlying();
            }

            if (!isFlying) {
                if (currentSound != null) {
                    client.getSoundManager().stop(currentSound);
                    currentSound = null;
                }
            } else {
                // If no sound is active, start a new one
                if (currentSound == null || !client.getSoundManager().isPlaying(currentSound)) {

                    SoundEvent nextSound = switch (javaRandom.nextInt(3)) {
                        case 0 -> GravititeUpgrade.LEVITATE_0;
                        case 1 -> GravititeUpgrade.LEVITATE_1;
                        default -> GravititeUpgrade.LEVITATE_2;
                    };

                    // Play at player location
                    double x = client.player.getX();
                    double y = client.player.getY();
                    double z = client.player.getZ();

                    currentSound = new PositionedSoundInstance(
                            nextSound.getId(),
                            SoundCategory.PLAYERS,
                            1.0f, 1.0f, // Volume, Pitch
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