package com.aljun.zombiegamereborn.common.entity.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 原 Capability Provider 的 NBT 逻辑门面（1.21.1 中数据本体在 ZombieData 的 Attachment 上，
 * 序列化由 AttachmentType.serializable + INBTSerializable 完成，此处保留静态工具方法供外部调用）
 */
public class ZombieDataProvider {

    public static CompoundTag serializeNBT(IZombieData data) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isSunSensitive", data.isSunSensitive());
        tag.putString("type", data.getTypeID().toString());
        tag.putDouble("miningSpeed", data.getMiningSpeed());
        tag.putBoolean("canSwim", data.canSwim());
        tag.putBoolean("isTypeInitialized", data.isTypeInitialized());
        tag.putBoolean("fireImmune", data.fireImmune());
        tag.putBoolean("isEmpowered", data.isEmpowered());
        tag.putDouble("movement_speed_modify", data.getTotalMovementSpeedModify());
        tag.putBoolean("canJumpAttack", data.canJumpAttack());
        tag.putBoolean("canThrowTNT", data.canThrowTNT());
        tag.putBoolean("followMustSee", data.followMustSee());
        tag.putBoolean("canZombieContinueUseWeaponsInHand", data.canZombieContinueUseWeaponsInHand());
        tag.putBoolean("fleeSun",data.fleeSun());
        tag.putDouble("ambientVolumeModify", data.getAmbientVolumeModify());
        tag.putDouble("stepVolumeModify", data.getStepVolumeModify());
        if (data.getCustomLootTable() != null) {
            tag.putString("customLootTable", data.getCustomLootTable().toString());
        }
        return tag;
    }

    public static void applyData(IZombieData data, CompoundTag tag) {
        data.setSunSensitive(tag.getBoolean("isSunSensitive"));
        data.setTypeID(ResourceLocation.parse(tag.getString("type")));
        data.setMiningSpeed(tag.getDouble("miningSpeed"));
        data.enableSwim(tag.getBoolean("canSwim"));
        data.setTypeInitialized(tag.getBoolean("isTypeInitialized"));
        data.setFireImmune(tag.getBoolean("fireImmune"));
        data.setEmpowered(tag.getBoolean("isEmpowered"));
        data.setAttributesMovementSpeedModify(tag.getDouble("movementSpeedModify"));
        data.enableJumpAttack(tag.getBoolean("canJumpAttack"));
        data.enableThrowTNT(tag.getBoolean("canThrowTNT"));
        data.setFollowMustSee(tag.getBoolean("followMustSee"));
        data.setZombieContinueUseWeaponsInHand(tag.getBoolean("canZombieContinueUseWeaponsInHand"));
        data.setFleeSun(tag.getBoolean("fleeSun"));
        data.setAmbientVolumeModify(tag.getDouble("ambientVolumeModify"));
        data.setStepVolumeModify(tag.getDouble("stepVolumeModify"));
        if (tag.contains("customLootTable")) {
            data.setCustomLootTable(ResourceLocation.parse(tag.getString("customLootTable")));
        }
    }
}
