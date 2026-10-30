package net.sitlog.mixin.client;

import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.sitlog.SitLogClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class BipedEntityModelMixin {

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

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void setAnglesMixin(HumanoidRenderState state, CallbackInfo info){
        if(!state.isPassenger && ((FabricRenderState) (Object) state).getDataOrDefault(SitLogClient.SITTING, false)){
            this.rightArm.xRot += (float) (-Math.PI / 5);
            this.leftArm.xRot += (float) (-Math.PI / 5);
            this.rightLeg.xRot = -1.5137167F;
            this.rightLeg.yRot = (float) (Math.PI / 10);
            this.rightLeg.zRot = 0.07853982F;
            this.leftLeg.xRot = -1.5137167F;
            this.leftLeg.yRot = (float) (-Math.PI / 10);
            this.leftLeg.zRot = -0.07853982F;

            this.rightLeg.y = 22.0F;
            this.leftLeg.y = 22.0F;
            this.head.y = 10.0F;
            this.body.y = 10.0F;
            this.leftArm.y = 12.0F;
            this.rightArm.y = 12.0F;
        }
    }
}
