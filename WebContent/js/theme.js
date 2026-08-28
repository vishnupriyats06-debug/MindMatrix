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
