package net.sitlog.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sitlog.SitlogMain;
import net.sitlog.access.LivingEntityAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements LivingEntityAccess {

    @Shadow
    @Nullable
    public abstract EntityAttributeInstance getAttributeInstance(RegistryEntry<EntityAttribute> attribute);

    @Unique
    private static final Identifier SITTING_MODIFIER_ID = SitlogMain.identifierOf("sitting");
    @Unique
    private static final EntityAttributeModifier SITTING_MODIFIER = new EntityAttributeModifier(SITTING_MODIFIER_ID, -0.9D, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    @Unique
    private boolean sitting;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void sitLog$setSitting(boolean sit) {
        if (!this.getWorld().isClient()) {
            if (sit) {
                if (!this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).hasModifier(SITTING_MODIFIER_ID)) {
                    this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).addTemporaryModifier(SITTING_MODIFIER);
                }
            } else {
                this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).removeModifier(SITTING_MODIFIER_ID);
            }
        }
        this.sitting = sit;
    }

    @Override
    public boolean sitLog$getSitting() {
        return this.sitting;
    }
}
