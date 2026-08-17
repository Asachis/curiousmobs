package com.curious_mobs.mixin;

import com.curious_mobs.util.MobControllerCompat;
import com.inolia_zaicek.mine_fargo.Util.MyGoUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 万物皆驯（mob_controller）受控生物佩戴魂石（MineFargo）时，令其 AOE 伤害与
 * 范围负面效果不波及以下目标（无论魂石配置文件如何设置）：
 * <ul>
 *   <li>友善生物：非 {@link Enemy} 的一切生物（猪/牛/羊/村民/铁傀儡等被动、中立生物）；</li>
 *   <li>控制者玩家本人（未取到控制者 UUID 时保护任意玩家，失败即安全方向）；</li>
 *   <li>万物皆驯友军 {@link MobControllerCompat#isAlly}：控制者本人、原版驯服宠物、
 *       同控制者的其他受控生物、骑乘友方乘客；</li>
 *   <li>原版 {@link OwnableEntity} 宠物，且其主人正是控制者玩家。</li>
 * </ul>
 * 拦截点覆盖魂石全部三处伤害/负面效果咽喉：
 * <ul>
 *   <li>{@code MyGoUtil.canAttack}：绝大多数 AOE 伤害（磁性/混沌/危险/雪女王/燃烧印记/灵光弱化等）
 *       与负面效果（减速/虚弱/燃烧）的唯一筛选口；</li>
 *   <li>{@code MyGoUtil.mobList}：范围取怪的最上游入口，用于覆盖不走 canAttack 的直伤
 *       （如毁灭敌意连锁击杀）；过滤时始终保留穿戴者自身，避免破坏“禁域/九头蛇”等
 *       探测型用法（它们以 {@code mobs == 穿戴者} 判断区域）；</li>
 *   <li>{@code MyGoUtil.getNearestNonFollowerOnPath}：视线锁定类（深渊/戈尔贡/遗髓等减速、
 *       虚弱、石化），其候选集合包含玩家与玩家驯服生物，命中即返回 null。</li>
 * </ul>
 * 穿戴者不是受控生物时全部放行，对原版玩家佩戴魂石的行为零影响。
 */
@Mixin(MyGoUtil.class)
public class MyGoUtilProtectMixin {

  private MyGoUtilProtectMixin() {
  }

  private static boolean curious_mobs$shouldBlock(LivingEntity wearer, LivingEntity target) {
    if (wearer == null || target == null || target == wearer) {
      return false;
    }
    if (!MobControllerCompat.isControlled(wearer)) {
      return false;
    }
    if (target instanceof Player player) {
      UUID owner = MobControllerCompat.controllerUuid(wearer);
      return owner == null || owner.equals(player.getUUID());
    }
    if (!(target instanceof Enemy)) {
      return true;
    }
    if (MobControllerCompat.isAlly(wearer, target)) {
      return true;
    }
    UUID owner = MobControllerCompat.controllerUuid(wearer);
    if (owner != null && target instanceof OwnableEntity ownable && owner.equals(ownable.getOwnerUUID())) {
      return true;
    }
    return false;
  }

  @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
      at = @At("RETURN"),
      cancellable = true,
      remap = false,
      require = 0)
  private static void curious_mobs$protectCanAttack(
      LivingEntity attacked, LivingEntity attacker, CallbackInfoReturnable<Boolean> cir) {
    if (cir.getReturnValue() && curious_mobs$shouldBlock(attacker, attacked)) {
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "mobList(DLnet/minecraft/world/entity/LivingEntity;)Ljava/util/List;",
      at = @At("RETURN"),
      cancellable = true,
      remap = false,
      require = 0)
  private static void curious_mobs$protectMobList(
      double range, LivingEntity entity, CallbackInfoReturnable<List<Mob>> cir) {
    List<Mob> list = cir.getReturnValue();
    if (list == null || list.isEmpty() || !MobControllerCompat.isControlled(entity)) {
      return;
    }
    List<Mob> filtered = new ArrayList<>(list);
    filtered.removeIf(mob -> mob != entity && curious_mobs$shouldBlock(entity, mob));
    cir.setReturnValue(filtered);
  }

  @Inject(method = "getNearestNonFollowerOnPath(Lnet/minecraft/world/entity/LivingEntity;D)Lnet/minecraft/world/entity/LivingEntity;",
      at = @At("RETURN"),
      cancellable = true,
      remap = false,
      require = 0)
  private static void curious_mobs$protectNearest(
      LivingEntity livingEntity, double range, CallbackInfoReturnable<LivingEntity> cir) {
    if (cir.getReturnValue() != null && curious_mobs$shouldBlock(livingEntity, cir.getReturnValue())) {
      cir.setReturnValue(null);
    }
  }
}