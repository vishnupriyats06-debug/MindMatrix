/**
 * MindMatrix – Unified Standard Part & Level Completion Modal
 * Master reference template across ALL Levels 1–20.
 */

(function() {
    // ── 1. Inject Unified CSS Styles ──
    const modalStyleId = 'mm-standard-completion-style';
    if (!document.getElementById(modalStyleId)) {
        const style = document.createElement('style');
        style.id = modalStyleId;
        style.textContent = `
            /* ══════════════════════════════════════════
               STANDARD RESULT POPUP OVERLAY (Master UI)
            ══════════════════════════════════════════ */
            .result-overlay {
                display: none; position: fixed; inset: 0; z-index: 9999;
                background: rgba(5, 8, 20, 0.85); backdrop-filter: blur(14px); -webkit-backdrop-filter: blur(14px);
                align-items: center; justify-content: center;
                animation: mmFadeIn 0.25s ease both;
                padding: 16px;
            }
            .result-overlay.open { display: flex !important; }
            @keyframes mmFadeIn { from { opacity: 0; } to { opacity: 1; } }

            .result-card {
                background: rgba(11, 15, 36, 0.97);
                border: 1px solid rgba(255, 255, 255, 0.10);
                border-radius: 28px; padding: 44px 40px;
                width: 100%; max-width: 420px;
                box-shadow: 0 32px 80px rgba(0, 0, 0, 0.70), 0 0 40px rgba(139, 92, 246, 0.15);
                animation: mmPopIn 0.4s cubic-bezier(0.22, 1, 0.36, 1) both;
                text-align: center; position: relative; overflow: hidden;
            }
            @keyframes mmPopIn {
                from { opacity: 0; transform: scale(0.85) translateY(24px); }
                to   { opacity: 1; transform: scale(1) translateY(0); }
            }
            .result-card.win { border-color: rgba(52, 211, 153, 0.35); }
            .result-card.lose { border-color: rgba(248, 113, 113, 0.30); }

            .result-emoji {
                font-size: 64px; line-height: 1; margin-bottom: 16px;
                animation: mmPulseGlow 2s ease-in-out infinite;
            }
            @keyframes mmPulseGlow {
                0%, 100% { transform: scale(1); filter: drop-shadow(0 0 0 transparent); }
                50% { transform: scale(1.08); filter: drop-shadow(0 0 16px rgba(52, 211, 153, 0.4)); }
            }

            .result-title {
                font-family: 'Outfit', system-ui, sans-serif;
                font-size: 28px; font-weight: 800; letter-spacing: -0.5px;
                margin-bottom: 8px; color: #34d399;
            }
            .result-card.lose .result-title { color: #f87171; }

            .result-desc {
                font-family: 'Outfit', system-ui, sans-serif;
                font-size: 14px; color: rgba(240, 244, 255, 0.65); line-height: 1.6;
                margin-bottom: 28px;
            }

            .result-stats {
                display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
                margin-bottom: 28px;
            }
            .result-stat {
                background: rgba(255, 255, 255, 0.04); border: 1px solid rgba(255, 255, 255, 0.09);
                border-radius: 14px; padding: 16px 14px; text-align: center;
            }
            .result-stat .rs-val {
                font-family: 'Outfit', system-ui, sans-serif;
                font-size: 22px; font-weight: 800; color: #a78bfa; margin-bottom: 2px;
            }
            .result-stat .rs-lbl {
                font-family: 'Outfit', system-ui, sans-serif;
                font-size: 10px; color: rgba(240, 244, 255, 0.35);
                text-transform: uppercase; letter-spacing: 0.6px; font-weight: 600;
            }

            .result-buttons {
                display: flex; flex-direction: column; gap: 10px;
            }
            .result-buttons .btn-primary {
                width: 100%; display: inline-flex; align-items: center; justify-content: center; gap: 8px;
                padding: 14px 28px; border: none; border-radius: 14px;
                background: linear-gradient(135deg, #8b5cf6, #06b6d4);
                color: #fff; font-family: 'Outfit', system-ui, sans-serif;
                font-size: 15px; font-weight: 700; cursor: pointer;
                box-shadow: 0 4px 24px rgba(139, 92, 246, 0.45);
                transition: transform 0.18s ease, box-shadow 0.18s ease;
            }
            .result-buttons .btn-primary:hover {
                transform: translateY(-2px);
                box-shadow: 0 8px 36px rgba(139, 92, 246, 0.55);
            }
            .result-buttons .btn-primary:active { transform: translateY(0); }

            .result-buttons .btn-secondary {
                width: 100%; display: inline-flex; align-items: center; justify-content: center; gap: 8px;
                padding: 12px 24px; border-radius: 12px;
                background: rgba(255, 255, 255, 0.04); border: 1px solid rgba(255, 255, 255, 0.09);
                color: rgba(240, 244, 255, 0.60); font-family: 'Outfit', system-ui, sans-serif;
                font-size: 14px; font-weight: 600; cursor: pointer;
                transition: all 0.2s ease;
            }
            .result-buttons .btn-secondary:hover {
                background: rgba(255, 255, 255, 0.08); color: #fff; border-color: rgba(139, 92, 246, 0.4);
            }

            #confetti-canvas {
                position: fixed; inset: 0; z-index: 9998; pointer-events: none;
            }

            @media (max-width: 560px) {
                .result-card { padding: 34px 20px; border-radius: 22px; }
                .result-title { font-size: 24px; }
                .result-emoji { font-size: 52px; margin-bottom: 12px; }
                .result-desc { font-size: 13px; margin-bottom: 22px; }
                .result-stats { margin-bottom: 22px; gap: 8px; }
            }
        `;
        document.head.appendChild(style);
    }

    // ── 2. Ensure Modal and Canvas DOM Exist ──
    function ensureModalDOM() {
        if (!document.getElementById('confetti-canvas')) {
            const canvas = document.createElement('canvas');
            canvas.id = 'confetti-canvas';
            document.body.appendChild(canvas);
        }

        if (!document.getElementById('result-overlay')) {
            const overlay = document.createElement('div');
            overlay.className = 'result-overlay';
            overlay.id = 'result-overlay';
            overlay.innerHTML = `
                <div class="result-card win" id="result-card">
                    <div class="result-emoji" id="result-emoji">🎉</div>
                    <div class="result-title" id="result-title">Correct!</div>
                    <div class="result-desc" id="result-desc">Great memory! You got the challenge right.</div>
                    <div class="result-stats">
                        <div class="result-stat">
                            <div class="rs-val" id="result-score">+50</div>
                            <div class="rs-lbl">Score Earned</div>
                        </div>
                        <div class="result-stat">
                            <div class="rs-val" id="result-part">Part 1/2</div>
                            <div class="rs-lbl">Progress</div>
                        </div>
                    </div>
                    <div class="result-buttons">
                        <button class="btn-primary" id="result-action-btn">Continue to Part 2 ▶</button>
                        <button class="btn-secondary" id="result-secondary-btn" style="display:none;">Back to Dashboard</button>
                    </div>
                </div>
            `;
            document.body.appendChild(overlay);
        }
    }

    // ── 3. Confetti Animation Engine ──
    let confettiParticles = [];
    let confettiAnimId = null;

    function fireStandardConfetti() {
        ensureModalDOM();
        const canvas = document.getElementById('confetti-canvas');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;

        const colors = ['#8b5cf6', '#06b6d4', '#34d399', '#f59e0b', '#f87171', '#ec4899', '#ffffff'];

        confettiParticles = [];
        for (let i = 0; i < 120; i++) {
            confettiParticles.push({
                x: canvas.width / 2 + (Math.random() - 0.5) * 220,
                y: canvas.height / 2,
                vx: (Math.random() - 0.5) * 16,
                vy: Math.random() * -18 - 4,
                w: Math.random() * 8 + 4,
                h: Math.random() * 6 + 2,
                color: colors[Math.floor(Math.random() * colors.length)],
                rotation: Math.random() * 360,
                rotSpeed: (Math.random() - 0.5) * 12,
                gravity: 0.25 + Math.random() * 0.15,
                opacity: 1,
            });
        }

        if (confettiAnimId) cancelAnimationFrame(confettiAnimId);

        function animate() {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            let alive = false;
            confettiParticles.forEach(p => {
                p.x += p.vx;
                p.vy += p.gravity;
                p.y += p.vy;
                p.rotation += p.rotSpeed;
                p.opacity -= 0.005;
                if (p.opacity <= 0) return;
                alive = true;

                ctx.save();
                ctx.translate(p.x, p.y);
                ctx.rotate((p.rotation * Math.PI) / 180);
                ctx.globalAlpha = Math.max(0, p.opacity);
                ctx.fillStyle = p.color;
                ctx.fillRect(-p.w / 2, -p.h / 2, p.w, p.h);
                ctx.restore();
            });
            if (alive) {
                confettiAnimId = requestAnimationFrame(animate);
            }
        }
        animate();
    }

    function clearStandardConfetti() {
        if (confettiAnimId) cancelAnimationFrame(confettiAnimId);
        const canvas = document.getElementById('confetti-canvas');
        if (canvas) {
            const ctx = canvas.getContext('2d');
            ctx.clearRect(0, 0, canvas.width, canvas.height);
        }
        confettiParticles = [];
    }

    // ── 4. Main Exported API ──
    window.showPartCompletion = function(opts) {
        ensureModalDOM();
        const overlay = document.getElementById('result-overlay');
        const card = document.getElementById('result-card');
        const emoji = document.getElementById('result-emoji');
        const title = document.getElementById('result-title');
        const desc = document.getElementById('result-desc');
        const scoreEl = document.getElementById('result-score');
        const partEl = document.getElementById('result-part');
        const actionBtn = document.getElementById('result-action-btn');
        const secBtn = document.getElementById('result-secondary-btn');

        const part = opts.part || 1;
        const totalParts = opts.totalParts || 2;
        const isFinal = opts.isFinal !== undefined ? opts.isFinal : (part >= totalParts);
        const scoreEarned = opts.score !== undefined ? opts.score : 50;

        card.className = 'result-card win';
        emoji.textContent = opts.emoji || '🎉';
        title.textContent = opts.title || 'Correct!';
        desc.textContent = opts.message || (isFinal ? 'Amazing! You mastered all challenges. Level complete!' : 'Excellent memory! You recalled the sequence perfectly.');

        scoreEl.textContent = (scoreEarned >= 0 ? '+' : '') + scoreEarned;
        partEl.textContent = `Part ${part}/${totalParts}${isFinal ? ' ✓' : ''}`;

        let defaultBtnText = isFinal ? '🏆 Level Complete – Continue' : `Continue to Part ${part + 1} ▶`;
        actionBtn.textContent = opts.buttonText || defaultBtnText;

        actionBtn.onclick = function() {
            window.closePartCompletion();
            if (typeof opts.onContinue === 'function') {
                opts.onContinue();
            }
        };

        if (opts.showDashboardBtn) {
            secBtn.style.display = 'inline-flex';
            secBtn.onclick = function() {
                window.closePartCompletion();
                if (typeof opts.onSecondary === 'function') {
                    opts.onSecondary();
                } else {
                    window.location.href = 'dashboard.html';
                }
            };
        } else {
            secBtn.style.display = 'none';
        }

        overlay.classList.add('open');
        fireStandardConfetti();
    };

    window.closePartCompletion = function() {
        const overlay = document.getElementById('result-overlay');
        if (overlay) overlay.classList.remove('open');
        clearStandardConfetti();
    };

    // ── 5. Standard Level Authentication Guard ──
    function checkLevelAuth() {
        const isLocallyAuth = localStorage.getItem('mm_unlocked_level') !== null ||
                              localStorage.getItem('mm_score') !== null ||
                              localStorage.getItem('mm_username') !== null;

        fetch('getProgress?t=' + Date.now())
            .then(function(r) {
                if (r.status === 401) {
                    if (!isLocallyAuth) {
                        window.location.href = 'login.html?error=' + encodeURIComponent('Please sign in to access game levels.');
                    }
                } else if (r.ok) {
                    return r.json();
                }
            })
            .then(function(data) {
                if (data && !data.error) {
                    var u = data.username || localStorage.getItem('mm_current_user') || 'default';
                    localStorage.setItem('mm_current_user', u);
                    localStorage.setItem('mm_username', u);
                    
                    if (data.unlockedLevel !== undefined) {
                        localStorage.setItem('mm_unlocked_level', data.unlockedLevel);
                        localStorage.setItem('mm_' + u + '_unlocked_level', data.unlockedLevel);
                    }
                    if (data.score !== undefined) {
                        localStorage.setItem('mm_score', data.score);
                        localStorage.setItem('mm_' + u + '_score', data.score);
                    }
                    if (data.avatarId !== undefined) {
                        localStorage.setItem('mm_avatar_id', data.avatarId);
                        localStorage.setItem('mm_' + u + '_avatar_id', data.avatarId);
                    }
                    if (data.streak !== undefined) {
                        localStorage.setItem('mm_streak', data.streak);
                        localStorage.setItem('mm_' + u + '_streak', data.streak);
                    }
                    if (data.gamesPlayed !== undefined) {
                        localStorage.setItem('mm_games_played', data.gamesPlayed);
                        localStorage.setItem('mm_' + u + '_games_played', data.gamesPlayed);
                    }
                    if (data.bestStreak !== undefined) {
                        localStorage.setItem('mm_best_streak', data.bestStreak);
                        localStorage.setItem('mm_' + u + '_best_streak', data.bestStreak);
                    }
                    if (data.lastPlayedDate) {
                        localStorage.setItem('mm_last_played_date', data.lastPlayedDate);
                        localStorage.setItem('mm_' + u + '_last_played_date', data.lastPlayedDate);
                    }
                    if (data.bestScores) {
                        var scStr = typeof data.bestScores === 'string' ? data.bestScores : JSON.stringify(data.bestScores);
                        localStorage.setItem('mm_best_scores', scStr);
                        localStorage.setItem('mm_' + u + '_best_scores', scStr);
                    }
                    if (data.bestTimes) {
                        var tmStr = typeof data.bestTimes === 'string' ? data.bestTimes : JSON.stringify(data.bestTimes);
                        localStorage.setItem('mm_best_times', tmStr);
                        localStorage.setItem('mm_' + u + '_best_times', tmStr);
                    }
                    if (data.stars) {
                        var stStr = typeof data.stars === 'string' ? data.stars : JSON.stringify(data.stars);
                        localStorage.setItem('mm_stars', stStr);
                        localStorage.setItem('mm_' + u + '_stars', stStr);
                    }
                    if (data.activityDates) {
                        var adStr = typeof data.activityDates === 'string' ? data.activityDates : JSON.stringify(data.activityDates);
                        localStorage.setItem('mm_activity_dates', adStr);
                        localStorage.setItem('mm_' + u + '_activity_dates', adStr);
                    }
                }
            })
            .catch(function() {
                if (!isLocallyAuth) {
                    window.location.href = 'login.html?error=' + encodeURIComponent('Please sign in to access game levels.');
                }
            });
    }

    window.getAvatarPath = function(avatarId) {
        var id = parseInt(avatarId, 10) || 0;
        if (id < 1 || id > 20) return '';
        var folder = id <= 10 ? 'female' : 'male';
        var pad = (id < 10 ? '0' : '') + id;
        return 'images/avatars/' + folder + '/avatar' + pad + '.svg';
    };

    window.getLocalDateString = function() {
        var d = new Date();
        var year = d.getFullYear();
        var month = String(d.getMonth() + 1).padStart(2, '0');
        var day = String(d.getDate()).padStart(2, '0');
        return year + '-' + month + '-' + day;
    };

    window.calculateNewStreak = function(currentStreak, lastPlayedDateStr, isSuccess) {
        if (!isSuccess) return { streak: (currentStreak || 0), dateStr: lastPlayedDateStr || "" };
        var todayStr = window.getLocalDateString();
        if (!lastPlayedDateStr) return { streak: 1, dateStr: todayStr };

        if (lastPlayedDateStr > todayStr) {
            lastPlayedDateStr = todayStr;
        }

        if (lastPlayedDateStr === todayStr) {
            // Played today already: streak stays same on multiple plays today
            return { streak: (currentStreak > 0 ? currentStreak : 1), dateStr: todayStr };
        }

        var today = new Date(todayStr + 'T00:00:00');
        var lastPlayed = new Date(lastPlayedDateStr + 'T00:00:00');
        var diffDays = Math.round((today - lastPlayed) / (1000 * 60 * 60 * 24));

        var newStreak = currentStreak || 0;
        if (diffDays === 1) {
            // Played yesterday: increment streak by 1
            newStreak = (currentStreak > 0 ? currentStreak : 0) + 1;
        } else {
            // Missed 1 or more full days: reset streak to 1 today
            newStreak = 1;
        }
        return { streak: newStreak, dateStr: todayStr };
    };

    window.checkLevelAuth = checkLevelAuth;

    // Attach to DOMContentLoaded
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            ensureModalDOM();
            checkLevelAuth();
        });
    } else {
        ensureModalDOM();
        checkLevelAuth();
    }
})();
