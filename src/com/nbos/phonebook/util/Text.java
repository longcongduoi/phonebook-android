package com.nbos.phonebook.util;

public class Text {
    public static boolean isEmpty(CharSequence str) {
        return (str == null || str.toString().trim().length() == 0);
    }

}
