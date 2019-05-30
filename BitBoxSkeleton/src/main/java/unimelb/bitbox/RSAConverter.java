package unimelb.bitbox;


import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

import java.security.PrivateKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Base64;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;



/*
 * Helper class to convert local public key file to compatible public format
 * https://stackoverflow.com/questions/3531506/using-public-key-from-authorized-keys-with-java-security
 */
public class RSAConverter {
    public static byte[] bytes;
    public static int pos;
    public static String identity;

    public static PublicKey decodePublicKey(String keyLine) throws Exception {
        bytes = null;
        pos = 0;

        // look for the Base64 encoded part of the line to decode
        // both ssh-rsa and ssh-dss begin with "AAAA" due to the length bytes
        for (String part : keyLine.split(" ")) {
            if (part.contains("@")){
                identity = part;
            }
            if (part.startsWith("AAAA")) {
                bytes = Base64.getDecoder().decode(part);
            }
        }
        if (bytes == null) {
            throw new IllegalArgumentException("no Base64 part to decode");
        }

        String type = decodeType();
        if (type.equals("ssh-rsa")) {
            BigInteger e = decodeBigInt();
            BigInteger m = decodeBigInt();
            RSAPublicKeySpec spec = new RSAPublicKeySpec(m, e);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } else if (type.equals("ssh-dss")) {
            BigInteger p = decodeBigInt();
            BigInteger q = decodeBigInt();
            BigInteger g = decodeBigInt();
            BigInteger y = decodeBigInt();
            DSAPublicKeySpec spec = new DSAPublicKeySpec(y, p, q, g);
            return KeyFactory.getInstance("DSA").generatePublic(spec);
        } else {
            throw new IllegalArgumentException("unknown type " + type);
        }
    }

    /*
     * HELPER method for converting local private key file into compatible format
     * from PEM PKCS#1 format to Privatekey format
     * http://magnus-k-karlsson.blogspot.com/2018/05/how-to-read-pem-pkcs1-or-pkcs8-encoded.html
     * @param local private key string
     * @return Private key object
     */
    public static PrivateKey convertPriKey(String key_str) throws Exception{

        String new_key_str;
        new_key_str = key_str.replaceAll("\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "");
        System.out.println("new Private key\n"+new_key_str);

        byte[] bytes = Base64.getDecoder().decode(new_key_str);

        DerInputStream derReader = new DerInputStream(bytes);
        DerValue[] seq = derReader.getSequence(0);
        // skip version seq[0];
        BigInteger modulus = seq[1].getBigInteger();
        BigInteger publicExp = seq[2].getBigInteger();
        BigInteger privateExp = seq[3].getBigInteger();
        BigInteger prime1 = seq[4].getBigInteger();
        BigInteger prime2 = seq[5].getBigInteger();
        BigInteger exp1 = seq[6].getBigInteger();
        BigInteger exp2 = seq[7].getBigInteger();
        BigInteger crtCoef = seq[8].getBigInteger();

        RSAPrivateCrtKeySpec keySpec =
                new RSAPrivateCrtKeySpec(modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        System.out.println(privateKey);

        return privateKey;
    }


    private static String decodeType() {
        int len = decodeInt();
        String type = new String(bytes, pos, len);
        pos += len;
        return type;
    }

    private static int decodeInt() {
        return ((bytes[pos++] & 0xFF) << 24) | ((bytes[pos++] & 0xFF) << 16)
                | ((bytes[pos++] & 0xFF) << 8) | (bytes[pos++] & 0xFF);
    }

    private static BigInteger decodeBigInt() {
        int len = decodeInt();
        byte[] bigIntBytes = new byte[len];
        System.arraycopy(bytes, pos, bigIntBytes, 0, len);
        pos += len;
        return new BigInteger(bigIntBytes);
    }

}
