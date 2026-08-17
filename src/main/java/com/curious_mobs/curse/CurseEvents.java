package com.curious_mobs.curse;

import com.curious_mobs.CuriousMobs;
import com.curious_mobs.api.PermanentFlagAccessor;
import com.curious_mobs.mixin.CapabilityProviderInvoker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class CurseEvents {

  private static final double AGGRO_RANGE = 16.0D;

  private CurseEvents() {
  }

  /**
   * 兼容层：非玩家实体（含替死稻草人、以及经“妙手空空”装备饰品的生物）
   * 佩戴诅咒饰品时，补上“仅对玩家生效”的诅咒效果；取下时清理。
   */
  @SubscribeEvent
  public static void onCurioChange(CurioChangeEvent event) {
    LivingEntity entity = event.getEntity();

    if (entity.level().isClientSide || entity instanceof Player) {
      return;
    }

    ItemStack from = event.getFrom();
    ItemStack to = event.getTo();

    if (!to.isEmpty() && CurseHelper.isCursedCurio(to, entity)) {
      CurseHelper.applyWornCurse(entity, to);
    } else if (!from.isEmpty() && CurseHelper.isCursedCurio(from, entity)) {
      CurseHelper.removeWornCurse(entity, from);
    }
  }

  /**
   * 永久解除标记（curious_mobs:permanent）随玩家重生/跨维度/重进保留：
   * 复制到新实体，防止死亡后 EL 重新对玩家施加诅咒。
   * 同时镜像到新实体的 SynchedEntityData（客户端也需可见）。
   */
  @SubscribeEvent
  public static void onPlayerClone(PlayerEvent.Clone event) {
    boolean permanent = event.getOriginal().getPersistentData()
        .getBoolean("curious_mobs:permanent");

    if (permanent && !event.getEntity().level().isClientSide) {
      event.getEntity().getPersistentData().putBoolean("curious_mobs:permanent", true);
      if (event.getEntity() instanceof PermanentFlagAccessor accessor) {
        accessor.curiousMobs$setPermanent(true);
      }
      CuriousMobs.LOGGER.info("[CT] onPlayerClone carried permanent-freedom marker");
    }
  }

  /**
   * 玩家登录时（重进世界）把 persistentData 里的永久解除标记镜像到
   * SynchedEntityData，使客户端本地玩家可见。
   */
  @SubscribeEvent
  public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() == null || event.getEntity().level().isClientSide) {
      return;
    }
    if (CurseHelper.isPermanent(event.getEntity())) {
      if (event.getEntity() instanceof PermanentFlagAccessor accessor) {
        accessor.curiousMobs$setPermanent(true);
      }
      CuriousMobs.LOGGER.info("[CT] onPlayerLoggedIn mirrored permanent-freedom marker");
    }
  }

  /**
   * 奥术方匣放出仆从的 ForgeCaps 恢复：收容时把 serializeCaps() 结果暂存进 persistentData
   * （随 ForgeData 一起存进方匣），实体入世界后这里反序列化回能力（Curios 饰品栏、
   * 万物皆驯的 MobControlCapability 等），再清除暂存键。对契约仆从与 goety 原生
   * OwnableEntity 宠物均生效，且只在携带暂存键时触发，不影响普通实体入世界。
   */
  @SubscribeEvent
  public static void onServantJoinLevel(EntityJoinLevelEvent event) {
    Entity entity = event.getEntity();
    if (entity == null || entity.level().isClientSide) {
      return;
    }
    CompoundTag data = entity.getPersistentData();
    if (!data.contains(CuriousMobs.STASHED_CAPS_KEY, 10)) {
      return;
    }
    CompoundTag stashed = data.getCompound(CuriousMobs.STASHED_CAPS_KEY);
    if (entity instanceof CapabilityProviderInvoker invoker) {
      try {
        invoker.curiousMobs$deserializeCaps(stashed);
        // 恢复成功才清除暂存键，失败时保留以便下次 join 重试；
        // 捕获的异常已在下方记录，不会阻断实体入世界。
        data.remove(CuriousMobs.STASHED_CAPS_KEY);
        CuriousMobs.LOGGER.info("[CT] restored ForgeCaps for {} from tesseract",
            entity.getName().getString());
      } catch (Throwable t) {
        CuriousMobs.LOGGER.warn("[CT] failed to restore ForgeCaps for {}: {}",
            entity.getName().getString(), t.toString());
      }
    } else {
      data.remove(CuriousMobs.STASHED_CAPS_KEY);
    }
  }

  /**
   * 兼容层：让带有诅咒标记（curious_mobs:curse 生命上限修饰符）或实际佩戴诅咒饰品
   * 的非玩家实体，像七咒之戒的佩戴者一样承受特殊诅咒：
   * <ul>
   *   <li>范围内的所有可攻击生物主动攻击；</li>
   *   <li>佩戴 EL 诅咒饰品（如七咒之戒）时持续燃烧（永燃，入水可扑灭）。</li>
   * </ul>
   * 幻翼与无法睡眠等玩家专属诅咒不需要对实体生效，故不实现。
   */
  @SubscribeEvent
  public static void onLivingTick(LivingEvent.LivingTickEvent event) {
    LivingEntity entity = event.getEntity();

    if (entity.level().isClientSide || entity instanceof Player) {
      return;
    }

    // 每 20 tick 扫描一次，避免每 tick 反复遍历饰品栏
    if (entity.tickCount % 20 != 0) {
      return;
    }

    boolean cursed = hasCurseMarker(entity) || hasCursedCurio(entity);
    if (!cursed) {
      return;
    }

    // 永燃：佩戴 EL 诅咒饰品（如七咒之戒），或承受了来自 EL 诅咒饰品的转移诅咒的
    // 实体不会因装备而被自动点燃——只有被火/岩浆等火源点燃后才进入永燃，并被持续
    // 维持燃烧；一旦被水或雨水扑灭则熄灭，之后若再次被火/岩浆点燃又重新永燃
    // （即"被水扑灭后，直到再次被火源点燃才复燃"）。
    if (hasElCursedCurio(entity) || hasElCurseFlag(entity)) {
      if (entity.isInWaterOrRain()) {
        if (entity.getRemainingFireTicks() > 0) {
          entity.clearFire();
          CuriousMobs.LOGGER.info("[CT] EL eternal fire extinguished for {} by water/rain",
              entity.getName().getString());
        }
      } else if (entity.getRemainingFireTicks() > 0) {
        // 已被火/岩浆点燃：维持永燃，避免火焰自然熄灭
        if (entity.getRemainingFireTicks() < 20) {
          entity.setSecondsOnFire(8);
        }
      }
    }

    // 每 60 tick（约 3 秒）：范围内所有可攻击的生物主动攻击目标
    if (entity.tickCount % 60 != 0) {
      return;
    }

    Level level = entity.level();
    AABB box = entity.getBoundingBox().inflate(AGGRO_RANGE);
    TargetingConditions conditions = TargetingConditions.forCombat()
        .range(AGGRO_RANGE);

    for (Mob mob : level.getNearbyEntities(Mob.class, conditions, entity, box)) {
      if (mob.isNoAi() || !mob.canAttack(entity)) {
        continue;
      }
      mob.setTarget(entity);
      if (mob instanceof NeutralMob neutral) {
        neutral.setRemainingPersistentAngerTime(600);
        neutral.setPersistentAngerTarget(entity.getUUID());
      }
      CuriousMobs.LOGGER.info("[CT] cursed entity {} caused {} to attack",
          entity.getName().getString(), mob.getName().getString());
    }
  }

  private static boolean hasElCurseFlag(LivingEntity entity) {
    return entity.getPersistentData().getBoolean("curious_mobs:el_fire");
  }

  private static boolean hasCursedCurio(LivingEntity entity) {
    boolean[] result = new boolean[1];
    CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
      for (ICurioStacksHandler entry : handler.getCurios().values()) {
        IDynamicStackHandler stacks = entry.getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
          if (CurseHelper.isCursedCurio(stacks.getStackInSlot(i), entity)) {
            result[0] = true;
            return;
          }
        }
      }
    });
    return result[0];
  }

  private static boolean hasElCursedCurio(LivingEntity entity) {
    boolean[] result = new boolean[1];
    CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
      for (ICurioStacksHandler entry : handler.getCurios().values()) {
        IDynamicStackHandler stacks = entry.getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
          if (CurseHelper.isElCursedCurio(stacks.getStackInSlot(i))) {
            result[0] = true;
            return;
          }
        }
      }
    });
    return result[0];
  }

  private static boolean hasCurseMarker(LivingEntity entity) {
    AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);

    if (health == null) {
      return false;
    }
    return health.getModifiers().stream()
        .anyMatch(m -> "curious_mobs:curse".equals(m.getName()));
  }
}