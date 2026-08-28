#!/bin/bash
# ============================================================
#  MindMatrix – Oracle Cloud Server Setup Script
#  Run this on your Ubuntu VM after SSH-ing in:
#    chmod +x setup_server.sh && sudo ./setup_server.sh
# ============================================================

set -e

echo ""
echo "========================================"
echo "   MindMatrix Cloud Server Setup"
echo "========================================"
echo ""

# ── Step 1: Update system ────────────────────────────────────
echo "[1/6] Updating system packages..."
apt update && apt upgrade -y

# ── Step 2: Install Java 17 ─────────────────────────────────
echo "[2/6] Installing Java 17..."
apt install -y openjdk-17-jdk
java -version

# ── Step 3: Install Tomcat 9 ────────────────────────────────
echo "[3/6] Installing Tomcat 9..."
apt install -y tomcat9 tomcat9-admin
systemctl enable tomcat9

# ── Step 4: Install MySQL ───────────────────────────────────
echo "[4/6] Installing MySQL Server..."
apt install -y mysql-server
systemctl enable mysql
systemctl start mysql

# ── Step 5: Set up the database ─────────────────────────────
echo "[5/6] Setting up MindMatrix database..."

# Generate a random password for the DB user
DB_PASS=$(openssl rand -base64 16 | tr -dc 'a-zA-Z0-9' | head -c 16)

mysql -u root <<EOF
CREATE DATABASE IF NOT EXISTS mindmatrix
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'mindmatrix'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON mindmatrix.* TO 'mindmatrix'@'localhost';
FLUSH PRIVILEGES;
EOF

echo "   Database 'mindmatrix' created."
echo "   DB User: mindmatrix"
echo "   DB Pass: ${DB_PASS}"

# ── Step 6: Configure Tomcat environment variables ──────────
echo "[6/6] Configuring Tomcat environment..."

# Set DB credentials as environment variables for Tomcat
cat > /etc/default/tomcat9-mindmatrix <<EOF2
# MindMatrix Database Configuration
MM_DB_URL=jdbc:mysql://localhost:3306/mindmatrix?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
MM_DB_USER=mindmatrix
MM_DB_PASS=${DB_PASS}
EOF2

# Add the environment file to Tomcat's systemd service
TOMCAT_OVERRIDE="/etc/systemd/system/tomcat9.service.d"
mkdir -p "$TOMCAT_OVERRIDE"
cat > "$TOMCAT_OVERRIDE/mindmatrix.conf" <<EOF3
[Service]
EnvironmentFile=/etc/default/tomcat9-mindmatrix
EOF3

systemctl daemon-reload

# ── Open firewall ────────────────────────────────────────────
echo "Opening port 8080..."
iptables -I INPUT -p tcp --dport 8080 -j ACCEPT

# Try to save iptables rules persistently
if command -v netfilter-persistent &> /dev/null; then
    netfilter-persistent save
else
    apt install -y iptables-persistent
    netfilter-persistent save
fi

# ── Done ─────────────────────────────────────────────────────
echo ""
echo "========================================"
echo "   SETUP COMPLETE!"
echo "========================================"
echo ""
echo "Database Credentials (SAVE THESE):"
echo "   DB Name: mindmatrix"
echo "   DB User: mindmatrix"
echo "   DB Pass: ${DB_PASS}"
echo ""
echo "Next Steps:"
echo "   1. Upload your mindmatrix.war file:"
echo "      scp mindmatrix.war ubuntu@YOUR_IP:/tmp/"
echo ""
echo "   2. Upload your mindmatrix.sql file:"
echo "      scp mindmatrix.sql ubuntu@YOUR_IP:/tmp/"
echo ""
echo "   3. Then run the deploy script:"
echo "      sudo ./deploy.sh"
echo ""
