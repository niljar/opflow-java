package com.devebot.opflow.supports;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 *
 * @author pnhung177
 */
public class OpflowStringUtil {
    private static final String INT_RANGE_DELIMITER = "-";
    private static final String INT_RANGE_PATTERN_STRING = "[\\d]{1,}\\s*\\-\\s*[\\d]{1,}";
    
    private static final String INT_ARRAY_DELIMITER = ",";
    private static final String INT_ARRAY_PATTERN_STRING = "[\\d]{1,}\\s*(\\s*,\\s*[\\d]{1,}){0,}";
    
    public static String join(String delimiter, String ... parts) {
        return String.join(delimiter, parts);
    }
    
    public static String[] splitByComma(String source) {
        return splitByComma(source, String.class);
    }
    
    public static <T> T[] splitByComma(String source, Class<T> type) {
        return splitByDelimiter(source, type, ",");
    }
    
    public static <T> T[] splitByDelimiter(String source, Class<T> type, String delimiter) {
        if (source == null) return null;
        String[] arr = source.split(delimiter);
        ArrayList<T> list = new ArrayList<>(arr.length);
        for(String item: arr) {
            String str = item.trim();
            if (str.length() > 0) list.add(OpflowConverter.convert(str, type));
        }
        return list.toArray((T[]) Array.newInstance(type, 0));
    }
    
    public static boolean isIntegerArray(String intArray) {
        return intArray != null && intArray.matches(INT_ARRAY_PATTERN_STRING);
    }
    
    public static Integer[] splitIntegerArray(String intArray) {
        return splitByDelimiter(intArray, Integer.class, INT_ARRAY_DELIMITER);
    }
    
    public static boolean isIntegerRange(String intRange) {
        return intRange != null && intRange.matches(INT_RANGE_PATTERN_STRING);
    }
    
    public static Integer[] getIntegerRange(String range) {
        try {
            Integer[] minmax = OpflowStringUtil.splitByDelimiter(range, Integer.class, INT_RANGE_DELIMITER);
            if (minmax == null) {
                return null;
            }
            if (minmax.length != 2) {
                return new Integer[0];
            }
            if (minmax[0] > minmax[1]) {
                int min = minmax[1];
                minmax[1] = minmax[0];
                minmax[0] = min;
            }
            return minmax;
        }
        catch (Exception e) {
            return null;
        }
    }
    
    public static String fromInputStream(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try {
            return fromReader(new InputStreamReader(inputStream, "UTF-8"), true);
        }
        catch (UnsupportedEncodingException e) {
            return null;
        }
    }
    
    public static String fromReader(Reader initialReader, boolean closeAtTheEnd) {
        if (initialReader == null) {
            return null;
        }
        try {
            char[] arr = new char[4 * 1024];
            StringBuilder buffer = new StringBuilder();
            int numCharsRead;
            while ((numCharsRead = initialReader.read(arr, 0, arr.length)) != -1) {
                buffer.append(arr, 0, numCharsRead);
            }
            if (closeAtTheEnd) {
                initialReader.close();
            }
            return buffer.toString();
        }
        catch (IOException ioe) {
            return null;
        }
    }
}
