package voicetunnelmod.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import voicetunnelmod.fabric.runtime.VoiceTunnelRuntime;

import java.net.SocketAddress;

@Pseudo
@Mixin(targets = "de.maxhenkel.voicechat.plugins.impl.ClientVoicechatSocketImpl")
public class MixinClientVoicechatSocketImpl {

    @Inject(method = "open", at = @At("HEAD"), cancellable = true)
    private void vtm$open(CallbackInfo ci) {
        if (VoiceTunnelRuntime.enabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "isClosed", at = @At("HEAD"), cancellable = true)
    private void vtm$isClosed(CallbackInfoReturnable<Boolean> cir) {
        if (VoiceTunnelRuntime.enabled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "send", at = @At("HEAD"), cancellable = true)
    private void vtm$send(byte[] data, SocketAddress address, CallbackInfo ci) {
        if (VoiceTunnelRuntime.enabled()) {
            VoiceTunnelRuntime.sendVoiceFromClient(data);
            ci.cancel();
        }
    }

    @Inject(method = "read", at = @At("HEAD"), cancellable = true)
    private void vtm$read(CallbackInfoReturnable<Object> cir) {
        if (VoiceTunnelRuntime.enabled()) {
            try {
                Object packet = VoiceTunnelRuntime.pollClientInboundBlocking();
                cir.setReturnValue(packet);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
