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
        if (player.getWorld().isClient) return;

        // 1. Calculate Max Capacity
        int pieces = 0;
        for (ItemStack stack : player.getArmorItems()) {
            if (stack.getItem().toString().toLowerCase().contains("gravitite")) {
                pieces++;
            }
        }
        float maxTime = pieces * 1.5f * 20;

        boolean isFlying = this.aether$isGravititeFlying();
        float timer = this.aether$getFlightTimer();

        // 2. State Management - GROUND
        if (player.isOnGround()) {
            // FORCE DISABLE FLIGHT if touching ground
            if (isFlying) {
                this.aether$setGravititeFlying(false);
                isFlying = false;
            }
            // Recharge
            if (timer < maxTime) {
                this.aether$setFlightTimer(timer + 1);
            }
        }
        // 3. State Management - AIR
        else if (isFlying) {
            // Deplete Logic: Only if moving horizontally or moving UP
            Vec3d vel = player.getVelocity();
            double horizontalSpeed = vel.horizontalLengthSquared();

            // Threshold 0.0001 handles "drift", ensure explicit movement or climbing
            if (horizontalSpeed > 0.0001 || vel.y > 0) {
                this.aether$setFlightTimer(timer - 1);
            }

            // Cut flight if empty
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