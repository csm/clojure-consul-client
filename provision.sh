#!/usr/bin/env bash

set -e

serverIp=$1

dpkg -s unzip &>/dev/null || {
    apt-get -y update && apt-get install -y unzip
}

if [[ ! -f /tmp/consul.zip ]]; then
    version='1.6.0'
    wget -q https://releases.hashicorp.com/consul/${version}/consul_${version}_linux_amd64.zip -O /tmp/consul.zip
fi

if [[ ! -f /usr/local/bin/consul ]]; then
    cd /usr/local/bin
    unzip /tmp/consul.zip
fi

if [[ ! -f /etc/systemd/system/consul.service ]]; then
    cat > /etc/systemd/system/consul.service <<EOF
[Unit]
Description=consul agent
Requires=network-online.target
After=network-online.target

[Service]
EnvironmentFile=-/etc/sysconfig/consul
Restart=on-failure
ExecStart=/usr/local/bin/consul agent \$CONSUL_FLAGS -config-dir=/etc/systemd/system/consul.d
ExecReload=/bin/kill -HUP \$MAINPID

[Install]
WantedBy=multi-user.target
EOF
fi

if [[ ! -f /etc/systemd/system/consul.d/init.json ]]; then
    mkdir -p /etc/systemd/system/consul.d
    cat >/etc/systemd/system/consul.d/init.json <<EOF
{
    "server": true,
    "ui": true,
    "advertise_addr": "${serverIp}",
    "client_addr": "${serverIp}",
    "data_dir": "/tmp/consul",
    "bootstrap_expect": 3
}
EOF
fi