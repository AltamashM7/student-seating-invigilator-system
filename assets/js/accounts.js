(async()=>{
    if (!await requireRole('ADMIN')) return;
    const node=id=>document.getElementById(id);
    if(location.hash==='#resetAccount')node('resetAccount').open=true;
    node('userForm').addEventListener('submit',event=>{
        event.preventDefault();AppUI.busy(event.submitter,async()=>{
            try {
                await apiRequest(AppUI.url('api/auth/register'),{method:'POST',body:formBody({name:node('name').value,email:node('email').value,username:node('username').value,password:node('password').value,role:'ADMIN'})});
                event.target.reset();AppUI.message(node('message'),'Administrator account created.',true);
            } catch(error){AppUI.message(node('message'),error.message);}
        },'Creating account…');
    });
    node('adminResetForm').addEventListener('submit',event=>{
        event.preventDefault();AppUI.busy(event.submitter,async()=>{
            try {
                await apiRequest(AppUI.url('api/auth/reset-password'),{method:'POST',body:formBody({username:node('resetUsername').value,email:node('resetEmail').value,password:node('newPassword').value})});
                event.target.reset();AppUI.message(node('resetMessage'),'Account password reset successfully.',true);
            } catch(error){AppUI.message(node('resetMessage'),error.message);}
        },'Resetting password…');
    });
})();
