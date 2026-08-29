package name.blockrooms.entity.secret;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * 玩家傀儡：保留玩家外观（渲染用玩家模型），
 * 但已被"块室意志"同化，主动攻击其他玩家。
 * 血量 = 被转化玩家最大生命值的 2 倍，攻击 20。
 */
public class PlayerPuppet extends Player {

    private Player target;

    /** EntityType 工厂用构造器 */
    public PlayerPuppet(EntityType<? extends PlayerPuppet> type, Level level) {
        super(level, new GameProfile(UUID.randomUUID(), "PlayerPuppet"));
    }

    public PlayerPuppet(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        // 寻找目标：攻击最近的其他玩家
        if (this.target == null || !this.target.isAlive() || this.target.isSpectator()) {
            this.target = null;
            if (this.level() instanceof ServerLevel serverLevel) {
                Player nearest = serverLevel.getNearestPlayer(this, 48.0);
                if (nearest != null && nearest != this) this.target = nearest;
            }
        }
        if (this.target != null && this.distanceToSqr(this.target) < 2.5 * 2.5 && this.tickCount % 20 == 0) {
            this.target.hurt(this.damageSources().mobAttack(this), 20.0F);
        }
    }

    /** 转化：把源玩家生命值翻倍 */
    public void convertFrom(Player source) {
        this.setHealth(source.getMaxHealth() * 2.0F);
    }

    @Override
    public @Nullable GameType gameMode() {
        return GameType.SURVIVAL;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }
}
