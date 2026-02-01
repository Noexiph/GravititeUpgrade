package noexiph.gravititeupgrade.mixin;

import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import noexiph.gravititeupgrade.IGravititeFlightAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class MixinPlayerFlight {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void overrideTravel(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof IGravititeFlightAccess access && access.aether$isGravititeFlying()) {

            // THIS IS KEY:
            // We act like creative flight for one tick
            player.getAbilities().flying = true;
            player.updateVelocity(0.05F, movementInput); // 0.05F is standard flight speed
            player.move(MovementType.SELF, player.getVelocity());
            player.setVelocity(player.getVelocity().multiply(0.9)); // Friction

            // IMPORTANT: We must NOT leave abilities.flying = true,
            // otherwise vanilla might kick us or act weird next tick.
            // However, setting it to false immediately might break the 'move' call above?
            // Actually, 'move' uses the velocity we just set.

            // If we are NOT in creative, revert the flag immediately after moving
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().flying = false;
            }

            ci.cancel(); // Prevent vanilla travel
        }
    }
}