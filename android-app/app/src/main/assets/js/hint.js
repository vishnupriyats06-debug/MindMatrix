/* ═══════════════════════════════════════════════════════════════
   MindMatrix – Game Hint System (hint.js)
   A premium, reusable hint utility for all game levels.
   ═══════════════════════════════════════════════════════════════ */

(function() {
    // Inject Styles on load
    injectStyles();

    // Track state
    let checkCanRevealCallback = null;
    let onRevealCallback = null;
    let currentMathChallenge = null;

    // Sync hints with server on load
    syncHintsFromServer();

    // Export initialization globally
    window.initHintSystem = function(checkCanReveal, onReveal) {
        checkCanRevealCallback = checkCanReveal;
        onRevealCallback = onReveal;

        // Try to inject DOM elements
        injectDOM();
        syncHintsFromServer();
    };

    function syncHintsFromServer() {
        fetch('getProgress?t=' + Date.now())
            .then(r => r.ok ? r.json() : null)
            .then(data => {
                if (data && data.hints !== undefined) {
                    localStorage.setItem('mm_hints', data.hints.toString());
                    updateDisplays();
                }
            })
            .catch(() => {});
    }

    function getHints() {
        return parseInt(localStorage.getItem('mm_hints') || '3', 10);
    }

    function setHints(val) {
        let capped = Math.max(val, 0);
        localStorage.setItem('mm_hints', capped.toString());
        updateDisplays();

        // Real-time server database synchronization
        fetch('updateHints', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: 'hints=' + encodeURIComponent(capped)
        }).catch(err => {
            console.error('[hint.js] Error updating hints on server:', err);
        });
    }

    function updateDisplays() {
        const count = getHints();
        const hintBtnCount = document.getElementById('hint-count');
        const modalCount = document.getElementById('hint-modal-count');
        const useBtn = document.getElementById('btn-use-hint');
        const earnBtn = document.getElementById('btn-goto-earn');

        if (hintBtnCount) hintBtnCount.textContent = count;
        if (modalCount) modalCount.textContent = count;

        if (useBtn) {
            if (count > 0) {
                useBtn.disabled = false;
                useBtn.textContent = `Use Hint (-1)`;
                useBtn.style.opacity = '1';
                useBtn.style.cursor = 'pointer';
            } else {
                useBtn.disabled = true;
                useBtn.textContent = `No Hints Left`;
                useBtn.style.opacity = '0.5';
                useBtn.style.cursor = 'not-allowed';
            }
        }

        if (earnBtn) {
            if (count === 0) {
                earnBtn.style.display = 'block';
            } else {
                earnBtn.style.display = 'none';
            }
        }
    }

    function injectStyles() {
        const style = document.createElement('style');
        style.innerHTML = `
            /* Hint Button */
            .btn-hint {
                display: inline-flex;
                align-items: center;
                gap: 5px;
                padding: 6px 12px;
                border-radius: 100px;
                background: rgba(245, 158, 11, 0.12);
                border: 1px solid rgba(245, 158, 11, 0.25);
                font-family: 'Outfit', sans-serif;
                font-size: 12px;
                font-weight: 600;
                color: #f59e0b;
                cursor: pointer;
                transition: all 0.2s ease;
                white-space: nowrap;
                flex-shrink: 0;
                user-select: none;
                margin-right: 0;
            }
            .btn-hint:hover {
                background: rgba(245, 158, 11, 0.22);
                border-color: rgba(245, 158, 11, 0.45);
                box-shadow: 0 0 10px rgba(245, 158, 11, 0.2);
            }
            @media (max-width: 600px) {
                .btn-hint {
                    padding: 4px 8px;
                    font-size: 11px;
                    gap: 3px;
                }
            }

            /* Hint Overlay & Modal */
            .hint-overlay {
                display: none;
                position: fixed;
                inset: 0;
                z-index: 9999;
                background: rgba(5, 8, 20, 0.85);
                backdrop-filter: blur(12px);
                -webkit-backdrop-filter: blur(12px);
                align-items: center;
                justify-content: center;
                animation: hint-fade-in 0.25s ease;
            }
            .hint-overlay.open {
                display: flex;
            }
            @keyframes hint-fade-in {
                from { opacity: 0; }
                to { opacity: 1; }
            }

            .hint-modal-card {
                position: relative;
                background: #0b0f24;
                border: 1px solid rgba(255, 255, 255, 0.1);
                border-radius: 24px;
                padding: 32px 28px;
                width: 90%;
                max-width: 400px;
                box-shadow: 
                    0 20px 50px rgba(0, 0, 0, 0.6),
                    0 0 40px rgba(245, 158, 11, 0.15);
                text-align: center;
                animation: hint-pop-in 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
            }
            @keyframes hint-pop-in {
                from { transform: scale(0.9) translateY(15px); opacity: 0; }
                to { transform: scale(1) translateY(0); opacity: 1; }
            }

            .hint-close-btn {
                position: absolute;
                top: 16px;
                right: 20px;
                background: none;
                border: none;
                color: rgba(245, 244, 255, 0.4);
                font-size: 24px;
                cursor: pointer;
                transition: color 0.2s;
            }
            .hint-close-btn:hover {
                color: #fff;
            }

            .hint-emoji {
                font-size: 48px;
                margin-bottom: 12px;
            }
            .hint-title {
                font-size: 22px;
                font-weight: 700;
                color: #f59e0b;
                margin-bottom: 12px;
            }
            .hint-modal-desc {
                font-size: 13px;
                color: rgba(240, 244, 255, 0.65);
                margin-bottom: 20px;
                line-height: 1.6;
            }

            .hint-status-bar {
                display: flex;
                justify-content: space-between;
                align-items: center;
                background: rgba(255, 255, 255, 0.03);
                border: 1px solid rgba(255, 255, 255, 0.05);
                border-radius: 12px;
                padding: 12px 18px;
                margin-bottom: 24px;
            }
            .hint-status-bar span {
                font-size: 14px;
                font-weight: 500;
            }
            .hint-count-val {
                color: #f59e0b;
                font-weight: 700;
                font-size: 16px;
            }

            .hint-modal-buttons {
                display: flex;
                flex-direction: column;
                gap: 10px;
            }
            .hint-btn-action {
                width: 100%;
                padding: 12px !important;
                font-size: 14px !important;
                height: auto !important;
                border-radius: 12px !important;
                font-weight: 600 !important;
            }

            /* Math Puzzle UI */
            .math-question-container {
                background: rgba(255, 255, 255, 0.04);
                border: 1px solid rgba(255, 255, 255, 0.08);
                border-radius: 12px;
                padding: 16px;
                font-size: 24px;
                font-weight: 700;
                color: #f0f4ff;
                margin-bottom: 16px;
                letter-spacing: 1px;
            }
            .math-answer-input {
                width: 100%;
                background: rgba(255, 255, 255, 0.03);
                border: 1px solid rgba(255, 255, 255, 0.1);
                border-radius: 10px;
                padding: 10px 16px;
                color: #fff;
                font-size: 16px;
                text-align: center;
                font-family: 'Outfit', sans-serif;
                margin-bottom: 8px;
                transition: border-color 0.2s;
            }
            .math-answer-input:focus {
                border-color: #f59e0b;
                outline: none;
            }
            .math-feedback {
                font-size: 13px;
                font-weight: 600;
                margin-top: 8px;
                min-height: 20px;
            }
            .math-feedback.correct {
                color: #34d399;
            }
            .math-feedback.wrong {
                color: #f87171;
                animation: math-shake 0.3s ease;
            }
            @keyframes math-shake {
                0%, 100% { transform: translateX(0); }
                25% { transform: translateX(-6px); }
                75% { transform: translateX(6px); }
            }

            /* Toasts */
            .hint-toast {
                position: fixed;
                bottom: 24px;
                left: 50%;
                transform: translateX(-50%);
                background: rgba(11, 15, 36, 0.95);
                border: 1px solid rgba(255, 255, 255, 0.1);
                padding: 12px 24px;
                border-radius: 12px;
                color: #fff;
                font-family: 'Outfit', sans-serif;
                font-size: 14px;
                font-weight: 500;
                z-index: 99999;
                box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
                transition: opacity 0.3s ease, transform 0.3s ease;
                opacity: 0;
                transform: translate(-50%, 10px);
                pointer-events: none;
            }
            .hint-toast.show {
                opacity: 1;
                transform: translate(-50%, 0);
            }
            .hint-toast.warning { border-color: #fbbf24; color: #fbbf24; }
            .hint-toast.success { border-color: #34d399; color: #34d399; }
            .hint-toast.error { border-color: #f87171; color: #f87171; }
        `;
        document.head.appendChild(style);
    }

    function showToast(message, type = 'warning') {
        let toast = document.getElementById('hint-toast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'hint-toast';
            document.body.appendChild(toast);
        }
        toast.className = `hint-toast show ${type}`;
        toast.textContent = message;
        
        setTimeout(() => {
            toast.classList.remove('show');
        }, 3000);
    }

    function injectDOM() {
        // Find header topbar-right to place hint button
        const topbarRight = document.querySelector('.topbar-right');
        if (topbarRight && !document.getElementById('btn-hint')) {
            const btn = document.createElement('button');
            btn.className = 'btn-hint';
            btn.id = 'btn-hint';
            btn.innerHTML = `💡 Hint (<span id="hint-count">${getHints()}</span>)`;
            
            // Insert at the beginning of topbar-right
            topbarRight.insertBefore(btn, topbarRight.firstChild);
            
            btn.addEventListener('click', onHintButtonClick);
        }

        // Inject Hint Modal if not present
        if (!document.getElementById('hint-overlay')) {
            const modalContainer = document.createElement('div');
            modalContainer.id = 'hint-overlay';
            modalContainer.className = 'hint-overlay';
            modalContainer.innerHTML = `
                <div class="hint-modal-card">
                    <button class="hint-close-btn" id="hint-close-btn">&times;</button>
                    <div class="hint-emoji">💡</div>
                    <h2 class="hint-title">Game Hint</h2>
                    
                    <!-- Choice View -->
                    <div id="hint-choice-view">
                        <p class="hint-modal-desc">Stuck? You can use a hint to reveal the next correct value, or earn +1 hint by solving a simple math challenge!</p>
                        <div class="hint-status-bar">
                            <span>Available Hints:</span>
                            <span class="hint-count-val" id="hint-modal-count">3</span>
                        </div>
                        <div class="hint-modal-buttons">
                            <button class="btn-primary hint-btn-action" id="btn-use-hint">Use Hint (-1)</button>
                            <button class="btn-secondary hint-btn-action" id="btn-goto-earn">Earn Hint (+1)</button>
                        </div>
                    </div>

                    <!-- Earn View -->
                    <div id="hint-earn-view" style="display: none;">
                        <p class="hint-modal-desc">Solve this math puzzle to get 1 free hint:</p>
                        <div class="math-question-container">
                            <span id="math-question-text">?</span>
                        </div>
                        <div class="math-input-container">
                            <input type="number" id="math-answer-input" placeholder="Your answer" class="math-answer-input" />
                        </div>
                        <div class="math-feedback" id="math-feedback"></div>
                        <div class="hint-modal-buttons" style="margin-top: 16px;">
                            <button class="btn-primary hint-btn-action" id="btn-submit-math">Submit Answer</button>
                            <button class="btn-secondary hint-btn-action" id="btn-back-to-choice">Back</button>
                        </div>
                    </div>
                </div>
            `;
            document.body.appendChild(modalContainer);

            // Setup listeners
            document.getElementById('hint-close-btn').addEventListener('click', closeModal);
            modalContainer.addEventListener('click', (e) => {
                if (e.target === modalContainer) closeModal();
            });

            document.getElementById('btn-use-hint').addEventListener('click', useHint);
            document.getElementById('btn-goto-earn').addEventListener('click', showEarnView);
            document.getElementById('btn-back-to-choice').addEventListener('click', showChoiceView);
            document.getElementById('btn-submit-math').addEventListener('click', submitMathAnswer);
            document.getElementById('math-answer-input').addEventListener('keydown', (e) => {
                if (e.key === 'Enter') submitMathAnswer();
            });
        }
    }

    function onHintButtonClick() {
        // Check if the current game state allows reveals
        if (checkCanRevealCallback && !checkCanRevealCallback()) {
            showToast("Hints can only be used during the input phase!");
            return;
        }
        
        // Open Modal
        const overlay = document.getElementById('hint-overlay');
        overlay.classList.add('open');
        showChoiceView();
        updateDisplays();
    }

    function closeModal() {
        const overlay = document.getElementById('hint-overlay');
        if (overlay) overlay.classList.remove('open');
    }

    function showChoiceView() {
        document.getElementById('hint-choice-view').style.display = 'block';
        document.getElementById('hint-earn-view').style.display = 'none';
        document.getElementById('math-feedback').textContent = '';
        document.getElementById('math-feedback').className = 'math-feedback';
    }

    function showEarnView() {
        const count = getHints();
        if (count > 0) {
            showToast("You can only earn hints when you have 0 hints left!", "warning");
            showChoiceView();
            return;
        }

        document.getElementById('hint-choice-view').style.display = 'none';
        document.getElementById('hint-earn-view').style.display = 'block';
        
        // Generate math
        currentMathChallenge = generateMathQuestion();
        document.getElementById('math-question-text').textContent = currentMathChallenge.text;
        
        const input = document.getElementById('math-answer-input');
        input.value = '';
        input.focus();
        
        document.getElementById('math-feedback').textContent = '';
        document.getElementById('math-feedback').className = 'math-feedback';
    }

    function useHint() {
        const count = getHints();
        if (count <= 0) {
            showToast("You don't have any hints left! Earn one.", "warning");
            return;
        }

        let success = true;
        if (onRevealCallback) {
            try {
                const res = onRevealCallback();
                if (res === false) {
                    success = false;
                }
            } catch (err) {
                console.error("[hint.js] Error executing hint reveal callback:", err);
            }
        }

        if (success) {
            setHints(count - 1);
            showToast("Hint applied!", "success");
            closeModal();
        } else {
            showToast("No empty slots remaining to fill!");
        }
    }

    function generateMathQuestion() {
        const ops = ['+', '-', '*', '/'];
        const op = ops[Math.floor(Math.random() * ops.length)];
        let a, b, qText, ans;
        switch (op) {
            case '+':
                a = Math.floor(Math.random() * 20) + 1;
                b = Math.floor(Math.random() * 20) + 1;
                qText = `${a} + ${b}`;
                ans = a + b;
                break;
            case '-':
                a = Math.floor(Math.random() * 20) + 10;
                b = Math.floor(Math.random() * 9) + 1;
                qText = `${a} - ${b}`;
                ans = a - b;
                break;
            case '*':
                a = Math.floor(Math.random() * 8) + 2;
                b = Math.floor(Math.random() * 8) + 2;
                qText = `${a} × ${b}`;
                ans = a * b;
                break;
            case '/':
                b = Math.floor(Math.random() * 8) + 2; 
                ans = Math.floor(Math.random() * 8) + 2; 
                a = b * ans; 
                qText = `${a} ÷ ${b}`;
                break;
        }
        return { text: qText, answer: ans };
    }

    function submitMathAnswer() {
        const input = document.getElementById('math-answer-input');
        const feedback = document.getElementById('math-feedback');
        
        if (!input.value.trim()) return;

        const val = parseInt(input.value, 10);
        if (val === currentMathChallenge.answer) {
            // Correct
            const current = getHints();
            if (current > 0) {
                showToast("You can only earn hints when you have 0 hints left!", "warning");
                closeModal();
                return;
            }
            setHints(current + 1);
            feedback.textContent = "Correct! +1 Hint added!";
            feedback.className = "math-feedback correct";
            
            setTimeout(() => {
                showChoiceView();
            }, 1200);
        } else {
            // Wrong
            feedback.textContent = "Wrong! Try again.";
            feedback.className = "math-feedback wrong";
            input.value = '';
            input.focus();
            
            // Trigger animation re-run
            feedback.style.animation = 'none';
            feedback.offsetHeight; // trigger reflow
            feedback.style.animation = null;
        }
    }
})();
