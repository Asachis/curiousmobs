package com.curious_mobs.item;

import com.curious_mobs.menu.MobCuriosMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class SleightOfHandItem extends Item {

  public SleightOfHandItem(Item.Properties properties) {
    super(properties);
  }

  @Override
  public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
      TooltipFlag flag) {
    tooltip.add(Component.translatable("item.curious_mobs.sleight_of_hand.tooltip"));
    super.appendHoverText(stack, level, tooltip, flag);
  }

  @Override
  public InteractionResult interactLivingEntity(ItemStack stack, Player player,
      LivingEntity target, InteractionHand hand) {
    if (player.level().isClientSide) {
      return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    if (target == player || target instanceof Player) {
      player.displayClientMessage(
          Component.translatable("message.curious_mobs.no_player"), true);
      return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    if (player instanceof ServerPlayer serverPlayer && target.isAlive()) {
      NetworkHooks.openScreen(serverPlayer,
          new SimpleMenuProvider(
              (id, inv, p) -> new MobCuriosMenu(id, inv, target),
              target.getName()),
          buf -> buf.writeInt(target.getId()));
      stack.hurtAndBreak(1, player, item -> player.broadcastBreakEvent(hand));
    }
    return InteractionResult.sidedSuccess(player.level().isClientSide);
  }
}