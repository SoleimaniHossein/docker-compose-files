#!/bin/bash

set -e

echo "🚀 Setting up Nextcloud 32 with High-Performance Backend for 100+ video calls"

# Create directory structure
mkdir -p \
  config/{postgres,nginx,php,janus,signaling,nextcloud} \
  env \
  data/certs/live/${DOMAIN} \
  data/{db,redis,nextcloud_html,nextcloud_data,nats,signaling} \
  apps

# Generate secrets
export HPB_STATIC_SECRET=$(openssl rand -hex 32)
export HPB_API_SECRET=$(openssl rand -hex 32)
export TURN_SHARED_SECRET=$(openssl rand -hex 32)
export JANUS_ADMIN_PW=$(openssl rand -hex 16)
export POSTGRES_PASSWORD=$(openssl rand -hex 16)
export NEXTCLOUD_ADMIN_PASSWORD=$(openssl rand -hex 12)

# Create .env file
cat > .env <<EOF
DOMAIN=nc.local
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
NEXTCLOUD_ADMIN_USER=admin
NEXTCLOUD_ADMIN_PASSWORD=${NEXTCLOUD_ADMIN_PASSWORD}
HPB_STATIC_SECRET=${HPB_STATIC_SECRET}
HPB_API_SECRET=${HPB_API_SECRET}
TURN_SHARED_SECRET=${TURN_SHARED_SECRET}
TURN_REALM=nc.local
TURN_EXTERNAL_IP=192.168.1.1
JANUS_ADMIN_PW=${JANUS_ADMIN_PW}
EOF

# Create PHP-FPM pool configuration for NC32
cat > config/nextcloud/www.conf <<EOF
[www]
user = www-data
group = www-data
listen = 0.0.0.0:9000
listen.owner = www-data
listen.group = www-data
pm = dynamic
pm.max_children = 100
pm.start_servers = 10
pm.min_spare_servers = 5
pm.max_spare_servers = 20
pm.max_requests = 500
clear_env = no
EOF

# Set proper permissions
chmod 600 .env
chmod -R 750 config data

echo "✅ Nextcloud 32 setup complete!"
echo "📋 Nextcloud 32 brings:"
echo "   - Improved Talk performance"
echo "   - Better High-Performance Backend integration"
echo "   - Enhanced security features"
echo "   - Latest WebRTC improvements"