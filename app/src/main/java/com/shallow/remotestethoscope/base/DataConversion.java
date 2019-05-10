package com.shallow.remotestethoscope.base;

public class DataConversion {

    private final static String [] hexArray = {"0", "1", "2", "3", "4", "5", "6", "7"
            , "8", "9", "A", "B", "C", "D", "E", "F"};

    public static int getUnsignedByte(byte data) {
        return data & 0xff;
    }

    public static int byteToInt(byte high, byte low) {
        String highHex = byteToHex(getUnsignedByte(high));
        String lowHex = byteToHex(getUnsignedByte(low));
        String resultHex = highHex + lowHex;
        return Integer.valueOf(resultHex, 16);
    }

    private static String byteToHex(int n) {
        int decade = n / 16;
        int unit = n % 16;
        return hexArray[decade] + hexArray[unit];
    }

    public static byte[] stringToByteArray(String s) {
        byte[] result = new byte[s.length() / 2];
        if (s.length() % 2 == 0) {
            for (int i = 0; i < s.length();i +=2) {
                String sub = s.substring(i, i + 2);
                byte b = (byte) Integer.parseInt(sub, 16);
                result[i/2] = b;
            }
        }
        return result;
    }

    public static String byteArrayToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            int unsignedValue = getUnsignedByte(aByte);
            sb.append(Integer.toHexString(unsignedValue));
        }
        return sb.toString();
    }

}
