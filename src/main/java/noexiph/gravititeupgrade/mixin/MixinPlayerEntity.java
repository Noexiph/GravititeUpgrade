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

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    protected void initGravititeData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(IS_GRAVITITE_FLYING, false);
        builder.add(FLIGHT_TIMER, 0f);
    }

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

    @Inject(method = "tick", at = @At("HEAD"))
    private void manageGravititeFlight(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // --- VANILLA FLIGHT BRIDGE ---
        // This connects our custom variable to the actual Vanilla flight mechanics
        if (this.aether$isGravititeFlying()) {
            if (!player.getAbilities().allowFlying) {
                player.getAbilities().allowFlying = true;
            }
            player.getAbilities().flying = true;
        } else {
            // Reset to survival defaults if we aren't actually in creative
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().allowFlying = false;
                player.getAbilities().flying = false;
            }
        }

        // --- LOGIC (Server Only) ---
        if (player.getWorld().isClient) return;

        int pieces = 0;
        for (ItemStack stack : player.getArmorItems()) {
            if (stack.getItem().toString().toLowerCase().contains("gravitite")) {
                pieces++;
            }
        }
        float maxTime = pieces * 1.5f * 20;

        boolean isFlying = this.aether$isGravititeFlying();
        float timer = this.aether$getFlightTimer();

        if (player.isOnGround()) {
            if (isFlying) {
                this.aether$setGravititeFlying(false);
                isFlying = false;
            }
            if (timer < maxTime) {
                this.aether$setFlightTimer(timer + 1);
            }
        } else if (isFlying) {
            // Deplete Logic: Only if moving horizontally or moving UP
            Vec3d vel = player.getVelocity();
            double horizontalSpeed = vel.horizontalLengthSquared();

            if (horizontalSpeed > 0.0001 || vel.y > 0) {
                this.aether$setFlightTimer(timer - 1);
            }

            if (timer <= 0) {
                this.aether$setGravititeFlying(false);
            }
        }
    }

    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void cancelFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (this.aether$getFlightTimer() > 0 || this.aether$isGravititeFlying()) {
            cir.setReturnValue(false);
        }
    }
}