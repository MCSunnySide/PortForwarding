# PortForwarding
A bukkit plugin use JSch to forwarding local port to remote server through SSH Tunnel.

## Download
https://ci.codemc.io/job/Ghost-chu/job/PortForwarding/

## Configuration
```
ssh-host: 1.1.1.1
ssh-usr: root
ssh-pwd: 12345 #"" for disable
ssh-port: 22
private-key: /path #"" for disable
passphrase: somepassword #For private key, "" for disable
rules:
  - "localport:remoteport"
```
