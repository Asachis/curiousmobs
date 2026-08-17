package com.curious_mobs.menu;

import java.util.Optional;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import com.curious_mobs.curse.CurseHelper;

public class MobCurioSlot extends SlotItemHandler {

  private final String identifier;
  private final LivingEntity wearer;
  private final int page;
  private final MobCuriosMenu owner;

  public MobCurioSlot(IItemHandlerModifiable itemHandler, int index, int xPosition,
      int yPosition, String identifier, Level level, LivingEntity wearer, int page,
      MobCuriosMenu owner) {
    super(itemHandler, index, xPosition, yPosition);
    this.identifier = identifier;
    this.wearer = wearer;
    this.page = page;
    this.owner = owner;
    CuriosApi.getSlot(identifier, level)
        .ifPresent(slotType -> this.setBackground(InventoryMenu.BLOCK_ATLAS, slotType.getIcon()));
  }

  public int getPage() {
    return this.page;
  }

  /** 仅渲染当前页的槽位；其余页的槽位保持注册但不可见。 */
  @Override
  public boolean isActive() {
    return this.page == this.owner.getCurioPage();
  }

  public String getIdentifier() {
    return this.identifier;
  }

  public LivingEntity getWearer() {
    return this.wearer;
  }

  /**
   * 灾厄之册（SlotFacet）会动态增减实体的蚀刻/护符槽位：装上身时扩容、
   * 摘下时收缩。妙手空空菜单在打开时一次性构建槽位引用，若实体槽位此后
   * 收缩，菜单中的旧槽位会越界。这里对所有槽位访问做边界保护，避免
   * DynamicStackHandler.validateStackIndex 抛出 RuntimeException 导致崩溃。
   */
  private boolean validIndex() {
    return this.getSlotIndex() >= 0 && this.getSlotIndex() < this.getItemHandler().getSlots();
  }

  @Override
  public boolean mayPlace(ItemStack stack) {
    if (!validIndex()) {
      return false;
    }
    if (this.getItemHandler().isItemValid(this.getSlotIndex(), stack)) {
      return true;
    }
    // 灾厄之册等"仅限玩家"的诅咒物品（celestial_artifacts:require_curse）：
    // ModularCurio.canEquip 只允许带诅咒的玩家佩戴，妙手空空菜单在此放行，
    // 允许其被主动安装到非玩家实体身上（安装后由 CurseEvents.onCurioChange 兼容层施加诅咒）。
    return CurseHelper.isRequireCurseItem(stack);
  }

  @Override
  public ItemStack getItem() {
    if (!validIndex()) {
      return ItemStack.EMPTY;
    }
    return super.getItem();
  }

  @Override
  public boolean hasItem() {
    return validIndex() && super.hasItem();
  }

  @Override
  public void set(ItemStack stack) {
    if (validIndex()) {
      super.set(stack);
    }
  }

  @Override
  public void setChanged() {
    if (validIndex()) {
      super.setChanged();
    }
  }

  @Override
  public boolean mayPickup(Player player) {
    return isRemovable(player);
  }

  /**
   * 创造模式检测：普通模式下仍沿用「自由取下」，但佩戴者拒绝卸下的
   * 绑定饰品（ICurio.canUnequip 返回 false，如七咒之戒等永恒绑定饰品）
   * 无法被取下；创造模式下解除该限制，可取下任意饰品。
   */
  public boolean isRemovable(Player player) {
    if (player.isCreative()) {
      return true;
    }
    ItemStack stack = getItem();
    if (stack.isEmpty()) {
      return true;
    }
    Optional<ICurio> curio = CuriosApi.getCurio(stack).resolve();
    if (curio.isEmpty()) {
      return true;
    }
    SlotContext context = new SlotContext(this.identifier, this.wearer, getSlotIndex(), false,
        true);
    return curio.get().canUnequip(context);
  }

  @OnlyIn(Dist.CLIENT)
  public String getSlotName() {
    String key = "curios.identifier." + this.identifier;
    if (I18n.exists(key)) {
      return I18n.get(key);
    }
    return Character.toUpperCase(this.identifier.charAt(0))
        + this.identifier.substring(1).toLowerCase();
  }
}
