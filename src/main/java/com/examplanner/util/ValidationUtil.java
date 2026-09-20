package com.examplanner.util;

public class ValidationUtil {
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean validEmail(String email) {
        return email != null && email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    public static boolean validPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        for (char c : password.toCharArray()) {
            upper |= Character.isUpperCase(c);
            lower |= Character.isLowerCase(c);
            digit |= Character.isDigit(c);
        }
        return upper && lower && digit;
    }
}
