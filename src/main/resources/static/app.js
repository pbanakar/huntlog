/**
 * HuntLog — Single Page Application (SPA)
 * Pure Vanilla JavaScript & Modern CSS
 */

// ==========================================
// --- CONFIG & STATE ---
// ==========================================
const API_BASE = '/api/v1';

let cachedApplications = [];
let cachedAnalytics = null;
let activeEditingAppId = null;
let statusChartInstance = null;

// ==========================================
// --- AUTHENTICATION HELPERS ---
// ==========================================
function getToken() {
    return localStorage.getItem('huntlog_jwt_token');
}

function getUserName() {
    return localStorage.getItem('huntlog_user_name') || 'User';
}

function getUserEmail() {
    return localStorage.getItem('huntlog_user_email') || 'user@example.com';
}

function getUserMemberSince() {
    let stored = localStorage.getItem('huntlog_user_since');
    if (!stored) {
        const token = getToken();
        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                if (payload.iat) {
                    stored = new Date(payload.iat * 1000).toLocaleDateString(undefined, {
                        year: 'numeric',
                        month: 'short',
                        day: 'numeric'
                    });
                }
            } catch (e) {
                // Ignore decoding errors
            }
        }
        if (!stored) {
            stored = new Date().toLocaleDateString(undefined, {
                year: 'numeric',
                month: 'short',
                day: 'numeric'
            });
        }
        localStorage.setItem('huntlog_user_since', stored);
    }
    return stored;
}

function setAuthSession(token, name, email) {
    localStorage.setItem('huntlog_jwt_token', token);
    localStorage.setItem('huntlog_user_name', name || 'User');
    localStorage.setItem('huntlog_user_email', email || 'user@example.com');
    // Save member since timestamp if not yet set
    if (!localStorage.getItem('huntlog_user_since')) {
        localStorage.setItem('huntlog_user_since', new Date().toLocaleDateString(undefined, {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        }));
    }
}

function clearAuthSession() {
    localStorage.removeItem('huntlog_jwt_token');
    localStorage.removeItem('huntlog_user_name');
    localStorage.removeItem('huntlog_user_email');
    localStorage.removeItem('huntlog_user_since');
}

function isLoggedIn() {
    return !!getToken();
}

async function login(email, password) {
    const response = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
    });

    const data = await response.json();
    if (!response.ok) {
        throw new Error(data.message || 'Login failed. Please check your credentials.');
    }

    setAuthSession(data.token, data.name, data.email);
    return data;
}

async function register(name, email, password) {
    const response = await fetch(`${API_BASE}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, email, password })
    });

    const data = await response.json();
    if (!response.ok) {
        throw new Error(data.message || 'Registration failed. Please check your details.');
    }

    setAuthSession(data.token, data.name, data.email);
    return data;
}

function logout() {
    clearAuthSession();
    window.location.hash = '#login';
}

// ==========================================
// --- TOAST NOTIFICATION SYSTEM ---
// ==========================================
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    container.appendChild(toast);

    requestAnimationFrame(() => {
        toast.classList.add('show');
    });

    setTimeout(() => {
        toast.classList.remove('show');
        toast.classList.add('hide');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 350);
    }, 3000);
}

// ==========================================
// --- API CALLS ---
// ==========================================
async function authFetch(endpoint, options = {}) {
    hideNetworkBanner();
    const token = getToken();
    const headers = {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
        ...options.headers
    };

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, {
            ...options,
            headers
        });

        if (response.status === 401) {
            clearAuthSession();
            window.location.hash = '#login';
            throw new Error('Session expired. Please log in again.');
        }

        return response;
    } catch (err) {
        if (err.message && err.message.includes('Session expired')) {
            throw err;
        }
        showNetworkBanner();
        throw new Error('Could not reach server. Is the API running?');
    }
}

async function fetchApplications(statusFilter = null) {
    let url = '/applications?page=0&size=50&sort=lastUpdated,desc';
    if (statusFilter && statusFilter !== 'ALL') {
        url += `&status=${encodeURIComponent(statusFilter)}`;
    }

    const response = await authFetch(url, { method: 'GET' });
    if (!response.ok) {
        const err = await response.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to load applications.');
    }
    const pageData = await response.json();
    return pageData.content || [];
}

async function createApplication(data) {
    const response = await authFetch('/applications', {
        method: 'POST',
        body: JSON.stringify(data)
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(body.message || 'Failed to create application.');
    }
    return body;
}

async function updateApplication(id, data) {
    const response = await authFetch(`/applications/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data)
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(body.message || 'Failed to update application.');
    }
    return body;
}

async function updateApplicationStatus(id, newStatus, notes = null) {
    const payload = { status: newStatus };
    if (notes !== null && notes.trim() !== '') {
        payload.notes = notes.trim();
    }

    return await updateApplication(id, payload);
}

async function deleteApplication(id) {
    const response = await authFetch(`/applications/${id}`, {
        method: 'DELETE'
    });

    if (!response.ok && response.status !== 204) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || 'Failed to delete application.');
    }
    return true;
}

async function fetchAnalytics() {
    const response = await authFetch('/analytics', { method: 'GET' });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || 'Failed to load analytics.');
    }
    return await response.json();
}

async function fetchUserPreferences() {
    const response = await authFetch('/users/me/preferences', { method: 'GET' });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || 'Failed to load user preferences.');
    }
    return await response.json();
}

async function updateUserPreferences(emailRemindersEnabled) {
    const response = await authFetch('/users/me/preferences', {
        method: 'PUT',
        body: JSON.stringify({ emailRemindersEnabled })
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(body.message || 'Failed to update preferences.');
    }
    return body;
}

async function deleteUserAccount() {
    const response = await authFetch('/users/me', {
        method: 'DELETE'
    });

    if (!response.ok && response.status !== 204) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || 'Failed to delete account.');
    }
    return true;
}

// ==========================================
// --- UI UTILITIES ---
// ==========================================
function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function getDaysSince(dateString) {
    if (!dateString) return 0;
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now - date);
    return Math.floor(diffTime / (1000 * 60 * 60 * 24));
}

function updateCharCounter(inputId, counterId, maxLen = 2000) {
    const input = document.getElementById(inputId);
    const counter = document.getElementById(counterId);
    if (!input || !counter) return;
    const len = input.value.length;
    counter.textContent = `${len} / ${maxLen}`;
    if (len >= maxLen) {
        counter.style.color = 'var(--danger)';
    } else {
        counter.style.color = 'var(--text-muted)';
    }
}

async function copyJobUrl(url) {
    if (!url) return;
    try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
            await navigator.clipboard.writeText(url);
        } else {
            const textarea = document.createElement('textarea');
            textarea.value = url;
            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand('copy');
            document.body.removeChild(textarea);
        }
        showToast('✓ Job URL copied to clipboard', 'success');
    } catch (err) {
        showToast('✗ Failed to copy URL', 'error');
    }
}

function renderStatusBadge(status) {
    const s = (status || '').toUpperCase();
    return `<span class="badge badge-${s.toLowerCase()}">${escapeHtml(s)}</span>`;
}

// ==========================================
// --- STATS CARDS RENDERING ---
// ==========================================
function renderStatsCards(analytics) {
    if (!analytics) return;

    const total = analytics.totalApplications || 0;
    const thisWeek = analytics.appliedThisWeek || 0;
    const rate = typeof analytics.responseRate === 'number' ? analytics.responseRate : 0.0;
    const rateFormatted = `${rate.toFixed(1)}%`;
    const oldest = analytics.oldestPendingDays;

    // Applications View Cards
    const cardTotal = document.getElementById('card-total-apps');
    const sideTotal = document.getElementById('side-total-apps');
    if (cardTotal) cardTotal.textContent = total;
    if (sideTotal) sideTotal.textContent = total;

    const cardWeek = document.getElementById('card-this-week');
    const sideWeek = document.getElementById('side-this-week');
    if (cardWeek) cardWeek.textContent = thisWeek;
    if (sideWeek) sideWeek.textContent = thisWeek;

    const cardRate = document.getElementById('card-response-rate');
    const sideRate = document.getElementById('side-response-rate');
    if (cardRate) {
        cardRate.textContent = rateFormatted;
        cardRate.className = 'stat-value ' + (rate > 50 ? 'rate-high' : (rate >= 20 ? 'rate-mid' : 'rate-low'));
    }
    if (sideRate) sideRate.textContent = rateFormatted;

    const cardOldest = document.getElementById('card-oldest-pending');
    if (cardOldest) {
        if (oldest !== null && oldest !== undefined) {
            cardOldest.textContent = `${oldest} d`;
            cardOldest.className = 'stat-value ' + (oldest > 14 ? 'oldest-alert' : '');
        } else {
            cardOldest.textContent = 'None';
            cardOldest.className = 'stat-value';
        }
    }

    // Analytics View Cards
    const anTotal = document.getElementById('analytics-total-apps');
    const anWeek = document.getElementById('analytics-this-week');
    const anRate = document.getElementById('analytics-response-rate');
    const anOldest = document.getElementById('analytics-oldest-pending');

    if (anTotal) anTotal.textContent = total;
    if (anWeek) anWeek.textContent = thisWeek;
    if (anRate) {
        anRate.textContent = rateFormatted;
        anRate.className = 'stat-value ' + (rate > 50 ? 'rate-high' : (rate >= 20 ? 'rate-mid' : 'rate-low'));
    }
    if (anOldest) {
        if (oldest !== null && oldest !== undefined) {
            anOldest.textContent = `${oldest} d`;
            anOldest.className = 'stat-value ' + (oldest > 14 ? 'oldest-alert' : '');
        } else {
            anOldest.textContent = 'None';
            anOldest.className = 'stat-value';
        }
    }
}

// ==========================================
// --- APPLICATIONS TABLE RENDERING ---
// ==========================================
function renderApplicationsTable(applications) {
    const tbody = document.getElementById('applications-tbody');
    const emptyState = document.getElementById('empty-state');
    const countBadge = document.getElementById('filtered-count-badge');
    if (!tbody) return;

    tbody.innerHTML = '';
    const count = applications ? applications.length : 0;
    if (countBadge) {
        countBadge.textContent = `${count} application${count === 1 ? '' : 's'}`;
    }

    if (!applications || applications.length === 0) {
        if (emptyState) emptyState.classList.remove('hidden');
        return;
    }

    if (emptyState) emptyState.classList.add('hidden');

    applications.forEach(app => {
        const tr = document.createElement('tr');
        
        const company = escapeHtml(app.company || '');
        const role = escapeHtml(app.role || '');
        const location = escapeHtml(app.location || '—');
        const appliedDate = app.appliedDate ? escapeHtml(app.appliedDate) : '—';
        
        // Notes column with truncation and tooltip
        let notesHtml = '<span class="link-empty">—</span>';
        if (app.notes && app.notes.trim()) {
            const rawNotes = app.notes.trim();
            const previewText = rawNotes.length > 30 ? escapeHtml(rawNotes.substring(0, 30)) + '…' : escapeHtml(rawNotes);
            notesHtml = `<span class="table-notes-preview" title="${escapeHtml(rawNotes)}">${previewText}</span>`;
        }

        // Job URL link column
        let jobUrlHtml = '<span class="link-empty">—</span>';
        if (app.jobUrl && app.jobUrl.trim()) {
            jobUrlHtml = `<a href="${escapeHtml(app.jobUrl)}" target="_blank" rel="noopener noreferrer" class="link-job-view" title="${escapeHtml(app.jobUrl)}">View ↗</a>`;
        }

        // Days Since Update column
        const days = getDaysSince(app.lastUpdated);
        const daysClass = days > 14 ? 'days-stale' : 'days-normal';
        const daysText = days === 0 ? 'Today' : `${days}d ago`;
        const daysHtml = `<span class="${daysClass}" title="Last updated: ${escapeHtml(app.lastUpdated || '')}">${daysText}</span>`;

        // Action Buttons
        const hasNext = app.allowedNextStatuses && app.allowedNextStatuses.length > 0;
        const updateBtnClass = hasNext ? 'btn btn-sm btn-outline' : 'btn btn-sm btn-disabled';
        const updateBtnTitle = hasNext ? 'Update Status' : 'Terminal status (No further transitions)';

        const copyBtnHtml = app.jobUrl && app.jobUrl.trim()
            ? `<button type="button" class="btn btn-sm btn-outline" title="Copy Job URL" onclick="copyJobUrl('${escapeHtml(app.jobUrl.replace(/'/g, "\\'"))}')">Copy URL</button>`
            : '';

        tr.innerHTML = `
            <td><strong>${company}</strong></td>
            <td>${role}</td>
            <td>${renderStatusBadge(app.status)}</td>
            <td>${appliedDate}</td>
            <td>${location}</td>
            <td>${notesHtml}</td>
            <td>${jobUrlHtml}</td>
            <td>${daysHtml}</td>
            <td class="actions-col">
                <div class="actions-cell">
                    <button type="button" class="${updateBtnClass}" title="${updateBtnTitle}" onclick="openUpdateModalById(${app.id})">
                        Status
                    </button>
                    <button type="button" class="btn btn-sm btn-outline" title="Edit Application" onclick="openEditModalById(${app.id})">
                        Edit
                    </button>
                    ${copyBtnHtml}
                    <button type="button" class="btn btn-sm btn-danger-outline" title="Delete application" onclick="confirmDeleteApplication(${app.id}, '${escapeHtml(app.company.replace(/'/g, "\\'"))}')">
                        Delete
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// ==========================================
// --- CLIENT-SIDE SEARCH & FILTER ---
// ==========================================
function applyClientFilters() {
    const searchInput = document.getElementById('search-input');
    const statusSelect = document.getElementById('status-filter');
    const fromInput = document.getElementById('filter-date-from');
    const toInput = document.getElementById('filter-date-to');

    const query = searchInput ? searchInput.value.trim().toLowerCase() : '';
    const status = statusSelect ? statusSelect.value : 'ALL';
    const fromDate = fromInput ? fromInput.value : '';
    const toDate = toInput ? toInput.value : '';

    let filtered = cachedApplications;

    if (status !== 'ALL') {
        filtered = filtered.filter(app => (app.status || '').toUpperCase() === status);
    }

    if (query !== '') {
        filtered = filtered.filter(app => {
            const company = (app.company || '').toLowerCase();
            const role = (app.role || '').toLowerCase();
            const location = (app.location || '').toLowerCase();
            const notes = (app.notes || '').toLowerCase();
            return company.includes(query) || role.includes(query) || location.includes(query) || notes.includes(query);
        });
    }

    if (fromDate) {
        filtered = filtered.filter(app => app.appliedDate && app.appliedDate >= fromDate);
    }

    if (toDate) {
        filtered = filtered.filter(app => app.appliedDate && app.appliedDate <= toDate);
    }

    renderApplicationsTable(filtered);
}

function handleClientSearch() {
    applyClientFilters();
}

function handleStatusFilterChange() {
    applyClientFilters();
}

function clearDateFilter() {
    const fromInput = document.getElementById('filter-date-from');
    const toInput = document.getElementById('filter-date-to');
    if (fromInput) fromInput.value = '';
    if (toInput) toInput.value = '';
    applyClientFilters();
}

// ==========================================
// --- DATA LOADING ---
// ==========================================
async function loadApplicationsData() {
    const tableLoading = document.getElementById('table-loading');
    if (tableLoading) tableLoading.classList.remove('hidden');

    try {
        const [apps, analytics] = await Promise.all([
            fetchApplications(),
            fetchAnalytics().catch(err => {
                console.warn('Could not fetch analytics:', err);
                return null;
            })
        ]);

        cachedApplications = apps;
        cachedAnalytics = analytics;

        if (analytics) {
            renderStatsCards(analytics);
        }

        applyClientFilters();
    } catch (err) {
        console.error('Failed to load applications:', err);
        showToast('✗ ' + err.message, 'error');
    } finally {
        if (tableLoading) tableLoading.classList.add('hidden');
    }
}

async function loadAnalyticsPage() {
    const loadingEl = document.getElementById('analytics-loading');
    if (loadingEl) loadingEl.classList.remove('hidden');

    try {
        const analytics = await fetchAnalytics();
        cachedAnalytics = analytics;
        renderStatsCards(analytics);
        renderAnalyticsChart(analytics);
        renderTopCompaniesTable(analytics);
    } catch (err) {
        console.error('Failed to load analytics:', err);
        showToast('✗ ' + err.message, 'error');
    } finally {
        if (loadingEl) loadingEl.classList.add('hidden');
    }
}

async function loadSettingsPage() {
    // Populate user details
    const nameEl = document.getElementById('settings-user-name');
    const emailEl = document.getElementById('settings-user-email');
    const sinceEl = document.getElementById('settings-user-since');
    const toggleEl = document.getElementById('toggle-email-reminders');

    if (nameEl) nameEl.textContent = getUserName();
    if (emailEl) emailEl.textContent = getUserEmail();
    if (sinceEl) sinceEl.textContent = getUserMemberSince();

    try {
        const prefs = await fetchUserPreferences();
        if (toggleEl) {
            toggleEl.checked = Boolean(prefs.emailRemindersEnabled);
        }
    } catch (err) {
        console.warn('Failed to load preferences:', err);
        showToast('✗ ' + err.message, 'error');
    }
}

// ==========================================
// --- ANALYTICS CHART & TOP COMPANIES ---
// ==========================================
function renderAnalyticsChart(analytics) {
    const canvas = document.getElementById('statusChart');
    if (!canvas || typeof Chart === 'undefined') return;

    const byStatus = analytics.byStatus || {};
    const statuses = ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'ACCEPTED', 'REJECTED', 'WITHDRAWN'];
    const counts = statuses.map(s => byStatus[s] || 0);

    const statusColors = [
        '#3b82f6', // APPLIED (blue)
        '#8b5cf6', // SCREENING (purple)
        '#f97316', // INTERVIEW (orange)
        '#22c55e', // OFFER (green)
        '#0d9488', // ACCEPTED (teal)
        '#ef4444', // REJECTED (red)
        '#64748b'  // WITHDRAWN (slate)
    ];

    if (statusChartInstance) {
        statusChartInstance.destroy();
    }

    const ctx = canvas.getContext('2d');
    statusChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: statuses,
            datasets: [{
                label: 'Applications',
                data: counts,
                backgroundColor: statusColors,
                borderRadius: 6,
                borderSkipped: false
            }]
        },
        options: {
            indexAxis: 'y',
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: (ctx) => ` ${ctx.raw} application${ctx.raw === 1 ? '' : 's'}`
                    }
                }
            },
            scales: {
                x: {
                    beginAtZero: true,
                    ticks: { precision: 0, font: { family: 'inherit' } },
                    grid: { color: '#f1f5f9' }
                },
                y: {
                    ticks: { font: { family: 'inherit', weight: 'bold' } },
                    grid: { display: false }
                }
            }
        }
    });
}

function renderTopCompaniesTable(analytics) {
    const tbody = document.getElementById('top-companies-tbody');
    const emptyEl = document.getElementById('analytics-empty-companies');
    if (!tbody) return;

    tbody.innerHTML = '';
    const companies = analytics.topCompaniesByApplications || [];

    if (companies.length === 0) {
        if (emptyEl) emptyEl.classList.remove('hidden');
        return;
    }

    if (emptyEl) emptyEl.classList.add('hidden');

    companies.forEach(item => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${escapeHtml(item.company)}</strong></td>
            <td>${item.count}</td>
        `;
        tbody.appendChild(tr);
    });
}

// ==========================================
// --- SETTINGS ACTIONS ---
// ==========================================
async function handleToggleEmailReminders(e) {
    const checked = e.target.checked;
    try {
        await updateUserPreferences(checked);
        showToast('✓ Preferences updated', 'success');
    } catch (err) {
        e.target.checked = !checked; // Revert switch state on error
        showToast('✗ ' + err.message, 'error');
    }
}

async function handleDeleteAccount() {
    const ok = window.confirm('Are you sure? This will permanently delete your account and all applications.');
    if (!ok) return;

    try {
        await deleteUserAccount();
        showToast('✓ Account permanently deleted', 'info');
        clearAuthSession();
        window.location.hash = '#login';
    } catch (err) {
        showToast('✗ ' + err.message, 'error');
    }
}

// ==========================================
// --- MODAL CONTROLS ---
// ==========================================
function openAddModal() {
    const form = document.getElementById('add-app-form');
    const errorEl = document.getElementById('add-app-error');
    const dateInput = document.getElementById('add-applied-date');
    if (form) form.reset();
    if (errorEl) {
        errorEl.textContent = '';
        errorEl.classList.add('hidden');
    }
    if (dateInput) {
        dateInput.value = new Date().toISOString().split('T')[0];
    }
    updateCharCounter('add-notes', 'add-notes-counter');
    const modal = document.getElementById('add-modal');
    if (modal) modal.classList.remove('hidden');
}

function openEditModalById(id) {
    activeEditingAppId = id;
    const app = cachedApplications.find(a => a.id === id);
    if (!app) return;

    const modal = document.getElementById('edit-modal');
    const compInput = document.getElementById('edit-company');
    const roleInput = document.getElementById('edit-role');
    const locInput = document.getElementById('edit-location');
    const urlInput = document.getElementById('edit-job-url');
    const notesInput = document.getElementById('edit-notes');
    const errorEl = document.getElementById('edit-app-error');

    if (errorEl) {
        errorEl.textContent = '';
        errorEl.classList.add('hidden');
    }

    if (compInput) compInput.value = app.company || '';
    if (roleInput) roleInput.value = app.role || '';
    if (locInput) locInput.value = app.location || '';
    if (urlInput) urlInput.value = app.jobUrl || '';
    if (notesInput) notesInput.value = app.notes || '';

    updateCharCounter('edit-notes', 'edit-notes-counter');

    if (modal) modal.classList.remove('hidden');
}

function openUpdateModalById(id) {
    const app = cachedApplications.find(a => a.id === id);
    if (!app) return;

    activeEditingAppId = app.id;
    const modal = document.getElementById('update-modal');
    const select = document.getElementById('update-status-select');
    const selectGroup = document.getElementById('update-select-group');
    const noTransitions = document.getElementById('update-no-transitions');
    const submitBtn = document.getElementById('update-submit-btn');
    const errorEl = document.getElementById('update-app-error');
    const badgeWrap = document.getElementById('update-current-badge-wrap');
    const targetComp = document.getElementById('update-target-company');
    const targetRole = document.getElementById('update-target-role');
    const notesInput = document.getElementById('update-notes');
    const daysInfo = document.getElementById('update-days-ago-info');
    const staleWarning = document.getElementById('update-stale-warning');

    if (errorEl) {
        errorEl.textContent = '';
        errorEl.classList.add('hidden');
    }
    if (targetComp) targetComp.textContent = app.company || 'Application';
    if (targetRole) targetRole.textContent = app.role || '';
    if (badgeWrap) badgeWrap.innerHTML = renderStatusBadge(app.status);
    if (notesInput) notesInput.value = app.notes || '';

    // Calculate days since last update
    const days = getDaysSince(app.lastUpdated);
    if (daysInfo) {
        daysInfo.textContent = days === 0 ? 'Last updated today' : `Last updated ${days} day${days === 1 ? '' : 's'} ago`;
    }

    if (staleWarning) {
        if (days > 14) {
            staleWarning.textContent = `⚠ This application hasn't been updated in ${days} days`;
            staleWarning.classList.remove('hidden');
        } else {
            staleWarning.classList.add('hidden');
        }
    }

    updateCharCounter('update-notes', 'update-notes-counter');

    if (select) {
        select.innerHTML = '';
        if (app.allowedNextStatuses && app.allowedNextStatuses.length > 0) {
            app.allowedNextStatuses.forEach(st => {
                const opt = document.createElement('option');
                opt.value = st;
                opt.textContent = st;
                select.appendChild(opt);
            });
            if (selectGroup) selectGroup.classList.remove('hidden');
            if (noTransitions) noTransitions.classList.add('hidden');
            if (submitBtn) submitBtn.disabled = false;
        } else {
            if (selectGroup) selectGroup.classList.add('hidden');
            if (noTransitions) noTransitions.classList.remove('hidden');
            if (submitBtn) submitBtn.disabled = true;
        }
    }

    if (modal) modal.classList.remove('hidden');
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('hidden');
    activeEditingAppId = null;
}

function handleModalBackdropClick(event, modalId) {
    if (event.target && event.target.id === modalId) {
        closeModal(modalId);
    }
}

// ==========================================
// --- FORM SUBMIT HANDLERS ---
// ==========================================
async function handleAddApplication(e) {
    e.preventDefault();
    const company = document.getElementById('add-company').value.trim();
    const role = document.getElementById('add-role').value.trim();
    const appliedDate = document.getElementById('add-applied-date').value || null;
    const location = document.getElementById('add-location').value.trim() || null;
    const jobUrl = document.getElementById('add-job-url').value.trim() || null;
    const notes = document.getElementById('add-notes').value.trim() || null;

    const errorEl = document.getElementById('add-app-error');
    const btn = document.getElementById('add-submit-btn');

    if (errorEl) {
        errorEl.textContent = '';
        errorEl.classList.add('hidden');
    }
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Creating...';
    }

    try {
        await createApplication({ company, role, appliedDate, location, jobUrl, notes });
        closeModal('add-modal');
        showToast('✓ Application added successfully', 'success');
        await loadApplicationsData();
    } catch (err) {
        if (errorEl) {
            errorEl.textContent = err.message;
            errorEl.classList.remove('hidden');
        }
        showToast('✗ ' + err.message, 'error');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Create Application';
        }
    }
}

async function handleEditApplication(e) {
    e.preventDefault();
    if (!activeEditingAppId) return;

    const company = document.getElementById('edit-company').value.trim();
    const role = document.getElementById('edit-role').value.trim();
    const location = document.getElementById('edit-location').value.trim() || null;
    const jobUrl = document.getElementById('edit-job-url').value.trim() || null;
    const notes = document.getElementById('edit-notes').value.trim() || null;

    const errorEl = document.getElementById('edit-app-error');
    const btn = document.getElementById('edit-submit-btn');

    if (errorEl) {
        errorEl.textContent = '';
        errorEl.classList.add('hidden');
    }
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Saving...';
    }

    try {
        await updateApplication(activeEditingAppId, { company, role, location, jobUrl, notes });
        closeModal('edit-modal');
        showToast('✓ Application updated successfully', 'success');
        await loadApplicationsData();
    } catch (err) {
        if (errorEl) {
            errorEl.textContent = err.message;
            errorEl.classList.remove('hidden');
        }
        showToast('✗ ' + err.message, 'error');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Save Changes';
        }
    }
}

async function handleUpdateStatusSubmit(e) {
    e.preventDefault();
    if (!activeEditingAppId) return;

    const select = document.getElementById('update-status-select');
    const notesInput = document.getElementById('update-notes');
    const errorEl = document.getElementById('update-app-error');
    const btn = document.getElementById('update-submit-btn');

    const newStatus = select ? select.value : null;
    const notes = notesInput ? notesInput.value : null;

    if (!newStatus) return;

    if (errorEl) {
        errorEl.textContent = '';
        errorEl.classList.add('hidden');
    }
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Updating...';
    }

    try {
        await updateApplicationStatus(activeEditingAppId, newStatus, notes);
        closeModal('update-modal');
        showToast(`✓ Status updated to ${newStatus}`, 'success');
        await loadApplicationsData();
    } catch (err) {
        if (errorEl) {
            errorEl.textContent = err.message;
            errorEl.classList.remove('hidden');
        }
        showToast('✗ ' + err.message, 'error');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Save Changes';
        }
    }
}

async function confirmDeleteApplication(id, company) {
    const ok = window.confirm(`Are you sure you want to delete application for "${company}"? This action cannot be undone.`);
    if (!ok) return;

    try {
        await deleteApplication(id);
        showToast('✓ Application deleted', 'success');
        await loadApplicationsData();
    } catch (err) {
        showToast('✗ ' + err.message, 'error');
    }
}

// ==========================================
// --- AUTH FORMS ---
// ==========================================
function switchAuthTab(tab) {
    const tabLogin = document.getElementById('tab-login');
    const tabRegister = document.getElementById('tab-register');
    const loginForm = document.getElementById('login-form');
    const regForm = document.getElementById('register-form');
    const loginErr = document.getElementById('login-error');
    const regErr = document.getElementById('register-error');

    if (loginErr) loginErr.classList.add('hidden');
    if (regErr) regErr.classList.add('hidden');

    if (tab === 'login') {
        if (tabLogin) tabLogin.classList.add('active');
        if (tabRegister) tabRegister.classList.remove('active');
        if (loginForm) loginForm.classList.remove('hidden');
        if (regForm) regForm.classList.add('hidden');
    } else {
        if (tabRegister) tabRegister.classList.add('active');
        if (tabLogin) tabLogin.classList.remove('active');
        if (regForm) regForm.classList.remove('hidden');
        if (loginForm) loginForm.classList.add('hidden');
    }
}

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;
    const errorEl = document.getElementById('login-error');
    const btn = document.getElementById('login-submit-btn');

    if (errorEl) {
        errorEl.textContent = '';
        errorEl.classList.add('hidden');
    }
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Signing in...';
    }

    try {
        await login(email, password);
        window.location.hash = '#dashboard';
    } catch (err) {
        if (errorEl) {
            errorEl.textContent = err.message;
            errorEl.classList.remove('hidden');
        }
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Sign In';
        }
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const name = document.getElementById('reg-name').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const password = document.getElementById('reg-password').value;
    const errorEl = document.getElementById('register-error');
    const btn = document.getElementById('register-submit-btn');

    if (errorEl) {
        errorEl.textContent = '';
        errorEl.classList.add('hidden');
    }
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Creating account...';
    }

    try {
        await register(name, email, password);
        window.location.hash = '#dashboard';
    } catch (err) {
        if (errorEl) {
            errorEl.textContent = err.message;
            errorEl.classList.remove('hidden');
        }
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Create Account';
        }
    }
}

// ==========================================
// --- MOBILE SIDEBAR DRAWER ---
// ==========================================
function toggleSidebarMobile() {
    const sidebar = document.getElementById('app-sidebar');
    const overlay = document.getElementById('sidebar-overlay');
    if (!sidebar) return;

    if (sidebar.classList.contains('open')) {
        closeSidebarMobile();
    } else {
        sidebar.classList.add('open');
        if (overlay) overlay.classList.remove('hidden');
    }
}

function closeSidebarMobile() {
    const sidebar = document.getElementById('app-sidebar');
    const overlay = document.getElementById('sidebar-overlay');
    if (sidebar) sidebar.classList.remove('open');
    if (overlay) overlay.classList.add('hidden');
}

function handleNavClick(viewName) {
    closeSidebarMobile();
    window.location.hash = '#' + viewName;
}

// ==========================================
// --- ROUTING & VIEW SWITCHING ---
// ==========================================
function refreshCurrentView() {
    const hash = (window.location.hash || '#dashboard').replace('#', '');
    if (hash === 'analytics') {
        loadAnalyticsPage();
    } else if (hash === 'settings') {
        loadSettingsPage();
    } else {
        loadApplicationsData();
    }
}

function updateNavActive(activeId) {
    const navItems = document.querySelectorAll('.nav-menu .nav-item');
    navItems.forEach(item => {
        if (item.id === activeId) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });
}

function route() {
    const rawHash = window.location.hash || '#dashboard';
    const hash = rawHash.replace('#', '');
    const authView = document.getElementById('auth-view');
    const dashboardContainer = document.getElementById('dashboard-view');

    if (!isLoggedIn()) {
        if (authView) authView.classList.remove('hidden');
        if (dashboardContainer) dashboardContainer.classList.add('hidden');
        if (hash !== 'login' && hash !== 'register') {
            window.location.hash = '#login';
        } else if (hash === 'register') {
            switchAuthTab('register');
        } else {
            switchAuthTab('login');
        }
        return;
    }

    // Authenticated
    if (hash === 'login' || hash === 'register') {
        window.location.hash = '#dashboard';
        return;
    }

    if (authView) authView.classList.add('hidden');
    if (dashboardContainer) dashboardContainer.classList.remove('hidden');

    // Update user profile in sidebar
    const nameEl = document.getElementById('sidebar-user-name');
    const emailEl = document.getElementById('sidebar-user-email');
    const initialEl = document.getElementById('sidebar-user-initial');
    const name = getUserName();
    const email = getUserEmail();
    if (nameEl) nameEl.textContent = name;
    if (emailEl) emailEl.textContent = email;
    if (initialEl) initialEl.textContent = (name || 'U').charAt(0).toUpperCase();

    // View Switching
    const viewApps = document.getElementById('view-applications');
    const viewAnalytics = document.getElementById('view-analytics');
    const viewSettings = document.getElementById('view-settings');
    const pageTitle = document.getElementById('page-title');
    const pageSubtitle = document.getElementById('page-subtitle');
    const topBarActions = document.getElementById('top-bar-actions');
    const topAddBtn = document.getElementById('btn-top-add');

    if (hash === 'analytics') {
        updateNavActive('nav-analytics');
        if (viewApps) viewApps.classList.add('hidden');
        if (viewAnalytics) viewAnalytics.classList.remove('hidden');
        if (viewSettings) viewSettings.classList.add('hidden');

        if (pageTitle) pageTitle.textContent = 'Analytics';
        if (pageSubtitle) pageSubtitle.textContent = 'Pipeline performance and conversion metrics.';
        if (topAddBtn) topAddBtn.classList.remove('hidden');

        loadAnalyticsPage();
    } else if (hash === 'settings') {
        updateNavActive('nav-settings');
        if (viewApps) viewApps.classList.add('hidden');
        if (viewAnalytics) viewAnalytics.classList.add('hidden');
        if (viewSettings) viewSettings.classList.remove('hidden');

        if (pageTitle) pageTitle.textContent = 'Settings';
        if (pageSubtitle) pageSubtitle.textContent = 'Manage email notifications and account security.';
        if (topAddBtn) topAddBtn.classList.add('hidden');

        loadSettingsPage();
    } else {
        // Default to applications view
        updateNavActive('nav-applications');
        if (viewApps) viewApps.classList.remove('hidden');
        if (viewAnalytics) viewAnalytics.classList.add('hidden');
        if (viewSettings) viewSettings.classList.add('hidden');

        if (pageTitle) pageTitle.textContent = 'Applications Dashboard';
        if (pageSubtitle) pageSubtitle.textContent = 'Track, update, and manage your active hiring pipeline.';
        if (topAddBtn) topAddBtn.classList.remove('hidden');

        loadApplicationsData();
    }
}

function showNetworkBanner() {
    const banner = document.getElementById('network-banner');
    if (banner) banner.classList.remove('hidden');
}

function hideNetworkBanner() {
    const banner = document.getElementById('network-banner');
    if (banner) banner.classList.add('hidden');
}

// ==========================================
// --- INITIALIZATION ---
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    const btnCloseBanner = document.getElementById('btn-close-banner');
    if (btnCloseBanner) {
        btnCloseBanner.addEventListener('click', hideNetworkBanner);
    }
    route();
});

window.addEventListener('hashchange', route);
