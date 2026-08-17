package com.curious_mobs.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 访问器混入：暴露 {@link CapabilityProvider} 的受保护 final 方法
 * {@code serializeCaps()}/{@code deserializeCaps(CompoundTag)}。
 * <p>
 * Forge 把实体的能力（Curios 饰品栏、mob_controller 的 MobControlCapability 等）
 * 序列化进 {@code ForgeCaps}，而 goety 的奥术方匣收容时只经 {@code addAdditionalSaveData}
 * 保存原版字段 + {@code ForgeData}（persistentData），从不保存 {@code ForgeCaps}，
 * 导致放出时新实体能力为空（饰品清空、万物皆驯模式重置）。
 * 本访问器用于：收容时序列化能力暂存进 persistentData（随 ForgeData 一起入匣），
 * 放出时把暂存的能力反序列化回新实体。
 */
@Mixin(value = CapabilityProvider.class, remap = false)
public interface CapabilityProviderInvoker {

  @Invoker(value = "serializeCaps", remap = false)
  CompoundTag curiousMobs$serializeCaps();

  @Invoker(value = "deserializeCaps", remap = false)
  void curiousMobs$deserializeCaps(CompoundTag tag);
}
