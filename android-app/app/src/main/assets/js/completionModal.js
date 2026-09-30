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
                background: rgba(5, 8, 20, 0.85); backdrop-filter: none !important; -webkit-backdrop-filter: none !important;
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

        const colors = [
            '#8b5cf6', '#06b6d4', '#34d399', '#f59e0b', '#f87171',
            '#ec4899', '#ffeb3b', '#e11d48', '#3b82f6', '#10b981', '#a855f7'
        ];

        confettiParticles = [];
        for (let i = 0; i < 160; i++) {
            confettiParticles.push({
                x: Math.random() * canvas.width,
                y: Math.random() * (canvas.height * 0.4) - 20,
                vx: (Math.random() - 0.5) * 8,
                vy: Math.random() * 5 + 3,
                w: Math.random() * 10 + 6,
                h: Math.random() * 8 + 4,
                shape: Math.random() > 0.3 ? 'rect' : 'circle',
                color: colors[Math.floor(Math.random() * colors.length)],
                rotation: Math.random() * 360,
                rotSpeed: (Math.random() - 0.5) * 10,
                gravity: 0.12 + Math.random() * 0.1,
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
                p.opacity -= 0.003;
                if (p.opacity <= 0 || p.y > canvas.height + 20) return;
                alive = true;

                ctx.save();
                ctx.translate(p.x, p.y);
                ctx.rotate((p.rotation * Math.PI) / 180);
                ctx.globalAlpha = Math.max(0, p.opacity);
                ctx.fillStyle = p.color;

                if (p.shape === 'circle') {
                    ctx.beginPath();
                    ctx.arc(0, 0, p.w / 2, 0, Math.PI * 2);
                    ctx.fill();
                } else {
                    ctx.fillRect(-p.w / 2, -p.h / 2, p.w, p.h);
                }

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

    // ── 4. Exported Completion APIs ──

    // Part 1 Completion Popup API
    window.showPartCompletion = function(opts) {
        opts = opts || {};
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

        if (card) card.className = 'result-card win';
        if (emoji) emoji.textContent = opts.emoji || '🎉';
        if (title) title.textContent = opts.title || 'Correct!';
        if (desc) desc.textContent = opts.message || 'Excellent memory! You recalled the sequence perfectly. Ready for the next challenge?';

        const scoreEarned = opts.score !== undefined ? opts.score : 50;
        if (scoreEl) scoreEl.textContent = (scoreEarned >= 0 ? '+' : '') + scoreEarned;

        const part = opts.part || 1;
        const totalParts = opts.totalParts || 2;
        if (partEl) partEl.textContent = opts.partProgress || `Part ${part}/${totalParts}`;

        if (actionBtn) {
            actionBtn.textContent = opts.buttonText || `Continue to Part ${part + 1} ▶`;
            actionBtn.style.display = 'inline-flex';
            actionBtn.onclick = function() {
                window.closePartCompletion();
                if (typeof opts.onContinue === 'function') {
                    opts.onContinue();
                }
            };
        }

        if (secBtn) secBtn.style.display = 'none';

        if (overlay) overlay.classList.add('open');
        fireStandardConfetti();
    };

    // Part 2 / Level Completion Popup API
    window.showLevelCompletion = function(opts) {
        opts = opts || {};
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

        const levelNum = opts.level || 1;

        if (card) card.className = 'result-card win';
        if (emoji) emoji.textContent = opts.emoji || '🎉';
        if (title) title.textContent = opts.title || 'Correct!';
        if (desc) desc.textContent = opts.message || `Amazing! Level ${levelNum} complete!`;

        const scoreEarned = opts.score !== undefined ? opts.score : 50;
        if (scoreEl) scoreEl.textContent = (scoreEarned >= 0 ? '+' : '') + scoreEarned;

        const part = opts.part || 2;
        const totalParts = opts.totalParts || 2;
        if (partEl) partEl.textContent = opts.partProgress || `Part ${part}/${totalParts} ✓`;

        let activeSavePromise = null;
        if (typeof window.recordLevelCompletion === 'function') {
            activeSavePromise = window.recordLevelCompletion(levelNum, scoreEarned, opts.timeStr || "10s");
        }

        function safeNavigate(targetUrl, customHandler) {
            window.closePartCompletion();
            let p = activeSavePromise;
            if (!p && typeof window.recordLevelCompletion === 'function') {
                p = window.recordLevelCompletion(levelNum, scoreEarned, opts.timeStr || "10s");
            }
            let navDone = false;
            function doNav() {
                if (navDone) return;
                navDone = true;
                if (typeof customHandler === 'function') {
                    customHandler();
                } else if (targetUrl) {
                    window.location.href = targetUrl;
                }
            }
            const timer = setTimeout(doNav, 3000);
            if (p && typeof p.then === 'function') {
                p.then(() => { clearTimeout(timer); doNav(); })
                 .catch(() => { clearTimeout(timer); doNav(); });
            } else {
                doNav();
            }
        }

        if (levelNum < 20) {
            const nextUrl = opts.nextLevelUrl || (`level${levelNum + 1}.html`);
            if (actionBtn) {
                actionBtn.textContent = opts.buttonText || 'Next Level ▶';
                actionBtn.style.display = 'inline-flex';
                actionBtn.onclick = function() {
                    safeNavigate(nextUrl, opts.onNextLevel);
                };
            }

            if (secBtn) {
                secBtn.textContent = 'Dashboard';
                secBtn.style.display = 'inline-flex';
                secBtn.onclick = function() {
                    safeNavigate('dashboard.html', null);
                };
            }
        } else {
            if (actionBtn) {
                actionBtn.textContent = opts.buttonText || '🏆 Back to Dashboard';
                actionBtn.style.display = 'inline-flex';
                actionBtn.onclick = function() {
                    safeNavigate('dashboard.html', null);
                };
            }
            if (secBtn) secBtn.style.display = 'none';
        }

        if (overlay) overlay.classList.add('open');
        fireStandardConfetti();
    };

    window.closePartCompletion = function() {
        const overlay = document.getElementById('result-overlay');
        if (overlay) {
            overlay.classList.remove('open');
        }
        clearStandardConfetti();
    };
})();
