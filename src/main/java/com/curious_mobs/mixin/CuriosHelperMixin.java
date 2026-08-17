package com.curious_mobs.mixin;

import com.curious_mobs.curse.CurseHelper;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.common.CuriosHelper;

/**
 * 永久解除（curious_mobs:permanent 标记）的玩家佩戴星穹饰品诅咒物品时，
 * 在 Curios 查询的<b>唯一入口</b> {@code CuriosHelper.findFirstCurio} 上拦截，
 * 让 FCA 判定该物品"未佩戴"，从而关闭其事件驱动诅咒。
 * <p>
 * FCA 的诅咒判定有的走 {@code FCAUtil.isCurioEquipped}（内部即调用
 * {@code findFirstCurio}），有的（死亡/恋爱/争斗/岁月/诡计）直接调用
 * {@code CuriosApi.getCuriosHelper().findFirstCurio(entity, item)}。在
 * {@code findFirstCurio} 单点拦截可同时覆盖两条路径。只过滤"诅咒载体"：
 * <ul>
 *   <li>恨意漫灌（hatred_inundate）：未集齐 12 位继承人时返回空 → 诅咒静默移除；
 *       集齐全部继承人（祝福态）后放行 → FCA 原生祝福（反转）机制生效；</li>
 *   <li>原罪（origin_sin）：恒为加冕诅咒，无祝福反转 → 一律返回空。</li>
 * </ul>
 * 继承人饰品与永恒爱恋等祝福判定不受影响。
 */
@Mixin(value = CuriosHelper.class, remap = false)
public class CuriosHelperMixin {

  @Inject(method = "findFirstCurio(Lnet/minecraft/world/entity/LivingEntity;"
          + "Lnet/minecraft/world/item/Item;)Ljava/util/Optional;",
      at = @At("HEAD"),
      cancellable = true,
      remap = false)
  private void curious_mobs$suppressFcaCurseLookup(
      LivingEntity entity, Item item, CallbackInfoReturnable<Optional<SlotResult>> cir) {
    if (!(entity instanceof Player player) || item == null
        || !CurseHelper.isPermanent(player)) {
      return;
    }

    ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
    if (key == null || !"flame_chase_artifacts".equals(key.getNamespace())) {
      return;
    }

    if ("hatred_inundate".equals(key.getPath())) {
      // 集齐 12 位继承人（祝福态）时放行，让 FCA 原生反转/祝福生效；
      // 未集齐时视为未佩戴，关闭诅咒。
      if (!CurseHelper.isFcaBlessed(player)) {
        cir.setReturnValue(Optional.empty());
      }
    } else if ("origin_sin".equals(key.getPath())) {
      // 原罪无祝福反转，永久解除的玩家一律关闭其诅咒。
      cir.setReturnValue(Optional.empty());
    }
  }
}
