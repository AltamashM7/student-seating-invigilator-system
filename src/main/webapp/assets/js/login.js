document.getElementById('loginForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const message = document.getElementById('message');
    message.textContent = '';

    try {
        const data = await apiRequest('api/auth/login', {
            method: 'POST',
            body: formBody({
                username: document.getElementById('username').value,
                password: document.getElementById('password').value
            })
        });
        location.href = data.role === 'ADMIN'
            ? 'pages/admin/dashboard.html'
            : 'pages/faculty/dashboard.html';
    } catch (error) {
        message.textContent = error.message;
    }
});
