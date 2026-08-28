package name.blockrooms.item.impl;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
public class RevivalArmorItem extends Item {
    private final float toughness;

    public RevivalArmorItem(ArmorMaterial material, ArmorType type, float toughness, Properties properties) {
        super(properties
                .durability(type.getDurability(material.durability()))
                .attributes(createRevivalAttributes(material, type, toughness))
                .enchantable(material.enchantmentValue())
                .component(DataComponents.EQUIPPABLE,
                        Equippable.builder(type.getSlot())
                                .setEquipSound(material.equipSound())
                                .setAsset(material.assetId())
                                .build())
                .repairable(material.repairIngredient()));
        this.toughness = toughness;
    }

    private static ItemAttributeModifiers createRevivalAttributes(ArmorMaterial material, ArmorType type, float toughness) {
        int defense = material.defense().getOrDefault(type, 0);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        EquipmentSlotGroup slotGroup = EquipmentSlotGroup.bySlot(type.getSlot());
        Identifier id = Identifier.withDefaultNamespace("armor." + type.getName());
        builder.add(Attributes.ARMOR, new AttributeModifier(id, defense, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        builder.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, toughness, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        return builder.build();
    }

    public float getToughness() {
        return toughness;
    }
}
