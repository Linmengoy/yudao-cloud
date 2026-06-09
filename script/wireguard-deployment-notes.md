# WireGuard 与 Caddy 反代部署记录

## 背景

- UCloud 作为公网入口服务器，运行 Docker 版 Caddy。
- manman 作为项目源站服务器，运行前端、后端网关及业务容器。
- junzun 后续加入同一 WireGuard 内网，便于服务器间互通。
- Caddy 上游已从 manman 公网 IP 切换为 WireGuard 内网 IP，避免公网源站直连。

## 服务器信息

| 节点 | SSH 别名 | 公网 IP | WireGuard IP | 角色 |
| --- | --- | --- | --- | --- |
| UCloud | `ucould` | `152.32.130.151` | `10.66.0.1` | WireGuard Hub、Caddy 公网入口 |
| manman | `manman` | `111.228.39.103` | `10.66.0.2` | 项目源站 |
| junzun | `junzun` | `111.198.60.216` | `10.66.0.3` | WireGuard 成员节点 |
| 本机 | - | - | `10.66.0.100` | WireGuard 客户端 |

## WireGuard 拓扑

采用 UCloud 作为 Hub：

```text
本机 / manman / junzun
        ↓
    UCloud Hub
        ↓
  10.66.0.0/24 内网互通
```

### UCloud

- WireGuard 地址：`10.66.0.1/24`
- 监听端口：`51820/udp`
- 服务：`wg-quick@wg0`
- 配置文件：`/etc/wireguard/wg0.conf`
- 已开启 IPv4 转发：`net.ipv4.ip_forward=1`
- 已持久化 `wg0 -> wg0` 转发规则：

```ini
PostUp = iptables -C FORWARD -i wg0 -o wg0 -j ACCEPT 2>/dev/null || iptables -I FORWARD 1 -i wg0 -o wg0 -j ACCEPT
PostDown = iptables -D FORWARD -i wg0 -o wg0 -j ACCEPT 2>/dev/null || true
```

### manman

- WireGuard 地址：`10.66.0.2/24`
- 监听端口：`51820/udp`
- 服务：`wg-quick@wg0`
- 配置文件：`/etc/wireguard/wg0.conf`
- Peer 指向 UCloud：`152.32.130.151:51820`
- `AllowedIPs = 10.66.0.0/24`

### junzun

- 系统：CentOS 8
- WireGuard 地址：`10.66.0.3/24`
- 监听端口：`51820/udp`
- 服务：`wg-quick@wg0`
- 配置文件：`/etc/wireguard/wg0.conf`
- CentOS 8 当前内核不支持 WireGuard 内核模块，已使用用户态实现：`/usr/local/bin/wireguard-go`
- systemd override：`/etc/systemd/system/wg-quick@wg0.service.d/override.conf`
- override 内容：

```ini
[Service]
Environment=WG_QUICK_USERSPACE_IMPLEMENTATION=/usr/local/bin/wireguard-go
```

## 本机客户端

- 本机 WireGuard 地址：`10.66.0.100/32`
- 客户端配置已生成在 UCloud：`/etc/wireguard/clients/local-client.conf`
- 为避免泄露密钥，本文档不记录本机客户端私钥。
- 需要导入时，从 UCloud 查看或复制该文件：

```bash
ssh ucould 'sudo cat /etc/wireguard/clients/local-client.conf'
```

客户端配置的非敏感结构如下：

```ini
[Interface]
PrivateKey = <本机客户端私钥，见 UCloud /etc/wireguard/clients/local-client.conf>
Address = 10.66.0.100/32
DNS = 223.5.5.5

[Peer]
PublicKey = <UCloud WireGuard 公钥>
Endpoint = 152.32.130.151:51820
AllowedIPs = 10.66.0.0/24
PersistentKeepalive = 25
```

## Caddy 反代

UCloud 上已有 Docker 版 Caddy：

- 容器名：`caddy-proxy`
- 配置文件：`/opt/caddy/Caddyfile`
- compose 目录：`/opt/caddy`
- 监听端口：`80`、`443`、`443/udp`

Caddy 上游已切换为 WireGuard 内网 IP：

```text
10.66.0.2:48080
10.66.0.2:13000
10.66.0.2:8081
```

不再使用 manman 公网源站 IP：

```text
111.228.39.103
```

## 项目端口

manman 源站端口：

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| `draw2video-client` | `13000` | 用户端 Next.js 前端 |
| `draw2video-admin` | `8081` | 管理端前端 |
| `yudao-gateway` | `48080` | 后端网关 |

关键路径：

| 路径 | 目标 |
| --- | --- |
| `/app-api/*` | 用户端接口，经网关或 Next 代理 |
| `/admin-api/*` | 管理端接口，反代到网关 |
| `/aigc/workflow/ws` | 用户端画布协同 WebSocket，反代到网关 |
| `/infra/ws` | 管理端客服 WebSocket，反代到网关 |

## 已验证结果

- UCloud `10.66.0.1` 可以访问 manman `10.66.0.2`。
- UCloud `10.66.0.1` 可以访问 junzun `10.66.0.3`。
- junzun `10.66.0.3` 可以访问 UCloud `10.66.0.1`。
- junzun `10.66.0.3` 可以通过 UCloud Hub 访问 manman `10.66.0.2`。
- UCloud 通过 WireGuard 可访问 manman 源站端口：
  - `http://10.66.0.2:13000/`
  - `http://10.66.0.2:8081/`
  - `http://10.66.0.2:48080/`
- UCloud Caddy 配置校验通过，容器已重启成功。

## 常用命令

查看 WireGuard 状态：

```bash
sudo wg show wg0
ip -br addr show wg0
```

重启 WireGuard：

```bash
sudo systemctl restart wg-quick@wg0
sudo systemctl status wg-quick@wg0 --no-pager -l
```

验证互通：

```bash
ping 10.66.0.1
ping 10.66.0.2
ping 10.66.0.3
```

验证 manman 源站：

```bash
curl -I http://10.66.0.2:13000/
curl -I http://10.66.0.2:8081/
curl -I http://10.66.0.2:48080/
```

校验并重启 Caddy：

```bash
cd /opt/caddy
docker exec caddy-proxy caddy validate --config /etc/caddy/Caddyfile
docker compose restart caddy
docker ps --filter name=caddy-proxy
docker logs --since 2m caddy-proxy 2>&1 | tail -50
```

检查 Caddy 上游是否仍有公网 IP：

```bash
cd /opt/caddy
grep -n '10.66.0.2\|111.228.39.103' Caddyfile
```

## 备份文件

配置调整前已在服务器侧创建过备份，常见命名模式：

```text
/etc/wireguard/wg0.conf.bak.*
/opt/caddy/Caddyfile.bak.*
```

## 注意事项

- 本文档不记录任何 WireGuard 私钥。
- junzun 当前使用用户态 WireGuard，性能低于内核态；如果未来有维护窗口，建议升级内核后切换到内核态 WireGuard。
- UCloud 是 Hub，若 UCloud 的 `wg0` 或 `51820/udp` 不可用，其它节点之间的跨节点访问会受影响。
- Caddy 当前通过 WireGuard 内网访问 manman 源站，域名入口仍在 UCloud。
