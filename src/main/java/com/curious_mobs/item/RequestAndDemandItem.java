package com.curious_mobs.item;

import com.curious_mobs.CuriousMobs;
import com.curious_mobs.curse.CurseHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class RequestAndDemandItem extends Item {

  public RequestAndDemandItem(Item.Properties properties) {
    super(properties);
  }

  @Override
  public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
      TooltipFlag flag) {
    tooltip.add(Component.translatable("item.curious_mobs.request_and_demand.tooltip"));
    super.appendHoverText(stack, level, tooltip, flag);
  }

  @Override
  public InteractionResult interactLivingEntity(ItemStack stack, Player player,
      LivingEntity target, InteractionHand hand) {
    if (target == player || target instanceof Player) {
      if (!player.level().isClientSide) {
        player.displayClientMessage(
            Component.translatable("message.curious_mobs.no_player"), true);
      }
      return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    if (!player.level().isClientSide) {
      boolean reversed = CurseHelper.hasCurseReversal(target);
      CuriousMobs.LOGGER.info("[CT] request_and_demand used on target={} reversed={}",
          target.getName().getString(), reversed);
      int count = CurseHelper.transferCurses(player, target, reversed);

      if (count > 0) {
        stack.hurtAndBreak(1, player, item -> player.broadcastBreakEvent(hand));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ZOMBIE_INFECT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable(
            reversed ? "message.curious_mobs.transferred_reversed"
                : "message.curious_mobs.transferred", count), true);
      } else {
        player.displayClientMessage(
            Component.translatable("message.curious_mobs.no_curse"), true);
      }
    }
    return InteractionResult.sidedSuccess(player.level().isClientSide);
  }
}