/**
 * MindMatrix – sw.js
 * Service Worker for Background Notifications & Offline Push Alerts
 */

self.addEventListener('install', function(event) {
    self.skipWaiting();
});

self.addEventListener('activate', function(event) {
    event.waitUntil(self.clients.claim());
});

self.addEventListener('push', function(event) {
    var data = {};
    if (event.data) {
        try {
            data = event.data.json();
        } catch (e) {
            data = { body: event.data.text() };
        }
    }

    var title = data.title || "🔥 MindMatrix Streak Alert";
    var options = {
        body: data.body || "You haven't played MindMatrix today! Complete a puzzle now to protect your daily streak.",
        icon: 'favicon.ico',
        badge: 'favicon.ico',
        tag: 'mindmatrix-background-streak',
        renotify: true,
        data: { url: 'dashboard.html' }
    };

    event.waitUntil(
        self.registration.showNotification(title, options)
    );
});

self.addEventListener('notificationclick', function(event) {
    event.notification.close();
    var targetUrl = (event.notification.data && event.notification.data.url) ? event.notification.data.url : 'dashboard.html';

    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function(clientList) {
            for (var i = 0; i < clientList.length; i++) {
                var client = clientList[i];
                if (client.url.indexOf(targetUrl) !== -1 && 'focus' in client) {
                    return client.focus();
                }
            }
            if (clients.openWindow) {
                return clients.openWindow(targetUrl);
            }
        })
    );
});
