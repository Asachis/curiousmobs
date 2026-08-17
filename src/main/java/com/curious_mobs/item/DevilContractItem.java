package com.curious_mobs.item;

import com.curious_mobs.util.GoetyCompat;
import com.curious_mobs.util.MobControllerCompat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public class DevilContractItem extends Item {

  public static final String KEY_SERVANT = "curious_mobs:goety_servant";
  public static final String KEY_OWNER = "curious_mobs:servant_owner";

  public DevilContractItem(Item.Properties properties) {
    super(properties);
  }

  @Override
  public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip,
      TooltipFlag flag) {
    tooltip.add(Component.translatable("item.curious_mobs.devil_contract.tooltip"));
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
      Player owner = resolveOwner(target, player.level());
      if (owner == null) {
        player.displayClientMessage(
            Component.translatable("message.curious_mobs.not_tamed"), true);
        return InteractionResult.sidedSuccess(false);
      }

      CompoundTag data = target.getPersistentData();
      if (data.getBoolean(KEY_SERVANT)) {
        player.displayClientMessage(
            Component.translatable("message.curious_mobs.already_servant"), true);
        return InteractionResult.sidedSuccess(false);
      }

      data.putBoolean(KEY_SERVANT, true);
      data.putUUID(KEY_OWNER, owner.getUUID());
      if (target instanceof Mob mob) {
        mob.setPersistenceRequired();
      }

      boolean goety = GoetyCompat.addAllyEntity(owner, target);
      com.curious_mobs.CuriousMobs.LOGGER.info(
          "[CT] devil_contract sealed on target={} owner={} goety={}",
          target.getName().getString(), owner.getName().getString(), goety);

      stack.hurtAndBreak(1, player, item -> player.broadcastBreakEvent(hand));
      Level level = target.level();
      level.playSound(null, target.getX(), target.getY(), target.getZ(),
          SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);
      if (level instanceof ServerLevel serverLevel) {
        serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
            target.getX(), target.getY() + 1.0D, target.getZ(), 16,
            0.35D, 0.35D, 0.35D, 0.02D);
      }
      player.displayClientMessage(Component.translatable(
          "message.curious_mobs.contract_sealed", target.getName().getString()), true);
    }
    return InteractionResult.sidedSuccess(player.level().isClientSide);
  }

  /**
   * 确定契约的归属者：万物皆驯受控生物取控制者玩家（受控时以原版驯服归属为兜底），
   * 原版已驯服生物（OwnableEntity）取其主人。其余情况视为不可缔结。
   */
  private static Player resolveOwner(LivingEntity target, Level level) {
    if (MobControllerCompat.isAvailable()) {
      Player controller = MobControllerCompat.controller(target, level);
      if (controller != null) {
        return controller;
      }
    }
    if (target instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player owner) {
      return owner;
    }
    if (MobControllerCompat.isAvailable()) {
      // 万物皆驯驯服但控制者已离线：退还其存档中记录的控制者 UUID
      java.util.UUID uuid = MobControllerCompat.controllerUuid(target);
      if (uuid != null && level instanceof ServerLevel serverLevel) {
        net.minecraft.world.entity.player.Player byId = serverLevel.getServer()
            .getPlayerList().getPlayer(uuid);
        if (byId != null) {
          return byId;
        }
      }
    }
    return null;
  }
}