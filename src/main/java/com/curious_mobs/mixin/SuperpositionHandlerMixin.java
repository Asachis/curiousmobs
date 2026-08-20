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
 * 永久解除（解耦设计）：
 * <p>
 * 转移诅咒后玩家仍佩戴七咒之戒，但不再承受被动诅咒。
 * EL 的两类判定在此分开处理：
 * <ul>
 *   <li>{@code isTheCursedOne}：身份判定（天体果实等物品使用门槛、武器加成、
 *       击杀掉落加成、守护之心保护、世界诅咒标记等）→ 永久玩家返回
 *       <b>true</b>，恢复 EL 原版行为，确保需要"承受七咒之人"的物品仍可用；</li>
 *   <li>{@code hasCurio(entity, CURSED_RING)}：被动负面逻辑的唯一通用入口
 *       （永久着火 / 无法入睡 / 击退减益 / 受击翻倍 / 药水效果等，见
 *       EnigmaticEventHandlerMixin）→ 永久玩家返回 <b>false</b>，单点否决。</li>
 * </ul>
 * 唯一的 isTheCursedOne 负面调用点（onEntityHurt 的怪物伤害减益）由
 * EnigmaticEventHandlerMixin 的 ordinal 定向 @Redirect 单独压制。
 */
@Mixin(value = SuperpositionHandler.class, remap = false)
public class SuperpositionHandlerMixin {

  @Inject(method = "isTheCursedOne",
      at = @At("HEAD"),
      cancellable = true,
      remap = false)
  private static void curious_mobs$permanentlyFreedKeepsIdentity(
      Player player, CallbackInfoReturnable<Boolean> cir) {
    if (CurseHelper.isPermanent(player)) {
      cir.setReturnValue(true);
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