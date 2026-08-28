#!/bin/bash
# ============================================================
#  MindMatrix – Deploy WAR Script
#  Run after uploading mindmatrix.war and mindmatrix.sql to /tmp/
#    sudo ./deploy.sh
# ============================================================

set -e

echo ""
echo "========================================"
echo "   MindMatrix Deployment"
echo "========================================"
echo ""

# ── Step 1: Import database schema ──────────────────────────
echo "[1/3] Importing database schema..."
if [ -f /tmp/mindmatrix.sql ]; then
    # Read DB password from Tomcat config
    source /etc/default/tomcat9-mindmatrix
    mysql -u "$MM_DB_USER" -p"$MM_DB_PASS" < /tmp/mindmatrix.sql
    echo "   Database schema imported!"
else
    echo "   WARNING: /tmp/mindmatrix.sql not found. Skipping DB import."
fi

# ── Step 2: Deploy WAR file ─────────────────────────────────
echo "[2/3] Deploying WAR file..."
if [ -f /tmp/mindmatrix.war ]; then
    # Remove old deployment
    rm -rf /var/lib/tomcat9/webapps/mindmatrix
    rm -f  /var/lib/tomcat9/webapps/mindmatrix.war
    
    # Copy new WAR
    cp /tmp/mindmatrix.war /var/lib/tomcat9/webapps/
    chown tomcat:tomcat /var/lib/tomcat9/webapps/mindmatrix.war
    echo "   WAR deployed!"
else
    echo "   ERROR: /tmp/mindmatrix.war not found!"
    exit 1
fi

# ── Step 3: Restart Tomcat ──────────────────────────────────
echo "[3/3] Restarting Tomcat..."
systemctl restart tomcat9
sleep 5

# Check if Tomcat is running
if systemctl is-active --quiet tomcat9; then
    echo "   Tomcat is running!"
else
    echo "   ERROR: Tomcat failed to start. Check logs:"
    echo "   sudo journalctl -u tomcat9 --no-pager -n 50"
    exit 1
fi

# ── Get server IP ────────────────────────────────────────────
SERVER_IP=$(curl -s ifconfig.me 2>/dev/null || hostname -I | awk '{print $1}')

echo ""
echo "========================================"
echo "   DEPLOYMENT SUCCESSFUL!"
echo "========================================"
echo ""
echo "Your app is live at:"
echo "   http://${SERVER_IP}:8080/mindmatrix/"
echo ""
echo "Direct links:"
echo "   Register: http://${SERVER_IP}:8080/mindmatrix/register.html"
echo "   Login:    http://${SERVER_IP}:8080/mindmatrix/login.html"
echo "   Dashboard: http://${SERVER_IP}:8080/mindmatrix/dashboard.html"
echo ""
