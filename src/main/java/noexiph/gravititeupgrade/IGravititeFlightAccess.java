package noexiph.gravititeupgrade;

public interface IGravititeFlightAccess {
    boolean aether$isGravititeFlying();
    void aether$setGravititeFlying(boolean flying);
    float aether$getFlightTimer(); // 0.0 to 6.0 (max)
    void aether$setFlightTimer(float time);
}