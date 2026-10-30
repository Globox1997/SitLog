package net.sitlog.mixin;

import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.sitlog.access.LivingEntityAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Avatar.class)
public abstract class AvatarMixin extends LivingEntity {

    @Unique
    private static final EntityDimensions SITTING_POSE = EntityDimensions.fixed(0.6F, 1.3F).withEyeHeight(1.12F);

    public AvatarMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void getBaseDimensionsMixin(Pose pose, CallbackInfoReturnable<EntityDimensions> info) {
        if (((LivingEntityAccess) this).sitLog$getSitting()) {
            info.setReturnValue(SITTING_POSE);
        }
    }
}
