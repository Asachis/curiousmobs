package com.curious_mobs.util;

import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

/**
 * 万物皆驯（Mob Controller）的纯反射兼容层：万物皆饰模组不直接依赖该模组编译，
 * 运行时通过反射调用其已确认存在的静态方法（mob_controller-2.0.1 已用 javap 核验）：
 * <ul>
 *   <li>{@code MobControlledData.isControlledEntity(LivingEntity)} → boolean</li>
 *   <li>{@code MobControlledData.getControllerUUID(LivingEntity)} → UUID</li>
 *   <li>{@code MobControlUtil.isAlly(LivingEntity, Entity)} → boolean</li>
 * </ul>
 * 模组未加载时全部回退为“不是受控生物/不是友军”，保持原版行为，零副作用。
 */
public final class MobControllerCompat {

  private static final String MOB_CONTROLLER_MOD_ID = "mob_controller";
  private static final String DATA_CLASS = "net.xiaoyu.mob_controller.util.MobControlledData";
  private static final String UTIL_CLASS = "net.xiaoyu.mob_controller.util.MobControlUtil";

  private static boolean checked = false;
  private static boolean available = false;
  private static Method isControlledEntity;
  private static Method getControllerUUID;
  private static Method getController;
  private static Method isAlly;
  private static Method addControlledMob;

  private MobControllerCompat() {
  }

  /** 万物皆驯是否已确认加载且反射解析成功。 */
  public static boolean isAvailable() {
    resolve();
    return available;
  }

  private static void resolve() {
    if (checked) {
      return;
    }
    checked = true;
    if (ModList.get() == null || !ModList.get().isLoaded(MOB_CONTROLLER_MOD_ID)) {
      return;
    }
    try {
      Class<?> data = Class.forName(DATA_CLASS);
      Class<?> util = Class.forName(UTIL_CLASS);
      isControlledEntity = data.getMethod("isControlledEntity", LivingEntity.class);
      getControllerUUID = data.getMethod("getControllerUUID", LivingEntity.class);
      getController = data.getMethod("getController", LivingEntity.class, Level.class);
      isAlly = util.getMethod("isAlly", LivingEntity.class, Entity.class);
      addControlledMob = data.getMethod("addControlledMob", UUID.class, Mob.class);
      available = true;
    } catch (ReflectiveOperationException | LinkageError ignored) {
      available = false;
    }
  }

  /** 目标是否为万物皆驯受控生物（控制模式下佩戴灵魂石触发 AOE 的穿戴者）。 */
  public static boolean isControlled(LivingEntity entity) {
    resolve();
    if (!available || entity == null) {
      return false;
    }
    try {
      return (boolean) isControlledEntity.invoke(null, entity);
    } catch (ReflectiveOperationException | LinkageError e) {
      return false;
    }
  }

  /** 控制者玩家的 UUID；非受控或解析失败时返回 null。 */
  public static UUID controllerUuid(LivingEntity entity) {
    resolve();
    if (!available || entity == null) {
      return null;
    }
    try {
      return (UUID) getControllerUUID.invoke(null, entity);
    } catch (ReflectiveOperationException | LinkageError e) {
      return null;
    }
  }

  /** 控制者玩家实体；受控但不在线/无法定位时返回 null。 */
  public static net.minecraft.world.entity.player.Player controller(LivingEntity entity, Level level) {
    resolve();
    if (!available || entity == null) {
      return null;
    }
    try {
      Object result = getController.invoke(null, entity, level);
      return result instanceof net.minecraft.world.entity.player.Player player ? player : null;
    } catch (ReflectiveOperationException | LinkageError e) {
      return null;
    }
  }

  /**
   * 万物皆驯的友军判定：控制者本人、原版已经驯服的生物（TamableAnimal）、
   * 同控制者下的其他受控生物、以及骑乘中的友方乘客。非受控场景恒为 false。
   */
  public static boolean isAlly(LivingEntity attacker, Entity target) {
    resolve();
    if (!available || attacker == null || target == null) {
      return false;
    }
    try {
      return (boolean) isAlly.invoke(null, attacker, target);
    } catch (ReflectiveOperationException | LinkageError e) {
      return false;
    }
  }

  /**
   * 将生物重新登记为控制者的万物皆驯受控生物（恢复方匣放出后丢失的驯服状态）。
   * 万物皆驯把 controllerUUID 存在每个生物实体的 Forge 能力（MobControlCapability）里，
   * 而 goety 收容时只保存了 persistentData，重建实体后该能力为空，需要此调用补回。
   */
  public static boolean addControlledMob(UUID controllerUuid, Mob mob) {
    resolve();
    if (!available || controllerUuid == null || mob == null) {
      return false;
    }
    try {
      return (boolean) addControlledMob.invoke(null, controllerUuid, mob);
    } catch (ReflectiveOperationException | LinkageError e) {
      return false;
    }
  }
}