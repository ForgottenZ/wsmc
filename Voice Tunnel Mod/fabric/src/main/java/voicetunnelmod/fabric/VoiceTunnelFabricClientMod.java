package voicetunnelmod.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import voicetunnelmod.fabric.runtime.VoiceTunnelRuntime;

public class VoiceTunnelFabricClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (!VoiceTunnelRuntime.enabled()) {
            return;
        }

        VoiceTunnelRuntime.registerPayloadTypes();

        ClientPlayNetworking.registerGlobalReceiver(VoiceTunnelRuntime.S2CVoicePayload.ID,
                (payload, context) -> context.client().execute(
                        () -> VoiceTunnelRuntime.onClientInbound(payload.data())));

        System.out.println("[VoiceTunnelMod] client bridge enabled: SVC voice over Minecraft TCP channel");
    }
}
