package noexiph.gravititeupgrade;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GravititeUpgrade implements ModInitializer {
	public static final String MOD_ID = "gravititeupgrade";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
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

        LOGGER.info("Gravitite Upgrade Initialized!");
	}
}