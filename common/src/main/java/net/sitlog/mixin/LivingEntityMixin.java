package net.sitlog.mixin;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.sitlog.init.ConfigInit;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.sitlog.SitlogMain;
import net.sitlog.access.LivingEntityAccess;
import net.sitlog.util.SitLogUtil;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements LivingEntityAccess {

    @Shadow
    @Nullable
    public abstract AttributeInstance getAttribute(Holder<Attribute> attribute);

    @Unique
    private static final Identifier SITTING_MODIFIER_ID = SitlogMain.identifierOf("sitting");
    @Unique
    private static final AttributeModifier SITTING_MODIFIER = new AttributeModifier(SITTING_MODIFIER_ID, -0.9D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    @Unique
    private boolean sitting;

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void damageMixin(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (info.getReturnValue()) {
            SitLogUtil.onDamage((LivingEntity) (Object) this, source);
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void jumpMixin(CallbackInfo info) {
        if ((Object) this instanceof Player && this.sitting) {
            boolean canJump = this.level().isClientSide() && ConfigInit.SYNCED_SITTING_PLAYER_CAN_JUMP != null ? ConfigInit.SYNCED_SITTING_PLAYER_CAN_JUMP : ConfigInit.CONFIG.sittingPlayerCanJump;
            if (!canJump) {
                info.cancel();
            }
        }
    }

    @Override
    public void sitLog$setSitting(boolean sit) {
        if (!this.level().isClientSide()) {
            if (sit) {
                if (!this.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(SITTING_MODIFIER_ID)) {
                    this.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(SITTING_MODIFIER);
                }
            } else {
                this.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SITTING_MODIFIER_ID);
            }
        }
        this.sitting = sit;
    }

    @Override
    public boolean sitLog$getSitting() {
        return this.sitting;
    }
}
