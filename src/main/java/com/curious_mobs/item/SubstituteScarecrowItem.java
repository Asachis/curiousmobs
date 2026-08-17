package com.curious_mobs.item;

import com.curious_mobs.curse.CurseHelper;
import com.curious_mobs.entity.ScarecrowEntity;
import com.curious_mobs.registration.ModEntityTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SubstituteScarecrowItem extends Item {

  public SubstituteScarecrowItem(Item.Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    if (!level.isClientSide) {
      ScarecrowEntity scarecrow = ModEntityTypes.SCARECROW.get().create(level);

      if (scarecrow != null) {
        scarecrow.moveTo(player.getX(), player.getY(), player.getZ(),
            player.getYRot(), player.getXRot());
        // 稻草人是静态装饰实体：关闭 AI，防止每 tick 的寻路/朝向逻辑把
        // moveTo 设置的朝向平滑重置回默认方向，从而跟随玩家面朝方向生成。
        scarecrow.setNoAi(true);
        scarecrow.setYRot(player.getYRot());
        scarecrow.yBodyRot = player.getYRot();
        scarecrow.yHeadRot = player.getYRot();
        scarecrow.setCustomName(Component.translatable("entity.curious_mobs.scarecrow"));
        scarecrow.setPlayerUuid(player.getUUID());
        level.addFreshEntity(scarecrow);
        CurseHelper.transferEquippedCuriosToEntity(player, scarecrow);
        CurseHelper.applyWornCurses(scarecrow);
        // 转移后清除针对玩家的残留中立生物/末影人仇恨（七咒之戒 curioTick、
        // FCA 命途继承人的 setLastHurtByPlayer 都不会随饰品离手自动清除）。
        CurseHelper.clearNeutralMobAnger(player);
        player.getItemInHand(hand).shrink(1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
      }
    }
    return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
  }
}