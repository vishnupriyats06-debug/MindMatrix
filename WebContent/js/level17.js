/**
 * Level 17 – Advanced Memory Challenge
 * Part 1: Relationship Reconstruction (Person → Object → Location)
 * Part 2: Memory Calculation Challenge (Addition, Subtraction, Multiplication)
 */

// ═══════════════════════════════════════════════════════════════════════════
// GLOBAL STATE
// ═══════════════════════════════════════════════════════════════════════════

let gameState = {
    currentPart: 1,
    part1: {
        difficulty: 'medium', // easy, medium, hard, advanced
        relationships: [], // [ { person, object, location }, ... ]
        shuffled: { people: [], objects: [], locations: [] },
        userConnections: [], // [ { person, object, location }, ... ]
        attempts: 0,
        maxAttempts: 3
    },
    part2: {
        objectNumbers: {}, // { emoji: number, ... }
        currentSection: 'addition', // addition, subtraction, multiplication
        sections: {
            addition: { questions: [], currentIndex: 0, score: 0 },
            subtraction: { questions: [], currentIndex: 0, score: 0 },
            multiplication: { questions: [], currentIndex: 0, score: 0 }
        },
        totalScore: 0,
        hintsUsed: 0
    }
};

let timerInterval = null;

// ═══════════════════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

function $(id) {
    return document.getElementById(id);
}

function showState(stateId) {
    document.querySelectorAll('.game-state').forEach(state => {
        state.classList.remove('active');
    });
    requestAnimationFrame(() => {
        const state = $(stateId);
        if (state) state.classList.add('active');
    });
}

function shuffle(arr) {
    const copy = [...arr];
    for (let i = copy.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [copy[i], copy[j]] = [copy[j], copy[i]];
    }
    return copy;
}

function startTimer(duration, timerId, progressId, onComplete) {
    const timerText = $(timerId);
    const timerProgress = $(progressId);
    const circumference = 2 * Math.PI * 45; // SVG circle radius
    let remaining = duration;

    timerText.textContent = remaining;
    timerProgress.style.strokeDashoffset = '0';
    clearInterval(timerInterval);

    timerInterval = setInterval(() => {
        remaining--;
        timerText.textContent = remaining;
        const offset = circumference - (remaining / duration) * circumference;
        timerProgress.style.strokeDashoffset = offset;
        if (remaining <= 0) {
            clearInterval(timerInterval);
            onComplete();
        }
    }, 1000);
}

// ═══════════════════════════════════════════════════════════════════════════
// PART 1: RELATIONSHIP RECONSTRUCTION
// ═══════════════════════════════════════════════════════════════════════════

function startPart1() {
    gameState.part1.attempts = 0;
    gameState.part1.userConnections = [];
    generatePart1Relationships();
    displayPart1Relationships();
    showState('state-part1-showing');
    startTimer(15, 'timer-text-part1', 'timer-progress-part1', transitionToPart1Reconstruction);
}

function generatePart1Relationships() {
    // Use medium difficulty by default
    const difficulty = gameState.part1.difficulty;
    const counts = {
        easy: 4,
        medium: 5,
        hard: 6,
        advanced: 6
    };

    const count = counts[difficulty] || 5;

    // Define pools of people, objects, and locations
    const peopleCandidates = [
        { emoji: '👩', name: 'Anu' },
        { emoji: '👨', name: 'Ravi' },
        { emoji: '👧', name: 'Maya' },
        { emoji: '👦', name: 'Arun' },
        { emoji: '👱', name: 'Sam' },
        { emoji: '🧔', name: 'Alex' }
    ];

    const objectCandidates = [
        { emoji: '🎒', name: 'Bag' },
        { emoji: '📱', name: 'Phone' },
        { emoji: '📕', name: 'Book' },
        { emoji: '🎧', name: 'Headphones' },
        { emoji: '⌚', name: 'Watch' },
        { emoji: '👓', name: 'Glasses' }
    ];

    const locationCandidates = [
        { emoji: '🪑', name: 'Chair' },
        { emoji: '🛋️', name: 'Sofa' },
        { emoji: '🪟', name: 'Window' },
        { emoji: '🚪', name: 'Door' },
        { emoji: '🛏️', name: 'Bed' },
        { emoji: '🚗', name: 'Car' }
    ];

    // Shuffle and pick
    const people = shuffle(peopleCandidates).slice(0, count);
    const objects = shuffle(objectCandidates).slice(0, count);
    const locations = shuffle(locationCandidates).slice(0, count);

    // Create relationships
    gameState.part1.relationships = people.map((person, i) => ({
        person: person.emoji + ' ' + person.name,
        object: objects[i].emoji + ' ' + objects[i].name,
        location: locations[i].emoji + ' ' + locations[i].name
    }));

    // Shuffle for reconstruction
    gameState.part1.shuffled = {
        people: shuffle(people.map((p, i) => ({ ...p, index: i }))),
        objects: shuffle(objects.map((o, i) => ({ ...o, index: i }))),
        locations: shuffle(locations.map((l, i) => ({ ...l, index: i })))
    };
}

function displayPart1Relationships() {
    const container = $('relationship-display');
    container.innerHTML = '';

    gameState.part1.relationships.forEach(rel => {
        const row = document.createElement('div');
        row.className = 'relationship-row';
        row.innerHTML = `
            <div class="relationship-item">${rel.person}</div>
            <div class="relationship-arrow">→</div>
            <div class="relationship-item">${rel.object}</div>
            <div class="relationship-arrow">→</div>
            <div class="relationship-item">${rel.location}</div>
        `;
        container.appendChild(row);
    });
}

function transitionToPart1Reconstruction() {
    buildPart1ReconstructionUI();
    showState('state-part1-reconstruction');
}

function buildPart1ReconstructionUI() {
    const container = $('reconstruction-container');
    container.innerHTML = '';

    // People Section
    const peopleSection = document.createElement('div');
    peopleSection.className = 'category-section';
    peopleSection.innerHTML = '<div class="category-title">People</div>';
    const peopleList = document.createElement('div');
    peopleList.className = 'items-list';
    gameState.part1.shuffled.people.forEach(person => {
        const btn = document.createElement('button');
        btn.className = 'item-btn';
        btn.textContent = person.emoji + ' ' + person.name;
        btn.dataset.type = 'person';
        btn.dataset.value = person.index;
        btn.onclick = () => selectPart1Item(btn, 'person', person.index);
        peopleList.appendChild(btn);
    });
    peopleSection.appendChild(peopleList);
    container.appendChild(peopleSection);

    // Objects Section
    const objectsSection = document.createElement('div');
    objectsSection.className = 'category-section';
    objectsSection.innerHTML = '<div class="category-title">Objects</div>';
    const objectsList = document.createElement('div');
    objectsList.className = 'items-list';
    gameState.part1.shuffled.objects.forEach(object => {
        const btn = document.createElement('button');
        btn.className = 'item-btn';
        btn.textContent = object.emoji + ' ' + object.name;
        btn.dataset.type = 'object';
        btn.dataset.value = object.index;
        btn.onclick = () => selectPart1Item(btn, 'object', object.index);
        objectsList.appendChild(btn);
    });
    objectsSection.appendChild(objectsList);
    container.appendChild(objectsSection);

    // Locations Section
    const locationsSection = document.createElement('div');
    locationsSection.className = 'category-section';
    locationsSection.innerHTML = '<div class="category-title">Locations</div>';
    const locationsList = document.createElement('div');
    locationsList.className = 'items-list';
    gameState.part1.shuffled.locations.forEach(location => {
        const btn = document.createElement('button');
        btn.className = 'item-btn';
        btn.textContent = location.emoji + ' ' + location.name;
        btn.dataset.type = 'location';
        btn.dataset.value = location.index;
        btn.onclick = () => selectPart1Item(btn, 'location', location.index);
        locationsList.appendChild(btn);
    });
    locationsSection.appendChild(locationsList);
    container.appendChild(locationsSection);

    updatePart1ConnectionDisplay();
}

let part1Selection = { person: null, object: null, location: null };

function selectPart1Item(btn, type, index) {
    const wasSelected = btn.classList.contains('selected');

    // Deselect previous selection of same type
    document.querySelectorAll(`button[data-type="${type}"].selected`).forEach(b => {
        b.classList.remove('selected');
    });

    if (!wasSelected) {
        btn.classList.add('selected');
        part1Selection[type] = index;
    } else {
        part1Selection[type] = null;
    }

    // Auto-add connection if all three are selected
    if (part1Selection.person !== null && part1Selection.object !== null && part1Selection.location !== null) {
        addPart1Connection();
        resetPart1Selection();
    }

    updatePart1ConnectionDisplay();
}

function resetPart1Selection() {
    part1Selection = { person: null, object: null, location: null };
    document.querySelectorAll('button.selected').forEach(b => {
        b.classList.remove('selected');
    });
}

function addPart1Connection() {
    const rel = gameState.part1.relationships[part1Selection.person];
    gameState.part1.userConnections.push({
        personIndex: part1Selection.person,
        objectIndex: part1Selection.object,
        locationIndex: part1Selection.location,
        person: rel.person,
        object: gameState.part1.shuffled.objects[part1Selection.object],
        location: gameState.part1.shuffled.locations[part1Selection.location]
    });

    // Mark items as used
    document.querySelectorAll(`button[data-value="${part1Selection.person}"][data-type="person"]`).forEach(b => {
        b.classList.add('used');
        b.disabled = true;
    });
    document.querySelectorAll(`button[data-value="${part1Selection.object}"][data-type="object"]`).forEach(b => {
        b.classList.add('used');
        b.disabled = true;
    });
    document.querySelectorAll(`button[data-value="${part1Selection.location}"][data-type="location"]`).forEach(b => {
        b.classList.add('used');
        b.disabled = true;
    });
}

function updatePart1ConnectionDisplay() {
    const display = $('connections-display');
    display.innerHTML = '';

    gameState.part1.userConnections.forEach((conn, idx) => {
        const div = document.createElement('div');
        div.className = 'connection-item';
        const correctObject = gameState.part1.relationships[conn.personIndex].object;
        const correctLocation = gameState.part1.relationships[conn.personIndex].location;
        const isCorrect = 
            conn.object.emoji + ' ' + conn.object.name === correctObject &&
            conn.location.emoji + ' ' + conn.location.name === correctLocation;

        if (isCorrect) div.classList.add('correct');

        div.innerHTML = `
            <span>${conn.person}</span>
            <span style="color: var(--accent-2); font-weight: bold;">→</span>
            <span>${conn.object.emoji} ${conn.object.name}</span>
            <span style="color: var(--accent-2); font-weight: bold;">→</span>
            <span>${conn.location.emoji} ${conn.location.name}</span>
        `;
        display.appendChild(div);
    });

    // Progress indicator
    const total = gameState.part1.relationships.length;
    const progress = gameState.part1.userConnections.length;
    if (progress < total) {
        const progressDiv = document.createElement('div');
        progressDiv.className = 'connection-item';
        progressDiv.style.opacity = '0.6';
        progressDiv.innerHTML = `<span>Progress: ${progress} / ${total}</span>`;
        display.appendChild(progressDiv);
    }
}

function resetPart1Connections() {
    gameState.part1.userConnections = [];
    resetPart1Selection();
    buildPart1ReconstructionUI();
}

function submitPart1Connections() {
    const total = gameState.part1.relationships.length;
    if (gameState.part1.userConnections.length !== total) {
        showFeedback('error', 'Please complete all relationships before submitting.');
        return;
    }

    // Validate all connections
    let allCorrect = true;
    gameState.part1.userConnections.forEach(conn => {
        const correctObject = gameState.part1.relationships[conn.personIndex].object;
        const correctLocation = gameState.part1.relationships[conn.personIndex].location;
        if (conn.object.emoji + ' ' + conn.object.name !== correctObject ||
            conn.location.emoji + ' ' + conn.location.name !== correctLocation) {
            allCorrect = false;
        }
    });

    showPart1Result(allCorrect);
}

function showPart1Result(correct) {
    const content = $('part1-result-content');

    if (correct) {
        content.innerHTML = `
            <div style="text-align: center;">
                <div class="completion-emoji" style="font-size: 56px;">✅</div>
                <h3 style="font-size: 24px; font-weight: 800; color: var(--success); margin: 16px 0;">
                    Excellent! You reconstructed all relationships correctly!
                </h3>
                <p style="color: var(--text-sec); margin-top: 12px;">
                    You successfully remembered and reconnected all relationships.
                </p>
            </div>
        `;
    } else {
        gameState.part1.attempts++;
        if (gameState.part1.attempts < gameState.part1.maxAttempts) {
            content.innerHTML = `
                <div style="text-align: center;">
                    <div style="font-size: 48px; margin-bottom: 16px;">❌</div>
                    <h3 style="font-size: 22px; font-weight: 800; color: var(--error); margin: 16px 0;">
                        Incorrect! Some relationships are wrong.
                    </h3>
                    <p style="color: var(--text-sec); margin-top: 12px;">
                        Attempt ${gameState.part1.attempts} of ${gameState.part1.maxAttempts}
                    </p>
                    <p style="color: var(--text-muted); margin-top: 8px; font-size: 13px;">
                        Try again with renewed focus.
                    </p>
                </div>
            `;
            $('continue-btn').textContent = 'Try Again';
            $('continue-btn').onclick = () => startPart1();
        } else {
            content.innerHTML = `
                <div style="text-align: center;">
                    <div style="font-size: 48px; margin-bottom: 16px;">⏱️</div>
                    <h3 style="font-size: 22px; font-weight: 800; color: var(--warning); margin: 16px 0;">
                        Max Attempts Reached
                    </h3>
                    <p style="color: var(--text-sec); margin-top: 12px;">
                        You've used all your attempts. Let's move to Part 2 anyway!
                    </p>
                </div>
            `;
        }
    }

    showState('state-part1-result');
}

function continueToPart2() {
    updatePartIndicators(2);
    startPart2();
}

function updatePartIndicators(part) {
    if (part === 2) {
        $('part1-dot').classList.remove('active');
        $('part1-dot').classList.add('done');
        $('part2-dot').classList.add('active');
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PART 2: MEMORY CALCULATION CHALLENGE
// ═══════════════════════════════════════════════════════════════════════════

function startPart2() {
    gameState.currentPart = 2;
    $('current-section').textContent = 'Part 2: Memory Calculation Challenge';
    generatePart2ObjectNumbers();
    displayPart2ObjectNumbers();
    showState('state-part2-memorizing');
    startTimer(15, 'timer-text-part2', 'timer-progress-part2', transitionToPart2Sections);
}

function generatePart2ObjectNumbers() {
    const objects = [
        { emoji: '🍎', name: 'Apple' },
        { emoji: '⭐', name: 'Star' },
        { emoji: '🔵', name: 'Blue' },
        { emoji: '🔺', name: 'Triangle' },
        { emoji: '🟩', name: 'Square' }
    ];

    const numbers = [7, 4, 9, 3, 6];
    gameState.part2.objectNumbers = {};

    objects.forEach((obj, i) => {
        gameState.part2.objectNumbers[obj.emoji] = numbers[i];
    });

    // Generate questions for each section
    generatePart2Questions();
}

function generatePart2Questions() {
    const emojis = Object.keys(gameState.part2.objectNumbers);

    // Addition Questions
    gameState.part2.sections.addition.questions = [
        { emojis: [emojis[0], emojis[1]], operation: '+' },
        { emojis: [emojis[0], emojis[1], emojis[2]], operation: '+' },
        { emojis: [emojis[1], emojis[2], emojis[3]], operation: '+' },
        { emojis: [emojis[0], emojis[2], emojis[3], emojis[4]], operation: '+' },
        { emojis: [emojis[1], emojis[3]], operation: '+' }
    ];

    // Subtraction Questions
    gameState.part2.sections.subtraction.questions = [
        { emojis: [emojis[2], emojis[3]], operation: '-' },
        { emojis: [emojis[2], emojis[1], emojis[3]], operation: '-' },
        { emojis: [emojis[4], emojis[1]], operation: '-' },
        { emojis: [emojis[2], emojis[0], emojis[3]], operation: '-' },
        { emojis: [emojis[4], emojis[3]], operation: '-' }
    ];

    // Multiplication Questions
    gameState.part2.sections.multiplication.questions = [
        { emojis: [emojis[0], emojis[3]], operation: '×' },
        { emojis: [emojis[1], emojis[3]], operation: '×' },
        { emojis: [emojis[2], emojis[1]], operation: '×' },
        { emojis: [emojis[3], emojis[4]], operation: '×' },
        { emojis: [emojis[0], emojis[1]], operation: '×' }
    ];
}

function displayPart2ObjectNumbers() {
    const grid = $('object-number-grid');
    grid.innerHTML = '';

    Object.keys(gameState.part2.objectNumbers).forEach(emoji => {
        const num = gameState.part2.objectNumbers[emoji];
        const card = document.createElement('div');
        card.className = 'object-card';
        card.innerHTML = `
            <div class="object-emoji">${emoji}</div>
            <div class="object-value">${num}</div>
        `;
        grid.appendChild(card);
    });
}

function transitionToPart2Sections() {
    gameState.part2.currentSection = 'addition';
    gameState.part2.sections.addition.currentIndex = 0;
    showPart2Section();
}

function showPart2Section() {
    const section = gameState.part2.sections[gameState.part2.currentSection];
    const totalQuestions = section.questions.length;
    const currentIndex = section.currentIndex;

    $('section-tag').textContent = gameState.part2.currentSection.charAt(0).toUpperCase() + gameState.part2.currentSection.slice(1) + ' Challenge';
    $('section-title').textContent = 'Recall and calculate';

    updatePart2Progress();
    displayPart2Question();

    showState('state-part2-section');
}

function displayPart2Question() {
    const section = gameState.part2.sections[gameState.part2.currentSection];
    const question = section.questions[section.currentIndex];
    const container = $('calc-question');

    let html = '';
    question.emojis.forEach((emoji, i) => {
        if (i > 0) {
            html += `<span class="calc-operator">${question.operation}</span>`;
        }
        html += `<span class="calc-emoji">${emoji}</span>`;
    });
    html += '<span class="calc-operator">=</span><span>?</span>';

    container.innerHTML = html;

    const input = $('calc-answer');
    input.value = '';
    input.classList.remove('error');
    input.focus();

    $('calc-feedback').classList.remove('active');
}

function updatePart2Progress() {
    const section = gameState.part2.sections[gameState.part2.currentSection];
    const currentIndex = section.currentIndex;
    const totalQuestions = section.questions.length;

    $('progress-text').textContent = `Question ${currentIndex + 1} of ${totalQuestions}`;
    const percent = Math.round(((currentIndex + 1) / totalQuestions) * 100);
    $('progress-percent').textContent = `${percent}%`;
    $('progress-fill').style.width = `${percent}%`;
}

function submitCalcAnswer() {
    const input = $('calc-answer');
    const answer = parseInt(input.value, 10);

    if (isNaN(answer)) {
        input.classList.add('error');
        showFeedback('error', 'Please enter a valid number.');
        return;
    }

    const section = gameState.part2.sections[gameState.part2.currentSection];
    const question = section.questions[section.currentIndex];

    // Calculate correct answer
    let correctAnswer = gameState.part2.objectNumbers[question.emojis[0]];
    for (let i = 1; i < question.emojis.length; i++) {
        const value = gameState.part2.objectNumbers[question.emojis[i]];
        if (question.operation === '+') correctAnswer += value;
        else if (question.operation === '-') correctAnswer -= value;
        else if (question.operation === '×') correctAnswer *= value;
    }

    if (answer === correctAnswer) {
        showFeedback('success', 'Correct! You remembered the values and solved it!');
        section.score++;
        gameState.part2.totalScore++;

        setTimeout(() => {
            section.currentIndex++;
            if (section.currentIndex < section.questions.length) {
                displayPart2Question();
            } else {
                completeSection();
            }
        }, 1200);
    } else {
        input.classList.add('error');
        showFeedback('error', `Incorrect. The answer is ${correctAnswer}. Try to recall the values.`);
    }
}

function skipQuestion() {
    const section = gameState.part2.sections[gameState.part2.currentSection];
    section.currentIndex++;

    if (section.currentIndex < section.questions.length) {
        displayPart2Question();
    } else {
        completeSection();
    }
}

function completeSection() {
    const section = gameState.part2.currentSection;

    if (section === 'addition') {
        gameState.part2.currentSection = 'subtraction';
        gameState.part2.sections.subtraction.currentIndex = 0;
        showPart2Section();
    } else if (section === 'subtraction') {
        gameState.part2.currentSection = 'multiplication';
        gameState.part2.sections.multiplication.currentIndex = 0;
        showPart2Section();
    } else {
        showPart2Complete();
    }
}

function showPart2Complete() {
    const content = $('part2-complete-content');
    const totalScore = gameState.part2.totalScore;

    content.innerHTML = `
        <div style="text-align: center;">
            <div class="completion-emoji" style="font-size: 56px;">🎯</div>
            <h3 style="font-size: 24px; font-weight: 800; color: var(--success); margin: 16px 0;">
                Part 2 Completed!
            </h3>
            <p style="color: var(--text-sec); margin-top: 12px;">
                You successfully completed all calculation challenges!
            </p>
            <div style="margin: 20px 0; padding: 16px; background: rgba(139,92,246,0.12); border: 1.5px solid rgba(139,92,246,0.25); border-radius: 14px;">
                <div style="font-size: 14px; color: var(--text-sec); margin: 8px 0;">
                    <strong>Addition:</strong> ${gameState.part2.sections.addition.score} / ${gameState.part2.sections.addition.questions.length}
                </div>
                <div style="font-size: 14px; color: var(--text-sec); margin: 8px 0;">
                    <strong>Subtraction:</strong> ${gameState.part2.sections.subtraction.score} / ${gameState.part2.sections.subtraction.questions.length}
                </div>
                <div style="font-size: 14px; color: var(--text-sec); margin: 8px 0;">
                    <strong>Multiplication:</strong> ${gameState.part2.sections.multiplication.score} / ${gameState.part2.sections.multiplication.questions.length}
                </div>
                <hr style="border: none; border-top: 1px solid rgba(139,92,246,0.25); margin: 12px 0;">
                <div style="font-size: 16px; font-weight: 700; color: var(--accent-2);">
                    Total Score: ${totalScore} / 15
                </div>
            </div>
        </div>
    `;

    showState('state-part2-complete');
}

function completeLevelAndSave() {
    saveProgressToServer();
}

// ═══════════════════════════════════════════════════════════════════════════
// SAVE PROGRESS & SERVER COMMUNICATION
// ═══════════════════════════════════════════════════════════════════════════

function saveProgressToServer() {
    // Calculate final score
    const calculationScore = gameState.part2.totalScore * 10; // 0-150 points
    const relationshipBonus = (gameState.part1.attempts === 0) ? 100 : 50;
    const finalScore = calculationScore + relationshipBonus;

    fetch('getProgress?t=' + new Date().getTime())
        .then(r => r.ok ? r.json() : Promise.reject())
        .then(data => {
            const score = (data.score || 0) + finalScore;
            const gamesPlayed = (data.gamesPlayed || 0) + 1;
            const unlockedLevel = Math.max(data.unlockedLevel || 1, 18);
            const lastPlayedDateStr = data.lastPlayedDate || localStorage.getItem('mm_last_played_date') || '';
            const streakRes = (window.calculateNewStreak ? window.calculateNewStreak(data.streak || 0, lastPlayedDateStr, true) : { streak: (data.streak || 0), dateStr: (window.getLocalDateString ? window.getLocalDateString() : new Date().toISOString().split('T')[0]) });
            const streak = streakRes.streak;
            const lastPlayedDate = streakRes.dateStr;
            const bestStreak = Math.max(data.bestStreak || 0, streak);
            const bestScores = data.bestScores || Array(20).fill(0);
            const bestTimes = data.bestTimes || Array(20).fill('-');
            const stars = data.stars || Array(20).fill('0');

            // Update best score for level 17 (index 16)
            const scoresArr = Array.isArray(bestScores) ? bestScores : (typeof bestScores === 'string' ? JSON.parse(bestScores) : Array(20).fill(0));
            scoresArr[16] = Math.max(scoresArr[16] || 0, finalScore);

            localStorage.setItem('mm_last_played_date', lastPlayedDate);
            localStorage.setItem('mm_streak', streak);
            localStorage.setItem('mm_best_streak', bestStreak);

            const payload = {
                score: score,
                streak: streak,
                unlockedLevel: unlockedLevel,
                gamesPlayed: gamesPlayed,
                bestStreak: bestStreak,
                bestScores: JSON.stringify(scoresArr),
                bestTimes: typeof bestTimes === 'string' ? bestTimes : JSON.stringify(bestTimes),
                stars: typeof stars === 'string' ? stars : JSON.stringify(stars),
                lastPlayedDate: lastPlayedDate,
                clientDate: lastPlayedDate
            };

            return fetch('saveProgress', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams(payload)
            });
        })
        .then(r => {
            if (r.ok) {
                showState('state-level-complete');
            } else {
                showFeedback('error', 'Failed to save progress. Please try again.');
            }
        })
        .catch(err => {
            console.error('Error saving progress:', err);
            showFeedback('error', 'Error saving progress. You can still continue!');
        });
}

// ═══════════════════════════════════════════════════════════════════════════
// FEEDBACK & UTILITIES
// ═══════════════════════════════════════════════════════════════════════════

function showFeedback(type, message) {
    const feedback = $('calc-feedback');
    feedback.textContent = message;
    feedback.className = 'feedback-message ' + type + ' active';
}

function requestHint() {
    // Integration with existing hint system
    if (window.initHintSystem) {
        const hints = parseInt(localStorage.getItem('mm_hints') || '3', 10);
        if (hints > 0) {
            // Deduct hint
            localStorage.setItem('mm_hints', Math.max(0, hints - 1).toString());
            
            if (gameState.currentPart === 1) {
                showHintPart1();
            } else {
                showHintPart2();
            }
            
            // Update hint count display
            $('hint-count').textContent = Math.max(0, hints - 1);
        }
    }
}

function showHintPart1() {
    if (gameState.part1.relationships.length === 0) return;
    
    const randomRel = gameState.part1.relationships[Math.floor(Math.random() * gameState.part1.relationships.length)];
    alert(`Hint: ${randomRel.person} was associated with the ${randomRel.object.split(' ')[1]}.`);
}

function showHintPart2() {
    const emojis = Object.keys(gameState.part2.objectNumbers);
    if (emojis.length === 0) return;
    
    const randomEmoji = emojis[Math.floor(Math.random() * emojis.length)];
    const value = gameState.part2.objectNumbers[randomEmoji];
    alert(`Hint: Remember ${randomEmoji} = ${value}`);
}

// ═══════════════════════════════════════════════════════════════════════════
// INITIALIZATION
// ═══════════════════════════════════════════════════════════════════════════

window.addEventListener('load', () => {
    // Initialize hint system if available
    if (window.initHintSystem) {
        window.initHintSystem(() => true, () => {});
    }

    // Set initial hint count display
    const hints = parseInt(localStorage.getItem('mm_hints') || '3', 10);
    $('hint-count').textContent = hints;
});
