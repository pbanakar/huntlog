/**
 * HuntLog — Vanilla JS Single Page Application (SPA)
 * 
 * Note on Authentication Storage:
 * JWT tokens are stored in browser localStorage for simplicity in this single-user / personal tool.
 * In higher-security production systems, storing tokens in secure, httpOnly cookies is preferred
 * to mitigate Cross-Site Scripting (XSS) risks.
 */

// ==========================================
// --- CONFIG ---
// ==========================================
const API_BASE = '/api/v1';

// Global cache for client-side search & filtering
let cachedApplications = [];
let activeEditingAppId = null;

// ==========================================
// --- AUTH ---
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

function setAuthSession(token, name, email) {
    localStorage.setItem('huntlog_jwt_token', token);
    localStorage.setItem('huntlog_user_name', name || 'User');
    localStorage.setItem('huntlog_user_email', email || 'user@example.com');
}

function clearAuthSession() {
    localStorage.removeItem('huntlog_jwt_token');
    localStorage.removeItem('huntlog_user_name');
    localStorage.removeItem('huntlog_user_email');
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

async function updateApplicationStatus(id, newStatus, notes = null) {
    const payload = { status: newStatus };
    if (notes !== null && notes.trim() !== '') {
        payload.notes = notes.trim();
    }

    const response = await authFetch(`/applications/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(body.message || `Failed to update status to ${newStatus}.`);
    }
    return body;
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

// ==========================================
// --- RENDER ---
// ==========================================
function renderStatusBadge(status) {
    const s = (status || '').toUpperCase();
    return `<span class="badge badge-${s.toLowerCase()}">${escapeHtml(s)}</span>`;
}

function renderStatsCards(analytics) {
    if (!analytics) return;

    // 1. Total Apps
    const total = analytics.totalApplications || 0;
    const cardTotal = document.getElementById('card-total-apps');
    const sideTotal = document.getElementById('side-total-apps');
    if (cardTotal) cardTotal.textContent = total;
    if (sideTotal) sideTotal.textContent = total;

    // 2. Applied This Week
    const thisWeek = analytics.appliedThisWeek || 0;
    const cardWeek = document.getElementById('card-this-week');
    const sideWeek = document.getElementById('side-this-week');
    if (cardWeek) cardWeek.textContent = thisWeek;
    if (sideWeek) sideWeek.textContent = thisWeek;

    // 3. Response Rate %
    const rate = typeof analytics.responseRate === 'number' ? analytics.responseRate : 0.0;
    const rateFormatted = `${rate.toFixed(1)}%`;
    const cardRate = document.getElementById('card-response-rate');
    const sideRate = document.getElementById('side-response-rate');
    if (cardRate) {
        cardRate.textContent = rateFormatted;
        cardRate.className = 'stat-value ' + (rate > 50 ? 'rate-high' : (rate >= 20 ? 'rate-mid' : 'rate-low'));
    }
    if (sideRate) sideRate.textContent = rateFormatted;

    // 4. Oldest Pending
    const oldest = analytics.oldestPendingDays;
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
}

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
        
        // Escape content
        const company = escapeHtml(app.company || '');
        const role = escapeHtml(app.role || '');
        const location = escapeHtml(app.location || '—');
        const appliedDate = app.appliedDate ? escapeHtml(app.appliedDate) : '—';
        const jobUrl = app.jobUrl ? `<a href="${escapeHtml(app.jobUrl)}" target="_blank" rel="noopener noreferrer" class="link-job-url" title="${escapeHtml(app.jobUrl)}">🔗</a>` : '';

        // Check if application has allowed next transitions
        const hasNext = app.allowedNextStatuses && app.allowedNextStatuses.length > 0;
        const updateBtnClass = hasNext ? 'btn btn-sm btn-outline' : 'btn btn-sm btn-disabled';
        const updateBtnTitle = hasNext ? 'Update Status' : 'Terminal status (No further transitions)';

        tr.innerHTML = `
            <td class="company-cell">
                <strong>${company}</strong> ${jobUrl}
                ${app.notes ? `<div class="table-notes" title="${escapeHtml(app.notes)}">${escapeHtml(app.notes)}</div>` : ''}
            </td>
            <td>${role}</td>
            <td>${renderStatusBadge(app.status)}</td>
            <td>${appliedDate}</td>
            <td>${location}</td>
            <td class="actions-col">
                <button type="button" class="${updateBtnClass}" title="${updateBtnTitle}" onclick="openUpdateModalById(${app.id})">
                    Status
                </button>
                <button type="button" class="btn btn-sm btn-danger-outline" title="Delete application" onclick="confirmDeleteApplication(${app.id}, '${escapeHtml(app.company.replace(/'/g, "\\'"))}')">
                    Delete
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function openUpdateModal(applicationId, allowedNextStatuses, currentStatus, company, role, notes) {
    activeEditingAppId = applicationId;
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

    if (errorEl) {
        errorEl.textContent = '';
        errorEl.classList.add('hidden');
    }
    if (targetComp) targetComp.textContent = company || 'Application';
    if (targetRole) targetRole.textContent = role || '';
    if (badgeWrap) badgeWrap.innerHTML = renderStatusBadge(currentStatus);
    if (notesInput) notesInput.value = notes || '';

    if (select) {
        select.innerHTML = '';
        if (allowedNextStatuses && allowedNextStatuses.length > 0) {
            allowedNextStatuses.forEach(st => {
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

function openUpdateModalById(id) {
    const app = cachedApplications.find(a => a.id === id);
    if (!app) return;
    openUpdateModal(app.id, app.allowedNextStatuses, app.status, app.company, app.role, app.notes);
}

async function renderDashboard() {
    // Update user profile info in sidebar
    const nameEl = document.getElementById('sidebar-user-name');
    const emailEl = document.getElementById('sidebar-user-email');
    const initialEl = document.getElementById('sidebar-user-initial');
    const name = getUserName();
    const email = getUserEmail();

    if (nameEl) nameEl.textContent = name;
    if (emailEl) emailEl.textContent = email;
    if (initialEl) initialEl.textContent = (name || 'U').charAt(0).toUpperCase();

    await loadDashboardData();
}

async function loadDashboardData() {
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
        
        // Render stats cards
        if (analytics) {
            renderStatsCards(analytics);
        }

        // Apply any active UI filters on the freshly loaded data
        applyClientFilters();
    } catch (err) {
        console.error('Failed to load dashboard data:', err);
    } finally {
        if (tableLoading) tableLoading.classList.add('hidden');
    }
}

// ==========================================
// --- CLIENT-SIDE SEARCH & FILTER ---
// ==========================================
function applyClientFilters() {
    const searchInput = document.getElementById('search-input');
    const statusSelect = document.getElementById('status-filter');

    const query = searchInput ? searchInput.value.trim().toLowerCase() : '';
    const status = statusSelect ? statusSelect.value : 'ALL';

    let filtered = cachedApplications;

    if (status !== 'ALL') {
        filtered = filtered.filter(app => (app.status || '').toUpperCase() === status);
    }

    if (query !== '') {
        filtered = filtered.filter(app => {
            const company = (app.company || '').toLowerCase();
            const role = (app.role || '').toLowerCase();
            const location = (app.location || '').toLowerCase();
            return company.includes(query) || role.includes(query) || location.includes(query);
        });
    }

    renderApplicationsTable(filtered);
}

function handleClientSearch() {
    applyClientFilters();
}

function handleStatusFilterChange() {
    applyClientFilters();
}

// ==========================================
// --- MODAL & FORM HANDLERS ---
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

function openAddModal() {
    const form = document.getElementById('add-app-form');
    const errorEl = document.getElementById('add-app-error');
    const dateInput = document.getElementById('add-applied-date');
    if (form) form.reset();
    if (errorEl) {
        errorEl.textContent = '';
        errorEl.classList.add('hidden');
    }
    // Default date to today
    if (dateInput) {
        dateInput.value = new Date().toISOString().split('T')[0];
    }
    const modal = document.getElementById('add-modal');
    if (modal) modal.classList.remove('hidden');
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('hidden');
    if (modalId === 'update-modal') activeEditingAppId = null;
}

function handleModalBackdropClick(event, modalId) {
    if (event.target && event.target.id === modalId) {
        closeModal(modalId);
    }
}

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
        await loadDashboardData();
    } catch (err) {
        if (errorEl) {
            errorEl.textContent = err.message;
            errorEl.classList.remove('hidden');
        }
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Create Application';
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
        await loadDashboardData();
    } catch (err) {
        if (errorEl) {
            errorEl.textContent = err.message;
            errorEl.classList.remove('hidden');
        }
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
        await loadDashboardData();
    } catch (err) {
        alert(err.message || 'Failed to delete application.');
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

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// ==========================================
// --- ROUTER ---
// ==========================================
function route() {
    const hash = window.location.hash || '#dashboard';
    const authView = document.getElementById('auth-view');
    const dashboardView = document.getElementById('dashboard-view');

    if (!isLoggedIn()) {
        if (authView) authView.classList.remove('hidden');
        if (dashboardView) dashboardView.classList.add('hidden');
        if (hash !== '#login' && hash !== '#register') {
            window.location.hash = '#login';
        } else if (hash === '#register') {
            switchAuthTab('register');
        } else {
            switchAuthTab('login');
        }
        return;
    }

    // User is logged in
    if (hash === '#login' || hash === '#register') {
        window.location.hash = '#dashboard';
        return;
    }

    if (authView) authView.classList.add('hidden');
    if (dashboardView) dashboardView.classList.remove('hidden');
    renderDashboard();
}

// ==========================================
// --- INIT ---
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    const btnCloseBanner = document.getElementById('btn-close-banner');
    if (btnCloseBanner) {
        btnCloseBanner.addEventListener('click', hideNetworkBanner);
    }
    route();
});

window.addEventListener('hashchange', route);
