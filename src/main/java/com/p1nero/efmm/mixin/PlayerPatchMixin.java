package com.p1nero.efmm.mixin;

import com.p1nero.efmm.efmodel.ServerModelManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mixin(value = PlayerPatch.class, remap = false)
public abstract class PlayerPatchMixin<T extends Player> extends LivingEntityPatch<T> {

    @Inject(method = "getModelMatrix", at = @At("RETURN"), remap = false, cancellable = true)
    private void efhm$getModelMatrix(float partialTicks, CallbackInfoReturnable<OpenMatrix4f> cir){
        if(ServerModelManager.hasNewModel(this.original) && !this.original.isSpectator()){
            Vec3f scale = ServerModelManager.getScaleFor(this.getOriginal());
            cir.setReturnValue(cir.getReturnValue().scale(scale.x, scale.y, scale.z));
        }
    }

}
