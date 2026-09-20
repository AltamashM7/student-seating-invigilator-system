async function apiRequest(url, options = {}) {
    const response = await fetch(url, options);
    let data = {};
    try {
        data = await response.json();
    } catch (_) {
        data = { error: 'Unexpected server response' };
    }
    if (!response.ok) {
        const error = new Error(data.error || 'Request failed');
        error.status = response.status;
        error.data = data;
        throw error;
    }
    return data;
}

function formBody(values) {
    const body = new URLSearchParams();
    Object.entries(values).forEach(([key, value]) => {
        if (value !== undefined && value !== null) body.append(key, value);
    });
    return body;
}
