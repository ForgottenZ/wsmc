package voicetunnelmod.fabric;

import net.fabricmc.api.ModInitializer;

public class VoiceTunnelFabricMod implements ModInitializer {

    public static final String MOD_ID = "voice_tunnel_mod";

    @Override
    public void onInitialize() {
        System.out.println("[VoiceTunnelMod] Initialized for Fabric 1.21.1 / Loader 0.18.4");
    }
}
