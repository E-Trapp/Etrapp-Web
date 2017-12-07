package cat.udl.etrapp.server.utils;

import cat.udl.etrapp.server.models.SessionToken;

import javax.ws.rs.core.HttpHeaders;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Utils {

    public static SessionToken generateSessionToken() {
        final String token = UUID.randomUUID().toString();
        return new SessionToken(token, getHashedString(token.getBytes()));
    }

    public static String getHashedString(String input) {
        return getHashedString(input.getBytes());
    }

    public static String getHashedString(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return byteArrayToHexString(md.digest(input));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String byteArrayToHexString(byte[] b) {
        StringBuilder result = new StringBuilder();
        for (byte aB : b) {
            result.append(Integer.toString((aB & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    public static String getAuthToken(HttpHeaders headers) {
        return headers.getHeaderString(HttpHeaders.AUTHORIZATION).substring("Bearer".length()).trim();
    }

    public static String generateSQLPatch(String table, Map<String, Object> updates, long id) {
        String SQLStatement = "UPDATE %s SET %s WHERE id = %d;";
        String SETStatement = "";
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            SETStatement = SETStatement.concat(entry.getKey().concat(" = "));
            if (entry.getValue() instanceof String) {
                SETStatement = SETStatement.concat("'"+ entry.getValue() + "',");
            } else {
                SETStatement = SETStatement.concat(entry.getValue() + ",");
            }
        }
        SETStatement = SETStatement.substring(0, SETStatement.length() - 1);
        return String.format(SQLStatement, table, SETStatement, id);
    }

    public static void main(String args[]) {
        System.out.println("Custom token for development: qweqweqweqwe -> " + getHashedString("qweqweqweqwe"));

    }

}
