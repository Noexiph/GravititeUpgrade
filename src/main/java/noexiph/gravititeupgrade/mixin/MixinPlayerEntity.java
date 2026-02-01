package noexiph.gravititeupgrade.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import noexiph.gravititeupgrade.IGravititeFlightAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity implements IGravititeFlightAccess {

    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    // Unique variables to store our custom data
    @Unique private boolean isGravititeFlying = false;
    @Unique private float flightTimer = 0f;

    // --- 1. Interface Implementation (THIS WAS MISSING) ---

    @Override
    public boolean aether$isGravititeFlying() {
        return this.isGravititeFlying;
    }

    @Override
    public void aether$setGravititeFlying(boolean flying) {
        this.isGravititeFlying = flying;
    }

    @Override
    public float aether$getFlightTimer() {
        return this.flightTimer;
    }

    @Override
    public void aether$setFlightTimer(float time) {
        this.flightTimer = time;
    }

    // --- 2. Logic Injection ---

    @Inject(method = "tick", at = @At("HEAD"))
    private void manageGravititeFlight(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient) return; // Run logic mostly on server, sync via packets if needed (or run both sides for prediction)

        // Calculate Max Capacity based on Armor (1.5s per piece * 20 ticks)
        int pieces = 0;
        for (ItemStack stack : player.getArmorItems()) {
            if (stack.getItem().toString().toLowerCase().contains("gravitite")) {
                pieces++;
            }
        }
        float maxTime = pieces * 1.5f * 20;

        // Recharge Logic (On Ground)
        if (player.isOnGround()) {
            this.isGravititeFlying = false; // Landed
            if (this.flightTimer < maxTime) {
                this.flightTimer += 1; // Recharge speed
            }
        }

        // Deplete Logic (Flying)
        if (this.isGravititeFlying && !player.isOnGround()) {
            // Deplete only if moving horizontally or vertically up
            if (player.getVelocity().lengthSquared() > 0.005) {
                this.flightTimer--;
            }

            if (this.flightTimer <= 0) {
                this.isGravititeFlying = false; // Out of fuel
            }
        }
    }

    // Prevent Fall Damage if timer > 0 or recently flying
    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void cancelFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        // Simple check: if we have "fuel", we can cushion the fall
        if (this.flightTimer > 0 || this.isGravititeFlying) {
            cir.setReturnValue(false);
        }
    }
}