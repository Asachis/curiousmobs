package com.curious_mobs.mixin;

import com.inolia_zaicek.mine_fargo.Item.MineCraft.SoulOfSupernaturalItem;
import com.inolia_zaicek.mine_fargo.Item.MineCraft.Supernatural.MagnetSoulStoneItem;
import com.inolia_zaicek.mine_fargo.Item.SoulOfInoliaItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 任何非玩家生物佩戴磁铁魂石（以及含磁铁功能的万灵/诡恶超自然综合魂石）时，
 * 默认关闭“持续吸取范围内物品”功能 —— 含经“妙手空空”佩戴的情况。
 * <p>
 * 魂石原判定：{@code Entity.getPersistentData().getInt("mine_fargo:magnet_soul_stone_open") <= 50}
 * 为开启（25=开、75=关），而 NBT 从未写入任何值时 {@code getInt} 返回 0，因此默认恒为“开”。
 * 切开关仅存在于 {@code TickEvent} 的客户端按键分支且要求 {@code livingEntity instanceof Player}，
 * 生物（非玩家）永远无法关闭，导致跟随玩家时一路吸走范围掉落物/经验球。
 * <p>
 * 本混入在三个目标类 {@code onTick} 的 HEAD 处检测到非玩家穿戴者时，立刻把该标记一次性置为
 * 75（关），此后原始磁铁块条件（{@code <=50}）恒为假，吸取逻辑不再执行。
 * 该标记由原版死亡/克隆流程复制，重生后依然保持关闭。玩家穿戴者不受影响（仍可用按键切换）。
 * <p>
 * 由于三个类含同构的磁铁代码（javap 已核验 onTick 描述符与 lambda 结构一致），
 * 用一个注入方法同时覆盖三者。
 */
@Mixin({MagnetSoulStoneItem.class, SoulOfSupernaturalItem.class, SoulOfInoliaItem.class})
public class MagnetDefaultOffMixin {

  private static final String MAGNET_OPEN_FLAG = "mine_fargo:magnet_soul_stone_open";

  private MagnetDefaultOffMixin() {
  }

  @Inject(method = "onTick(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraftforge/event/entity/living/LivingEvent$LivingTickEvent;)V",
      at = @At("HEAD"),
      remap = false,
      require = 0)
  private void curious_mobs$magnetDefaultOff(
      LivingEntity livingEntity, LivingEvent.LivingTickEvent event, CallbackInfo ci) {
    if (!(livingEntity instanceof Player)
        && livingEntity.getPersistentData().getInt(MAGNET_OPEN_FLAG) <= 50) {
      livingEntity.getPersistentData().putInt(MAGNET_OPEN_FLAG, 75);
    }
  }
}