// Runs before first paint: applies the saved theme, or the system preference if none was chosen.
(function () {
  var saved = null;
  try {
    saved = localStorage.getItem('theme');
  } catch (e) {
    // Storage can be unavailable (private mode, blocked cookies); fall back to the system theme.
  }
  var dark = saved ? saved === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches;
  document.documentElement.dataset.theme = dark ? 'dark' : 'light';
})();
