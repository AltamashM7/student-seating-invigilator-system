package com.examplanner.util;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.*;

public class PasswordUtil {
    private static final int ITERATIONS=120000;
    public static String hash(String password) {
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
        return "pbkdf2$"+ITERATIONS+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(derive(password,salt,ITERATIONS));
    }
    public static boolean verify(String password,String stored) {
        if(password==null||stored==null)return false;
        try {
            if(stored.startsWith("pbkdf2$")) {
                String[] parts=stored.split("\\$");if(parts.length!=4)return false;
                int rounds=Integer.parseInt(parts[1]);if(rounds<1000||rounds>1000000)return false;
                return MessageDigest.isEqual(derive(password,Base64.getDecoder().decode(parts[2]),rounds),Base64.getDecoder().decode(parts[3]));
            }
            byte[] legacy=MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(legacy,HexFormat.of().parseHex(stored));
        }catch(IllegalArgumentException|NoSuchAlgorithmException e){return false;}
    }
    public static boolean needsUpgrade(String stored){return stored!=null&&!stored.startsWith("pbkdf2$");}
    private static byte[] derive(String password,byte[] salt,int rounds){
        PBEKeySpec key=new PBEKeySpec(password.toCharArray(),salt,rounds,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(key).getEncoded();}
        catch(GeneralSecurityException e){throw new IllegalStateException("Password hashing unavailable",e);}
        finally{key.clearPassword();}
    }
}
