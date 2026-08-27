package name.blockrooms.compat;

import name.blockrooms.Blockrooms;
import name.blockrooms.environment.BlockLevel2Temperature;
import name.blockrooms.item.HeatInsulationArmor;
import name.blockrooms.util.ModLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import toughasnails.api.temperature.IPlayerTemperatureModifier;
import toughasnails.api.temperature.IPositionalTemperatureModifier;
import toughasnails.api.temperature.ITemperature;
import toughasnails.api.temperature.TemperatureHelper;
import toughasnails.api.temperature.TemperatureLevel;


public final class TanTemperatureCompat implements IPositionalTemperatureModifier, IPlayerTemperatureModifier {
    private static final int GRADIENT_INTERVAL = 10;

    public static void register() {
        TanTemperatureCompat compat = new TanTemperatureCompat();
        TemperatureHelper.registerPositionalTemperatureModifier(compat);
        TemperatureHelper.registerPlayerTemperatureModifier(compat);
        Blockrooms.LOGGER.info("TAN: temperature compat registered");
    }

    @Override
    public TemperatureLevel modify(Level level, BlockPos pos, TemperatureLevel original) {
        if (level.isClientSide() || !level.dimension().equals(ModLevels.BLOCKLEVEL_2)) {
            return original;
        }
        float t = BlockLevel2Temperature.temperatureAt(level, pos);
        if (t <= 40.0F) {
            return TemperatureLevel.NEUTRAL;
        }
        if (t <= 43.0F) {
            return TemperatureLevel.WARM;
        }
        return TemperatureLevel.HOT;
    }

    @Override
    public TemperatureLevel modify(Player player, TemperatureLevel current) {
        int worn = HeatInsulationArmor.wornCount(player);
        if (worn == 0) {
            return current;
        }
        TemperatureLevel target = current;
        for (int i = 0; i < worn; i++) {
            if (target.ordinal() > TemperatureLevel.NEUTRAL.ordinal()) {
                target = target.decrement(1);
            } else if (target.ordinal() < TemperatureLevel.NEUTRAL.ordinal()) {
                target = target.increment(1);
            } else {
                break;
            }
        }
        if (!player.level().isClientSide() && target != current && player.tickCount % GRADIENT_INTERVAL == 0) {
            ITemperature data = TemperatureHelper.getTemperatureData(player);
            TemperatureLevel level = data.getLevel();
            if (level.ordinal() > target.ordinal()) {
                data.setLevel(level.decrement(1));
            } else if (level.ordinal() < target.ordinal()) {
                data.setLevel(level.increment(1));
            }
        }
        return target;
    }
}