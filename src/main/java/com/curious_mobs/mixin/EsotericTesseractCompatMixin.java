package com.curious_mobs.mixin;

import com.Polarice3.Goety.common.items.magic.EsotericTesseract;
import com.Polarice3.Goety.config.ItemConfig;
import com.Polarice3.Goety.utils.BlockFinder;
import com.curious_mobs.CuriousMobs;
import com.curious_mobs.item.DevilContractItem;
import com.curious_mobs.util.MobControllerCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

/**
 * 让诡厄巫法的「奥术方匣」（EsotericTesseract）认可并收容/放出魔鬼契约仆从。
 * <p>
 * 之前尝试给全部 {@link LivingEntity} 注入 {@code OwnableEntity} 接口来让方匣的
 * {@code target instanceof OwnableEntity && owned.getOwner() == player} 判定通过，
 * 但 1.20.1 SRG 下 {@code OwnableEntity.level()} 声明返回 {@code EntityGetter}，
 * 而 {@code Entity.level()} 返回 {@code Level}，二者是不同方法签名，注入接口后所有
 * 非驯服动物（如铁傀儡）都缺失该 SRG 描述符方法的实现，任何模组对普通生物调用
 * {@code OwnableEntity.getOwner()} 即抛 AbstractMethodError（实测由 enigmaticaddons
 * 的 LivingTickEvent 触发）。因此「全局接口注入」方案已放弃。
 * <p>
 * 本混入改为直接补丁 goety 的物品类（局部、{@code require=0}，不影响其它模组）：
 * <ul>
 *   <li>收容：对携带契约印记的生物放行 {@code instanceof OwnableEntity} 门槛，其余
 *       逻辑（容量/体型限制/入匣音效/移除生物）与 goety 原实现逐字段一致；</li>
 *   <li>放出：{@code ejectSingleServant} 与 {@code ejectAllServants} 读取已存的
 *       {@code ForgeData}（含契约标记），按 goety 原恢复流程还原生物属性并重新入世界，
 *       契约数据不会因非 OwnableEntity 而丢失。</li>
 * </ul>
 * goety 未加载时本混入不生效（目标是 mod 类，缺失则静默跳过），普通宠物行为零影响。
 */
@Mixin(EsotericTesseract.class)
public abstract class EsotericTesseractCompatMixin {

  private EsotericTesseractCompatMixin() {
  }

  /** 该生物是否为某玩家的魔鬼契约仆从（持久标签判据）。 */
  private static boolean curious_mobs$isContractServantOf(Player player, LivingEntity entity) {
    if (player == null || entity == null) {
      return false;
    }
    CompoundTag data = entity.getPersistentData();
    return data.getBoolean(DevilContractItem.KEY_SERVANT)
        && data.hasUUID(DevilContractItem.KEY_OWNER)
        && data.getUUID(DevilContractItem.KEY_OWNER).equals(player.getUUID());
  }

  /**
   * 收容时把实体的 ForgeCaps（Curios 饰品栏、万物皆驯的 MobControlCapability 等）暂存进
   * persistentData：goety 会把 persistentData 拷进 ForgeData 随方匣保存，放出后由
   * CurseEvents 的 EntityJoinLevelEvent 处理器在入世界事件恢复。这同时覆盖 goety 原生收容
   * （OwnableEntity 宠物）与本模组契约收容两条路径，杜绝饰品清空/万物皆驯模式重置。
   */
  @Inject(method = "putServantIntoTesseract", at = @At("HEAD"),
      remap = false, require = 0)
  private static void curious_mobs$stashServantCaps(
      ItemStack tesseract, Mob mob, int count, CallbackInfo ci) {
    if (mob == null) {
      return;
    }
    try {
      CompoundTag caps =
          ((CapabilityProviderInvoker) (Object) mob).curiousMobs$serializeCaps();
      if (caps != null && !caps.isEmpty()) {
        mob.getPersistentData().put(CuriousMobs.STASHED_CAPS_KEY, caps);
      }
    } catch (Throwable ignored) {
      // 能力序列化失败时静默跳过，不影响原收容流程
    }
  }

  /** 已存入方匣的仆从标签是否带契约标记（读 ForgeData）。 */
  private static boolean curious_mobs$isContractServantTag(CompoundTag servantTag) {
    if (servantTag == null || !servantTag.contains("ForgeData", 10)) {
      return false;
    }
    return servantTag.getCompound("ForgeData").getBoolean(DevilContractItem.KEY_SERVANT);
  }

  /** goety 原实现中 {@code IPersist.isNotBroken}：未损坏即允许操作。 */
  private static boolean curious_mobs$isNotBroken(ItemStack stack) {
    return stack.getDamageValue() < stack.getMaxDamage() - 1;
  }

  /**
   * 收容门槛放行：契约仆从被当作「玩家的随身宠物」处理，走 goety 原收容流程
   * （容量/体型限制一致），入匣后 ForgeData 带上契约标志供放出时识别。
   */
  @Inject(method = "onLeftClickEntity", at = @At("HEAD"), cancellable = true,
      remap = false, require = 0)
  private void curious_mobs$captureContractServant(
      ItemStack stack, Player player, Entity target, CallbackInfoReturnable<Boolean> cir) {
    if (!(target instanceof Mob mob)
        || !mob.isAlive()
        || !mob.canChangeDimensions()
        || !curious_mobs$isContractServantOf(player, mob)) {
      return;
    }
    if (stack.getItem() != (Object) this || !curious_mobs$isNotBroken(stack)) {
      cir.setReturnValue(true);
      return;
    }
    if (!player.level().isClientSide) {
      int count = EsotericTesseract.getServantsInTesseract(stack);
      if (count < ItemConfig.TesseractCapacity.get()) {
        boolean flag = true;
        if (EsotericTesseract.isLarge(mob)) {
          if (count > 0) {
            flag = false;
          }
        } else if (EsotericTesseract.isMedium(mob)) {
          if (count > ItemConfig.TesseractCapacity.get() - 4) {
            flag = false;
          }
        } else if (EsotericTesseract.isSmall(mob)) {
          if (count > ItemConfig.TesseractCapacity.get() - 2) {
            flag = false;
          }
        }
        if (flag) {
          // 在 persistentData 里暂存身份信息：goety 保存仆从时会把 persistentData
          // 拷进 ForgeData，放出时可据此恢复原 UUID 与万物皆驯控制关系。
          CompoundTag stash = mob.getPersistentData();
          stash.putUUID("curious_mobs:original_uuid", mob.getUUID());
          UUID mcController = MobControllerCompat.controllerUuid(mob);
          if (mcController != null) {
            stash.putUUID("curious_mobs:mc_controller", mcController);
          }
          EsotericTesseract.putServantIntoTesseract(stack, mob, count + 1);
          player.playSound(SoundEvents.BOTTLE_FILL_DRAGONBREATH, 1.0F, 0.75F);
          mob.discard();
        }
      }
    }
    cir.setReturnValue(true);
  }

  /**
   * 放出一只契约束仆：与 goety {@code ejectSingleServant} 对 OwnableEntity 的恢复
   * 逐字段一致（含召唤位置）。仅当方匣里第一个仆从是契约生物时接管，否则放行原逻辑。
   */
  @Inject(method = "ejectSingleServant", at = @At("HEAD"), cancellable = true,
      remap = false, require = 0)
  private static void curious_mobs$ejectContractSingle(
      ItemStack stack, Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
    if (stack.isEmpty() || stack.getTag() == null) {
      return;
    }
    Optional<String> optional = stack.getTag().getAllKeys().stream()
        .filter(string -> string.contains("Servant")).findFirst();
    if (optional.isEmpty()) {
      return;
    }
    String tagInfo = optional.get();
    CompoundTag servantTag = stack.getTag().getCompound(tagInfo);
    if (!curious_mobs$isContractServantTag(servantTag)) {
      return;
    }
    int servantCount = curious_mobs$countServant(servantTag);
    EntityType<?> entityType =
        ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(servantTag.getString("ServantType")));
    if (entityType != null) {
      Entity entity = entityType.create(level);
      if (entity instanceof Mob servant) {
        curious_mobs$restoreServant(servant, servantTag);
        curious_mobs$restoreContractIdentity(servant);
        BlockPos blockPos = BlockFinder.SummonRadius(pos, servant, level, 4);
        servant.moveTo(blockPos.getX() + 0.5D, blockPos.getY(), blockPos.getZ() + 0.5D, 0.0F, 0.0F);
        if (level instanceof ServerLevel) {
          level.addFreshEntity(servant);
        }
      }
    }
    stack.getTag().remove(tagInfo);
    cir.setReturnValue(servantCount);
  }

  /**
   * 放出全部契约仆从：接管仅在方匣里存在契约生物时发生，循环还原其中所有仆从；
   * 契约与非契约混装的场景下，非契约仆从仍按 goety 原规则（仅 OwnableEntity）恢复，
   * 结果与 goety 原生放出完全一致，无数据遗漏。
   */
  @Inject(method = "ejectAllServants", at = @At("HEAD"), cancellable = true,
      remap = false, require = 0)
  private static void curious_mobs$ejectContractAll(
      ItemStack stack, Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
    if (stack.isEmpty() || stack.getTag() == null) {
      return;
    }
    boolean contractFound = stack.getTag().getAllKeys().stream()
        .filter(string -> string.contains("Servant"))
        .map(string -> stack.getTag().getCompound(string))
        .anyMatch(EsotericTesseractCompatMixin::curious_mobs$isContractServantTag);
    if (!contractFound) {
      return;
    }
    int servantCount = 0;
    for (String tagInfo : stack.getTag().getAllKeys()) {
      if (!tagInfo.contains("Servant")) {
        continue;
      }
      CompoundTag servantTag = stack.getTag().getCompound(tagInfo);
      boolean contract = curious_mobs$isContractServantTag(servantTag);
      servantCount++;
      EntityType<?> entityType =
          ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(servantTag.getString("ServantType")));
      if (entityType != null) {
        Entity entity = entityType.create(level);
        if (entity instanceof Mob servant && (contract || entity instanceof OwnableEntity)) {
          curious_mobs$restoreServant(servant, servantTag);
          if (contract) {
            curious_mobs$restoreContractIdentity(servant);
          }
          BlockPos blockPos = pos;
          if (servantCount > 1) {
            blockPos = BlockFinder.SummonRadius(pos, servant, level, 3);
          }
          servant.moveTo(blockPos.getX() + 0.5D, blockPos.getY(), blockPos.getZ() + 0.5D, 0.0F, 0.0F);
          if (!level.isClientSide()) {
            level.addFreshEntity(servant);
          }
        }
      }
    }
    stack.setTag(new CompoundTag());
    cir.setReturnValue(servantCount);
  }

  /** 按 goety 体型槽位计算单个仆从占用的容量。 */
  private static int curious_mobs$countServant(CompoundTag servantTag) {
    if (servantTag.contains(EsotericTesseract.HUGE)) {
      return ItemConfig.TesseractCapacity.get();
    }
    if (servantTag.contains(EsotericTesseract.LARGE)) {
      return 4;
    }
    if (servantTag.contains(EsotericTesseract.BIG)) {
      return 2;
    }
    return 1;
  }

  /** 恢复仆从属性：与 goety eject 路径逐字段对应（不含 IServant 跟随/粒子装饰）。 */
  private static void curious_mobs$restoreServant(Mob servant, CompoundTag servantTag) {
    if (servantTag.contains("Invulnerable")) {
      servant.setInvulnerable(servantTag.getBoolean("Invulnerable"));
    }
    if (servantTag.contains("Silent")) {
      servant.setSilent(servantTag.getBoolean("Silent"));
    }
    if (servantTag.contains("NoGravity")) {
      servant.setNoGravity(servantTag.getBoolean("NoGravity"));
    }
    if (servantTag.contains("CanUpdate")) {
      servant.canUpdate(servantTag.getBoolean("CanUpdate"));
    }
    if (servantTag.contains("Tags", 9)) {
      servant.getTags().clear();
      ListTag listtag = servantTag.getList("Tags", 8);
      int i = Math.min(listtag.size(), 1024);
      for (int j = 0; j < i; ++j) {
        servant.getTags().add(listtag.getString(j));
      }
    }
    if (servantTag.contains("ForgeData", 10)) {
      servant.getPersistentData().merge(servantTag.getCompound("ForgeData"));
    }
    servant.readAdditionalSaveData(servantTag);
    if (!servantTag.getString("CustomName").isEmpty()) {
      servant.setCustomName(Component.Serializer.fromJson(servantTag.getString("CustomName")));
    }
  }

  /**
   * 恢复契约仆从的「身份连续性」：方匣放出产生的是全新实体（新 UUID、能力为空），
   * 而 goety 的 allyList 按原实体 UUID 登记、万物皆驯把 controllerUUID 存在实体能力里，
   * 两者都不随 goety 的 ForgeData 拷贝流动。这里从 persistentData 里读回收容时暂存的
   * 原始 UUID 与控制者 UUID，把仆从恢复成原先登记过的那个实体（从而重新被判定为盟友/
   * 受控生物，避免被当作野怪互相攻击）。
   */
  private static void curious_mobs$restoreContractIdentity(Mob servant) {
    CompoundTag data = servant.getPersistentData();
    if (data.hasUUID("curious_mobs:original_uuid")) {
      servant.setUUID(data.getUUID("curious_mobs:original_uuid"));
      data.remove("curious_mobs:original_uuid");
    }
    if (data.hasUUID("curious_mobs:mc_controller")) {
      UUID controller = data.getUUID("curious_mobs:mc_controller");
      MobControllerCompat.addControlledMob(controller, servant);
      data.remove("curious_mobs:mc_controller");
    }
  }
}