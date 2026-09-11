// level4.js – Position Memory Matrix game logic (fully client-side generation)
// Matches the self-contained architecture used in levels 1, 2, and 3.

let currentPart = 1;   // 1 = 4×4 numeric, 2 = 5×5 alphanumeric with 3 blanks
let matrixData  = [];  // the generated matrix (2D array of strings/numbers)
let missingCells = []; // for part 2: [[row,col], ...]
let timerInterval;

// ---------- Utility ----------
function $(id) { return document.getElementById(id); }

function showState(id) {
    document.querySelectorAll('.game-state').forEach(s => s.classList.remove('active'));
    requestAnimationFrame(() => $(id) && $(id).classList.add('active'));
}

function shuffle(arr) {
    for (let i = arr.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
}

// ---------- Matrix Generators ----------
function generateNumericMatrix(size) {
    const nums = shuffle([...Array(size * size).keys()].map(i => i + 1));
    const m = [];
    for (let r = 0; r < size; r++) {
        m.push(nums.slice(r * size, r * size + size).map(String));
    }
    return m;
}

function generateAlphaNumericMatrix(size) {
    const total = size * size;
    const numCount = Math.floor(total / 2);
    let pool = [];
    for (let i = 1; i <= numCount; i++) pool.push(String(i));
    let letter = 65; // 'A'
    while (pool.length < total) { pool.push(String.fromCharCode(letter++)); }
    shuffle(pool);
    const m = [];
    for (let r = 0; r < size; r++) {
        m.push(pool.slice(r * size, r * size + size));
    }
    return m;
}

function pickMissingCells(size, count) {
    const cells = [];
    while (cells.length < count) {
        const r = Math.floor(Math.random() * size);
        const c = Math.floor(Math.random() * size);
        if (!cells.some(p => p[0] === r && p[1] === c)) cells.push([r, c]);
    }
    return cells;
}

// ---------- Timer ----------
function startTimer(seconds, onDone) {
    const timerText     = $('timer-text');
    const timerProgress = $('timer-progress');
    const circumference = 2 * Math.PI * 25;
    let remaining = seconds;

    timerText.textContent = remaining;
    timerProgress.style.strokeDashoffset = '0';
    clearInterval(timerInterval);

    timerInterval = setInterval(() => {
        remaining--;
        timerText.textContent = remaining;
        const offset = circumference - (remaining / seconds) * circumference;
        timerProgress.style.strokeDashoffset = offset;
        if (remaining <= 0) {
            clearInterval(timerInterval);
            onDone();
        }
    }, 1000);
}

// ---------- Rendering ----------
function renderMatrixDisplay() {
    const container = $('matrix-display-area');
    container.innerHTML = '';
    const table = document.createElement('table');
    table.className = 'matrix-table';
    matrixData.forEach(row => {
        const tr = document.createElement('tr');
        row.forEach(val => {
            const td = document.createElement('td');
            td.className = 'matrix-cell';
            td.textContent = val;
            tr.appendChild(td);
        });
        table.appendChild(tr);
    });
    container.appendChild(table);
}

function renderInputGrid() {
    const container = $('matrix-inputs');
    container.innerHTML = '';
    const table = document.createElement('table');
    table.className = 'matrix-input-table';
    matrixData.forEach((row, r) => {
        const tr = document.createElement('tr');
        row.forEach((val, c) => {
            const td = document.createElement('td');
            const isMissing = currentPart === 1 ||
                missingCells.some(cell => cell[0] === r && cell[1] === c);
            if (isMissing) {
                const inp = document.createElement('input');
                inp.type = 'text';
                inp.className = 'matrix-input';
                inp.dataset.row = r;
                inp.dataset.col = c;
                inp.maxLength = 2;
                inp.addEventListener('input', () => { inp.value = inp.value.toUpperCase(); inp.style.borderColor = ''; });
                inp.addEventListener('keydown', e => { if (e.key === 'Enter') submitAnswer(); });
                td.appendChild(inp);
            } else {
                const span = document.createElement('span');
                span.className = 'static-cell';
                span.textContent = val;
                td.appendChild(span);
            }
            tr.appendChild(td);
        });
        table.appendChild(tr);
    });
    container.appendChild(table);
    const first = container.querySelector('input');
    if (first) first.focus();
}

// ---------- Game Flow ----------
function startPart(part) {
    missingCells = [];
    if (part === 1) {
        matrixData = generateNumericMatrix(4);
    } else {
        matrixData = generateAlphaNumericMatrix(5);
        missingCells = pickMissingCells(5, 3);
    }
    renderMatrixDisplay();
    showState('state-showing');
    startTimer(5, transitionToInput);
}

function transitionToInput() {
    renderInputGrid();
    showState('state-input');
}

function submitAnswer() {
    const inputs = Array.from(document.querySelectorAll('.matrix-input'));

    // Highlight empty fields
    let hasEmpty = false;
    inputs.forEach(inp => {
        if (!inp.value.trim()) { inp.style.borderColor = 'var(--error)'; hasEmpty = true; }
    });
    if (hasEmpty) return;

    // Check answers
    let correct = true;
    if (currentPart === 1) {
        // All 16 cells must match
        inputs.forEach(inp => {
            const r = parseInt(inp.dataset.row);
            const c = parseInt(inp.dataset.col);
            if (inp.value.trim().toUpperCase() !== matrixData[r][c].toUpperCase()) correct = false;
        });
    } else {
        // Only the 3 hidden cells must match
        inputs.forEach(inp => {
            const r = parseInt(inp.dataset.row);
            const c = parseInt(inp.dataset.col);
            if (inp.value.trim().toUpperCase() !== matrixData[r][c].toUpperCase()) correct = false;
        });
    }

    showResult(correct);
}

// ---------- Save Progress (Part 2 completion only) ----------
function saveProgressToServer() {
    // Use same relative-URL pattern as levels 1, 2, 3
    fetch('getProgress?t=' + new Date().getTime())
        .then(r => r.ok ? r.json() : Promise.reject())
        .then(data => {
            const score         = (data.score        || 0) + 50;
            const gamesPlayed   = (data.gamesPlayed  || 0) + 1;
            const unlockedLevel = Math.max(data.unlockedLevel || 1, 5);
            const lastPlayedDateStr = data.lastPlayedDate || localStorage.getItem('mm_last_played_date') || '';
            const streakRes         = (window.calculateNewStreak ? window.calculateNewStreak(data.streak || 0, lastPlayedDateStr, true) : { streak: (data.streak || 0), dateStr: (window.getLocalDateString ? window.getLocalDateString() : new Date().toISOString().split('T')[0]) });
            const streak            = streakRes.streak;
            const lastPlayedDate    = streakRes.dateStr;
            const bestStreak        = Math.max(data.bestStreak || 0, streak);
            const bestScores        = safeParseArray ? safeParseArray(data.bestScores, Array(20).fill(0)) : (data.bestScores || Array(20).fill(0));
            const bestTimes         = safeParseArray ? safeParseArray(data.bestTimes, Array(20).fill('-')) : (data.bestTimes || Array(20).fill('-'));
            const stars             = safeParseArray ? safeParseArray(data.stars, Array(20).fill('0')) : (data.stars || Array(20).fill('0'));

            if (50 > (bestScores[3] || 0)) bestScores[3] = 50;
            stars[3]    = '\u2605\u2605\u2605';
            bestTimes[3] = '5s';

            localStorage.setItem('mm_last_played_date', lastPlayedDate);
            localStorage.setItem('mm_streak', streak);
            localStorage.setItem('mm_best_streak', bestStreak);

            return fetch('saveProgress', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({
                    score, streak, unlockedLevel, gamesPlayed, bestStreak,
                    bestScores:    JSON.stringify(bestScores),
                    bestTimes:     JSON.stringify(bestTimes),
                    stars:         JSON.stringify(stars),
                    lastPlayedDate,
                    clientDate: lastPlayedDate
                })
            });
        })
        .catch(() => { /* silently ignore – offline or session expired */ });
}

// ---------- Result Popup ----------
function showResult(isCorrect) {
    const overlay     = $('result-overlay');
    const card        = $('result-card');
    const emoji       = $('result-emoji');
    const title       = $('result-title');
    const desc        = $('result-desc');
    const scoreEl     = $('result-score');
    const partEl      = $('result-part');
    const actionBtn   = $('result-action-btn');
    const secondaryBtn = $('result-secondary-btn');

    card.className = 'result-card ' + (isCorrect ? 'win' : 'lose');

    if (isCorrect) {
        scoreEl.textContent = '+50';
        emoji.textContent   = '🎉';
        title.textContent   = 'Congratulations!';
        secondaryBtn.style.display = 'none';

        if (currentPart === 1) {
            desc.textContent    = 'Part 1 Completed! Get ready for Part 2.';
            partEl.textContent  = 'Part 1 / 2 ✓';
            actionBtn.textContent = 'Next Part ▶';
            actionBtn.onclick = () => {
                closeResult();
                currentPart = 2;
                setupIntroForPart2();
            };
        } else {
            desc.textContent    = 'Level 4 Completed! Amazing memory!';
            partEl.textContent  = 'Part 2 / 2 ✓';
            actionBtn.textContent = 'Back to Dashboard ▶';
            saveProgressToServer();
            actionBtn.onclick = () => {
                closeResult();
                window.location.href = 'dashboard.html';
            };
        }
    } else {
        emoji.textContent   = '❌';
        title.textContent   = 'Wrong Answer!';
        desc.textContent    = 'Not quite right. Try again!';
        partEl.textContent  = currentPart === 1 ? 'Part 1 / 2' : 'Part 2 / 2';
        actionBtn.textContent = 'Try Again';
        secondaryBtn.style.display = 'none';
        actionBtn.onclick = () => {
            closeResult();
            startPart(currentPart); // regenerate with a fresh matrix
        };
    }

    overlay.classList.add('open');
}

function closeResult() {
    $('result-overlay').classList.remove('open');
}

// ---------- Part 2 Intro ----------
function setupIntroForPart2() {
    $('intro-badge').textContent    = '⚡ Level 4 • Part 2';
    $('intro-emoji').textContent    = '🔤';
    $('intro-title').textContent    = 'Alphanumeric Matrix';
    $('intro-subtitle').textContent = 'Numbers & letters — three cells will be hidden.';
    $('intro-desc').textContent     = 'A 5×5 grid filled with numbers and letters appears for 5 seconds. Three cells will be blank — fill them in correctly.';
    $('topbar-part-label').textContent = 'Part 2: Alphanumeric Matrix';
    $('dot-1').className = 'part-dot done';
    $('dot-2').className = 'part-dot active';
    showState('state-intro');
}

// ---------- Event Listeners ----------
$('btn-start').addEventListener('click', () => startPart(currentPart));
$('btn-submit').addEventListener('click', submitAnswer);

// ---------- Init ----------
function init() {
    $('intro-badge').textContent    = '⚡ Level 4 • Part 1';
    $('intro-emoji').textContent    = '🧩';
    $('intro-title').textContent    = 'Position Memory Matrix';
    $('intro-subtitle').textContent = 'Remember numbers in a 4×4 grid';
    $('intro-desc').textContent     = 'A 4×4 grid filled with numbers (1–16) will appear for 5 seconds. Memorize the layout, then reproduce it.';
    $('topbar-part-label').textContent = 'Part 1: Numeric Matrix';
    $('dot-1').className = 'part-dot active';
    $('dot-2').className = 'part-dot';
    showState('state-intro');

    // Hint system
    initHintSystem(
        () => {
            const inputState = $('state-input');
            return inputState && inputState.classList.contains('active');
        },
        () => {
            const inputs = document.querySelectorAll('.matrix-input');
            for (let inp of inputs) {
                if (!inp.value.trim()) {
                    const r = parseInt(inp.dataset.row);
                    const c = parseInt(inp.dataset.col);
                    inp.value = matrixData[r][c];
                    inp.dispatchEvent(new Event('input'));
                    return true;
                }
            }
            return false;
        }
    );
}

init();
