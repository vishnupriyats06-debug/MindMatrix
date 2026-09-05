/**
 * MindMatrix – main.js
 * Shared utilities for registration and login pages
 */

/* ═══════════════════════════════════════════
   URL Error Message Display
   Reads ?error=... from URL and shows banner
═══════════════════════════════════════════ */
function showUrlError() {
    const params = new URLSearchParams(window.location.search);
    const msg    = params.get('error');
    const succ   = params.get('success');
    if (msg) showAlert(decodeURIComponent(msg), 'error');
    else if (succ) showAlert(decodeURIComponent(succ), 'success');

    // Clean URL (no reload)
    const cleanUrl = window.location.pathname;
    window.history.replaceState({}, '', cleanUrl);
}

/* ═══════════════════════════════════════════
   Alert Banner
═══════════════════════════════════════════ */
function showAlert(message, type = 'error') {
    const banner = document.getElementById('alert-banner');
    if (!banner) return;

    banner.className = 'alert-banner ' + type;

    const icon = type === 'error'
        ? `<svg class="alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
               <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/>
               <line x1="12" y1="16" x2="12.01" y2="16"/>
           </svg>`
        : `<svg class="alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
               <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
               <polyline points="22 4 12 14.01 9 11.01"/>
           </svg>`;

    banner.innerHTML = icon + `<span>${message}</span>`;
    banner.style.display = 'flex';
}

function hideAlert() {
    const banner = document.getElementById('alert-banner');
    if (banner) banner.style.display = 'none';
}

/* ═══════════════════════════════════════════
   Password Visibility Toggle
═══════════════════════════════════════════ */
function initPasswordToggle(inputId, toggleId) {
    const input  = document.getElementById(inputId);
    const toggle = document.getElementById(toggleId);
    if (!input || !toggle) return;

    toggle.addEventListener('click', () => {
        const isPassword = input.type === 'password';
        input.type = isPassword ? 'text' : 'password';
        toggle.innerHTML = isPassword ? eyeOffIcon() : eyeIcon();
    });
}

function eyeIcon() {
    return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
        <circle cx="12" cy="12" r="3"/>
    </svg>`;
}

function eyeOffIcon() {
    return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
        <line x1="1" y1="1" x2="23" y2="23"/>
    </svg>`;
}

/* ═══════════════════════════════════════════
   Live Input Validation
═══════════════════════════════════════════ */
function addLiveValidation() {
    document.querySelectorAll('.form-input').forEach(input => {
        input.addEventListener('blur', () => validateField(input));
        input.addEventListener('input', () => {
            if (input.classList.contains('input-error')) validateField(input);
        });
    });
}

function validateField(input) {
    const val = input.value.trim();
    let valid = true;

    if (input.id === 'email') {
        valid = /^[\w.+-]+@[\w-]+\.[\w.]+$/.test(val);
    } else if (input.id === 'password') {
        valid = val.length >= 6;
    } else {
        valid = val.length > 0;
    }

    input.classList.toggle('input-error', !valid && val.length > 0);
    input.classList.toggle('input-valid', valid);
}

/* ═══════════════════════════════════════════
   Button Loading State
═══════════════════════════════════════════ */
function setButtonLoading(btn, loading) {
    if (loading) {
        btn.classList.add('loading');
        btn.disabled = true;
    } else {
        btn.classList.remove('loading');
        btn.disabled = false;
    }
}

/* ═══════════════════════════════════════════
   Animated Background Orbs
═══════════════════════════════════════════ */
function initBackgroundOrbs() {
    const scene = document.querySelector('.bg-scene');
    if (!scene) return;

    const orbs = [
        { size: 350, color: '#7c3aed', top: '30%', left: '60%', dur: '20s' },
        { size: 250, color: '#0891b2', top: '70%', left: '20%', dur: '25s' },
        { size: 200, color: '#db2777', top: '10%', left: '80%', dur: '22s' },
    ];

    orbs.forEach(o => {
        const el = document.createElement('div');
        el.className = 'orb';
        el.style.cssText = `
            width:${o.size}px; height:${o.size}px;
            background:${o.color};
            top:${o.top}; left:${o.left};
            animation-duration:${o.dur};
        `;
        scene.appendChild(el);
    });
}

/* ═══════════════════════════════════════════
   Init on DOMContentLoaded
═══════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
    showUrlError();
    initBackgroundOrbs();
    addLiveValidation();
    initPasswordToggle('password', 'pw-toggle');
    initPasswordToggle('password2', 'pw-toggle2'); // confirm password (register only)
});
