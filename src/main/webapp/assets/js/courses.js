requireRole('ADMIN');
let courses = [];

async function loadDepartments() {
    const data = await apiRequest('../../api/admin/departments');
    document.getElementById('departmentId').innerHTML = data.departments.map(d => `<option value="${d.id}">${d.code} - ${d.name}</option>`).join('');
}

async function loadCourses() {
    const data = await apiRequest('../../api/admin/courses');
    courses = data.courses;
    document.getElementById('courseRows').innerHTML = courses.map(c => `<tr><td>${c.code}</td><td>${c.name}</td><td>${c.departmentCode}</td><td>${c.semester}</td><td><button class="secondary" onclick="editCourse(${c.id})">Edit</button> <button class="danger" onclick="deleteCourse(${c.id})">Delete</button></td></tr>`).join('');
}

function editCourse(id) {
    const c = courses.find(item => item.id === id);
    document.getElementById('courseId').value = c.id;
    document.getElementById('code').value = c.code;
    document.getElementById('name').value = c.name;
    document.getElementById('departmentId').value = c.departmentId;
    document.getElementById('semester').value = c.semester;
}
function resetCourseForm() { document.getElementById('courseForm').reset(); document.getElementById('courseId').value = ''; }
async function deleteCourse(id) {
    if (!confirm('Delete this course?')) return;
    try { await apiRequest('../../api/admin/courses', { method: 'POST', body: formBody({ action: 'delete', id }) }); await loadCourses(); }
    catch (error) { alert(error.message); }
}

document.getElementById('courseForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const id = document.getElementById('courseId').value;
    const message = document.getElementById('message');
    try {
        await apiRequest('../../api/admin/courses', { method: 'POST', body: formBody({ action: id ? 'update' : 'create', id, code: document.getElementById('code').value, name: document.getElementById('name').value, departmentId: document.getElementById('departmentId').value, semester: document.getElementById('semester').value }) });
        message.textContent = ''; resetCourseForm(); await loadCourses();
    } catch (error) { message.textContent = error.message; }
});
(async () => { await loadDepartments(); await loadCourses(); })();
