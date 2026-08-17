package com.curious_mobs.api;

/**
 * 供其他代码强转 Player 使用，读取/设置"永久解除诅咒"同步标记
 * （该标记存放于 SynchedEntityData，客户端可自动同步）。
 */
public interface PermanentFlagAccessor {

  boolean curiousMobs$isPermanent();

  void curiousMobs$setPermanent(boolean value);
}
