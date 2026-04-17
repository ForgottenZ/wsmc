package voicetunnelmod.demo;

import voicetunnelmod.svc.SvcSocketCompatLayer;
import voicetunnelmod.svc.VoicePacketSink;
import voicetunnelmod.transport.InMemoryTunnelPipe;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 最小可运行 Demo：
 * - 模拟 client/server 两端通过 tunnel 互发语音包
 * - 不依赖 SVC / Minecraft 运行时
 */
public class LoopbackDemo {

    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(2);

        VoicePacketSink clientSink = payload -> {
            System.out.println("CLIENT <- " + new String(payload, StandardCharsets.UTF_8));
            latch.countDown();
        };

        VoicePacketSink serverSink = payload -> {
            System.out.println("SERVER <- " + new String(payload, StandardCharsets.UTF_8));
            latch.countDown();
        };

        InMemoryTunnelPipe pipe = new InMemoryTunnelPipe(clientSink, serverSink);

        SvcSocketCompatLayer clientCompat = new SvcSocketCompatLayer(pipe.clientTransport(), clientSink);
        SvcSocketCompatLayer serverCompat = new SvcSocketCompatLayer(pipe.serverTransport(), serverSink);

        clientCompat.sendVoicePacket("voice-from-client".getBytes(StandardCharsets.UTF_8));
        serverCompat.sendVoicePacket("voice-from-server".getBytes(StandardCharsets.UTF_8));

        boolean ok = latch.await(3, TimeUnit.SECONDS);
        pipe.close();

        if (!ok) {
            throw new IllegalStateException("Loopback demo timed out");
        }

        System.out.println("LoopbackDemo PASS");
    }
}
