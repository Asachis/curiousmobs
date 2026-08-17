package com.curious_mobs.mixin;

import com.aizistral.enigmaticlegacy.items.CursedRing;
import com.curious_mobs.curse.CurseHelper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.SlotContext;

/**
 * 永久解除：当佩戴者是带 curious_mobs:permanent 标记的玩家时，
 * 让七咒之戒不再返回负面属性修正（armor/armor_toughness 各 -0.30），
 * 并在 tick 中不再对中立生物/末影人设定攻击目标，
 * 从而玩家保留戒指、保留其他增益，却不再承担属性削弱与"中立生物主动攻击"诅咒。
 */
@Mixin(value = CursedRing.class, remap = false)
public class CursedRingMixin {

  @Inject(method = "getAttributeModifiers",
      at = @At("HEAD"),
      cancellable = true,
      remap = false)
  private void curious_mobs$noDebuffsForFreedPlayers(
      SlotContext slotContext, UUID pUuid, ItemStack stack,
      CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
    if (slotContext.entity() instanceof Player player && CurseHelper.isPermanent(player)) {
      cir.setReturnValue(ArrayListMultimap.create());
    }
  }

  /**
   * 中立生物 / 末影人愤怒逻辑位于 curioTick：佩戴者每 tick 会对范围内的
   * NeutralMob 直接调用 setTarget 摇动它们（第二诅咒）。直接跳过该逻辑，
   * 彻底关闭"中立生物主动攻击玩家"的诅咒。
   */
  @Inject(method = "curioTick(Ltop/theillusivec4/curios/api/SlotContext;Lnet/minecraft/world/item/ItemStack;)V",
      at = @At("HEAD"),
      cancellable = true,
      remap = false)
  private void curious_mobs$noNeutralMobAngerForFreedPlayers(
      SlotContext slotContext, ItemStack stack, CallbackInfo ci) {
    if (slotContext.entity() instanceof Player player && CurseHelper.isPermanent(player)) {
      ci.cancel();
    }
  }
}