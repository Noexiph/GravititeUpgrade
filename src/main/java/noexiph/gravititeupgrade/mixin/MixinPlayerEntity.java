package noexiph.gravititeupgrade.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import noexiph.gravititeupgrade.IGravititeFlightAccess;
import noexiph.gravititeupgrade.registry.GravititeGameRules; // Import the rules
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

    @Unique
    private static final float VANILLA_FLIGHT_SPEED = 0.05f;

    @Unique
    private static final TrackedData<Boolean> IS_GRAVITITE_FLYING = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique
    private static final TrackedData<Float> FLIGHT_TIMER = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.FLOAT);
    @Unique
    private static final TrackedData<Integer> FLIGHT_SPEED_PERCENT = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique
    private Vec3d aether$lastPos = Vec3d.ZERO;

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    protected void initGravititeData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(IS_GRAVITITE_FLYING, false);
        builder.add(FLIGHT_TIMER, 0f);
        builder.add(FLIGHT_SPEED_PERCENT, 50);
    }

    // --- NBT SAVING & LOADING (Updated) ---

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void writeGravititeNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putFloat("GravititeFlightTimer", this.aether$getFlightTimer());
        // Save the flying state so we don't fall on login
        nbt.putBoolean("GravititeIsFlying", this.aether$isGravititeFlying());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void readGravititeNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("GravititeFlightTimer")) {
            this.aether$setFlightTimer(nbt.getFloat("GravititeFlightTimer"));
        }
        if (nbt.contains("GravititeIsFlying")) {
            // Restore the flying state
            this.aether$setGravititeFlying(nbt.getBoolean("GravititeIsFlying"));
        }
    }

    @Override
    public boolean aether$isGravititeFlying() { return this.dataTracker.get(IS_GRAVITITE_FLYING); }

    @Override
    public void aether$setGravititeFlying(boolean flying) { this.dataTracker.set(IS_GRAVITITE_FLYING, flying); }

    @Override
    public float aether$getFlightTimer() { return this.dataTracker.get(FLIGHT_TIMER); }

    @Override
    public void aether$setFlightTimer(float time) { this.dataTracker.set(FLIGHT_TIMER, time); }

    @Inject(method = "tick", at = @At("TAIL"))
    private void manageGravititeFlight(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // --- LOGIC SPLIT START ---

        // 1. SERVER: Read GameRule and Sync to Client via DataTracker
        if (!player.getWorld().isClient) {
            int actualRuleValue = player.getWorld().getGameRules().getInt(GravititeGameRules.GRAVITITE_FLIGHT_SPEED_PERCENT);

            // Only update if different to prevent packet spam
            if (player.getDataTracker().get(FLIGHT_SPEED_PERCENT) != actualRuleValue) {
                player.getDataTracker().set(FLIGHT_SPEED_PERCENT, actualRuleValue);
            }
        }

        // 2. BOTH SIDES: Read from DataTracker (Client now knows the correct value!)
        int syncedSpeedPercent = player.getDataTracker().get(FLIGHT_SPEED_PERCENT);
        float customSpeed = VANILLA_FLIGHT_SPEED * (syncedSpeedPercent / 100.0f);

        // --- LOGIC SPLIT END ---

        // 1. Vanilla Bridge
        if (this.aether$isGravititeFlying()) {
            if (!player.getAbilities().allowFlying) player.getAbilities().allowFlying = true;
            player.getAbilities().flying = true;

            // Now both Client and Server agree on 'customSpeed'
            player.getAbilities().setFlySpeed(customSpeed);

        } else {
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().allowFlying = false;
                player.getAbilities().flying = false;
                player.getAbilities().setFlySpeed(VANILLA_FLIGHT_SPEED);
            }
        }

        if (player.getWorld().isClient) return;

        if (this.aether$lastPos.equals(Vec3d.ZERO)) {
            this.aether$lastPos = player.getPos();
        }

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
            double distSq = player.getPos().squaredDistanceTo(this.aether$lastPos);
            if (distSq > 0.0001) {
                this.aether$setFlightTimer(timer - 1);
            }
            if (timer <= 0) {
                this.aether$setGravititeFlying(false);
            }
        }
        this.aether$lastPos = player.getPos();
    }

    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void cancelFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (this.aether$getFlightTimer() > 0 || this.aether$isGravititeFlying()) {
            cir.setReturnValue(false);
        }
    }
}