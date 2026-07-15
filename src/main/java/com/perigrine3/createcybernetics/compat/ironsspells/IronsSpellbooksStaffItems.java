package com.perigrine3.createcybernetics.compat.ironsspells;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class IronsSpellbooksStaffItems {
    private IronsSpellbooksStaffItems() {}

    public static Item createAnomalousStaff() {
        Item.Properties properties = new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .attributes(createAnomalousStaffAttributes());

        properties = IronsSpellbooksSpellContainerCompat.withSpellContainerAndRightClickCasting(
                properties,
                6,
                true,
                false
        );

        return new Item(properties);
    }

    private static ItemAttributeModifiers createAnomalousStaffAttributes() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        builder.add(
                Attributes.ATTACK_DAMAGE,
                modifier("anomalous_staff_attack_damage", 3.0D, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
        );

        builder.add(
                Attributes.ATTACK_SPEED,
                modifier("anomalous_staff_attack_speed", -3.0D, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
        );

        addIronsAttribute(
                builder,
                IronsSpellbooksCompat.ATTR_MAX_MANA,
                "anomalous_staff_max_mana",
                100.0D,
                AttributeModifier.Operation.ADD_VALUE
        );

        addIronsAttribute(
                builder,
                IronsSpellbooksCompat.ATTR_MANA_REGEN,
                "anomalous_staff_mana_regen",
                0.20D,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        addIronsAttribute(
                builder,
                IronsSpellbooksCompat.ATTR_SPELL_POWER,
                "anomalous_staff_spell_power",
                0.10D,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        addIronsAttribute(
                builder,
                IronsSpellbooksCompat.ATTR_LIGHTNING_SPELL_POWER,
                "anomalous_staff_lightning_spell_power",
                0.15D,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        return builder.build();
    }

    private static void addIronsAttribute(
            ItemAttributeModifiers.Builder builder,
            ResourceLocation attributeId,
            String modifierPath,
            double amount,
            AttributeModifier.Operation operation
    ) {
        var holder = IronsSpellbooksCompat.getAttributeHolder(attributeId);
        if (holder == null) {
            return;
        }

        builder.add(
                holder,
                modifier(modifierPath, amount, operation),
                EquipmentSlotGroup.MAINHAND
        );
    }

    private static AttributeModifier modifier(String path, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, path),
                amount,
                operation
        );
    }
}