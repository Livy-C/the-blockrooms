package name.blockrooms.compat;

import name.blockrooms.environment.BlockLevel2Temperature;
import name.blockrooms.util.ModLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import toughasnails.api.temperature.IPositionalTemperatureModifier;
import toughasnails.api.temperature.TemperatureHelper;
import toughasnails.api.temperature.TemperatureLevel;

/**
 * Tough As Nails（意志坚定）温度兼容。
 * <p>
 * 通过 TAN 官方的位置温度修改器接口接入：
 * <ul>
 *   <li><b>仅 BlockLevel 2 生效</b>：把 TAN 的环境温度等级替换为 BL2 温度系统的映射——
 *       37℃ 基础 → {@link TemperatureLevel#NEUTRAL}（人体舒适温度），
 *       40~43℃ → {@link TemperatureLevel#WARM}，超过 43℃ → {@link TemperatureLevel#HOT}；
 *       机器热场与封闭空间加成通过 {@link BlockLevel2Temperature#temperatureAt} 自动体现，
 *       玩家靠近机器时 TAN 体感温度随之升高；</li>
 *   <li><b>其他维度：返回原等级</b>，TAN 保持自身的正常温度逻辑，本模组不做任何干预。</li>
 * </ul>
 * 注册入口 {@link #register()} 仅在检测到 TAN 已加载时调用（见 {@code Blockrooms.commonSetup}），
 * 未安装 TAN 时本类不会被加载。
 */
public final class TanTemperatureCompat implements IPositionalTemperatureModifier {

    public static void register() {
        TemperatureHelper.registerPositionalTemperatureModifier(new TanTemperatureCompat());
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
}
