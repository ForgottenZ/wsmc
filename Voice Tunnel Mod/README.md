# Voice Tunnel Mod (SVC Compatibility Layer - 方案 A)

这是一个面向 **Simple Voice Chat (SVC)** 的兼容层模组骨架，目标是在 UDP 受限（例如 CDN / 防火墙）时，把语音数据封装到可达的 TCP/WebSocket 通道中传输。

> 对应 `docs/voicechat_wsmc_analysis_zh.md` 的 **方案 A**：新增独立 Voice Tunnel Mod，在客户端/服务端替换 SVC 的 socket 发送路径，并通过隧道转发语音包。

## Fabric 目标版本

- Minecraft: **1.21.1**
- Fabric Loader: **0.18.4**
- Fabric API: **0.118.4+1.21.1**
- Java: **21**

Fabric 子项目路径：`Voice Tunnel Mod/fabric`

## 已实现内容

- 隧道协议：`TunnelFrame` / `TunnelCodec` / `FrameType` / `Priority`
- 流控策略：控制包优先 + 媒体包有界队列（`DROP_OLDEST`）
- 客户端隧道：`TunnelClient`
- 服务端会话层：`TunnelServer` + `TunnelSession`
- SVC 适配入口：`SvcSocketCompatLayer` + `SvcIntegrationExample`
- 本地联调通路：`InMemoryTunnelPipe` + `LoopbackDemo`
- Fabric 入口：`voicetunnelmod.fabric.VoiceTunnelFabricMod`

## 项目结构

- `src/main/java/voicetunnelmod/*`：隧道核心逻辑
- `fabric/src/main/java`：Fabric 入口代码
- `fabric/src/main/resources/fabric.mod.json`：Fabric 元数据
- `src/test/java`：轻量级自测

## 本地构建

### 1) 核心逻辑本地自测构建

```bash
bash "Voice Tunnel Mod/build.sh"
```

### 2) Fabric mod 构建（1.21.1 / loader 0.18.4）

```bash
cd "Voice Tunnel Mod/fabric"
gradle -p "Voice Tunnel Mod/fabric" clean build
```

产物路径：

- `Voice Tunnel Mod/fabric/build/libs/voice-tunnel-mod-fabric-<version>.jar`

## GitHub Actions（手动触发）

- `.github/workflows/voice-tunnel-mod-build.yml`
  - 构建并上传：`voice-tunnel-mod-fabric-mc1.21.1-loader0.18.4`
- `.github/workflows/voice-tunnel-mod-release.yml`
  - 手动输入 `version`
  - 上传版本化 jar，文件名带 `mc1.21.1-loader0.18.4`

## 接入 SVC 的最小路径

1. 客户端把原 `DatagramSocket.send()` 替换为 `SvcSocketCompatLayer.sendVoicePacket()`。
2. 服务端接收到隧道语音后调用 `SvcSocketCompatLayer.onInboundVoicePacket()` 回灌 SVC 处理链。
3. 控制帧（AUTH/PING/PONG）由隧道层自动处理。

