package name.blockrooms.environment;

import name.blockrooms.item.HeatInsulationArmor;
import name.blockrooms.world.generator.BlockLevel2Generator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * BlockLevel 2「隧道」温度系统。
 * <ul>
 *   <li>基础温度 <b>37℃</b>（舒适，接近人体温度）；</li>
 *   <li>红热铁块机器（"机器"，见 {@link BlockLevel2Generator#isHeatMachineChunk}）半径
 *       {@link #MACHINE_RADIUS} 格内线性升温：边缘 +4℃（41℃）→ 中心 +11℃（48℃）；</li>
 *   <li>封闭空间（死胡同/小房间，水平 4 方向中 ≥3 方向 3 格内被堵）再 +{@link #ENCLOSED_BONUS}℃
 *       （约 45℃）；</li>
 *   <li>温度超过 {@link #DAMAGE_THRESHOLD}（40℃）玩家持续流失生命，伤害间隔由温度决定
 *       （越热越频繁，见 {@link #damageInterval}）；</li>
 *   <li>隔热套装按件减免伤害（每件 {@link #PROTECTION_PER_PIECE}），穿满 4 件完全免疫。</li>
 * </ul>
 * 全部行为由 {@link Predicate}（判定）与 {@link Consumer}（执行）组成，可自由组合替换。
 */
public final class BlockLevel2Temperature {
    /** 基础温度（℃） */
    public static final float BASE_TEMPERATURE = 37.0F;
    /** 掉血阈值（℃）：温度超过该值开始持续流失生命 */
    public static final float DAMAGE_THRESHOLD = 40.0F;
    /** 封闭空间温度加成（℃）：37 + 8 = 45 */
    public static final float ENCLOSED_BONUS = 8.0F;
    /** 机器热场半径（格） */
    public static final float MACHINE_RADIUS = 400.0F;
    /** 机器热场边缘升温（℃）：边缘 = 37 + 4 = 41，刚过阈值 */
    public static final float MACHINE_EDGE_OVERHEAT = 4.0F;
    /** 机器热场中心升温（℃）：中心 = 37 + 11 = 48 */
    public static final float MACHINE_PEAK_OVERHEAT = 11.0F;
    /** 每次造成的伤害（1 = 半颗心） */
    public static final float DAMAGE_PER_HIT = 1.0F;
    /** 每穿一件隔热装备减免的伤害比例 */
    public static final float PROTECTION_PER_PIECE = 0.25F;
    /** 伤害间隔上限（tick）：40℃ 时约 3 秒一次 */
    public static final int MAX_INTERVAL = 60;
    /** 伤害间隔下限（tick）：55℃ 及以上时 0.5 秒一次 */
    public static final int MIN_INTERVAL = 10;

    /** 判定：可以被环境灼伤（排除创造/旁观/无敌/死亡） */
    public static final Predicate<ServerPlayer> CAN_BE_HURT = player ->
            !player.isCreative() && !player.isSpectator() && !player.isDeadOrDying() && !player.isInvulnerable();

    /** 判定：当前环境温度高于 40℃ */
    public static final Predicate<ServerPlayer> IS_OVERHEATED = player ->
            temperatureAt(player.level(), player.blockPosition()) > DAMAGE_THRESHOLD;

    /** 执行：按隔热装备件数减免后施加灼热伤害，并在玩家眼前播撒烫伤粒子 */
    public static final Consumer<ServerPlayer> APPLY_HEAT_DAMAGE = player -> {
        int worn = HeatInsulationArmor.wornCount(player);
        float multiplier = Math.max(0.0F, 1.0F - PROTECTION_PER_PIECE * worn);
        if (multiplier <= 0.0F || !(player.level() instanceof ServerLevel sl)) {
            return;
        }
        player.hurtServer(sl, player.damageSources().onFire(), DAMAGE_PER_HIT * multiplier);
        Vec3 eye = player.getEyePosition();
        sl.sendParticles(new DustParticleOptions(0xFFFF5533, 1.0F),
                eye.x, eye.y, eye.z, 6, 0.25, 0.25, 0.25, 0.0);
    };

    /**
     * 环境温度（℃）= 37 基础 + 机器热场 + 封闭空间加成。
     * 机器位置由区块哈希确定性重算（与生成一致），无需记录生成点。
     */
    public static float temperatureAt(Level level, BlockPos pos) {
        float t = BASE_TEMPERATURE + machineHeat(level, pos);
        if (isEnclosed(level, pos)) {
            t += ENCLOSED_BONUS;
        }
        return t;
    }

    /** 机器热场：半径 400 格内线性升温，边缘 +4℃、中心 +11℃；范围外 0 */
    private static float machineHeat(Level level, BlockPos pos) {
        BlockPos machine = nearestMachine(level, pos);
        if (machine == null) {
            return 0.0F;
        }
        double d = Math.hypot(pos.getX() - machine.getX(), pos.getZ() - machine.getZ());
        if (d >= MACHINE_RADIUS) {
            return 0.0F;
        }
        return MACHINE_EDGE_OVERHEAT + (MACHINE_PEAK_OVERHEAT - MACHINE_EDGE_OVERHEAT) * (float) (1.0 - d / MACHINE_RADIUS);
    }

    /** 距 pos 最近的机器中心：扫描玩家周围 25 区块半径内的机器区块（每次调用重算，无状态） */
    private static BlockPos nearestMachine(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) {
            return null;
        }
        long seed = sl.getSeed();
        int cx0 = pos.getX() >> 4;
        int cz0 = pos.getZ() >> 4;
        int r = (int) Math.ceil(MACHINE_RADIUS / 16.0);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int cx = cx0 - r; cx <= cx0 + r; cx++) {
            for (int cz = cz0 - r; cz <= cz0 + r; cz++) {
                if (!BlockLevel2Generator.isHeatMachineChunk(seed, cx, cz)) {
                    continue;
                }
                BlockPos m = BlockLevel2Generator.heatMachineCenter(cx, cz);
                double d = Math.hypot(pos.getX() - m.getX(), pos.getZ() - m.getZ());
                if (d < bestD) {
                    bestD = d;
                    best = m;
                }
            }
        }
        return best;
    }

    /**
     * 封闭空间判定：玩家所在层水平 4 个方向中，≥3 个方向在 3 格内被实体方块堵住
     * （死胡同、小房间）；走廊/十字路口/大房间不满足。
     */
    public static boolean isEnclosed(Level level, BlockPos pos) {
        int blocked = 0;
        if (directionBlocked(level, pos, 1, 0)) blocked++;
        if (directionBlocked(level, pos, -1, 0)) blocked++;
        if (directionBlocked(level, pos, 0, 1)) blocked++;
        if (directionBlocked(level, pos, 0, -1)) blocked++;
        return blocked >= 3;
    }

    /** 该水平方向 3 格内是否存在非空气方块（视为被堵） */
    private static boolean directionBlocked(Level level, BlockPos pos, int dx, int dz) {
        for (int i = 1; i <= 3; i++) {
            if (!level.getBlockState(pos.offset(dx * i, 0, dz * i)).isAir()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 伤害间隔（tick）：温度越高越频繁。
     * 40℃ → 60 tick（3 秒一次）；45℃ → 35 tick；48℃ → 20 tick（1 秒一次）；≥55℃ → 10 tick（0.5 秒一次）。
     */
    public static int damageInterval(float temperature) {
        if (temperature <= DAMAGE_THRESHOLD) {
            return Integer.MAX_VALUE;
        }
        int interval = MAX_INTERVAL - (int) Math.round((temperature - DAMAGE_THRESHOLD) * 5.0F);
        return Math.max(MIN_INTERVAL, Math.min(MAX_INTERVAL, interval));
    }

    /** 每个游戏刻调用：按当前温度取间隔，先由 Predicate 判定、命中后交给 Consumer 执行 */
    public static void onPlayerTick(ServerPlayer player) {
        float temperature = temperatureAt(player.level(), player.blockPosition());
        int interval = damageInterval(temperature);
        if (player.tickCount % interval != 0) {
            return;
        }
        if (CAN_BE_HURT.and(IS_OVERHEATED).test(player)) {
            APPLY_HEAT_DAMAGE.accept(player);
        }
    }

    private BlockLevel2Temperature() {
    }
}
