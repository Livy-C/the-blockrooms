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
    public static final ResourceKey<EquipmentAsset> HEAT_INSULATION_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Blockrooms.MODID, "heat_insulation")
    );

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

    public static final ResourceKey<EquipmentAsset> SOUL_ALLOY_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Blockrooms.MODID, "soul_alloy")
    );

    public static final ArmorMaterial SOUL_ALLOY = new ArmorMaterial(
            23,
            Map.of(
                    ArmorType.HELMET, 2,
                    ArmorType.CHESTPLATE, 7,
                    ArmorType.LEGGINGS, 5,
                    ArmorType.BOOTS, 2
            ),
            10,
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            0.0F,
            0.0F,
            net.minecraft.tags.ItemTags.create(
                    Identifier.fromNamespaceAndPath(Blockrooms.MODID, "repairs_soul_alloy_armor")),
            SOUL_ALLOY_ASSET
    );
}