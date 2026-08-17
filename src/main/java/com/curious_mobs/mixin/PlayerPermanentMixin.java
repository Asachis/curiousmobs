package com.curious_mobs.mixin;

import com.curious_mobs.api.PermanentFlagAccessor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把"永久解除诅咒"标记放进 SynchedEntityData，使其自动同步到客户端。
 * <p>
 * 背景：FCA 的诅咒事件（如 Trickery.buff 的 MobEffectEvent.Added）在服务端和
 * 客户端都会触发。服务端玩家的 persistentData 有 curious_mobs:permanent，
 * 而 Forge 不会把 persistentData 同步到客户端，因此客户端本地玩家的效果仍会
 * 被 FCA 缩短/延长（HUD 与实际时长不符）。改用实体同步数据后两端可见。
 */
@Mixin(Player.class)
public abstract class PlayerPermanentMixin implements PermanentFlagAccessor {

  @Unique
  private static final EntityDataAccessor<Boolean> CURIOUS_MOBS_PERMANENT =
      SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);

  @Inject(method = "defineSynchedData", at = @At("TAIL"))
  private void curious_mobs$definePermanentFlag(CallbackInfo ci) {
    ((Entity) (Object) this).getEntityData().define(CURIOUS_MOBS_PERMANENT, false);
  }

  @Override
  public boolean curiousMobs$isPermanent() {
    return ((Entity) (Object) this).getEntityData().get(CURIOUS_MOBS_PERMANENT);
  }

  @Override
  public void curiousMobs$setPermanent(boolean value) {
    ((Entity) (Object) this).getEntityData().set(CURIOUS_MOBS_PERMANENT, value);
  }
}
