# WSMC 模组行为与 Simple Voice Chat（UDP over TCP）兼容性分析

## 1. 这个 WSMC 模组当前做了什么

### 1.1 目标与总体机制
- WSMC 在 **不改动 Minecraft 协议本身** 的前提下，把原本 Minecraft Java 的 TCP 流量封装在 WebSocket 二进制帧中传输。
- 服务端和客户端都可安装；服务端可同时接受 Vanilla TCP 和 WebSocket（可通过配置禁用 Vanilla TCP）。
- WebSocket 与 Vanilla TCP 复用同一监听端口，服务端通过连接首字节是否为 `GET` 来分流 HTTP/WebSocket 与原生 TCP。

### 1.2 客户端侧关键路径
- `ServerAddress.parseString()` 被 Mixin 扩展，可直接解析 `ws://` / `wss://` 地址，并保存额外连接信息（URI、SNI、HTTP Host）。
- 连接阶段会给 Netty pipeline 插入 `HttpClientCodec` + `HttpObjectAggregator` + WebSocket 压缩 + `WebSocketClientHandler`。
- 对 `wss://` 会再注入 `SslHandler`，并设置 SNI。
- 握手成功前，Minecraft 发出的 ByteBuf 会被挂起到 `handshakeFuture` 回调后再发送；握手失败则连接关闭。

### 1.3 服务端侧关键路径
- 在服务端连接 pipeline 里先挂 `HttpGetSniffer`，检测前 3 字节：
  - `GET`：切换到 `HttpServerCodec` + `HttpServerHandler`，尝试 WS 升级。
  - 非 `GET`：按 Vanilla TCP 处理（或在 `wsmc.disableVanillaTCP=true` 时直接拒绝）。
- WS 握手成功后，HTTP handler 被替换为 `WebSocketServerHandler`，并把二进制帧内容当作 Minecraft 原始 ByteBuf 继续向后传。
- 握手请求 `HttpRequest` 被保存到 `Connection` 扩展字段，可供其它逻辑获取反代头（如 X-Forwarded-For）。

### 1.4 此模组“不做”的事情
- 不处理或转发 UDP。
- 不修改 gameplay。
- 不额外新增语音专用隧道协议。

---

## 2. 如果客户端尝试连接 `ws://example.com` 会发生什么

假设你在多人服务器地址里直接填 `ws://example.com`：

1. 地址会被识别为 WebSocket 连接，默认端口会变成 **80**，路径默认为 `/`。
2. Minecraft 客户端将先建立到 `example.com:80` 的 TCP 连接，再发 WebSocket 握手（Host: `example.com`）。
3. `example.com` 不是 Minecraft WebSocket 端点，通常会返回普通 HTTP 响应（例如 200/301），而不是 `101 Switching Protocols`。
4. `WebSocketClientHandshaker.finishHandshake(...)` 会抛 `WebSocketHandshakeException`，`handshakeFuture` 失败，连接关闭。
5. 结果表现为：服务器列表 ping / 进入服务器都会失败（本质是握手没升级成功，Minecraft 数据无法开始传输）。

> 只有当目标地址提供符合 RFC6455 的 WS 握手，并且后端实际上在 WebSocket 二进制帧里说 Minecraft 协议时，才会成功。

---

## 3. 你想做的“与 Simple Voice Chat 兼容（UDP over TCP）”现状判断

### 3.1 当前为什么不兼容
Simple Voice Chat 的语音平面是独立 UDP：
- 服务端 `VoicechatSocketImpl` 使用 `DatagramSocket` 监听/发送 UDP。
- 客户端 `ClientVoicechatSocketImpl` 也使用 `DatagramSocket`。
- 客户端连接对象 `ClientVoicechatConnection` 每次语音发包都直接发到 `InetSocketAddress(serverIP, serverPort)`（UDP）。
- `SecretPacket` 只是在 Minecraft 主连接（TCP）里下发语音 UDP 的 host/port/密钥，不会把语音数据走 Minecraft TCP 通道。

因此：即使 WSMC 让 Minecraft 主连接能走 WebSocket/TCP，语音流仍然走 UDP，NAT/CDN/防火墙不放行时语音仍会失败。

### 3.2 “UDP over TCP”要落地，最小可行架构
建议不要真的“UDP over TCP”逐包强行可靠重传，而是做“**语音包通过现有 Minecraft 通道转发**”或“**独立 TCP/WebSocket 语音隧道**”。

可选方案：

- 方案 A（侵入小，推荐）：
  - 新增一个“Voice Tunnel Mod”（客户端+服务端）。
  - 在双方各自 hook Simple Voice Chat 的 socket 实现：
    - client: 替换 `ClientVoicechatSocketImpl`，把 `send()` 改为写入隧道通道。
    - server: 替换 `VoicechatSocketImpl`，把语音包转发到隧道通道。
  - 隧道底层可复用 WSMC 的 WebSocket（同端口/同会话）或另开 wss 路径。

- 方案 B（侵入大，不推荐首版）：
  - 直接改 SVC 内核，把 UDP socket 抽象成可插拔 transport（UDP / WS / TCP）。
  - 维护成本高，升级 SVC 版本时冲突大。

### 3.3 实现细节建议（关键点）
1. **保留 SVC 现有加密与包格式**：仅替换底层传输，减少协议破坏风险。  
2. **引入有界队列 + 丢包策略**：语音低延迟优先；TCP 堵塞时丢旧包，避免排队爆延迟。  
3. **分离控制与媒体优先级**：KeepAlive/Auth 包优先，语音帧次之。  
4. **避免 Head-of-Line 放大**：若只能用 TCP，建议小帧、禁 Nagle（`TCP_NODELAY`）、限缓存。  
5. **协商机制**：登录后先协商是否支持 tunnel；不支持则回退 UDP（保持兼容）。

### 3.4 对这个仓库可做的下一步
1. 在 WSMC 增加一个可复用的“二进制子通道”抽象（区分 Minecraft 数据与 Voice 数据）。
2. 在 SVC 侧做 transport 适配层（先在该仓库内 PoC，不直接 fork 大改）。
3. 先实现单向上行语音通路（client->server），验证延迟与抖动；再补下行混音分发。
4. 最后再做配置：`voice_transport = udp | ws_tunnel`，默认仍 `udp`。

---

## 4. 结论
- WSMC 当前只解决 Minecraft 主连接的 WebSocket/TCP 传输，不覆盖 UDP。  
- `ws://example.com` 会因 WebSocket 握手失败而无法用于 Minecraft 连接。  
- 要兼容 Simple Voice Chat，必须给语音面额外做隧道/传输适配层；最可控路线是新增适配层并尽量复用 SVC 现有协议与加密，而不是重写语音协议本身。
