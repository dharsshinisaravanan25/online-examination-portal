// ExamNexus Core Application Logic
let currentUser = null;
let activeExamSession = null;
let examTimerInterval = null;
let currentQuestionIndex = 0;
let currentEditingExamId = null;
let currentQuestionBankExamId = null;

// Audio context for anti-cheat warning sound
let audioCtx = null;
function playWarningBeep() {
  try {
    if (!audioCtx) {
      audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    }
    if (audioCtx.state === 'suspended') {
      audioCtx.resume();
    }
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(440, audioCtx.currentTime);
    osc.frequency.exponentialRampToValueAtTime(880, audioCtx.currentTime + 0.25);
    gain.gain.setValueAtTime(0.25, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.25);
    osc.connect(gain);
    gain.connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.25);
  } catch (e) {
    console.warn('Audio play restricted:', e);
  }
}

// API Helper
async function apiCall(endpoint, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  if (currentUser && currentUser.id) {
    headers['X-User-Id'] = currentUser.id;
  }

  const response = await fetch(endpoint, {
    ...options,
    headers
  });

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.message || 'API request failed');
  }
  return data;
}

// Floating Toast Notification System
function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;
  const toast = document.createElement('div');
  toast.className = `toast-item ${type}`;
  const iconMap = { success: '✓', danger: '⚠️', warning: '⚡', info: 'ℹ️' };
  toast.innerHTML = `<span style="font-size: 1.15rem;">${iconMap[type] || 'ℹ️'}</span><span>${escapeHtml(message)}</span>`;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(50px)';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

// App Initialization
window.addEventListener('DOMContentLoaded', () => {
  const savedUser = localStorage.getItem('examnexus_user');
  if (savedUser) {
    try {
      currentUser = JSON.parse(savedUser);
      updateNavUser();
      routeUserView();
    } catch (e) {
      localStorage.removeItem('examnexus_user');
      switchView('viewAuth');
    }
  } else {
    switchView('viewAuth');
  }

  initAntiCheatListeners();
  setupNavEvents();
  initKeyboardShortcuts();
});

function initKeyboardShortcuts() {
  window.addEventListener('keydown', (e) => {
    if (!activeExamSession) return;
    if (['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName)) return;

    const key = e.key.toUpperCase();
    if (['A', 'B', 'C', 'D'].includes(key)) {
      selectOption(key);
    } else if (['1', '2', '3', '4'].includes(e.key)) {
      const map = { '1': 'A', '2': 'B', '3': 'C', '4': 'D' };
      selectOption(map[e.key]);
    } else if (e.key === 'ArrowRight') {
      navigateQuestion(1);
    } else if (e.key === 'ArrowLeft') {
      navigateQuestion(-1);
    }
  });
}

// View Routing
function switchView(viewId) {
  document.querySelectorAll('.view-section').forEach(sec => sec.classList.remove('active'));
  const target = document.getElementById(viewId);
  if (target) {
    target.classList.add('active');
  }
}

function updateNavUser() {
  const navActions = document.getElementById('navActions');
  const navUserName = document.getElementById('navUserName');
  const navUserRole = document.getElementById('navUserRole');
  const userAvatar = document.getElementById('userAvatar');
  const btnNavStudent = document.getElementById('btnNavStudent');
  const btnNavAdmin = document.getElementById('btnNavAdmin');
  const btnNavHistory = document.getElementById('btnNavHistory');

  if (!currentUser) {
    navActions.style.display = 'none';
    return;
  }

  navActions.style.display = 'flex';
  navUserName.textContent = currentUser.fullName || currentUser.username;
  navUserRole.textContent = currentUser.role;
  userAvatar.textContent = (currentUser.fullName || currentUser.username || 'U').charAt(0).toUpperCase();

  if (currentUser.role === 'ADMIN') {
    navUserRole.className = 'role-badge role-admin';
    btnNavAdmin.style.display = 'inline-flex';
    btnNavStudent.style.display = 'inline-flex';
    btnNavHistory.style.display = 'none';
  } else {
    navUserRole.className = 'role-badge role-student';
    btnNavAdmin.style.display = 'none';
    btnNavStudent.style.display = 'inline-flex';
    btnNavHistory.style.display = 'inline-flex';
  }
}

function routeUserView() {
  if (!currentUser) {
    switchView('viewAuth');
    return;
  }
  if (currentUser.role === 'ADMIN') {
    showAdminDashboard();
  } else {
    showStudentDashboard();
  }
}

function setupNavEvents() {
  document.getElementById('btnLogout').addEventListener('click', handleLogout);
  document.getElementById('btnNavStudent').addEventListener('click', showStudentDashboard);
  document.getElementById('btnNavAdmin').addEventListener('click', showAdminDashboard);
  document.getElementById('btnNavHistory').addEventListener('click', showHistoryView);
  document.getElementById('navBrand').addEventListener('click', (e) => {
    e.preventDefault();
    if (activeExamSession) {
      if (confirm('An examination is currently active! Do you want to submit it?')) {
        openSubmitSummaryModal();
      }
    } else {
      routeUserView();
    }
  });
}

// Authentication Handlers
function switchAuthMode(mode) {
  const tabLogin = document.getElementById('tabLoginBtn');
  const tabRegister = document.getElementById('tabRegisterBtn');
  const formLogin = document.getElementById('formLogin');
  const formRegister = document.getElementById('formRegister');

  if (mode === 'login') {
    tabLogin.classList.add('active');
    tabRegister.classList.remove('active');
    formLogin.style.display = 'block';
    formRegister.style.display = 'none';
  } else {
    tabRegister.classList.add('active');
    tabLogin.classList.remove('active');
    formRegister.style.display = 'block';
    formLogin.style.display = 'none';
  }
}

function fillCredentials(username, password) {
  switchAuthMode('login');
  document.getElementById('loginUsername').value = username;
  document.getElementById('loginPassword').value = password;
}

async function handleLogin(e) {
  e.preventDefault();
  const username = document.getElementById('loginUsername').value.trim();
  const password = document.getElementById('loginPassword').value;
  const btn = document.getElementById('btnSubmitLogin');

  try {
    btn.disabled = true;
    btn.textContent = 'Verifying...';
    const res = await apiCall('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });

    currentUser = res;
    localStorage.setItem('examnexus_user', JSON.stringify(currentUser));
    updateNavUser();
    routeUserView();
  } catch (err) {
    alert(err.message || 'Login failed');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Sign In';
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const fullName = document.getElementById('regFullName').value.trim();
  const username = document.getElementById('regUsername').value.trim();
  const email = document.getElementById('regEmail').value.trim();
  const password = document.getElementById('regPassword').value;
  const role = document.getElementById('regRole').value;
  const btn = document.getElementById('btnSubmitRegister');

  try {
    btn.disabled = true;
    btn.textContent = 'Registering...';
    const res = await apiCall('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ fullName, username, email, password, role })
    });

    currentUser = res;
    localStorage.setItem('examnexus_user', JSON.stringify(currentUser));
    updateNavUser();
    routeUserView();
  } catch (err) {
    alert(err.message || 'Registration failed');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Create Account';
  }
}

async function handleLogout() {
  if (activeExamSession) {
    if (!confirm('You have an active exam! Exiting will submit your attempt. Continue?')) {
      return;
    }
    await confirmSubmitExam('USER_CLICK');
  }

  try {
    await apiCall('/api/auth/logout', { method: 'POST' });
  } catch (e) {}

  currentUser = null;
  activeExamSession = null;
  localStorage.removeItem('examnexus_user');
  updateNavUser();
  switchView('viewAuth');
}

// Candidate Dashboard State & Handlers
let loadedExamsList = [];
let activeCategoryFilter = 'ALL';
let currentSearchQuery = '';

async function showStudentDashboard() {
  switchView('viewStudent');
  const greetingElem = document.getElementById('candidateGreetingTitle');
  if (greetingElem && currentUser) {
    const hour = new Date().getHours();
    let timeGreeting = 'Good Morning';
    if (hour >= 12 && hour < 17) timeGreeting = 'Good Afternoon';
    else if (hour >= 17) timeGreeting = 'Good Evening';
    const name = currentUser.fullName || currentUser.username;
    greetingElem.textContent = `${timeGreeting}, ${name} 👋`;
  }
  await loadStudentExams();
}

async function loadStudentExams() {
  const grid = document.getElementById('studentExamsGrid');
  const countBadge = document.getElementById('examCountBadge');
  grid.innerHTML = '<div style="color: var(--text-muted); padding: 2rem;">Loading available certifications...</div>';

  try {
    loadedExamsList = await apiCall('/api/exams');
    renderFilteredExams();
  } catch (err) {
    grid.innerHTML = `<div style="color: #f87171; padding: 2rem;">Error loading examinations: ${escapeHtml(err.message)}</div>`;
  }
}

function filterExamsByCategory(category, btnElement) {
  activeCategoryFilter = category;
  document.querySelectorAll('.category-chip-btn').forEach(btn => btn.classList.remove('active'));
  if (btnElement) btnElement.classList.add('active');
  renderFilteredExams();
}

function handleExamSearch(query) {
  currentSearchQuery = (query || '').toLowerCase().trim();
  renderFilteredExams();
}

function renderFilteredExams() {
  const grid = document.getElementById('studentExamsGrid');
  const countBadge = document.getElementById('examCountBadge');
  if (!grid) return;

  let filtered = loadedExamsList || [];

  if (activeCategoryFilter !== 'ALL') {
    filtered = filtered.filter(e => (e.category || '').toLowerCase().includes(activeCategoryFilter.toLowerCase()));
  }

  if (currentSearchQuery) {
    filtered = filtered.filter(e => 
      (e.title || '').toLowerCase().includes(currentSearchQuery) ||
      (e.description || '').toLowerCase().includes(currentSearchQuery) ||
      (e.category || '').toLowerCase().includes(currentSearchQuery)
    );
  }

  countBadge.textContent = `${filtered.length} of ${loadedExamsList.length} Certifications`;

  if (filtered.length === 0) {
    grid.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; padding: 3.5rem 1rem; color: var(--text-muted);">
        <div style="font-size: 2.5rem; margin-bottom: 0.5rem;">🔍</div>
        <p style="font-size: 1.15rem; font-weight: 700; color: #fff; margin-bottom: 0.35rem;">No matching assessments found</p>
        <p style="font-size: 0.88rem;">Try clearing the search query or selecting 'All Assessments'.</p>
      </div>
    `;
    return;
  }

  grid.innerHTML = filtered.map(exam => `
    <div class="glass-panel exam-card">
      <div>
        <span class="exam-category">📚 ${escapeHtml(exam.category || 'General')}</span>
        <h3>${escapeHtml(exam.title)}</h3>
        <p class="exam-desc">${escapeHtml(exam.description || 'Proctored enterprise assessment.')}</p>
      </div>

      <div>
        <div class="exam-meta">
          <div class="meta-item">⏱️ Duration: <strong>${exam.durationMinutes}m</strong></div>
          <div class="meta-item">🏆 Marks: <strong>${exam.totalMarks}</strong></div>
          <div class="meta-item">🎯 Passing: <strong>${exam.passingMarks}</strong></div>
          ${exam.negativeMarksPerWrong > 0 
            ? `<div class="meta-item" style="color: #f87171;">⚠️ Penalty: <strong>-${exam.negativeMarksPerWrong}</strong></div>` 
            : `<div class="meta-item" style="color: #34d399;">✓ No Penalty</div>`}
        </div>

        <button class="btn btn-primary" style="width: 100%;" onclick="startExam(${exam.id})">
          🚀 Enter Proctored Exam Room →
        </button>
      </div>
    </div>
  `).join('');
}

// Student Attempts History
async function showHistoryView() {
  switchView('viewHistory');
  const tbody = document.getElementById('historyTableBody');
  tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">Loading attempt records...</td></tr>';

  try {
    const attempts = await apiCall('/api/session/my-attempts');
    if (attempts.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">No attempts recorded yet. Take an exam to view results!</td></tr>';
      return;
    }

    tbody.innerHTML = attempts.map(att => `
      <tr>
        <td style="font-weight: 600;">${escapeHtml(att.examTitle)}</td>
        <td style="color: var(--text-muted);">${formatDate(att.startTime)}</td>
        <td style="font-weight: 700;">${att.score} / ${att.totalMarks} (${att.percentage}%)</td>
        <td>
          <span class="role-badge ${att.passed ? 'role-student' : 'role-admin'}">
            ${att.passed ? 'PASSED' : 'FAILED'}
          </span>
        </td>
        <td style="color: ${att.strikes > 0 ? '#f87171' : 'var(--text-muted)'}; font-weight: 600;">
          ${att.strikes} ${att.strikes === 1 ? 'Strike' : 'Strikes'}
        </td>
        <td style="font-size: 0.8rem; color: var(--text-dim);">${escapeHtml(att.status)}</td>
        <td>
          <button class="btn btn-secondary btn-sm" onclick="viewScorecard(${att.attemptId})">View Scorecard</button>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" style="color: #f87171;">Error: ${escapeHtml(err.message)}</td></tr>`;
  }
}

// Live Proctored Exam Engine
async function startExam(examId) {
  if (!confirm('Are you ready to start this proctored examination?\n\n- The timer will begin immediately.\n- Switching tabs or leaving this window triggers anti-cheat strikes.\n- 3 strikes will result in immediate disqualification and auto-submission.')) {
    return;
  }

  try {
    const session = await apiCall(`/api/session/start/${examId}`, { method: 'POST' });
    activeExamSession = session;
    currentQuestionIndex = 0;

    switchView('viewExamRoom');
    document.getElementById('liveExamCategory').textContent = session.category || 'EXAM';
    document.getElementById('liveExamTitle').textContent = session.examTitle;

    updateStrikesUI(session.strikes || 0);
    startCountdownTimer(session.remainingSeconds);
    renderQuestionPalette();
    renderCurrentQuestion();

    // Request fullscreen for optimal proctoring experience
    try {
      if (document.documentElement.requestFullscreen) {
        document.documentElement.requestFullscreen().catch(() => {});
      }
    } catch (e) {}

  } catch (err) {
    alert(err.message || 'Failed to start exam');
  }
}

function startCountdownTimer(seconds) {
  if (examTimerInterval) {
    clearInterval(examTimerInterval);
  }

  let remaining = seconds;
  const timerElem = document.getElementById('liveExamTimer');

  function tick() {
    if (remaining <= 0) {
      clearInterval(examTimerInterval);
      timerElem.textContent = '00:00';
      alert('Time has expired! Submitting your exam automatically...');
      confirmSubmitExam('TIME_EXPIRED');
      return;
    }

    const mins = Math.floor(remaining / 60);
    const secs = remaining % 60;
    timerElem.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

    if (remaining <= 120) {
      timerElem.classList.add('urgent');
    } else {
      timerElem.classList.remove('urgent');
    }

    remaining--;
  }

  tick();
  examTimerInterval = setInterval(tick, 1000);
}

function renderCurrentQuestion() {
  if (!activeExamSession || !activeExamSession.questions) return;
  const q = activeExamSession.questions[currentQuestionIndex];
  const total = activeExamSession.questions.length;

  document.getElementById('currentQBadge').textContent = `Question ${currentQuestionIndex + 1} of ${total}`;
  
  const diffElem = document.getElementById('currentQDiff');
  if (diffElem) {
    const diff = (q.difficulty || 'MEDIUM').toUpperCase();
    diffElem.textContent = diff;
    diffElem.className = `difficulty-pill diff-${diff.toLowerCase()}`;
  }

  const negPenalty = (activeExamSession.negativeMarksPerWrong && activeExamSession.negativeMarksPerWrong > 0)
    ? ` / -${activeExamSession.negativeMarksPerWrong}` : '';
  document.getElementById('currentQMarks').textContent = `Marks: +${q.marks || 5}${negPenalty}`;
  document.getElementById('currentQText').textContent = q.questionText;

  const optionsList = document.getElementById('currentOptionsList');
  const options = [
    { key: 'A', text: q.optionA },
    { key: 'B', text: q.optionB },
    { key: 'C', text: q.optionC },
    { key: 'D', text: q.optionD }
  ];

  optionsList.innerHTML = options.map(opt => `
    <div class="option-item ${q.selectedOption === opt.key ? 'selected' : ''}" onclick="selectOption('${opt.key}')">
      <div class="option-letter">${opt.key}</div>
      <div class="option-content">${escapeHtml(opt.text)}</div>
    </div>
  `).join('');

  // Update button states
  document.getElementById('btnPrevQ').disabled = (currentQuestionIndex === 0);
  const btnNext = document.getElementById('btnNextQ');
  btnNext.textContent = (currentQuestionIndex === total - 1) ? 'Review & Submit Assessment →' : 'Save & Next Question →';

  const btnReview = document.getElementById('btnReviewQ');
  if (q.markedForReview) {
    btnReview.textContent = '✓ Marked for Review';
    btnReview.className = 'btn btn-secondary btn-sm';
  } else {
    btnReview.textContent = '🔖 Mark for Review';
    btnReview.className = 'btn btn-warning btn-sm';
  }

  renderQuestionPalette();
}

async function selectOption(letter) {
  if (!activeExamSession) return;
  const q = activeExamSession.questions[currentQuestionIndex];
  const prev = q.selectedOption;
  q.selectedOption = letter;

  // Render immediately for snappiness
  renderCurrentQuestion();

  if (prev !== letter) {
    showToast(`Question ${currentQuestionIndex + 1}: Selected Option ${letter}`, 'success');
  }

  // Auto-save to backend
  try {
    await apiCall('/api/session/save-answer', {
      method: 'POST',
      body: JSON.stringify({
        attemptId: activeExamSession.attemptId,
        questionId: q.id,
        selectedOption: letter,
        markedForReview: q.markedForReview
      })
    });
  } catch (err) {
    console.warn('Auto-save error:', err);
  }
}

async function clearCurrentSelection() {
  if (!activeExamSession) return;
  const q = activeExamSession.questions[currentQuestionIndex];
  q.selectedOption = null;

  renderCurrentQuestion();

  try {
    await apiCall('/api/session/save-answer', {
      method: 'POST',
      body: JSON.stringify({
        attemptId: activeExamSession.attemptId,
        questionId: q.id,
        selectedOption: null,
        markedForReview: q.markedForReview
      })
    });
  } catch (err) {
    console.warn('Clear error:', err);
  }
}

async function toggleMarkForReview() {
  if (!activeExamSession) return;
  const q = activeExamSession.questions[currentQuestionIndex];
  q.markedForReview = !q.markedForReview;

  renderCurrentQuestion();
  showToast(q.markedForReview ? `🔖 Question ${currentQuestionIndex + 1} marked for review` : `Review tag removed for Question ${currentQuestionIndex + 1}`, 'warning');

  try {
    await apiCall('/api/session/save-answer', {
      method: 'POST',
      body: JSON.stringify({
        attemptId: activeExamSession.attemptId,
        questionId: q.id,
        selectedOption: q.selectedOption,
        markedForReview: q.markedForReview
      })
    });
  } catch (err) {
    console.warn('Review save error:', err);
  }
}

function navigateQuestion(delta) {
  if (!activeExamSession) return;
  const total = activeExamSession.questions.length;
  const nextIndex = currentQuestionIndex + delta;

  if (nextIndex >= total) {
    openSubmitSummaryModal();
    return;
  }

  if (nextIndex >= 0 && nextIndex < total) {
    currentQuestionIndex = nextIndex;
    renderCurrentQuestion();
  }
}

function jumpToQuestion(index) {
  if (!activeExamSession) return;
  currentQuestionIndex = index;
  renderCurrentQuestion();
}

function renderQuestionPalette() {
  if (!activeExamSession || !activeExamSession.questions) return;
  const paletteGrid = document.getElementById('paletteGrid');
  const countProgress = document.getElementById('paletteCountProgress');
  const progBar = document.getElementById('paletteProgressBar');

  let answeredCount = 0;
  const total = activeExamSession.questions.length;

  paletteGrid.innerHTML = activeExamSession.questions.map((q, idx) => {
    let statusClass = '';
    const isAnswered = Boolean(q.selectedOption);
    const isReview = Boolean(q.markedForReview);

    if (isAnswered) answeredCount++;

    if (isAnswered && isReview) {
      statusClass = 'answered-review';
    } else if (isAnswered) {
      statusClass = 'answered';
    } else if (isReview) {
      statusClass = 'review';
    }

    if (idx === currentQuestionIndex) {
      statusClass += ' current';
    }

    return `
      <button class="palette-btn ${statusClass}" onclick="jumpToQuestion(${idx})" title="Question ${idx + 1}">
        ${idx + 1}
      </button>
    `;
  }).join('');

  const pct = total > 0 ? Math.round((answeredCount / total) * 100) : 0;
  if (progBar) progBar.style.width = `${pct}%`;
  countProgress.textContent = `${answeredCount}/${total} Answered (${pct}%)`;
}

// Anti-Cheat Engine (Tab switch & Window blur detection)
function initAntiCheatListeners() {
  document.addEventListener('visibilitychange', () => {
    if (document.hidden && activeExamSession) {
      triggerProctorViolation('TAB_SWITCH');
    }
  });

  window.addEventListener('blur', () => {
    if (activeExamSession) {
      triggerProctorViolation('BLUR');
    }
  });
}

let violationDebounce = false;
async function triggerProctorViolation(type) {
  if (!activeExamSession || violationDebounce) return;
  violationDebounce = true;
  setTimeout(() => { violationDebounce = false; }, 1500);

  playWarningBeep();

  try {
    const res = await apiCall('/api/session/violation', {
      method: 'POST',
      body: JSON.stringify({
        attemptId: activeExamSession.attemptId,
        violationType: type
      })
    });

    activeExamSession.strikes = res.strikes;
    updateStrikesUI(res.strikes);

    const alertBox = document.getElementById('violationAlert');
    const alertText = document.getElementById('violationText');
    alertText.textContent = `Tab switch or window blur detected! Strike ${res.strikes} of ${res.maxStrikes}.`;
    alertBox.style.display = 'block';

    setTimeout(() => {
      alertBox.style.display = 'none';
    }, 4500);

    if (res.disqualified) {
      alert('Maximum anti-cheat strikes exceeded (3/3)! Your examination has been terminated and submitted for evaluation.');
      confirmSubmitExam('CHEATING_VIOLATION');
    }
  } catch (err) {
    console.warn('Violation logging failed:', err);
  }
}

function updateStrikesUI(strikes) {
  document.getElementById('strikeDot1').className = `strike-dot ${strikes >= 1 ? 'active' : ''}`;
  document.getElementById('strikeDot2').className = `strike-dot ${strikes >= 2 ? 'active' : ''}`;
  document.getElementById('strikeDot3').className = `strike-dot ${strikes >= 3 ? 'active' : ''}`;
}

// Exam Submission
function openSubmitSummaryModal() {
  if (!activeExamSession) return;
  const answered = activeExamSession.questions.filter(q => q.selectedOption).length;
  const total = activeExamSession.questions.length;
  const unanswered = total - answered;

  document.getElementById('summaryAnsweredCount').textContent = answered;
  document.getElementById('summaryUnansweredCount').textContent = unanswered;
  document.getElementById('modalSubmitSummary').classList.add('active');
}

function closeSubmitSummaryModal() {
  document.getElementById('modalSubmitSummary').classList.remove('active');
}

async function confirmSubmitExam(reason = 'USER_CLICK') {
  closeSubmitSummaryModal();
  if (examTimerInterval) clearInterval(examTimerInterval);

  if (!activeExamSession) return;
  const attemptId = activeExamSession.attemptId;

  try {
    const result = await apiCall('/api/session/submit', {
      method: 'POST',
      body: JSON.stringify({
        attemptId,
        autoSubmitReason: reason
      })
    });

    activeExamSession = null;
    displayScorecard(result);

    // Exit fullscreen if active
    if (document.fullscreenElement && document.exitFullscreen) {
      document.exitFullscreen().catch(() => {});
    }
  } catch (err) {
    alert('Error submitting exam: ' + err.message);
  }
}

// Scorecard & Results Display
async function viewScorecard(attemptId) {
  try {
    const isAdmin = currentUser && currentUser.role === 'ADMIN';
    const result = await apiCall(`/api/session/result/${attemptId}?admin=${isAdmin}`);
    displayScorecard(result);
  } catch (err) {
    alert(err.message || 'Failed to load scorecard');
  }
}

function displayScorecard(res) {
  switchView('viewScorecard');

  const badge = document.getElementById('scoreStatusBadge');
  if (res.passed) {
    badge.textContent = '🏆 PASSED';
    badge.style.background = 'rgba(16, 185, 129, 0.2)';
    badge.style.color = '#6ee7b7';
    badge.style.border = '1px solid rgba(16, 185, 129, 0.4)';
    showToast('Congratulations! You passed the certification assessment.', 'success');
  } else {
    badge.textContent = '✗ FAILED';
    badge.style.background = 'rgba(239, 68, 68, 0.2)';
    badge.style.color = '#fca5a5';
    badge.style.border = '1px solid rgba(239, 68, 68, 0.4)';
    showToast('Assessment submitted. Check your solution analysis below.', 'warning');
  }

  // Circular SVG Gauge animation
  const percent = res.percentage || 0;
  const radialCircle = document.getElementById('scoreRadialCircle');
  const radialPercent = document.getElementById('scoreRadialPercent');
  if (radialPercent) radialPercent.textContent = `${percent}%`;
  if (radialCircle) {
    const circumference = 263.89; // 2 * pi * 42
    const offset = circumference - (percent / 100) * circumference;
    radialCircle.style.strokeDashoffset = offset;
  }

  document.getElementById('scoreValue').textContent = `${res.score} / ${res.totalMarks}`;
  document.getElementById('scoreExamTitle').textContent = res.examTitle;
  document.getElementById('scoreStudentMeta').textContent =
    `Candidate: ${res.studentFullName || res.studentUsername} | Percentage: ${res.percentage}% | Status: ${res.status}`;

  document.getElementById('resCorrectCount').textContent = res.correctCount;
  document.getElementById('resWrongCount').textContent = res.wrongCount;
  document.getElementById('resUnattemptedCount').textContent = res.unattemptedCount;
  document.getElementById('resStrikes').textContent = res.strikes;

  const breakdownList = document.getElementById('scoreBreakdownList');
  if (!res.questionBreakdowns || res.questionBreakdowns.length === 0) {
    breakdownList.innerHTML = '<p style="color: var(--text-muted);">No question breakdowns available.</p>';
    return;
  }

  breakdownList.innerHTML = res.questionBreakdowns.map((b, idx) => {
    let itemClass = 'unanswered';
    let statusText = '⚪ Unattempted (0 Marks)';
    if (b.isCorrect === true) {
      itemClass = 'correct';
      statusText = `✓ Correct (+${b.marksAwarded} Marks)`;
    } else if (b.selectedOption) {
      itemClass = 'wrong';
      statusText = `✗ Incorrect (${b.marksAwarded} Marks)`;
    }

    return `
      <div class="glass-panel review-item ${itemClass}">
        <div style="display: flex; justify-content: space-between; margin-bottom: 0.5rem;">
          <span style="font-weight: 700; font-size: 0.9rem;">Question ${idx + 1}</span>
          <span style="font-weight: 700; font-size: 0.85rem;">${statusText}</span>
        </div>
        <p style="font-size: 1rem; color: #fff; margin-bottom: 1rem;">${escapeHtml(b.questionText)}</p>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; font-size: 0.85rem; margin-bottom: 0.75rem;">
          <div style="padding: 0.4rem 0.6rem; border-radius: 4px; background: rgba(255,255,255,0.03);">A: ${escapeHtml(b.optionA)}</div>
          <div style="padding: 0.4rem 0.6rem; border-radius: 4px; background: rgba(255,255,255,0.03);">B: ${escapeHtml(b.optionB)}</div>
          <div style="padding: 0.4rem 0.6rem; border-radius: 4px; background: rgba(255,255,255,0.03);">C: ${escapeHtml(b.optionC)}</div>
          <div style="padding: 0.4rem 0.6rem; border-radius: 4px; background: rgba(255,255,255,0.03);">D: ${escapeHtml(b.optionD)}</div>
        </div>

        <div style="font-size: 0.85rem; margin-bottom: 0.5rem;">
          <span>Your Answer: <strong style="color: ${b.isCorrect ? '#10b981' : '#f87171'};">${b.selectedOption || 'None'}</strong></span>
          <span style="margin-left: 1.5rem;">Correct Answer: <strong style="color: #10b981;">Option ${b.correctOption}</strong></span>
        </div>

        ${b.explanation ? `
          <div class="explanation-box">
            💡 <strong>Explanation:</strong> ${escapeHtml(b.explanation)}
          </div>
        ` : ''}
      </div>
    `;
  }).join('');
}

// Admin Console Functions
async function showAdminDashboard() {
  switchView('viewAdmin');
  await loadAdminKpis();
  await loadAdminExams();
  await loadAdminSubmissions();
}

async function loadAdminKpis() {
  try {
    const stats = await apiCall('/api/admin/analytics');
    document.getElementById('adminKpiExams').textContent = stats.totalExams || 0;
    document.getElementById('adminKpiQuestions').textContent = stats.totalQuestions || 0;
    document.getElementById('adminKpiStudents').textContent = stats.totalStudents || 0;
    document.getElementById('adminKpiPassRate').textContent = `${stats.passPercentage || 0}%`;
  } catch (err) {
    console.warn('KPI load error:', err);
  }
}

function switchAdminTab(tab) {
  const btnExams = document.getElementById('tabAdminExamsBtn');
  const btnSubmissions = document.getElementById('tabAdminSubmissionsBtn');
  const contentExams = document.getElementById('adminExamsTabContent');
  const contentSubmissions = document.getElementById('adminSubmissionsTabContent');

  if (tab === 'exams') {
    btnExams.classList.add('active');
    btnSubmissions.classList.remove('active');
    contentExams.style.display = 'block';
    contentSubmissions.style.display = 'none';
    loadAdminExams();
  } else {
    btnSubmissions.classList.add('active');
    btnExams.classList.remove('active');
    contentSubmissions.style.display = 'block';
    contentExams.style.display = 'none';
    loadAdminSubmissions();
  }
}

async function loadAdminExams() {
  const tbody = document.getElementById('adminExamsTableBody');
  tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">Loading exam catalog...</td></tr>';

  try {
    const exams = await apiCall('/api/exams?all=true');
    if (exams.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">No exams found. Click "+ Create New Exam" to build one!</td></tr>';
      return;
    }

    tbody.innerHTML = exams.map(e => `
      <tr>
        <td style="font-weight: 700;">${escapeHtml(e.title)}</td>
        <td><span class="exam-category" style="margin: 0;">${escapeHtml(e.category || 'General')}</span></td>
        <td style="font-weight: 600;">${e.questionCount || 0} Questions</td>
        <td>${e.durationMinutes} mins</td>
        <td>${e.totalMarks} (Pass: ${e.passingMarks})</td>
        <td>
          <span class="role-badge ${e.published ? 'role-student' : 'role-admin'}">
            ${e.published ? 'PUBLISHED' : 'DRAFT'}
          </span>
        </td>
        <td>
          <div style="display: flex; gap: 0.4rem;">
            <button class="btn btn-secondary btn-sm" onclick="openQuestionsModal(${e.id}, '${escapeHtml(e.title)}')">Questions</button>
            <button class="btn btn-outline btn-sm" onclick="editExam(${e.id})">Edit</button>
            <button class="btn btn-danger btn-sm" onclick="deleteExam(${e.id})">Delete</button>
          </div>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" style="color: #f87171;">Error: ${escapeHtml(err.message)}</td></tr>`;
  }
}

async function loadAdminSubmissions() {
  const tbody = document.getElementById('adminSubmissionsTableBody');
  tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">Loading candidate submissions...</td></tr>';

  try {
    const subs = await apiCall('/api/admin/submissions');
    if (subs.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">No candidate submissions recorded yet.</td></tr>';
      return;
    }

    tbody.innerHTML = subs.map(s => `
      <tr>
        <td style="font-weight: 600;">${escapeHtml(s.studentFullName || s.studentUsername)}</td>
        <td>${escapeHtml(s.examTitle)}</td>
        <td style="font-weight: 700;">${s.score} / ${s.totalMarks}</td>
        <td>${s.percentage}%</td>
        <td style="color: ${s.strikes > 0 ? '#f87171' : 'var(--text-muted)'}; font-weight: 600;">
          ${s.strikes} ${s.strikes === 1 ? 'Strike' : 'Strikes'}
        </td>
        <td>
          <span class="role-badge ${s.passed ? 'role-student' : 'role-admin'}">
            ${escapeHtml(s.status)}
          </span>
        </td>
        <td>
          <button class="btn btn-secondary btn-sm" onclick="viewScorecard(${s.attemptId})">Audit Scorecard</button>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" style="color: #f87171;">Error: ${escapeHtml(err.message)}</td></tr>`;
  }
}

// Exam Creation & Editing Modals
function openExamModal(isEdit = false) {
  document.getElementById('modalExam').classList.add('active');
  if (!isEdit) {
    currentEditingExamId = null;
    document.getElementById('modalExamTitle').textContent = 'Create New Examination';
    document.getElementById('formExam').reset();
    document.getElementById('examIdField').value = '';
    document.getElementById('examPublishedInput').checked = true;
  }
}

function closeExamModal() {
  document.getElementById('modalExam').classList.remove('active');
}

async function editExam(examId) {
  try {
    const exam = await apiCall(`/api/exams/${examId}`);
    currentEditingExamId = exam.id;
    openExamModal(true);

    document.getElementById('modalExamTitle').textContent = 'Edit Examination';
    document.getElementById('examIdField').value = exam.id;
    document.getElementById('examTitleInput').value = exam.title;
    document.getElementById('examDescInput').value = exam.description || '';
    document.getElementById('examCategoryInput').value = exam.category || 'General';
    document.getElementById('examDurationInput').value = exam.durationMinutes;
    document.getElementById('examTotalMarksInput').value = exam.totalMarks;
    document.getElementById('examPassMarksInput').value = exam.passingMarks;
    document.getElementById('examNegativeMarksInput').value = exam.negativeMarksPerWrong || 0;
    document.getElementById('examShuffleInput').checked = Boolean(exam.shuffleQuestions);
    document.getElementById('examPublishedInput').checked = Boolean(exam.published);
  } catch (err) {
    alert('Error loading exam: ' + err.message);
  }
}

async function handleSaveExam(e) {
  e.preventDefault();
  const id = document.getElementById('examIdField').value;
  const payload = {
    title: document.getElementById('examTitleInput').value.trim(),
    description: document.getElementById('examDescInput').value.trim(),
    category: document.getElementById('examCategoryInput').value.trim(),
    durationMinutes: parseInt(document.getElementById('examDurationInput').value),
    totalMarks: parseInt(document.getElementById('examTotalMarksInput').value),
    passingMarks: parseInt(document.getElementById('examPassMarksInput').value),
    negativeMarksPerWrong: parseFloat(document.getElementById('examNegativeMarksInput').value) || 0.0,
    shuffleQuestions: document.getElementById('examShuffleInput').checked,
    published: document.getElementById('examPublishedInput').checked
  };

  const btn = document.getElementById('btnSaveExam');
  try {
    btn.disabled = true;
    if (id) {
      await apiCall(`/api/exams/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
    } else {
      await apiCall('/api/exams', { method: 'POST', body: JSON.stringify(payload) });
    }
    closeExamModal();
    loadAdminKpis();
    loadAdminExams();
  } catch (err) {
    alert('Error saving exam: ' + err.message);
  } finally {
    btn.disabled = false;
  }
}

async function deleteExam(id) {
  if (!confirm('Are you sure you want to permanently delete this exam and its questions?')) {
    return;
  }
  try {
    await apiCall(`/api/exams/${id}`, { method: 'DELETE' });
    loadAdminKpis();
    loadAdminExams();
  } catch (err) {
    alert(err.message || 'Failed to delete exam');
  }
}

// Question Bank Modal & Operations
async function openQuestionsModal(examId, examTitle) {
  currentQuestionBankExamId = examId;
  document.getElementById('modalQuestionsExamTitle').textContent = examTitle || 'Manage Questions';
  document.getElementById('modalQuestionsSubtitle').textContent = `Exam ID: ${examId}`;
  document.getElementById('modalQuestions').classList.add('active');
  cancelQuestionForm();
  await loadExamQuestions(examId);
}

function closeQuestionsModal() {
  document.getElementById('modalQuestions').classList.remove('active');
  loadAdminExams();
  loadAdminKpis();
}

async function loadExamQuestions(examId) {
  const tbody = document.getElementById('questionBankTableBody');
  tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted);">Loading questions...</td></tr>';

  try {
    const list = await apiCall(`/api/exams/${examId}/questions`);
    if (list.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-muted);">No questions added yet. Click "+ Add New Question" above.</td></tr>';
      return;
    }

    tbody.innerHTML = list.map((q, idx) => `
      <tr>
        <td>
          <div style="font-weight: 600; font-size: 0.9rem;">${idx + 1}. ${escapeHtml(q.questionText)}</div>
        </td>
        <td style="font-weight: 700; color: #10b981;">Option ${q.correctOption}</td>
        <td>+${q.marks}</td>
        <td><span class="role-badge" style="background: rgba(255,255,255,0.06);">${q.difficulty}</span></td>
        <td>
          <div style="display: flex; gap: 0.35rem;">
            <button class="btn btn-outline btn-sm" onclick="editQuestion(${q.id})">Edit</button>
            <button class="btn btn-danger btn-sm" onclick="deleteQuestion(${q.id})">Del</button>
          </div>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="5" style="color: #f87171;">Error: ${escapeHtml(err.message)}</td></tr>`;
  }
}

function showAddQuestionForm() {
  document.getElementById('formQuestion').reset();
  document.getElementById('questionIdField').value = '';
  document.getElementById('qFormTitle').textContent = 'Add Question';
  document.getElementById('formQuestion').style.display = 'block';
}

function cancelQuestionForm() {
  document.getElementById('formQuestion').reset();
  document.getElementById('formQuestion').style.display = 'none';
}

async function editQuestion(qId) {
  try {
    const list = await apiCall(`/api/exams/${currentQuestionBankExamId}/questions`);
    const q = list.find(item => item.id === qId);
    if (!q) return;

    document.getElementById('qFormTitle').textContent = 'Edit Question';
    document.getElementById('questionIdField').value = q.id;
    document.getElementById('qTextInput').value = q.questionText;
    document.getElementById('qOptA').value = q.optionA;
    document.getElementById('qOptB').value = q.optionB;
    document.getElementById('qOptC').value = q.optionC;
    document.getElementById('qOptD').value = q.optionD;
    document.getElementById('qCorrect').value = q.correctOption;
    document.getElementById('qMarks').value = q.marks;
    document.getElementById('qDifficulty').value = q.difficulty || 'MEDIUM';
    document.getElementById('qExplanation').value = q.explanation || '';
    document.getElementById('formQuestion').style.display = 'block';
  } catch (err) {
    alert('Error loading question: ' + err.message);
  }
}

async function handleSaveQuestion(e) {
  e.preventDefault();
  const qId = document.getElementById('questionIdField').value;
  const payload = {
    examId: currentQuestionBankExamId,
    questionText: document.getElementById('qTextInput').value.trim(),
    optionA: document.getElementById('qOptA').value.trim(),
    optionB: document.getElementById('qOptB').value.trim(),
    optionC: document.getElementById('qOptC').value.trim(),
    optionD: document.getElementById('qOptD').value.trim(),
    correctOption: document.getElementById('qCorrect').value,
    marks: parseInt(document.getElementById('qMarks').value) || 1,
    difficulty: document.getElementById('qDifficulty').value,
    explanation: document.getElementById('qExplanation').value.trim()
  };

  try {
    if (qId) {
      await apiCall(`/api/exams/questions/${qId}`, { method: 'PUT', body: JSON.stringify(payload) });
    } else {
      await apiCall(`/api/exams/${currentQuestionBankExamId}/questions`, { method: 'POST', body: JSON.stringify(payload) });
    }
    cancelQuestionForm();
    await loadExamQuestions(currentQuestionBankExamId);
  } catch (err) {
    alert('Error saving question: ' + err.message);
  }
}

async function deleteQuestion(qId) {
  if (!confirm('Are you sure you want to delete this question?')) return;
  try {
    await apiCall(`/api/exams/questions/${qId}`, { method: 'DELETE' });
    await loadExamQuestions(currentQuestionBankExamId);
  } catch (err) {
    alert(err.message || 'Failed to delete question');
  }
}

// Utilities
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function formatDate(isoStr) {
  if (!isoStr) return '';
  const d = new Date(isoStr);
  return d.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}
