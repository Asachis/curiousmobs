package com.curious_mobs.mixin;

import com.aizistral.enigmaticlegacy.handlers.SuperpositionHandler;
import com.aizistral.enigmaticlegacy.registries.EnigmaticItems;
import com.curious_mobs.curse.CurseHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 永久解除：当玩家带有 curious_mobs:permanent 标记时，
 * 让 EL 认为该玩家不再"被诅咒"，从而关闭其全部被动诅咒逻辑
 * （中立生物主动攻击、经验减少、掉落降低等），但饰品仍佩戴在身上。
 * <p>
 * hasCurio 是 EL 全部诅咒判定的唯一通用入口（七咒之戒已佩戴、幻翼生成
 * handler 等），在此单点否决，与 EnigmaticEventHandlerMixin /
 * PhantomSpawnerMixin 的入口级过滤器互为冗余加固，行为一致。
 */
@Mixin(value = SuperpositionHandler.class, remap = false)
public class SuperpositionHandlerMixin {

  @Inject(method = "isTheCursedOne",
      at = @At("HEAD"),
      cancellable = true,
      remap = false)
  private static void curious_mobs$permanentlyFreedIsNotCursed(
      Player player, CallbackInfoReturnable<Boolean> cir) {
    if (CurseHelper.isPermanent(player)) {
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "hasCurio",
      at = @At("HEAD"),
      cancellable = true,
      remap = false)
  private static void curious_mobs$permanentlyFreedHasNoCurseCurio(
      LivingEntity entity, Item curio, CallbackInfoReturnable<Boolean> cir) {
    if (entity instanceof Player player && curio == EnigmaticItems.CURSED_RING
        && CurseHelper.isPermanent(player)) {
      cir.setReturnValue(false);
    }
  }
}