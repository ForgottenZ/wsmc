package voicetunnelmod.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import voicetunnelmod.fabric.runtime.VoiceTunnelRuntime;

public class VoiceTunnelFabricMod implements ModInitializer {

    public static final String MOD_ID = "voice_tunnel_mod";

    @Override
    public void onInitialize() {
        if (!VoiceTunnelRuntime.enabled()) {
            System.out.println("[VoiceTunnelMod] disabled by -Dvtm.enable=false");
            return;
        }

        VoiceTunnelRuntime.registerPayloadTypes();

        ServerPlayNetworking.registerGlobalReceiver(VoiceTunnelRuntime.C2SVoicePayload.ID,
                (payload, context) -> context.server().execute(
                        () -> VoiceTunnelRuntime.onServerInbound(context.player(), payload.data())));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                VoiceTunnelRuntime.onPlayerDisconnect(handler.getPlayer()));

        System.out.println("[VoiceTunnelMod] server bridge enabled: SVC voice over Minecraft TCP channel");
    }
}
