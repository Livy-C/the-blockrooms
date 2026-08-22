package name.blockrooms.item;

import name.blockrooms.Blockrooms;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

public class ModArmorMaterials {
    /** 隔热套装的装备资产（客户端贴图定义，见 assets/blockrooms/equipment/heat_insulation.json） */
    public static final ResourceKey<EquipmentAsset> HEAT_INSULATION_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Blockrooms.MODID, "heat_insulation")
    );

    /**
     * 隔热套装材质：数值比皮革套略强
     * （耐久基数 6 vs 5，防御 头盔2/胸甲4/护腿3/靴子2 vs 皮革 1/3/2/1，附魔价值 12 vs 15）。
     * 暂时复用原版铜套的贴图（占位），修复材料为铜锭（占位，之后可换成灵魂合金锭）。
     */
    public static final ArmorMaterial HEAT_INSULATION = new ArmorMaterial(
            6,
            Map.of(
                    ArmorType.HELMET, 2,
                    ArmorType.CHESTPLATE, 4,
                    ArmorType.LEGGINGS, 3,
                    ArmorType.BOOTS, 2
            ),
            12,
            SoundEvents.ARMOR_EQUIP_COPPER,
            0.0F,
            0.0F,
            ItemTags.REPAIRS_COPPER_ARMOR,
            HEAT_INSULATION_ASSET
    );
}
