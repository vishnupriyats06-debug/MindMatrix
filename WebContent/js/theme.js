(function() {
    // 1. Determine active theme (localStorage preference, default is 'light')
    var savedTheme = localStorage.getItem('theme');
    if (!savedTheme) {
        savedTheme = 'light';
    }
    // 2. Immediately apply to html element to prevent transition flickering
    document.documentElement.setAttribute('data-theme', savedTheme);
})();

// Reusable function to set theme
function setTheme(theme) {
    if (theme !== 'light' && theme !== 'dark') return;
    localStorage.setItem('theme', theme);
    document.documentElement.setAttribute('data-theme', theme);
    
    // Update selector displays on DOM
    updateThemeUI(theme);
}

// Reusable function to toggle theme
function toggleTheme() {
    var currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
    var nextTheme = currentTheme === 'light' ? 'dark' : 'light';
    setTheme(nextTheme);
}

// Function to update input elements / selection highlights in Settings
function updateThemeUI(theme) {
    // 1. Update selector cards on settings menu if active
    var lightCard = document.getElementById('theme-card-light');
    var darkCard = document.getElementById('theme-card-dark');
    if (lightCard && darkCard) {
        if (theme === 'light') {
            lightCard.classList.add('selected');
            darkCard.classList.remove('selected');
        } else {
            darkCard.classList.add('selected');
            lightCard.classList.remove('selected');
        }
    }
    
    // 2. Update any quick toggle buttons (e.g., ☀️ / 🌙 in header navs)
    var headerToggle = document.getElementById('theme-header-toggle');
    if (headerToggle) {
        headerToggle.innerHTML = theme === 'light' ? '🌙' : '☀️';
        headerToggle.title = theme === 'light' ? 'Switch to Dark Mode' : 'Switch to Light Mode';
    }
}

// Setup initial UI states when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    var currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
    updateThemeUI(currentTheme);
});

// ══════════════════════════════════════════════════════════════
// GLOBAL UNIFIED LEVEL PROGRESSION & UNLOCK ENGINE
// ══════════════════════════════════════════════════════════════
window.safeParseArray = function(dataVal, defaultArray) {
    if (Array.isArray(dataVal)) return dataVal;
    if (typeof dataVal === 'string') {
        try {
            var parsed = JSON.parse(dataVal);
            if (Array.isArray(parsed)) return parsed;
            if (typeof parsed === 'string') {
                var doubleParsed = JSON.parse(parsed);
                if (Array.isArray(doubleParsed)) return doubleParsed;
            }
        } catch (e) {}
    }
    return defaultArray.slice();
};

window.getLocalDateString = function() {
    var d = new Date();
    var year = d.getFullYear();
    var month = String(d.getMonth() + 1).padStart(2, '0');
    var day = String(d.getDate()).padStart(2, '0');
    return year + '-' + month + '-' + day;
};

window.calculateNewStreak = function(currentStreak, lastPlayedDateStr, isSuccess) {
    if (!isSuccess) return { streak: 0, dateStr: lastPlayedDateStr };
    var todayStr = window.getLocalDateString();
    if (!lastPlayedDateStr) return { streak: 1, dateStr: todayStr };
    var today = new Date(todayStr + 'T00:00:00');
    var lastPlayed = new Date(lastPlayedDateStr + 'T00:00:00');
    var diffDays = Math.round((today - lastPlayed) / (1000 * 60 * 60 * 24));
    var newStreak = currentStreak;
    if (diffDays === 0) { if (newStreak === 0) newStreak = 1; }
    else if (diffDays === 1) { newStreak = currentStreak + 1; }
    else { newStreak = 1; }
    return { streak: newStreak, dateStr: todayStr };
};

window.recordLevelCompletion = function(levelNum, earnedScore, timeStr) {
    var levelIdx = levelNum - 1; // 0-indexed
    var targetUnlock = (levelNum >= 20 ? 21 : levelNum + 1);
    var earned = parseInt(earnedScore, 10) || 100;
    var time = timeStr || "10s";
    var u = localStorage.getItem('mm_current_user') || 'default';

    // 1. Read existing local data
    var curScore = parseInt(localStorage.getItem('mm_' + u + '_score') || localStorage.getItem('mm_score') || '0', 10);
    var curUnlocked = parseInt(localStorage.getItem('mm_' + u + '_unlocked_level') || localStorage.getItem('mm_unlocked_level') || '1', 10);
    var curGames = parseInt(localStorage.getItem('mm_' + u + '_games_played') || localStorage.getItem('mm_games_played') || '0', 10);
    var curStreak = parseInt(localStorage.getItem('mm_' + u + '_streak') || localStorage.getItem('mm_streak') || '0', 10);
    var curBestStreak = parseInt(localStorage.getItem('mm_' + u + '_best_streak') || localStorage.getItem('mm_best_streak') || '0', 10);
    var curScores = window.safeParseArray(localStorage.getItem('mm_' + u + '_best_scores') || localStorage.getItem('mm_best_scores'), Array(20).fill(0));
    var curTimes = window.safeParseArray(localStorage.getItem('mm_' + u + '_best_times') || localStorage.getItem('mm_best_times'), Array(20).fill('-'));
    var curStars = window.safeParseArray(localStorage.getItem('mm_' + u + '_stars') || localStorage.getItem('mm_stars'), Array(20).fill('0'));
    var curLastDate = localStorage.getItem('mm_' + u + '_last_played_date') || localStorage.getItem('mm_last_played_date') || '';

    var localScore = curScore + earned;
    var localUnlocked = Math.max(curUnlocked, targetUnlock);
    var localGames = curGames + 1;
    var streakRes = window.calculateNewStreak(curStreak, curLastDate, true);
    var localStreak = streakRes.streak;
    var localDate = streakRes.dateStr;
    var localBestStreak = Math.max(curBestStreak, localStreak);

    if (earned > (parseInt(curScores[levelIdx], 10) || 0)) curScores[levelIdx] = earned;
    curStars[levelIdx] = '3';
    curTimes[levelIdx] = time;

    // Synchronously write to LocalStorage immediately!
    function writeLocal(s, strk, unl, g, bstrk, scs, tms, strs, dt) {
        try {
            localStorage.setItem('mm_score', s);
            localStorage.setItem('mm_streak', strk);
            localStorage.setItem('mm_unlocked_level', unl);
            localStorage.setItem('mm_games_played', g);
            localStorage.setItem('mm_best_streak', bstrk);
            localStorage.setItem('mm_best_scores', JSON.stringify(scs));
            localStorage.setItem('mm_best_times', JSON.stringify(tms));
            localStorage.setItem('mm_stars', JSON.stringify(strs));
            if (dt) localStorage.setItem('mm_last_played_date', dt);

            localStorage.setItem('mm_' + u + '_score', s);
            localStorage.setItem('mm_' + u + '_streak', strk);
            localStorage.setItem('mm_' + u + '_unlocked_level', unl);
            localStorage.setItem('mm_' + u + '_games_played', g);
            localStorage.setItem('mm_' + u + '_best_streak', bstrk);
            localStorage.setItem('mm_' + u + '_best_scores', JSON.stringify(scs));
            localStorage.setItem('mm_' + u + '_best_times', JSON.stringify(tms));
            localStorage.setItem('mm_' + u + '_stars', JSON.stringify(strs));
            if (dt) localStorage.setItem('mm_' + u + '_last_played_date', dt);
        } catch(e) {}
    }

    writeLocal(localScore, localStreak, localUnlocked, localGames, localBestStreak, curScores, curTimes, curStars, localDate);

    // 2. Fetch server progress & save
    return fetch('getProgress?t=' + new Date().getTime())
        .then(function(r) {
            if (!r.ok) throw new Error('Failed to fetch getProgress');
            return r.json();
        })
        .then(function(data) {
            var dbScore = parseInt(data.score, 10) || 0;
            var dbUnlocked = parseInt(data.unlockedLevel, 10) || 1;
            var dbGames = parseInt(data.gamesPlayed, 10) || 0;
            var dbStreak = parseInt(data.streak, 10) || 0;
            var dbBestStreak = parseInt(data.bestStreak, 10) || 0;

            var finalScore = dbScore + earned;
            var finalUnlocked = Math.max(dbUnlocked, targetUnlock);
            var finalGames = dbGames + 1;

            var sResult = window.calculateNewStreak(dbStreak, data.lastPlayedDate || "", true);
            var finalStreak = sResult.streak;
            var finalDate = sResult.dateStr;
            var finalBestStreak = Math.max(dbBestStreak, finalStreak);

            var bestScores = window.safeParseArray(data.bestScores, Array(20).fill(0));
            var bestTimes  = window.safeParseArray(data.bestTimes, Array(20).fill("-"));
            var stars      = window.safeParseArray(data.stars, Array(20).fill("0"));

            if (earned > (parseInt(bestScores[levelIdx], 10) || 0)) bestScores[levelIdx] = earned;
            stars[levelIdx] = "3";
            bestTimes[levelIdx] = time;

            writeLocal(finalScore, finalStreak, finalUnlocked, finalGames, finalBestStreak, bestScores, bestTimes, stars, finalDate);

            return fetch('saveProgress', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({
                    score: finalScore,
                    streak: finalStreak,
                    unlockedLevel: finalUnlocked,
                    gamesPlayed: finalGames,
                    bestStreak: finalBestStreak,
                    bestScores: JSON.stringify(bestScores),
                    bestTimes: JSON.stringify(bestTimes),
                    stars: JSON.stringify(stars),
                    lastPlayedDate: finalDate,
                    clientDate: finalDate
                })
            });
        })
        .catch(function(err) {
            console.warn('[ProgressEngine] Server sync fallback:', err);
            return fetch('saveProgress', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({
                    score: localScore,
                    streak: localStreak,
                    unlockedLevel: localUnlocked,
                    gamesPlayed: localGames,
                    bestStreak: localBestStreak,
                    bestScores: JSON.stringify(curScores),
                    bestTimes: JSON.stringify(curTimes),
                    stars: JSON.stringify(curStars),
                    lastPlayedDate: localDate,
                    clientDate: localDate
                })
            }).catch(function() {});
        });
};
