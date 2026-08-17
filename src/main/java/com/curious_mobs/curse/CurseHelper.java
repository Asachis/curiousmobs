package com.curious_mobs.curse;

import com.curious_mobs.CuriousMobs;
import com.curious_mobs.config.CuriousMobsConfig;
import com.curious_mobs.api.PermanentFlagAccessor;
import com.google.common.collect.Multimap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class CurseHelper {

  private static final TagKey<Item> CURSED_TAG =
      TagKey.create(Registries.ITEM,
          ResourceLocation.fromNamespaceAndPath("curious_mobs", "cursed"));

  /**
   * 星穹饰品（FlameChaseArtifacts）的 12 位「翁法罗斯继承人」饰品注册名。
   * 恨意漫灌会对每一位未装备对应继承人饰品的系别施加诅咒；集齐全部 12 件
   * 则全部反转成祝福。
   */
  private static final String[] FCA_HEIR_ITEMS = {
      "sky_curios", "earth_curios", "ocean_curios", "romance_curios",
      "worldbearing_curios", "reason_curios", "trickery_curios", "strife_curios",
      "death_curios", "time_curios", "law_curios", "passage_curios"
  };
  private static final TagKey<Item> REVERSES_CURSE_TAG =
      TagKey.create(Registries.ITEM,
          ResourceLocation.fromNamespaceAndPath("curious_mobs", "reverses_curse"));

  /**
   * 星穹饰品争斗（Strife）使用固定 UUID 给护甲挂"临时修正"（减伤/祝福），
   * 该修正不会随诅咒饰品移除而自动清理（下次受伤事件才会 removeModifier）。
   * 永久解除时手动移除，避免玩家残留争斗诅咒的减甲效果。
   */
  private static final UUID STRIFE_ARMOR_MODIFIER =
      UUID.fromString("9FE96140-BE0B-CCBD-C20C-F4E8009ADC20");

  private CurseHelper() {
  }

  /**
   * 判定饰品是否“被诅咒”：位于诅咒 tag、实现 EL 的 ICursed、
   * 星月饰品的灾厄之册（CelestialArtifacts 的 CatastropheScroll），
   * 或（通用兜底）身上带有负面属性加成。
   */
  public static boolean isCursedCurio(ItemStack stack, LivingEntity wearer) {
    if (stack.isEmpty()) {
      return false;
    }
    if (stack.is(CURSED_TAG)) {
      return true;
    }
    try {
      Class<?> cursedInterface =
          Class.forName("com.aizistral.enigmaticlegacy.api.items.ICursed");
      if (cursedInterface.isInstance(stack.getItem())) {
        return true;
      }
    } catch (Throwable ignored) {
    }
    if (isCelestialCatastropheScroll(stack)) {
      // 灾厄之册本身不是"物品诅咒"，它承载的是玩家当前活跃的诅咒 flag。
      // 对玩家：没有活跃诅咒（如已转移/清除）时视为不再被诅咒，避免请求/强求反复转移；
      // 对非玩家实体（替死稻草人等）：仍以物品本身作为诅咒载体，保持 compat 层行为。
      if (wearer instanceof Player p) {
        return getCelestialCurseAmount(p) > 0;
      }
      return true;
    }
    // 星穹饰品：恨意漫灌/原罪。诅咒强度 = 未装备的翁法罗斯继承人数量（0-12）；
    // 集齐全部 12 位继承人时恨意漫灌转为祝福，不再视为诅咒（原罪恒为加冕诅咒）。
    if (isFlameChaseCurseItem(stack)) {
      return getFcaCurseAmount(wearer, stack) > 0;
    }
    return hasNegativeModifiers(stack, wearer);
  }

  /**
   * EL 的诅咒饰品（如七咒之戒）：其特殊诅咒（中立生物主动攻击、永燃）需要被镜像到
   * 实际佩戴的实体身上。通过注册名或 ICursed 标记接口识别，未安装 EL 时静默跳过。
   * 注：CursedRing 并未实现 ICursed（只实现 ICurioItem/IBindable），须以注册名匹配。
   */
  public static boolean isElCursedCurio(ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }
    try {
      ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
      if (key != null && "enigmaticlegacy".equals(key.getNamespace())
          && "cursed_ring".equals(key.getPath())) {
        return true;
      }
      Class<?> cursedInterface = Class.forName("com.aizistral.enigmaticlegacy.api.items.ICursed");
      return cursedInterface.isInstance(stack.getItem());
    } catch (Throwable ignored) {
      return false;
    }
  }

  /**
   * 星月饰品的"仅限玩家"诅咒物品（celestial_artifacts:require_curse，如灾厄之册）。
   * ModularCurio.canEquip 要求佩戴者为带诅咒的玩家，非玩家实体一律拒绝；
   * 妙手空空菜单借此识别并放行，允许这些物品被主动安装到非玩家实体身上。
   */
  public static boolean isRequireCurseItem(ItemStack stack) {
    if (stack.isEmpty()) {
      return false;
    }
    try {
      return stack.is(TagKey.create(Registries.ITEM,
          ResourceLocation.fromNamespaceAndPath("celestial_artifacts", "require_curse")));
    } catch (Throwable ignored) {
      return false;
    }
  }

  /**
   * 星月饰品（CelestialArtifacts）的灾厄之册：通过物品注册名识别，
   * 未安装该模组时自然不命中，无需硬性依赖。
   */
  private static boolean isCelestialCatastropheScroll(ItemStack stack) {
    try {
      ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
      return key != null && "celestial_artifacts".equals(key.getNamespace())
          && "catastrophe_scroll".equals(key.getPath());
    } catch (Throwable ignored) {
      return false;
    }
  }

  /**
   * 星穹饰品（FlameChaseArtifacts）的诅咒物品：恨意漫灌（hatred_inundate）与
   * 原罪（origin_sin）。通过物品注册名识别，未安装该模组时自然不命中。
   */
  private static boolean isFlameChaseCurseItem(ItemStack stack) {
    try {
      ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
      if (key == null || !"flame_chase_artifacts".equals(key.getNamespace())) {
        return false;
      }
      return "hatred_inundate".equals(key.getPath())
          || "origin_sin".equals(key.getPath());
    } catch (Throwable ignored) {
      return false;
    }
  }

  /**
   * 星穹饰品的"翁法罗斯继承人"饰品注册名是否命中某一件（未安装时静默跳过）。
   */
  private static boolean isFcaHeirItem(ItemStack stack, String heir) {
    try {
      ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
      return key != null && "flame_chase_artifacts".equals(key.getNamespace())
          && heir.equals(key.getPath());
    } catch (Throwable ignored) {
      return false;
    }
  }

  /**
   * 星穹饰品的实际诅咒强度（0-12）：
   * <ul>
   *   <li>恨意漫灌（hatred_inundate）：诅咒强度 = 未装备的翁法罗斯继承人数量；
   *       集齐 12 位 → 0（祝福态，不视为诅咒）；</li>
   *   <li>原罪（origin_sin）：恒为加冕诅咒，强度固定 12。</li>
   * </ul>
   * 未安装星穹饰品时返回 0。
   */
  public static int getFcaCurseAmount(LivingEntity wearer, ItemStack stack) {
    if (wearer == null || stack.isEmpty() || !isFlameChaseCurseItem(stack)) {
      return 0;
    }
    // 永久解除（curious_mobs:permanent）的玩家：CuriosHelperMixin 已让 FCA
    // 事件不再对其生效，这里同样视为不再被诅咒，避免请求/强求反复转移同一条
    // FCA 诅咒。
    if (wearer instanceof Player player && isPermanent(player)) {
      return 0;
    }
    try {
      ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
      if (key != null && "origin_sin".equals(key.getPath())) {
        return 12;
      }
    } catch (Throwable ignored) {
      return 0;
    }
    return countMissingFcaHeirs(wearer);
  }

  /**
   * 是否已集齐全部 12 位翁法罗斯继承人（恨意漫灌的祝福态/反转态）。
   * <p>
   * 与 {@code getFcaCurseAmount} 不同，此方法不做 permanent 判定：
   * 永久解除的玩家集齐继承人后仍视为祝福态，从而允许 FCA 原生祝福
   * （如大地祝福 {@code EarthCurios && HatredInundate}）重新生效。
   */
  public static boolean isFcaBlessed(LivingEntity wearer) {
    if (wearer == null) {
      return false;
    }
    return countMissingFcaHeirs(wearer) == 0;
  }

  /**
   * 统计未装备的翁法罗斯继承人数量（0-12）。
   */
  private static int countMissingFcaHeirs(LivingEntity wearer) {
    boolean[] worn = new boolean[FCA_HEIR_ITEMS.length];
    CuriosApi.getCuriosInventory(wearer).ifPresent(handler -> {
      for (ICurioStacksHandler entry : handler.getCurios().values()) {
        IDynamicStackHandler stacks = entry.getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
          ItemStack wornStack = stacks.getStackInSlot(i);
          if (wornStack.isEmpty()) {
            continue;
          }
          for (int h = 0; h < FCA_HEIR_ITEMS.length; h++) {
            if (isFcaHeirItem(wornStack, FCA_HEIR_ITEMS[h])) {
              worn[h] = true;
            }
          }
        }
      }
    });

    int missing = 0;
    for (boolean equipped : worn) {
      if (!equipped) {
        missing++;
      }
    }
    return missing;
  }

  /**
   * 玩家是否已"永久解除诅咒"。
   * <p>
   * 双源判定：
   * <ul>
   *   <li>persistentData（服务端权威，随存档保存，跨重生/重进保留）；</li>
   *   <li>SynchedEntityData 镜像（PlayerPermanentMixin，自动同步到客户端——
   *       Forge 不同步 persistentData，客户端本地玩家的效果事件（如 FCA
   *       Trickery.buff 的 MobEffectEvent.Added）需要该镜像才能正确识别永久解除）。</li>
   * </ul>
   */
  public static boolean isPermanent(Player player) {
    if (player == null) {
      return false;
    }
    if (player.getPersistentData().getBoolean("curious_mobs:permanent")) {
      return true;
    }
    return player instanceof PermanentFlagAccessor accessor && accessor.curiousMobs$isPermanent();
  }

  /**
   * 星月饰品灾厄之册当前生效的诅咒数量（7 种诅咒中已触发且未佩戴对应刻印的数量）。
   * 未安装星月饰品/灾厄之册时返回 0。
   */
  public static int getCelestialCurseAmount(LivingEntity wearer) {
    if (!(wearer instanceof Player player)) {
      return 0;
    }
    try {
      Class<?> utils = Class.forName("com.xiaoyue.celestial_artifacts.utils.CurioUtils");
      Method method = utils.getMethod("getCurseAmount", Player.class);
      Object result = method.invoke(null, player);
      return result instanceof Integer value ? value : 0;
    } catch (Throwable ignored) {
      return 0;
    }
  }

  /**
   * 清除玩家身上灾厄之册的全部诅咒 flag（PlayerFlagData 中的
   * CHAOS/ORIGIN/LIFE/TRUTH/DESIRE/NIHILITY/END），使其不再被诅咒。
   * 全部通过反射完成，未安装星月饰品时静默跳过。
   */
  public static void clearCelestialCurses(LivingEntity wearer) {
    if (wearer.level().isClientSide || !(wearer instanceof Player player)) {
      return;
    }
    try {
      Class<?> flagData = Class.forName("com.xiaoyue.celestial_core.content.generic.PlayerFlagData");
      Object holder = flagData.getField("HOLDER").get(null);

      if (holder == null) {
        return;
      }
      Method get = holder.getClass().getMethod("get", Player.class);
      Object data = get.invoke(holder, player);

      if (data == null) {
        return;
      }
      List<String> curseNames = celestialCurseFlagNames();
      Field flagsField = data.getClass().getDeclaredField("flags");
      flagsField.setAccessible(true);
      Object raw = flagsField.get(data);

      if (raw instanceof Set<?> flags) {
        boolean changed = flags.removeAll(curseNames);
        CuriousMobs.LOGGER.info("[CT] clearCelestialCurses player={} cleared={}",
            player.getName().getString(), curseNames);
        // 直接改了 flags 集合，绕过了 addFlag 的自动同步，必须手动推送客户端，
        // 否则灾厄之册 tooltip（ClientTokenHelper.flag）仍会读到旧的 flag 而显示红色诅咒。
        if (changed) {
          syncPlayerFlagData(player, holder);
        }
      }
    } catch (Throwable ignored) {
    }
  }

  private static List<String> celestialCurseFlagNames() {
    List<String> names = new ArrayList<>();
    try {
      Class<?> curses =
          Class.forName("com.xiaoyue.celestial_artifacts.content.curios.curse.CatastropheScroll$Curses");
      Object[] constants = curses.getEnumConstants();

      if (constants != null) {
        for (Object constant : constants) {
          names.add(constant.toString());
        }
      }
    } catch (Throwable ignored) {
    }
    return names;
  }

  /**
   * 把服务端清除后的 PlayerFlagData 状态同步到客户端（等价于 addFlag 内部
   * 的 HOLDER.network.toClientSyncAll(ServerPlayer)），否则客户端 tooltip
   * 仍按旧 flag 显示灾厄之册的红色诅咒状态。
   */
  private static void syncPlayerFlagData(Player player, Object holder) {
    try {
      Field network = holder.getClass().getField("network");
      Object handler = network.get(holder);
      Method sync = handler.getClass().getMethod("toClientSyncAll", ServerPlayer.class);
      sync.invoke(handler, player);
    } catch (Throwable ignored) {
    }
  }

  private static int resolveCursePower(ItemStack curseItem, LivingEntity wearer) {
    if (isCelestialCatastropheScroll(curseItem)) {
      return Math.max(1, getCelestialCurseAmount(wearer));
    }
    // 星穹饰品：实际诅咒强度（未装备的继承人数量 / 原罪恒 12）。
    int fcaAmount = getFcaCurseAmount(wearer, curseItem);
    if (fcaAmount > 0) {
      return fcaAmount;
    }
    return Math.max(1, getCursePower(curseItem));
  }

  private static boolean hasNegativeModifiers(ItemStack stack, LivingEntity wearer) {
    Optional<ICurio> curio = CuriosApi.getCurio(stack).resolve();

    if (curio.isEmpty()) {
      return false;
    }

    SlotContext context = new SlotContext("curio", wearer, 0, false, true);
    Multimap<Attribute, AttributeModifier> modifiers =
        curio.get().getAttributeModifiers(context, CuriosApi.getSlotUuid(context));

    for (AttributeModifier modifier : modifiers.values()) {
      if (modifier.getAmount() < 0.0D) {
        return true;
      }
    }
    return false;
  }

  /**
   * EL 的诅咒点数（CursedRing「七咒之戒」固定为 7，其余按诅咒附魔计数；
   * 未安装 EL 时返回 0）。
   */
  public static int getCursePower(ItemStack stack) {
    if (stack.isEmpty()) {
      return 0;
    }
    try {
      Class<?> handler = Class.forName("com.aizistral.enigmaticlegacy.handlers.SuperpositionHandler");
      Method method = handler.getMethod("getCurseAmount", ItemStack.class);
      Object result = method.invoke(null, stack);
      return result instanceof Integer value ? value : 0;
    } catch (Throwable ignored) {
      return 0;
    }
  }

  /**
   * 兼容层核心：把诅咒饰品的效果翻译后施加到任意目标生物身上。
   * <p>
   * EL 等许多诅咒（如七咒之戒）只对玩家生效；这里把“负面的属性加成”
   * 直接加成到目标属性上，并按诅咒点数扣减目标最大生命，从而对任何生物生效。
   *
   * @param reversed 为 true 时把诅咒转为增益（正面属性 + 更多生命上限）
   */
  public static void applyTransferredCurse(ItemStack curseItem, LivingEntity wearer,
      LivingEntity target, boolean reversed) {
    applyNegativeModifiers(curseItem, wearer, target, reversed);

    int power = resolveCursePower(curseItem, wearer);
    AttributeInstance health = target.getAttribute(Attributes.MAX_HEALTH);

    if (health != null) {
      UUID uuid = curseHealthUuid(target, curseItem);
      AttributeModifier mod = new AttributeModifier(uuid,
          reversed ? "curious_mobs:reversal" : "curious_mobs:curse",
          reversed ? power : -power, AttributeModifier.Operation.ADDITION);
      health.removeModifier(uuid);
      health.addPermanentModifier(mod);
    }

    if (reversed) {
      target.heal(power);
      target.addEffect(new MobEffectInstance(MobEffects.SATURATION, 400, 1));
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0));
    } else {
      target.hurt(target.damageSources().magic(), power);
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 1));
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 1));
      target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400, 1));
    }

    // 转移来源为 EL 诅咒饰品（如七咒之戒）时，给目标打上永燃标记：
    // 即便饰品本体留在玩家身上（transfer_curio_items=false），目标也要持续燃烧。
    if (!reversed && isElCursedCurio(curseItem)) {
      target.getPersistentData().putBoolean("curious_mobs:el_fire", true);
    }

    CuriousMobs.LOGGER.info("[CT] applyTransferredCurse item={} wearer={} target={} reversed={} cursePower={} targetHealth={}/{}",
        curseItem.getHoverName().getString(), wearer.getName().getString(),
        target.getName().getString(), reversed, power,
        target.getHealth(), target.getMaxHealth());
  }

  /**
   * 给身上实际佩戴诅咒饰品的实体（如替死稻草人）补上“仅对玩家生效的诅咒”。
   * 逐个饰品幂等施加，可安全重复调用（属性修正不重复叠加、掉血只触发一次）。
   */
  public static void applyWornCurses(LivingEntity wearer) {
    CuriosApi.getCuriosInventory(wearer).ifPresent(handler -> {
      for (ICurioStacksHandler entry : handler.getCurios().values()) {
        IDynamicStackHandler stacks = entry.getStacks();

        for (int i = 0; i < stacks.getSlots(); i++) {
          applyWornCurse(wearer, stacks.getStackInSlot(i));
        }
      }
    });
  }

  /**
   * 幂等地给单个佩戴者施加某件诅咒饰品的诅咒效果。
   * <p>
   * 已被 CurioChangeEvent 与稻草人放置路径复用；重复调用安全：
   * 生命上限修饰符以确定 UUID 先移除再加，掉血/负面效果仅在首次施加时触发。
   */
  public static void applyWornCurse(LivingEntity wearer, ItemStack stack) {
    if (wearer.level().isClientSide || stack.isEmpty() || !isCursedCurio(stack, wearer)) {
      return;
    }

    applyNegativeModifiers(stack, wearer, wearer, false);

    int power = resolveCursePower(stack, wearer);
    AttributeInstance health = wearer.getAttribute(Attributes.MAX_HEALTH);
    boolean firstTime = false;

    if (health != null) {
      UUID uuid = curseHealthUuid(wearer, stack);
      firstTime = health.getModifier(uuid) == null;
      AttributeModifier mod = new AttributeModifier(uuid, "curious_mobs:curse",
          -power, AttributeModifier.Operation.ADDITION);
      health.removeModifier(uuid);
      health.addPermanentModifier(mod);
    }

    if (firstTime) {
      wearer.hurt(wearer.damageSources().magic(), power);
      wearer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 0));
      wearer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 0));
    }

    CuriousMobs.LOGGER.info("[CT] applyWornCurse wearer={} item={} cursePower={} health={}/{} firstTime={}",
        wearer.getName().getString(), stack.getHoverName().getString(), power,
        wearer.getHealth(), wearer.getMaxHealth(), firstTime);
  }

  /**
   * 移除某个诅咒饰品给佩戴者施加的诅咒效果（属性修正 + 生命上限）。
   * 供 CurioChangeEvent 在饰品被取下时清理。
   */
  public static void removeWornCurse(LivingEntity wearer, ItemStack stack) {
    if (wearer.level().isClientSide || stack.isEmpty()) {
      return;
    }

    CuriosApi.getCurio(stack).ifPresent(curio -> {
      SlotContext context = new SlotContext("curio", wearer, 0, false, true);
      Multimap<Attribute, AttributeModifier> modifiers =
          curio.getAttributeModifiers(context, CuriosApi.getSlotUuid(context));

      for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
        AttributeModifier modifier = entry.getValue();

        if (modifier.getAmount() >= 0.0D) {
          continue;
        }

        Attribute attribute = entry.getKey();
        AttributeInstance instance = wearer.getAttribute(attribute);

        if (instance == null) {
          continue;
        }

        instance.removeModifier(modifier.getId());
        instance.removeModifier(modifierUuid(wearer, stack, attribute));
      }
    });

    AttributeInstance health = wearer.getAttribute(Attributes.MAX_HEALTH);

    if (health != null) {
      health.removeModifier(curseHealthUuid(wearer, stack));
    }

    CuriousMobs.LOGGER.info("[CT] removeWornCurse wearer={} item={}",
        wearer.getName().getString(), stack.getHoverName().getString());
  }

  private static void applyNegativeModifiers(ItemStack curseItem, LivingEntity wearer,
      LivingEntity target, boolean reversed) {
    CuriosApi.getCurio(curseItem).ifPresent(curio -> {
      SlotContext context = new SlotContext("curio", wearer, 0, false, true);
      Multimap<Attribute, AttributeModifier> modifiers =
          curio.getAttributeModifiers(context, CuriosApi.getSlotUuid(context));

      for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
        AttributeModifier modifier = entry.getValue();

        if (modifier.getAmount() >= 0.0D) {
          continue;
        }

        Attribute attribute = entry.getKey();
        AttributeInstance instance = target.getAttribute(attribute);

        if (instance == null) {
          continue;
        }

        // Curios 已自动为该饰品施加过原始修正（相同 UUID 的修正存在于属性上），
        // 则跳过，避免重复叠加（如 -0.30 被加两次变成 -0.60）。
        if (!reversed && instance.getModifier(modifier.getId()) != null) {
          continue;
        }

        UUID uuid = modifierUuid(target, curseItem, attribute);
        AttributeModifier applied = new AttributeModifier(uuid, modifier.getName(),
            reversed ? -modifier.getAmount() : modifier.getAmount(),
            modifier.getOperation());
        instance.removeModifier(uuid);
        instance.addPermanentModifier(applied);

        CuriousMobs.LOGGER.info("[CT] applyNegativeModifier target={} attribute={} amount={} op={}",
            target.getName().getString(), attribute.getDescriptionId(),
            applied.getAmount(), applied.getOperation());
      }
    });
  }

  private static UUID modifierUuid(LivingEntity target, ItemStack curseItem,
      Attribute attribute) {
    return UUID.nameUUIDFromBytes(
        ("curious_mobs:" + target.getStringUUID() + ":" + curseItem.getDescriptionId() + ":"
            + attribute.getDescriptionId()).getBytes(StandardCharsets.UTF_8));
  }

  private static UUID curseHealthUuid(LivingEntity target, ItemStack curseItem) {
    return UUID.nameUUIDFromBytes(
        ("curious_mobs:health:" + target.getStringUUID() + ":"
            + curseItem.getDescriptionId()).getBytes(StandardCharsets.UTF_8));
  }

  public static boolean hasCurseReversal(LivingEntity target) {
    boolean[] result = new boolean[1];
    CuriosApi.getCuriosInventory(target).ifPresent(handler -> {
      for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
        IDynamicStackHandler stacks = entry.getValue().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
          if (stacks.getStackInSlot(i).is(REVERSES_CURSE_TAG)) {
            result[0] = true;
            return;
          }
        }
      }
    });
    return result[0];
  }

  /**
   * 把玩家佩戴的全部诅咒饰品的“诅咒”转移到目标生物身上。
   * <p>
   * 根据配置决定诅咒饰品的去留：
   * <ul>
   *   <li>transferCurioItems=true：诅咒饰品本体移到目标身上（同替死稻草人）；</li>
   *   <li>transferCurioItems=false（默认）：饰品保留在玩家身上，只永久移除玩家的
   *       诅咒效果（剥除负面修正 + 打上 curious_mobs:permanent 标记压制 EL 被动诅咒），
   *       保留饰品提供的正面增益。</li>
   * </ul>
   *
   * @return 参与转移的诅咒饰品数量
   */
  public static int transferCurses(Player player, LivingEntity target, boolean reversed) {
    if (player.level().isClientSide || target == player) {
      return 0;
    }
    int[] counter = new int[1];
    boolean[] celestial = new boolean[1];

    CuriosApi.getCuriosInventory(player).ifPresent(playerHandler -> {
      Map<String, ICurioStacksHandler> curios = playerHandler.getCurios();

      for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
        IDynamicStackHandler stacks = entry.getValue().getStacks();
        int slots = stacks.getSlots();

        for (int i = 0; i < slots; i++) {
          ItemStack stack = stacks.getStackInSlot(i);

          if (stack.isEmpty() || !isCursedCurio(stack, player)) {
            continue;
          }

          if (isCelestialCatastropheScroll(stack)) {
            celestial[0] = true;
          }
          CuriousMobs.LOGGER.info("[CT] transferCurses found curse item={} slot={} id={}",
              stack.getHoverName().getString(), entry.getKey(), i);
          applyTransferredCurse(stack, player, target, reversed);
          counter[0]++;
        }
      }
    });

    int count = counter[0];

    if (count > 0) {
      if (CuriousMobsConfig.TRANSFER_CURIO_ITEMS.get()) {
        transferCursedCuriosToEntity(player, target);
      } else {
        permanentlyFreePlayer(player);
        // 星穹饰品诅咒绑定在"物品佩戴"上（事件驱动）：
        // permanentlyFreePlayer 打上的 curious_mobs:permanent 标记会被
        // FcaUtilMixin 拦截，使 FCA 事件对已永久解除的玩家不再生效——
        // 因此 FCA 诅咒饰品可以保留在玩家身上（不再需要强制搬走）。
      }
      // 清除既有针对该玩家的中立生物/末影人仇恨：EL 的七咒之戒在佩戴期间
      // 通过 CursedRing.curioTick 直接对附近 NeutralMob setTarget(player)，
      // FCA 的翁法罗斯·命途继承人（Passage）也会 setLastHurtByPlayer(player)。
      // 这些仇恨在饰品移除/永久解除后不会自动清除，导致玩家转移了诅咒仍被
      // 中立生物攻击。此处显式取消以该玩家为目标、或对其有仇恨状态的生物。
      clearNeutralMobAnger(player);
      // 灾厄之册的诅咒是玩家身上的一组 flag，须在饰品搬迁/剥离完成后再清除。
      // 放在最后：a) 上述清理仍能识别出诅咒饰品；b) 清除后 isCursedCurio(灾厄之册)
      // 判定为不再被诅咒，请求/强求无法反复转移。
      if (celestial[0]) {
        clearCelestialCurses(player);
      }
    }

    // 不再让被转移诅咒的目标生物直接转火攻击玩家（否则目标若是
    // Wolf/Enderman/Piglin 等中立生物，等于把“被中立生物攻击”重新施加回玩家）。
    // 目标承担的中立生物仇恨由 CurseEvents.onLivingTick 的 compat 层负责（围攻）。
    return count;
  }

  /**
   * 仅把诅咒饰品本体（而非全部饰品）从 from 移到 to 的对应饰品栏位。
   */
  public static void transferCursedCuriosToEntity(LivingEntity from, LivingEntity to) {
    CuriosApi.getCuriosInventory(from).ifPresent(fromHandler -> {
      CuriosApi.getCuriosInventory(to).ifPresent(toHandler -> {
        Map<String, ICurioStacksHandler> fromCurios = fromHandler.getCurios();
        Map<String, ICurioStacksHandler> toCurios = toHandler.getCurios();

        for (Map.Entry<String, ICurioStacksHandler> fromEntry : fromCurios.entrySet()) {
          String identifier = fromEntry.getKey();
          ICurioStacksHandler toEntry = toCurios.get(identifier);

          if (toEntry == null) {
            continue;
          }

          IDynamicStackHandler fromStacks = fromEntry.getValue().getStacks();
          IDynamicStackHandler toStacks = toEntry.getStacks();
          int slots = Math.min(fromStacks.getSlots(), toStacks.getSlots());

          for (int i = 0; i < slots; i++) {
            ItemStack stack = fromStacks.getStackInSlot(i);

            if (stack.isEmpty() || !isCursedCurio(stack, from)) {
              continue;
            }

            toStacks.setStackInSlot(i, stack.copy());
            fromStacks.setStackInSlot(i, ItemStack.EMPTY);
            CuriousMobs.LOGGER.info("[CT] transferCurioItem item={} -> {} slot={}/{}",
                stack.getHoverName().getString(), to.getName().getString(), identifier, i);
          }
        }
      });
    });
  }

  /**
   * 永久解除玩家自身的诅咒：
   * <ul>
   *   <li>先逐个诅咒饰品剥除其负面属性修正与生命上限扣减。必须在打上
   *       curious_mobs:permanent 标记<b>之前</b>完成，否则 EL 的
   *       CursedRingMixin 会让七咒之戒返回空的属性修正，导致残留的
   *       -30% 护甲/韧性修正无法被移除；</li>
   *   <li>随后在玩家持久数据打上 curious_mobs:permanent 标记，配合
   *       SuperpositionHandlerMixin / CursedRingMixin 让 EL 不再对其施加
   *       被动诅咒（中立生物攻击、伤害翻倍、无法入睡、火焰延长等），
   *       饰品与正面增益保留。</li>
   * </ul>
   */
  public static void permanentlyFreePlayer(Player player) {
    if (player.level().isClientSide) {
      return;
    }

    CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
      for (ICurioStacksHandler entry : handler.getCurios().values()) {
        IDynamicStackHandler stacks = entry.getStacks();

        for (int i = 0; i < stacks.getSlots(); i++) {
          ItemStack stack = stacks.getStackInSlot(i);

          if (stack.isEmpty() || !isCursedCurio(stack, player)) {
            continue;
          }

          removeWornCurse(player, stack);
          CuriousMobs.LOGGER.info("[CT] permanentlyFreePlayer stripped item={}",
              stack.getHoverName().getString());
        }
      }
    });

    player.getPersistentData().putBoolean("curious_mobs:permanent", true);

    // 同步到 SynchedEntityData，使客户端本地玩家也能识别永久解除标记
    // （Forge 不会把 persistentData 同步到客户端，而 FCA 的效果事件在
    // 客户端也会触发，需要该镜像避免客户端本地效果仍被缩短/延长）。
    if (player instanceof PermanentFlagAccessor accessor) {
      accessor.curiousMobs$setPermanent(true);
    }

    // 移除争斗（Strife）诅咒用固定 UUID 挂上的护甲临时修正，避免残留减甲。
    // 该修正由 Strife.hurt 在每次受伤时 addTransientModifier，若一直不受击
    // 则不会自行清除；永久解除后不再有此诅咒来源。
    AttributeInstance armor = player.getAttribute(Attributes.ARMOR);

    if (armor != null) {
      armor.removeModifier(STRIFE_ARMOR_MODIFIER);
    }

    CuriousMobs.LOGGER.info("[CT] permanentlyFreePlayer player={}",
        player.getName().getString());
  }

  /**
   * 清除范围内以目标玩家为攻击目标、或对其怀有仇恨（含末影人凝视）的中立生物。
   * <p>
   * EL 的七咒之戒（CursedRing.curioTick）与 FCA 的命途继承人（Passage）都会
   * 让中立生物把玩家记为攻击目标或仇恨来源；这些状态在诅咒饰品移除/转移后
   * 不会自动消失。永久解除/替死稻草人转移后调用本方法，把这些生物的
   * setTarget/仇恨时间/仇恨目标显式清空。
   */
  public static void clearNeutralMobAnger(Player player) {
    if (player == null || player.level().isClientSide) {
      return;
    }
    Level level = player.level();
    AABB box = player.getBoundingBox().inflate(64.0D);

    for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
      if (!(entity instanceof Mob mob)) {
        continue;
      }
      boolean targetingPlayer = mob.getTarget() == player;
      boolean hurtByPlayer = mob.getLastHurtByMob() == player;
      boolean angerOnPlayer = false;

      if (mob instanceof NeutralMob neutral) {
        UUID angerTarget = neutral.getPersistentAngerTarget();
        angerOnPlayer = player.getUUID().equals(angerTarget);
      }
      if (!targetingPlayer && !hurtByPlayer && !angerOnPlayer) {
        continue;
      }
      mob.setTarget(null);
      mob.setLastHurtByMob(null);

      if (mob instanceof NeutralMob neutral) {
        neutral.setPersistentAngerTarget(null);
        neutral.setRemainingPersistentAngerTime(0);
      }
      CuriousMobs.LOGGER.info("[CT] clearNeutralMobAnger player={} mob={}",
          player.getName().getString(), mob.getName().getString());
    }
  }

  public static void transferEquippedCuriosToEntity(LivingEntity from, LivingEntity to) {
    CuriosApi.getCuriosInventory(from).ifPresent(fromHandler -> {
      CuriosApi.getCuriosInventory(to).ifPresent(toHandler -> {
        IItemHandlerModifiable fromSlots = fromHandler.getEquippedCurios();
        IItemHandlerModifiable toSlots = toHandler.getEquippedCurios();
        int size = Math.min(fromSlots.getSlots(), toSlots.getSlots());

        for (int i = 0; i < size; i++) {
          ItemStack stack = fromSlots.getStackInSlot(i);

          if (!stack.isEmpty()) {
            toSlots.setStackInSlot(i, stack.copy());
            fromSlots.setStackInSlot(i, ItemStack.EMPTY);
          }
        }
      });
    });
  }
}