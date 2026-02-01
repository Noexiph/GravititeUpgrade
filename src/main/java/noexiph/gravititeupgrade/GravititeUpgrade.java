package noexiph.gravititeupgrade;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import noexiph.gravititeupgrade.registry.GravititeGameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GravititeUpgrade implements ModInitializer {
	public static final String MOD_ID = "gravititeupgrade";

    public static final Identifier ID_LEV_0 = Identifier.of("gravititeupgrade", "levitate0");
    public static final Identifier ID_LEV_1 = Identifier.of("gravititeupgrade", "levitate1");
    public static final Identifier ID_LEV_2 = Identifier.of("gravititeupgrade", "levitate2");

    public static final SoundEvent LEVITATE_0 = SoundEvent.of(ID_LEV_0);
    public static final SoundEvent LEVITATE_1 = SoundEvent.of(ID_LEV_1);
    public static final SoundEvent LEVITATE_2 = SoundEvent.of(ID_LEV_2);

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        GravititeGameRules.initialize();

        // 1. Register the Payload Type (Common for both sides)
        PayloadTypeRegistry.playC2S().register(GravititePayload.ID, GravititePayload.CODEC);

        // 2. Register the Server Receiver (Logic when packet arrives)
        ServerPlayNetworking.registerGlobalReceiver(GravititePayload.ID, (payload, context) -> {
            // Execute on the main server thread
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();

                // Check if player has our interface capability
                if (player instanceof IGravititeFlightAccess access) {
                    boolean isCurrentlyFlying = access.aether$isGravititeFlying();

                    // Toggle state
                    boolean newState = !isCurrentlyFlying;

                    // Verification: Only allow enabling flight if they have fuel (timer > 0)
                    if (newState && access.aether$getFlightTimer() <= 0) {
                        return; // Fail silently or send message
                    }

                    access.aether$setGravititeFlying(newState);
                }
            });
        });

        Registry.register(Registries.SOUND_EVENT, ID_LEV_0, LEVITATE_0);
        Registry.register(Registries.SOUND_EVENT, ID_LEV_1, LEVITATE_1);
        Registry.register(Registries.SOUND_EVENT, ID_LEV_2, LEVITATE_2);

        LOGGER.info("Gravitite Upgrade Initialized!");
	}
}