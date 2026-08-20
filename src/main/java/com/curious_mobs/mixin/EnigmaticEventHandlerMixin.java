package com.curious_mobs.mixin;

import com.aizistral.enigmaticlegacy.handlers.EnigmaticEventHandler;
import com.aizistral.enigmaticlegacy.handlers.SuperpositionHandler;
import com.aizistral.enigmaticlegacy.registries.EnigmaticItems;
import com.curious_mobs.curse.CurseHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 永久解除：EL 的 EnigmaticEventHandler 中有多处直接调用
 * hasCurio(entity, CURSED_RING) 的负面逻辑，绕过了 isTheCursedOne 的混入，
 * 导致「永久着火 / 无法入睡 / 击退减益 / 受击翻倍」在转移后仍然生效。
 * <p>
 * 这里仅在有永久解除标记的玩家上、且查询的正是七咒之戒时返回 false；
 * 其余调用（灾厄护符、残响戒指、狂暴徽章等）以及正面逻辑原样放行，不误伤。
 * <p>
 * onEntityHurt 内共 4 处 isTheCursedOne 调用：前 3 处是正面武器加成
 * （The Twist / EnderSlayer 等，随 SuperpositionHandlerMixin 恢复为 true），
 * 第 4 处（ordinal=3）是 CursedRing 的怪物伤害减益（负面），在此定向
 * 重定向为 false，permanent 玩家不再吃到该减益。
 * <p>
 * 另三处 hasCurio(CURSED_RING) 属于戒指的正面能力，同样被 hasCurio 的
 * 全局过滤误伤，这里逐一定向恢复为 true（onHarvestCheck 无视采掘等级、
 * onExperienceDrop 击杀经验加成、onProbableDeath 死亡保留戒指）。
 */
@Mixin(value = EnigmaticEventHandler.class, remap = false)
public class EnigmaticEventHandlerMixin {

  @Redirect(method = "onPlayerTick",
      at = @At(value = "INVOKE",
          target = "Lcom/aizistral/enigmaticlegacy/handlers/SuperpositionHandler;hasCurio" +
              "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/Item;)Z",
          remap = false),
      remap = false)
  private boolean curious_mobs$hasCurioTick(LivingEntity entity, Item curio) {
    return curious_mobs$filterHasCurio(entity, curio);
  }

  @Redirect(method = "onLivingKnockback",
      at = @At(value = "INVOKE",
          target = "Lcom/aizistral/enigmaticlegacy/handlers/SuperpositionHandler;hasCurio" +
              "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/Item;)Z",
          remap = false),
      remap = false)
  private boolean curious_mobs$hasCurioKnockback(LivingEntity entity, Item curio) {
    return curious_mobs$filterHasCurio(entity, curio);
  }

  @Redirect(method = "onEntityHurt",
      at = @At(value = "INVOKE",
          target = "Lcom/aizistral/enigmaticlegacy/handlers/SuperpositionHandler;hasCurio" +
              "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/Item;)Z",
          remap = false),
      remap = false)
  private boolean curious_mobs$hasCurioHurt(LivingEntity entity, Item curio) {
    return curious_mobs$filterHasCurio(entity, curio);
  }

  @Redirect(method = "onEntityHurt",
      at = @At(value = "INVOKE",
          target = "Lcom/aizistral/enigmaticlegacy/handlers/SuperpositionHandler;isTheCursedOne" +
              "(Lnet/minecraft/world/entity/player/Player;)Z",
          ordinal = 3,
          remap = false),
      remap = false)
  private boolean curious_mobs$isTheCursedOneHurt(Player player) {
    if (CurseHelper.isPermanent(player)) {
      return false;
    }
    return SuperpositionHandler.isTheCursedOne(player);
  }

  @Redirect(method = "onHarvestCheck",
      at = @At(value = "INVOKE",
          target = "Lcom/aizistral/enigmaticlegacy/handlers/SuperpositionHandler;hasCurio" +
              "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/Item;)Z",
          ordinal = 0,
          remap = false),
      remap = false)
  private boolean curious_mobs$hasCurioHarvest(LivingEntity entity, Item curio) {
    return curious_mobs$keepCurseCurio(entity, curio);
  }

  @Redirect(method = "onExperienceDrop",
      at = @At(value = "INVOKE",
          target = "Lcom/aizistral/enigmaticlegacy/handlers/SuperpositionHandler;hasCurio" +
              "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/Item;)Z",
          ordinal = 1,
          remap = false),
      remap = false)
  private boolean curious_mobs$hasCurioExp(LivingEntity entity, Item curio) {
    return curious_mobs$keepCurseCurio(entity, curio);
  }

  @Redirect(method = "onProbableDeath",
      at = @At(value = "INVOKE",
          target = "Lcom/aizistral/enigmaticlegacy/handlers/SuperpositionHandler;hasCurio" +
              "(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/Item;)Z",
          ordinal = 0,
          remap = false),
      remap = false)
  private boolean curious_mobs$hasCurioDeath(LivingEntity entity, Item curio) {
    return curious_mobs$keepCurseCurio(entity, curio);
  }

  private static boolean curious_mobs$filterHasCurio(LivingEntity entity, Item curio) {
    if (entity instanceof Player player && curio == EnigmaticItems.CURSED_RING
        && CurseHelper.isPermanent(player)) {
      return false;
    }
    return SuperpositionHandler.hasCurio(entity, curio);
  }

  private static boolean curious_mobs$keepCurseCurio(LivingEntity entity, Item curio) {
    if (entity instanceof Player player && curio == EnigmaticItems.CURSED_RING
        && CurseHelper.isPermanent(player)) {
      return true;
    }
    return SuperpositionHandler.hasCurio(entity, curio);
  }
}