package net.sitlog.mixin.client;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.sitlog.access.LivingEntityAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin <T extends LivingEntity> extends AnimalModel<T> {

    @Shadow @Mutable @Final
    public ModelPart head;
    @Shadow @Mutable @Final
    public ModelPart hat;
    @Shadow @Mutable @Final
    public ModelPart body;
    @Shadow @Mutable @Final
    public ModelPart rightArm;
    @Shadow @Mutable @Final
    public ModelPart leftArm;
    @Shadow @Mutable @Final
    public ModelPart rightLeg;
    @Shadow @Mutable @Final
    public ModelPart leftLeg;

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;copyTransform(Lnet/minecraft/client/model/ModelPart;)V"))
    private void setAnglesMixin(T livingEntity, float f, float g, float h, float i, float j, CallbackInfo info){
        if(!this.riding && ((LivingEntityAccess)livingEntity).sitLog$getSitting()){
            this.rightArm.pitch += (float) (-Math.PI / 5);
            this.leftArm.pitch += (float) (-Math.PI / 5);
            this.rightLeg.pitch = -1.5137167F;
            this.rightLeg.yaw = (float) (Math.PI / 10);
            this.rightLeg.roll = 0.07853982F;
            this.leftLeg.pitch = -1.5137167F;
            this.leftLeg.yaw = (float) (-Math.PI / 10);
            this.leftLeg.roll = -0.07853982F;

            this.rightLeg.pivotY = 22.0F;
            this.leftLeg.pivotY = 22.0F;
            this.head.pivotY = 10.0F;
            this.body.pivotY = 10.0F;
            this.leftArm.pivotY = 12.0F;
            this.rightArm.pivotY = 12.0F;
        }
    }
}
