package com.curious_mobs.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class ScarecrowEntity extends PathfinderMob {

  private static final String TAG_PLAYER_UUID = "PlayerUUID";

  private static final EntityDataAccessor<Optional<UUID>> DATA_PLAYER_UUID =
      SynchedEntityData.defineId(ScarecrowEntity.class, EntityDataSerializers.OPTIONAL_UUID);

  public ScarecrowEntity(EntityType<? extends ScarecrowEntity> entityType, Level level) {
    super(entityType, level);
    this.setPersistenceRequired();
    // 稻草人是静态装饰实体：无论由哪种方式生成（物品放置 / 命令 /
    // 其它模组召唤）都关闭 AI，避免每 tick 的寻路/朝向逻辑把
    // 设置好的朝向平滑重置回默认方向。
    this.setNoAi(true);
  }

  public void setPlayerUuid(UUID uuid) {
    this.entityData.set(DATA_PLAYER_UUID, Optional.ofNullable(uuid));
  }

  public Optional<UUID> getPlayerUuid() {
    return this.entityData.get(DATA_PLAYER_UUID);
  }

  @Override
  protected void defineSynchedData() {
    super.defineSynchedData();
    this.entityData.define(DATA_PLAYER_UUID, Optional.empty());
  }

  @Override
  public void addAdditionalSaveData(CompoundTag tag) {
    super.addAdditionalSaveData(tag);
    Optional<UUID> playerUuid = this.getPlayerUuid();
    if (playerUuid.isPresent()) {
      tag.putUUID(TAG_PLAYER_UUID, playerUuid.get());
    }
  }

  @Override
  public void readAdditionalSaveData(CompoundTag tag) {
    super.readAdditionalSaveData(tag);
    if (tag.contains(TAG_PLAYER_UUID)) {
      this.setPlayerUuid(tag.getUUID(TAG_PLAYER_UUID));
    } else {
      this.setPlayerUuid(null);
    }
  }

  public static AttributeSupplier.Builder createAttributes() {
    return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 20.0D)
        .add(Attributes.MOVEMENT_SPEED, 0.0D)
        .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
  }

  @Override
  protected void registerGoals() {
  }

  @Override
  public boolean isPushable() {
    return false;
  }

  @Override
  public void push(double x, double y, double z) {
  }
}