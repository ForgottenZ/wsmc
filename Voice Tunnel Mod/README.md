# Voice Tunnel Mod (SVC Compatibility Layer - 方案 A)

这是一个面向 **Simple Voice Chat (SVC)** 的兼容层模组骨架，目标是在 UDP 受限（例如 CDN / 防火墙）时，把语音数据封装到可达的 TCP/WebSocket 通道中传输。

> 对应 `docs/voicechat_wsmc_analysis_zh.md` 的 **方案 A**：新增独立 Voice Tunnel Mod，在客户端/服务端替换 SVC 的 socket 发送路径，并通过隧道转发语音包。

## 已实现内容

- 隧道协议：`TunnelFrame` / `TunnelCodec` / `FrameType` / `Priority`
- 流控策略：控制包优先 + 媒体包有界队列（`DROP_OLDEST`）
- 客户端隧道：`TunnelClient`
- 服务端会话层：`TunnelServer` + `TunnelSession`
- SVC 适配入口：`SvcSocketCompatLayer` + `SvcIntegrationExample`

## 项目结构

- `src/main/java/voicetunnelmod/protocol`：帧协议与编解码
- `src/main/java/voicetunnelmod/core`：队列与丢包策略
- `src/main/java/voicetunnelmod/client`：客户端隧道实现
- `src/main/java/voicetunnelmod/server`：服务端会话管理
- `src/main/java/voicetunnelmod/svc`：SVC 接入适配层
- `src/test/java`：轻量级自测

## 本地构建（可直接 build）

在仓库根目录执行：

```bash
bash "Voice Tunnel Mod/build.sh"
```

构建产物：

- `Voice Tunnel Mod/build/libs/voice-tunnel-mod.jar`

脚本会执行：
1. 编译主代码
2. 打包 jar
3. 编译并执行 `TunnelCodecSelfTest`

## GitHub Actions 手动构建

已提供工作流：`.github/workflows/voice-tunnel-mod-build.yml`

使用方式：
1. 进入 GitHub 仓库页面 -> **Actions**
2. 选择 **Build Voice Tunnel Mod (manual)**
3. 点击 **Run workflow**
4. 在 Artifacts 中下载 `voice-tunnel-mod-jar`

## 接入 SVC 的最小路径

1. 客户端把原 `DatagramSocket.send()` 替换为 `SvcSocketCompatLayer.sendVoicePacket()`。
2. 服务端接收到隧道语音后调用 `SvcSocketCompatLayer.onInboundVoicePacket()` 回灌 SVC 处理链。
3. 控制帧（AUTH/PING/PONG）由隧道层自动处理。

