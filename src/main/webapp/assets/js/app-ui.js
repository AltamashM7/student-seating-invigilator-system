/* Shared navigation, list controls and small form helpers. No framework required. */
const AppUI = (() => {
    const root = new URL('../../', document.currentScript.src);
    const url = path => new URL(path, root).href;
    const params = new URLSearchParams(location.search);
    function remember(values) {
        const next = new URL(location.href);
        Object.entries(values).forEach(([key, value]) => value === '' || value == null ? next.searchParams.delete(key) : next.searchParams.set(key, value));
        history.replaceState(null, '', next);
    }
    const adminGroups = [
        ['Overview', [['dashboard.html', 'Dashboard']]],
        ['Academic records', [['years.html', 'Academic Years'], ['departments.html', 'Departments'], ['divisions.html', 'Divisions'], ['students.html', 'Students'], ['enrollments.html', 'Enrollments'], ['faculty.html', 'Faculty'], ['rooms.html', 'Rooms'], ['courses.html', 'Courses']]],
        ['Examinations', [['exams.html', 'Exams'], ['seating.html', 'Seating'], ['import-students.html', 'CSV Import']]],
        ['Access', [['create-user.html', 'Admin Accounts']]]
    ];
    function initShell() {
        const isAdmin = document.body.dataset.shell === 'admin';
        const isFaculty = document.body.dataset.shell === 'faculty';
        const header = document.querySelector('[data-app-header]');
        if (!header) return;
        const staff = isAdmin || isFaculty;
        const home = staff ? `pages/${isAdmin ? 'admin' : 'faculty'}/dashboard.html` : '';
        header.className = 'app-header';
        header.innerHTML = `<div class="header-start">${staff ? '<button type="button" class="icon-button menu-button" id="menuToggle" aria-label="Open navigation" aria-expanded="false" aria-controls="appNavigation">☰</button>' : ''}<a class="brand" href="${url(home)}"><span class="brand-symbol" aria-hidden="true">EP</span><span>Exam Planner<small>${staff ? 'Staff workspace' : 'Student portal'}</small></span></a></div><nav class="header-links" aria-label="${staff ? 'Account' : 'Student navigation'}">${staff ? `<a href="${url('')}" class="quiet-link">Student view</a><span data-user-name class="account-name"></span><button type="button" id="logoutButton" class="secondary">Log out</button>` : `<a href="${url('')}" ${document.body.dataset.studentPage === 'home' ? 'aria-current="page"' : ''}>Home</a><a href="${url('pages/timetable.html')}" ${location.pathname.endsWith('/timetable.html') ? 'aria-current="page"' : ''}>Timetable</a><a href="${url('login.html')}" class="button secondary">Staff login</a>`}</nav>`;
        if (staff) {
            document.getElementById('logoutButton').addEventListener('click', () => logout());
            const navigation = document.querySelector('[data-app-nav]');
            navigation.id = 'appNavigation';
            navigation.className = 'app-navigation';
            navigation.setAttribute('aria-label', isAdmin ? 'Administration' : 'Faculty navigation');
            const groups = isAdmin ? adminGroups : [['Faculty', [['dashboard.html', 'Dashboard']]]];
            navigation.innerHTML = groups.map(([title, links]) => `<section class="nav-group"><h2>${title}</h2>${links.map(([path, label]) => `<a href="${url(`pages/${isAdmin ? 'admin' : 'faculty'}/${path}`)}" ${location.pathname.endsWith('/' + path) ? 'aria-current="page"' : ''}>${label}</a>`).join('')}</section>`).join('') + `<section class="nav-group"><h2>Public pages</h2><a href="${url('pages/timetable.html')}">Timetable</a></section>`;
            const toggle = document.getElementById('menuToggle');
            const close = () => { document.body.classList.remove('navigation-open'); toggle.setAttribute('aria-expanded', 'false'); toggle.setAttribute('aria-label', 'Open navigation'); };
            toggle.addEventListener('click', () => {
                const open = document.body.classList.toggle('navigation-open');
                toggle.setAttribute('aria-expanded', String(open));
                toggle.setAttribute('aria-label', open ? 'Close navigation' : 'Open navigation');
            });
            document.addEventListener('keydown', e => { if (e.key === 'Escape') { close(); toggle.focus(); } });
            navigation.addEventListener('click', e => { if (e.target.closest('a')) close(); });
        }
    }
    async function busy(button, action, label = 'Saving…') {
        if (button.disabled) return;
        const old = button.textContent;
        button.disabled = true; button.textContent = label;
        try { return await action(); } finally { button.disabled = false; button.textContent = old; }
    }
    function message(node, text, success = false) {
        node.textContent = text; node.hidden = !text;
        node.className = success ? 'success-box' : 'error-box';
    }
    function date(value) {
        return value ? new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(value.slice(0, 10) + 'T00:00:00')) : '—';
    }
    function time(value) {
        return value ? new Intl.DateTimeFormat('en-IN', { hour: 'numeric', minute: '2-digit' }).format(new Date('2000-01-01T' + value)) : '—';
    }
    function badge(value) { return `<span class="status-badge status-${esc(String(value).toLowerCase())}">${esc(String(value).toLowerCase().replace(/^./, c => c.toUpperCase()))}</span>`; }
    function studyYear(value) { return ['FY', 'SY', 'TY', 'BE'][Number(value) - 1] || '—'; }
    function searchableSelect(select, label) {
        if (select.multiple || select.options.length < 12) return;
        let input = select._searchInput;
        if (!input) {
            input = document.createElement('input'); input.type = 'search'; input.className = 'option-search'; input.autocomplete = 'off';
            input.setAttribute('aria-label', `Search ${label} options`); input.placeholder = `Search ${label}…`;
            // Keep search outside the select's label so its accessible name stays stable.
            const holder = document.createElement('div'); holder.className = 'option-search-wrap';
            const labelNode = select.closest('label');
            const field = document.createElement('div'); field.className = 'select-field';
            labelNode.before(field); field.append(labelNode, holder); holder.append(input);
            const help=document.createElement('small');help.className='helper-text';help.textContent='Narrow the options, then choose a record. Your current selection is kept.';holder.append(help);
            select.setAttribute('aria-label', label);
            input.addEventListener('input', () => {
                const chosen = select.value, query = input.value.trim().toLowerCase();
                select.innerHTML = input._options.filter(o => o.value === chosen || o.label.toLowerCase().includes(query)).map(o => `<option value="${esc(o.value)}" ${o.disabled ? 'disabled' : ''}>${esc(o.label)}</option>`).join('');
                select.value = chosen;
            });
            select._searchInput = input;
        }
        input._options = [...select.options].map(o => ({value:o.value, label:o.text, disabled:o.disabled})); input.value = '';
    }
    function list({search, size, previous, next, count, render}) {
        let items = [], page = 1;
        if (search) search.value = params.get('q') || '';
        if (size) size.value = params.get('size') || '25';
        function draw(reset = false) {
            if (reset) page = 1;
            const query = search?.value.trim().toLowerCase() || '';
            const filtered = items.filter(item => !query || item.text.toLowerCase().includes(query));
            const limit = Number(size?.value || 25), pages = Math.max(1, Math.ceil(filtered.length / limit));
            page = Math.min(page, pages);
            render(filtered.slice((page - 1) * limit, page * limit).map(item => item.value), filtered);
            if (count) count.textContent = filtered.length ? `Showing ${(page - 1) * limit + 1}–${Math.min(page * limit, filtered.length)} of ${filtered.length}${filtered.length !== items.length ? ` matches (${items.length} total)` : ' records'}` : `No matching records${items.length ? ` (${items.length} total)` : ''}.`;
            if (previous) previous.disabled = page === 1;
            if (next) next.disabled = page === pages;
        }
        search?.addEventListener('input', () => { remember({q:search.value.trim()}); draw(true); });
        size?.addEventListener('change', () => { remember({size:size.value}); draw(true); });
        previous?.addEventListener('click', () => { page--; draw(); });
        next?.addEventListener('click', () => { page++; draw(); });
        return {set(values) {items = values; draw();}, draw};
    }
    function dirty(form) {
        let changed = false;
        form.addEventListener('input', () => changed = true);
        form.addEventListener('change', () => changed = true);
        window.addEventListener('beforeunload', event => { if (changed) {event.preventDefault(); event.returnValue = '';} });
        return () => changed = false;
    }
    initShell();
    return {url, params, remember, busy, message, date, time, badge, studyYear, searchableSelect, list, dirty};
})();
