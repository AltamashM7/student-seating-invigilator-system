const loginNode=id=>document.getElementById(id);
loginNode('togglePassword').addEventListener('click',()=>{
    const show=loginNode('password').type==='password';
    loginNode('password').type=show?'text':'password';
    loginNode('togglePassword').textContent=show?'Hide':'Show';
    loginNode('togglePassword').setAttribute('aria-pressed',String(show));
});
loginNode('loginForm').addEventListener('submit',event=>{
    event.preventDefault();AppUI.busy(loginNode('loginButton'),async()=>{
        try {
            const data=await apiRequest(AppUI.url('api/auth/login'),{method:'POST',body:formBody({username:loginNode('username').value,password:loginNode('password').value})});
            location.href=AppUI.url(data.role==='ADMIN'?'pages/admin/dashboard.html':'pages/faculty/dashboard.html');
        } catch(error){AppUI.message(loginNode('message'),error.message);}
    },'Signing in…');
});
