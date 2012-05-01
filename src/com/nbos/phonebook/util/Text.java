package com.nbos.phonebook.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Text {
    public static boolean isEmpty(CharSequence str) {
        return (str == null || str.toString().trim().length() == 0);
    }
    
    public static boolean isEmail(String email){
    	 final String EMAIL_PATTERN = 
            "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    	Pattern pattern = Pattern.compile(EMAIL_PATTERN);
    	Matcher match = pattern.matcher(email);
    	return match.matches();
    }

}
