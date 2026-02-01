package noexiph.gravititeupgrade.registry;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.GameRules;

public class GravititeGameRules {
    public static final GameRules.Key<GameRules.IntRule> GRAVITITE_FLIGHT_SPEED_PERCENT =
            GameRuleRegistry.register("gravititeFlightSpeedPercent", GameRules.Category.PLAYER, GameRuleFactory.createIntRule(50));

    public static void initialize() {
    }
}
