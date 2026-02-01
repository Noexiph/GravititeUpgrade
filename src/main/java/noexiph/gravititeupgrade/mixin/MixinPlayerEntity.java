package noexiph.gravititeupgrade.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
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

    // Define Synced Data Keys
    @Unique
    private static final TrackedData<Boolean> IS_GRAVITITE_FLYING = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique
    private static final TrackedData<Float> FLIGHT_TIMER = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.FLOAT);

    // Initialize the DataTracker
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    protected void initGravititeData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(IS_GRAVITITE_FLYING, false);
        builder.add(FLIGHT_TIMER, 0f);
    }

    // --- Interface Implementation (Using DataTracker now) ---

    @Override
    public boolean aether$isGravititeFlying() {
        return this.dataTracker.get(IS_GRAVITITE_FLYING);
    }

    @Override
    public void aether$setGravititeFlying(boolean flying) {
        this.dataTracker.set(IS_GRAVITITE_FLYING, flying);
    }

    @Override
    public float aether$getFlightTimer() {
        return this.dataTracker.get(FLIGHT_TIMER);
    }

    @Override
    public void aether$setFlightTimer(float time) {
        this.dataTracker.set(FLIGHT_TIMER, time);
    }

    // --- Logic Injection ---

    @Inject(method = "tick", at = @At("HEAD"))
    private void manageGravititeFlight(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Only run logic on Server (Client gets updates via DataTracker)
        if (player.getWorld().isClient) return;

        // 1. Calculate Max Capacity (1.5s per piece)
        int pieces = 0;
        for (ItemStack stack : player.getArmorItems()) {
            if (stack.getItem().toString().toLowerCase().contains("gravitite")) {
                pieces++;
            }
        }
        float maxTime = pieces * 1.5f * 20;

        boolean isFlying = this.aether$isGravititeFlying();
        float timer = this.aether$getFlightTimer();

        // 2. State Management
        if (player.isOnGround()) {
            // Landed: Disable flight and Recharge
            if (isFlying) {
                this.aether$setGravititeFlying(false);
                isFlying = false;
            }

            if (timer < maxTime) {
                this.aether$setFlightTimer(timer + 1);
            }
        } else if (isFlying) {
            // In Air & Flying Mode

            // 3. Deplete Logic (FIXED)
            // Only deplete if player is actually inputting movement (Horizontal) or rising (Jumping/Flying Up)
            // We ignore negative Y velocity (falling) so hovering in place doesn't waste much fuel
            // (or you can decide hovering wastes fuel, but falling shouldn't).

            Vec3d vel = player.getVelocity();
            double horizontalSpeed = vel.horizontalLengthSquared(); // x^2 + z^2

            // If moving horizontally OR moving up (climbing)
            if (horizontalSpeed > 0.0001 || vel.y > 0) {
                this.aether$setFlightTimer(timer - 1);
            }

            // Safety: Cut flight if out of fuel
            if (timer <= 0) {
                this.aether$setGravititeFlying(false);
            }
        }
    }

    // Prevent Fall Damage
    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void cancelFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (this.aether$getFlightTimer() > 0 || this.aether$isGravititeFlying()) {
            cir.setReturnValue(false);
        }
    }
}