package com.curious_mobs.mixin;

import com.curious_mobs.curse.CurseHelper;
import com.inolia_zaicek.flame_chase_artifacts.util.FCAUtil;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 永久解除（curious_mobs:permanent 标记）的玩家佩戴星穹饰品诅咒物品时，
 * 在 FCA 的第二条检测路径 {@code FCAUtil.getCuriosItems}（直接遍历
 * {@code getEquippedCurios()} 构建已佩戴物品集合）上过滤掉诅咒载体。
 * <p>
 * FCA 的诅咒判定有两条完全独立的入口：
 * <ul>
 *   <li>{@code FCAUtil.isCurioEquipped}（内部调用 {@code findFirstCurio}，
 *       已由 {@link CuriosHelperMixin} 拦截）；</li>
 *   <li>{@code FCAUtil.getCuriosItems}（死亡/恋爱/争斗/岁月/诡计/天空/海洋/
 *       大地/穿越 等事件处理类直接遍历饰品栏取集合，绕过 findFirstCurio）——
 *       本混入在此过滤。</li>
 * </ul>
 * 过滤规则与 CuriosHelperMixin 一致：
 * <ul>
 *   <li>恨意漫灌（hatred_inundate）：未集齐 12 位继承人时从集合中移除 → 诅咒静默移除；
 *       集齐全部继承人（祝福态）后保留 → FCA 原生祝福（反转）机制生效；</li>
 *   <li>原罪（origin_sin）：恒为加冕诅咒，无祝福反转 → 一律从集合中移除。</li>
 * </ul>
 * 继承人饰品与永恒爱恋等祝福判定不受影响。
 */
@Mixin(FCAUtil.class)
public class FcaUtilMixin {

  @Inject(method = "getCuriosItems(Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Set;",
      at = @At("RETURN"),
      cancellable = true,
      remap = false)
  private static void curious_mobs$filterFcaCurseItems(
      LivingEntity entity, CallbackInfoReturnable<Set<Item>> cir) {
    if (!(entity instanceof Player player) || !CurseHelper.isPermanent(player)) {
      return;
    }
    Set<Item> items = cir.getReturnValue();
    if (items == null || items.isEmpty()) {
      return;
    }
    boolean blessed = CurseHelper.isFcaBlessed(player);
    Set<Item> filtered = new HashSet<>();
    for (Item item : items) {
      if (item != null) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key != null && "flame_chase_artifacts".equals(key.getNamespace())) {
          if ("origin_sin".equals(key.getPath())) {
            continue;
          }
          if ("hatred_inundate".equals(key.getPath()) && !blessed) {
            continue;
          }
        }
      }
      filtered.add(item);
    }
    cir.setReturnValue(filtered);
  }
}
