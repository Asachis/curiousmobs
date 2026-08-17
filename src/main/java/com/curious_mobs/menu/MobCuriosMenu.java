package com.curious_mobs.menu;

import com.curious_mobs.registration.ModMenuTypes;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class MobCuriosMenu extends AbstractContainerMenu {

  public static final int CURIOS_COLUMNS = 6;
  public static final int CURIOS_ROWS_PER_PAGE = 6;
  public static final int CURIOS_START_X = 8;
  public static final int CURIOS_START_Y = 26;
  public static final int PLAYER_INV_X = 140;
  public static final int PLAYER_INV_Y = 26;
  public static final int PLAYER_HOTBAR_Y = 84;

  private final int targetEntityId;
  private final LivingEntity target;
  private int curioSlotCount = 0;
  private int curioPageCount = 1;
  private int curioPage = 0;

  public MobCuriosMenu(int containerId, Inventory playerInventory, LivingEntity target) {
    super(ModMenuTypes.MOB_CURIOS.get(), containerId);
    this.targetEntityId = target.getId();
    this.target = target;
    buildSlots(playerInventory, target);
  }

  private MobCuriosMenu(int containerId, Inventory playerInventory, int targetEntityId) {
    super(ModMenuTypes.MOB_CURIOS.get(), containerId);
    this.targetEntityId = targetEntityId;
    this.target = (LivingEntity) playerInventory.player.level().getEntity(targetEntityId);
    buildSlots(playerInventory, this.target);
  }

  public static MobCuriosMenu fromNetwork(int containerId, Inventory playerInventory,
      FriendlyByteBuf buffer) {
    return new MobCuriosMenu(containerId, playerInventory, buffer.readInt());
  }

  private void buildSlots(Inventory playerInventory, LivingEntity entity) {
    if (entity != null) {
      CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
        int curioIndex = 0;
        for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
          IDynamicStackHandler stacks = entry.getValue().getStacks();
          for (int i = 0; i < stacks.getSlots(); i++) {
            int page = curioIndex / (CURIOS_COLUMNS * CURIOS_ROWS_PER_PAGE);
            int pageIndex = curioIndex % (CURIOS_COLUMNS * CURIOS_ROWS_PER_PAGE);
            int column = pageIndex % CURIOS_COLUMNS;
            int row = pageIndex / CURIOS_COLUMNS;
            addSlot(new MobCurioSlot(stacks, i,
                CURIOS_START_X + column * 18,
                CURIOS_START_Y + row * 18,
                entry.getKey(),
                playerInventory.player.level(),
                entity,
                page,
                this));
            curioIndex++;
          }
        }
        this.curioSlotCount = curioIndex;
        this.curioPageCount = Math.max(1,
            (curioIndex + CURIOS_COLUMNS * CURIOS_ROWS_PER_PAGE - 1)
                / (CURIOS_COLUMNS * CURIOS_ROWS_PER_PAGE));
        this.curioPage = 0;
      });
    }

    for (int row = 0; row < 3; row++) {
      for (int column = 0; column < 9; column++) {
        addSlot(new Slot(playerInventory, column + row * 9 + 9,
            PLAYER_INV_X + column * 18, PLAYER_INV_Y + row * 18));
      }
    }

    for (int column = 0; column < 9; column++) {
      addSlot(new Slot(playerInventory, column,
          PLAYER_INV_X + column * 18, PLAYER_HOTBAR_Y));
    }
  }

  public int getCurioSlotCount() {
    return this.curioSlotCount;
  }

  public int getCurioPageCount() {
    return this.curioPageCount;
  }

  public int getCurioPage() {
    return this.curioPage;
  }

  public void setCurioPage(int page) {
    this.curioPage = Math.max(0, Math.min(page, this.curioPageCount - 1));
  }

  public void nextCurioPage() {
    setCurioPage(this.curioPage + 1);
  }

  public void previousCurioPage() {
    setCurioPage(this.curioPage - 1);
  }

  /** 当前页实际占用的行数（最后一页可能不满）。 */
  public int getCurioRowCount() {
    int perPage = CURIOS_COLUMNS * CURIOS_ROWS_PER_PAGE;
    int onPage = Math.max(0, Math.min(this.curioSlotCount - this.curioPage * perPage, perPage));
    return onPage == 0 ? 0 : (onPage - 1) / CURIOS_COLUMNS + 1;
  }

  public LivingEntity getTarget() {
    return this.target;
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    Slot slot = this.slots.get(index);

    if (slot == null || !slot.hasItem()) {
      return ItemStack.EMPTY;
    }

    ItemStack stack = slot.getItem();
    ItemStack copy = stack.copy();

    if (index < this.curioSlotCount) {
      if (!slot.mayPickup(player)) {
        return ItemStack.EMPTY;
      }
      if (!this.moveItemStackTo(stack, this.curioSlotCount, this.slots.size(), true)) {
        return ItemStack.EMPTY;
      }
    } else {
      if (!this.moveItemStackTo(stack, 0, this.curioSlotCount, false)) {
        return ItemStack.EMPTY;
      }
    }

    if (stack.isEmpty()) {
      slot.set(ItemStack.EMPTY);
    } else {
      slot.setChanged();
    }

    if (stack.getCount() == copy.getCount()) {
      return ItemStack.EMPTY;
    }

    slot.onTake(player, stack);
    return copy;
  }

  @Override
  public boolean stillValid(Player player) {
    return this.target != null && this.target.isAlive()
        && player.distanceToSqr(this.target) < 64.0D;
  }
}