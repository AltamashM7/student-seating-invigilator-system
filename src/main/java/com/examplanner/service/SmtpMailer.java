package com.examplanner.service;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.net.ssl.*;

/** Small SMTP client using the JDK TLS sockets; no external email API. */
public class SmtpMailer {
    public static boolean configured() { return !env("SMTP_HOST","").isBlank()&&!env("SMTP_FROM","").isBlank(); }
    public static String env(String name,String fallback) {String value=System.getenv(name);return value==null||value.isBlank()?fallback:value;}
    public void send(String recipient,String subject,String body) throws IOException {
        if(!configured()) throw new IOException("SMTP is not configured");
        String host=env("SMTP_HOST",""),mode=env("SMTP_MODE","STARTTLS").toUpperCase(Locale.ROOT);
        if(!Set.of("STARTTLS","TLS").contains(mode)) throw new IOException("SMTP_MODE must be STARTTLS or TLS");
        int port;
        try {port=Integer.parseInt(env("SMTP_PORT","TLS".equals(mode)?"465":"587"));}
        catch(NumberFormatException e) {throw new IOException("Invalid SMTP port");}
        String from=address(env("SMTP_FROM","")),to=address(recipient);
        Socket raw=new Socket(); raw.connect(new InetSocketAddress(host,port),10000); raw.setSoTimeout(10000);
        try(Socket connected=raw) {
            Socket transport=raw;
            if("TLS".equals(mode)) transport=tls(raw,host,port);
            Channel channel=new Channel(transport); channel.expect(220); channel.command("EHLO exam-planner.local",250);
            if("STARTTLS".equals(mode)) {
                channel.command("STARTTLS",220); transport=tls(raw,host,port); channel=new Channel(transport); channel.command("EHLO exam-planner.local",250);
            }
            try(Socket secure=transport) {
                String username=env("SMTP_USERNAME","");
                if(!username.isEmpty()) {
                    channel.command("AUTH LOGIN",334);
                    channel.command(Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)),334);
                    channel.command(Base64.getEncoder().encodeToString(env("SMTP_PASSWORD","").getBytes(StandardCharsets.UTF_8)),235);
                }
                channel.command("MAIL FROM:<"+from+">",250); channel.command("RCPT TO:<"+to+">",250,251); channel.command("DATA",354);
                String encodedSubject=Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8));
                String encodedBody=Base64.getMimeEncoder(76,new byte[]{13,10}).encodeToString(body.getBytes(StandardCharsets.UTF_8));
                channel.write("From: "+from+"\r\nTo: "+to+"\r\nSubject: =?UTF-8?B?"+encodedSubject+"?=\r\nDate: "+java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)+"\r\nMIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n"+encodedBody+"\r\n.\r\n");
                channel.expect(250);
                // SMTP accepted the message. A connection loss during QUIT must not trigger a duplicate retry.
                try {channel.command("QUIT",221);} catch(IOException ignored) {}
            }
        }
    }
    private static SSLSocket tls(Socket socket,String host,int port) throws IOException {
        SSLSocket secure=(SSLSocket)((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(socket,host,port,true);
        SSLParameters parameters=secure.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");secure.setSSLParameters(parameters);
        secure.setSoTimeout(10000);secure.startHandshake();return secure;
    }
    private static String address(String value) throws IOException {
        if(value==null||value.length()>120||!value.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+")) throw new IOException("Invalid email address");
        return value;
    }
    private static class Channel {
        final BufferedReader input;final BufferedWriter output;
        Channel(Socket socket) throws IOException {input=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));output=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.US_ASCII));}
        void write(String text) throws IOException {output.write(text);output.flush();}
        void command(String command,int... codes) throws IOException {write(command+"\r\n");expect(codes);}
        void expect(int... expected) throws IOException {
            String line;int code=-1,lines=0;
            do {line=input.readLine();if(line==null||line.length()<3||++lines>100) throw new IOException("SMTP connection closed or invalid reply");
                try {code=Integer.parseInt(line.substring(0,3));} catch(NumberFormatException e){throw new IOException("Invalid SMTP response");}
            } while(line.length()>3&&line.charAt(3)=='-');
            for(int value:expected)if(value==code)return;
            throw new IOException("SMTP rejected the request (code "+code+")");
        }
    }
}
