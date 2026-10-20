package com.examplanner.filter;
import com.examplanner.util.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URI;

@WebFilter("/*")
public class SecurityFilter implements Filter {
    public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain) throws IOException,ServletException {
        HttpServletRequest r=(HttpServletRequest)request;HttpServletResponse s=(HttpServletResponse)response;
        r.setCharacterEncoding("UTF-8");
        s.setHeader("X-Content-Type-Options","nosniff");s.setHeader("X-Frame-Options","DENY");s.setHeader("Referrer-Policy","same-origin");
        s.setHeader("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'");
        if(r.getServletPath().startsWith("/api/"))s.setHeader("Cache-Control","no-store");
        if(!java.util.Set.of("GET","HEAD","OPTIONS").contains(r.getMethod())){
            boolean cross="cross-site".equals(r.getHeader("Sec-Fetch-Site"));String origin=r.getHeader("Origin");
            if(origin!=null){try{URI u=URI.create(origin);int port=u.getPort()<0?("https".equals(u.getScheme())?443:80):u.getPort();cross|=!r.getScheme().equalsIgnoreCase(u.getScheme())||!r.getServerName().equalsIgnoreCase(u.getHost())||port!=r.getServerPort();}catch(IllegalArgumentException e){cross=true;}}
            if(cross){JsonUtil.send(s,403,JsonUtil.message("error","Open the application directly before submitting changes"));return;}
        }
        chain.doFilter(request,response);
    }
}
