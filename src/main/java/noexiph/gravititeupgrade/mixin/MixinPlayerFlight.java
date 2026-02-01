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
        if (((IGravititeFlightAccess)player).aether$isGravititeFlying()) {

            // Allow creative-style flight movement
            player.getAbilities().flying = true;

            // Important: We only want this *physics*, not actual creative mode
            // You might need to manually apply velocity here to simulate the "Hover"
            // exactly like Origin Realms if creative flight feels too floaty.

            // Standard creative flight simulation:
            player.updateVelocity(0.05F, movementInput);
            player.move(MovementType.SELF, player.getVelocity());
            player.setVelocity(player.getVelocity().multiply(0.9));

            ci.cancel(); // Cancel vanilla travel logic
        } else {
            // Ensure we reset abilities if we stop flying in survival
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().flying = false;
            }
        }
    }
}