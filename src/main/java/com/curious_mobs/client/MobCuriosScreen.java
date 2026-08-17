package com.curious_mobs.client;

import com.curious_mobs.menu.MobCurioSlot;
import com.curious_mobs.menu.MobCuriosMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class MobCuriosScreen extends AbstractContainerScreen<MobCuriosMenu> {

  private static final int PANEL_BORDER = 0xFF8B8B8B;
  private static final int PANEL_INNER = 0xFFE3E3E3;
  private static final int CELL_BORDER = 0xFF9A9A9A;
  private static final int CELL_INNER = 0xFFF7F7F7;
  private static final int LABEL_COLOR = 0xFF404040;
  private static final int PAGE_ARROW_COLOR = 0xFF606060;
  private static final int PAGE_ARROW_HOVER = 0xFF202020;

  public MobCuriosScreen(MobCuriosMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title);
    this.imageWidth = MobCuriosMenu.PLAYER_INV_X + 9 * 18 + 8;
    this.imageHeight = MobCuriosMenu.CURIOS_START_Y
        + MobCuriosMenu.CURIOS_ROWS_PER_PAGE * 18 + 10;
  }

  private boolean isPageButtonHovered(double mouseX, double mouseY, int direction) {
    int buttonX = this.leftPos + MobCuriosMenu.CURIOS_START_X
        + MobCuriosMenu.CURIOS_COLUMNS * 18 - 4 + (direction == 1 ? 20 : 0);
    int buttonY = this.topPos + MobCuriosMenu.CURIOS_START_Y - 13;
    return mouseX >= buttonX && mouseX < buttonX + 10 && mouseY >= buttonY && mouseY < buttonY + 10;
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (button == 0) {
      if (this.menu.getCurioPageCount() > 1 && isPageButtonHovered(mouseX, mouseY, -1)) {
        this.menu.previousCurioPage();
        return true;
      }
      if (this.menu.getCurioPageCount() > 1 && isPageButtonHovered(mouseX, mouseY, 1)) {
        this.menu.nextCurioPage();
        return true;
      }
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    int left = this.leftPos;
    int top = this.topPos;

    graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFFC6C6C6);
    graphics.fill(left + 1, top + 1, left + this.imageWidth - 1, top + this.imageHeight - 1,
        0xFFF0F0F0);

    int rows = this.menu.getCurioRowCount();
    if (rows > 0) {
      int panelX = MobCuriosMenu.CURIOS_START_X - 3;
      int panelY = MobCuriosMenu.CURIOS_START_Y - 12;
      int panelW = MobCuriosMenu.CURIOS_COLUMNS * 18 + 6;
      int panelH = MobCuriosMenu.CURIOS_ROWS_PER_PAGE * 18 + 10;
      graphics.fill(left + panelX, top + panelY, left + panelX + panelW, top + panelY + panelH,
          PANEL_BORDER);
      graphics.fill(left + panelX + 1, top + panelY + 1, left + panelX + panelW - 1,
          top + panelY + panelH - 1, PANEL_INNER);
    }

    for (Slot slot : this.menu.slots) {
      if (slot.isActive()) {
        graphics.fill(slot.x - 1 + left, slot.y - 1 + top, slot.x + 17 + left,
            slot.y + 17 + top, CELL_BORDER);
        graphics.fill(slot.x + left, slot.y + top, slot.x + 16 + left, slot.y + 16 + top,
            CELL_INNER);
      }
    }

    // page arrows (only when more than one page)
    if (this.menu.getCurioPageCount() > 1) {
      int arrowY = top + MobCuriosMenu.CURIOS_START_Y - 13;
      int leftX = left + MobCuriosMenu.CURIOS_START_X + MobCuriosMenu.CURIOS_COLUMNS * 18 - 4;
      int rightX = leftX + 20;
      boolean canPrev = this.menu.getCurioPage() > 0;
      boolean canNext = this.menu.getCurioPage() < this.menu.getCurioPageCount() - 1;
      int prevColor = isPageButtonHovered(mouseX, mouseY, -1) ? PAGE_ARROW_HOVER
          : PAGE_ARROW_COLOR;
      int nextColor = isPageButtonHovered(mouseX, mouseY, 1) ? PAGE_ARROW_HOVER
          : PAGE_ARROW_COLOR;
      graphics.drawString(this.font, Component.literal("\u25C0"), leftX, arrowY,
          canPrev ? prevColor : 0xFFB0B0B0, false);
      graphics.drawString(this.font, Component.literal("\u25B6"), rightX, arrowY,
          canNext ? nextColor : 0xFFB0B0B0, false);
    }
  }

  @Override
  protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR,
        false);
    graphics.drawString(this.font, Component.translatable("gui.curious_mobs.curios"),
        MobCuriosMenu.CURIOS_START_X + 1, MobCuriosMenu.CURIOS_START_Y - 9, LABEL_COLOR, false);

    if (this.menu.getCurioPageCount() > 1) {
      String pageText = (this.menu.getCurioPage() + 1) + "/"
          + this.menu.getCurioPageCount();
      graphics.drawString(this.font, pageText,
          MobCuriosMenu.CURIOS_START_X + MobCuriosMenu.CURIOS_COLUMNS * 18 - 12,
          MobCuriosMenu.CURIOS_START_Y - 9, LABEL_COLOR, false);
    }

    graphics.drawString(this.font, Component.translatable("gui.curious_mobs.inventory"),
        MobCuriosMenu.PLAYER_INV_X, MobCuriosMenu.CURIOS_START_Y - 9, LABEL_COLOR, false);
  }

  @Override
  protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
    if (this.hoveredSlot instanceof MobCurioSlot curioSlot && !curioSlot.hasItem()
        && Minecraft.getInstance().player != null
        && Minecraft.getInstance().player.inventoryMenu.getCarried().isEmpty()) {
      graphics.renderTooltip(this.font, Component.literal(curioSlot.getSlotName()), mouseX,
          mouseY);
      return;
    }
    super.renderTooltip(graphics, mouseX, mouseY);
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    this.renderBackground(graphics);
    super.render(graphics, mouseX, mouseY, partialTick);
    this.renderTooltip(graphics, mouseX, mouseY);
  }
}