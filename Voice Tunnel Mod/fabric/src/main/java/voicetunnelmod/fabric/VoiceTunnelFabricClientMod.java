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

        ClientPlayNetworking.registerGlobalReceiver(VoiceTunnelRuntime.S2C_VOICE_ID,
                (client, handler, buf, responseSender) -> {
                    byte[] data = buf.readByteArray();
                    client.execute(() -> VoiceTunnelRuntime.onClientInbound(data));
                });

        System.out.println("[VoiceTunnelMod] client bridge enabled: SVC voice over Minecraft TCP channel");
    }
}
