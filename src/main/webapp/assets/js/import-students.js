requireRole('ADMIN');
document.getElementById('importForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const result = document.getElementById('result');
    const data = new FormData();
    data.append('file', document.getElementById('file').files[0]);
    try {
        const response = await apiRequest('../../api/admin/import/students', { method: 'POST', body: data });
        result.hidden = false;
        result.innerHTML = `<h3>Import Result</h3><p>Imported: ${response.imported} · Duplicates: ${response.duplicates} · Invalid: ${response.invalid}</p>`
            + (response.errors.length ? `<div class="error-box"><ul>${response.errors.map(e => `<li>${e}</li>`).join('')}</ul></div>` : '<div class="success-box">No row errors.</div>');
    } catch (error) {
        result.hidden = false;
        result.innerHTML = `<div class="error-box">${error.message}</div>`;
    }
});
