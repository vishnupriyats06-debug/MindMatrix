/**
 * MindMatrix – streakReminder.js
 * Device Notification Alerts & Streak Reminders Management
 */

(function(window) {
    'use strict';

    var StreakReminder = {
        STORAGE_KEY_PREFIX: 'mm_pref_',
        lastState: null,

        /**
         * Check if Web Notifications API is supported
         */
        isSupported: function() {
            return ('Notification' in window);
        },

        /**
         * Register background Service Worker for browser notifications
         */
        registerServiceWorker: function() {
            if ('serviceWorker' in navigator) {
                navigator.serviceWorker.register('sw.js').then(function(reg) {
                    console.log('MindMatrix ServiceWorker registered successfully:', reg.scope);
                }).catch(function(err) {
                    console.warn('ServiceWorker registration skipped:', err);
                });
            }
        },

        /**
         * Get current Notification permission status
         */
        getPermissionStatus: function() {
            if (!this.isSupported()) return 'unsupported';
            return Notification.permission; // 'granted', 'denied', or 'default'
        },

        /**
         * Request notification permission from user
         */
        requestPermission: function(callback) {
            if (!this.isSupported()) {
                if (callback) callback('unsupported');
                return;
            }

            Notification.requestPermission().then(function(permission) {
                if (callback) callback(permission);
            }).catch(function(err) {
                console.error("Error requesting notification permission:", err);
                if (callback) callback(Notification.permission);
            });
        },

        /**
         * Send a device notification alert
         */
        sendNotification: function(title, body, options) {
            if (!this.isSupported() || Notification.permission !== 'granted') {
                return false;
            }

            var opts = Object.assign({
                icon: 'favicon.ico',
                badge: 'favicon.ico',
                tag: 'mindmatrix-streak-alert',
                renotify: true,
                requireInteraction: false
            }, options || {});

            opts.body = body;

            try {
                if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
                    navigator.serviceWorker.ready.then(function(registration) {
                        registration.showNotification(title, opts);
                    }).catch(function() {
                        new Notification(title, opts);
                    });
                } else {
                    var notif = new Notification(title, opts);
                    notif.onclick = function() {
                        window.focus();
                        notif.close();
                    };
                }
                return true;
            } catch (e) {
                console.warn("Direct Notification constructor failed, fallback:", e);
                return false;
            }
        },

        /**
         * Load user preferences object from localStorage
         */
        loadPreferences: function(username) {
            var u = username || localStorage.getItem('mm_current_user') || 'default';
            var defaults = {
                sound: true,
                dailyReminder: true,
                animations: true,
                streakReminder: false,
                leaderboard: true
            };

            try {
                var raw = localStorage.getItem(this.STORAGE_KEY_PREFIX + u);
                if (raw) {
                    var parsed = JSON.parse(raw);
                    return Object.assign(defaults, parsed);
                }
            } catch (e) {
                console.error("Failed loading preferences:", e);
            }
            return defaults;
        },

        /**
         * Save user preferences object to localStorage & sync with Android Native App
         */
        savePreferences: function(username, prefs) {
            var u = username || localStorage.getItem('mm_current_user') || 'default';
            try {
                var current = this.loadPreferences(u);
                var updated = Object.assign(current, prefs);
                localStorage.setItem(this.STORAGE_KEY_PREFIX + u, JSON.stringify(updated));

                // Sync with native Android app background AlarmManager if present
                if (window.AndroidInterface && typeof window.AndroidInterface.setStreakReminderEnabled === 'function') {
                    window.AndroidInterface.setStreakReminderEnabled(!!updated.streakReminder);
                }

                return updated;
            } catch (e) {
                console.error("Failed saving preferences:", e);
                return prefs;
            }
        },

        /**
         * Enable Streak Reminders - requests permission and sends confirmation notification
         */
        enableStreakReminders: function(username, userState, onComplete) {
            var self = this;
            if (!this.isSupported()) {
                if (onComplete) onComplete(false, 'unsupported');
                return;
            }

            this.requestPermission(function(permission) {
                if (permission === 'granted') {
                    self.savePreferences(username, { streakReminder: true });
                    
                    // Send immediate confirmation notification alert on device
                    self.sendNotification(
                        '🔥 MindMatrix Streak Alert Enabled',
                        'Streak Reminders active! You will receive alert notifications on this device before your streak expires.',
                        { tag: 'mm-streak-toggle-on' }
                    );

                    if (userState) {
                        self.checkAndTriggerAlerts(userState);
                    }

                    if (onComplete) onComplete(true, 'granted');
                } else {
                    self.savePreferences(username, { streakReminder: false });
                    if (onComplete) onComplete(false, permission);
                }
            });
        },

        /**
         * Disable Streak Reminders
         */
        disableStreakReminders: function(username) {
            this.savePreferences(username, { streakReminder: false });
        },

        /**
         * Check if today's activity is done, and trigger reminder alert if not!
         */
        checkAndTriggerAlerts: function(userState) {
            if (userState) {
                this.lastState = userState;
            } else if (this.lastState) {
                userState = this.lastState;
            } else {
                var uName = localStorage.getItem('mm_current_user');
                if (uName) {
                    userState = {
                        username: uName,
                        streak: parseInt(localStorage.getItem('mm_' + uName + '_streak') || localStorage.getItem('mm_streak') || '0', 10),
                        lastPlayedDate: localStorage.getItem('mm_' + uName + '_last_played_date') || localStorage.getItem('mm_last_played_date') || ''
                    };
                }
            }

            if (!userState) return;

            var u = userState.username || localStorage.getItem('mm_current_user') || 'default';
            var prefs = this.loadPreferences(u);

            if (!prefs.streakReminder && !prefs.dailyReminder) return;

            var todayStr = new Date().toISOString().slice(0, 10);
            var lastPlayedStr = userState.lastPlayedDate || localStorage.getItem('mm_' + u + '_last_played_date') || localStorage.getItem('mm_last_played_date') || '';

            var playedToday = (lastPlayedStr && lastPlayedStr.substring(0, 10) === todayStr);

            var lastNotifDateKey = 'mm_' + u + '_last_notif_date';
            var lastNotifDate = localStorage.getItem(lastNotifDateKey);

            if (playedToday || lastNotifDate === todayStr) {
                return;
            }

            var currentStreak = userState.streak !== undefined ? userState.streak : (parseInt(localStorage.getItem('mm_' + u + '_streak'), 10) || 0);

            if (prefs.streakReminder && currentStreak > 0) {
                var sent = this.sendNotification(
                    '🔥 Don\'t Lose Your ' + currentStreak + '-Day Streak!',
                    'You haven\'t played MindMatrix today. Complete a puzzle today to keep your ' + currentStreak + '-day streak alive!',
                    { tag: 'mm-daily-streak-reminder' }
                );
                if (sent) {
                    localStorage.setItem(lastNotifDateKey, todayStr);
                }
            } else if (prefs.dailyReminder) {
                var sentDaily = this.sendNotification(
                    '🎮 MindMatrix Daily Reminder',
                    'Time for your daily brain exercise! Log in now to unlock high scores and level up.',
                    { tag: 'mm-daily-play-reminder' }
                );
                if (sentDaily) {
                    localStorage.setItem(lastNotifDateKey, todayStr);
                }
            }
        },

        /**
         * Initialize automated background timers & visibility watchers
         */
        initAutoCheckers: function() {
            var self = this;
            this.registerServiceWorker();
            
            // Check every 15 minutes in background
            setInterval(function() {
                self.checkAndTriggerAlerts();
            }, 15 * 60 * 1000);

            // Check whenever user switches back to the tab/app
            document.addEventListener('visibilitychange', function() {
                if (!document.hidden) {
                    self.checkAndTriggerAlerts();
                }
            });
        }
    };

    // Auto-start background checkers when script is loaded
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            StreakReminder.initAutoCheckers();
        });
    } else {
        StreakReminder.initAutoCheckers();
    }

    window.StreakReminder = StreakReminder;

})(window);
