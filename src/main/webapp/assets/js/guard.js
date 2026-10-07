async function requireRole(role) {
    try {
        const data = await apiRequest('../../api/auth/session');
        if (!data.authenticated || data.role !== role) {
            location.href = AppUI.url('login.html');
            return null;
        }
        const nameNode = document.querySelector('[data-user-name]');
        if (nameNode) nameNode.textContent = data.name || data.username;
        return data;
    } catch (_) {
        location.href = AppUI.url('login.html');
        return null;
    }
}

async function logout() {
    await apiRequest('../../api/auth/logout', { method: 'POST' });
    location.href = AppUI.url('');
}
