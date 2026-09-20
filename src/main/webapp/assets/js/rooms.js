requireRole('ADMIN');
let rooms = [];

async function loadRooms() {
    const data = await apiRequest('../../api/admin/rooms');
    rooms = data.rooms;
    document.getElementById('roomRows').innerHTML = rooms.map(room => `
        <tr><td>${room.roomNumber}</td><td>${room.capacity}</td><td>S001 - S${String(room.capacity).padStart(3, '0')}</td>
        <td><button class="secondary" onclick="editRoom(${room.id})">Edit</button> <button class="danger" onclick="deleteRoom(${room.id})">Delete</button></td></tr>`).join('');
}

function editRoom(id) {
    const room = rooms.find(item => item.id === id);
    document.getElementById('roomId').value = room.id;
    document.getElementById('roomNumber').value = room.roomNumber;
    document.getElementById('capacity').value = room.capacity;
}

function resetForm() {
    document.getElementById('roomForm').reset();
    document.getElementById('roomId').value = '';
}

async function deleteRoom(id) {
    if (!confirm('Delete this room?')) return;
    try {
        await apiRequest('../../api/admin/rooms', { method: 'POST', body: formBody({ action: 'delete', id }) });
        await loadRooms();
    } catch (error) { alert(error.message); }
}

document.getElementById('roomForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const id = document.getElementById('roomId').value;
    const message = document.getElementById('message');
    try {
        await apiRequest('../../api/admin/rooms', {
            method: 'POST',
            body: formBody({ action: id ? 'update' : 'create', id, roomNumber: document.getElementById('roomNumber').value, capacity: document.getElementById('capacity').value })
        });
        message.textContent = '';
        resetForm();
        await loadRooms();
    } catch (error) { message.textContent = error.message; }
});
loadRooms();
