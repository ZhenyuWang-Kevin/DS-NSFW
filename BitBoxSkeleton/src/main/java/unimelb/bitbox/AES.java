package unimelb.bitbox;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import java.security.NoSuchAlgorithmException;

import java.security.Key;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;
/*
 * Class for using AES128 to encrypt and decrypt
 */
public class AES {

    private static Logger log = Logger.getLogger(Key.class.getName());


    /*
    public static void main(String[] args) throws Exception {


        // secret key in Hex string
        String sKey = AES.generateSecreteKey(128);
        System.out.println("secrete key: " + sKey);

        byte[] encryptedMsg = AES.Encrypt("test encrypting AES", sKey);
        System.out.println("encrypted secret key: " + encryptedMsg);

        String decryptedMsg = AES.Decrypt(encryptedMsg, sKey);
        System.out.println("decrypted secret key: " + decryptedMsg);

    }
*?

    /*
     * generate secrete key for communicating with clients
     * @param length of the key generated
     * @return generated key
     */
    public static String generateSecreteKey(int length){
        SecretKey skey=null;

        try {
            KeyGenerator kgen = null;
            kgen = KeyGenerator.getInstance("AES");
            // secrete key length
            kgen.init(length);
            // generate the secrete key using AES
            skey = kgen.generateKey();

        }catch (NoSuchAlgorithmException e){
            e.printStackTrace();
            log.warning("No such Algorithm");
        }

        //        return skey.getEncoded();
        return parseByte2HexStr(skey.getEncoded());
    }

    // HELPER method for converting bytes into Hex string https://blog.csdn.net/qy20115549/article/details/83105736
    public static String parseByte2HexStr(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    // HELPER method for converting hex string into bytes string https://blog.csdn.net/qy20115549/article/details/83105736
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1)
            return null;
        byte[] result = new byte[hexStr.length()/2];
        for (int i = 0;i< hexStr.length()/2; i++) {
            int high = Integer.parseInt(hexStr.substring(i*2, i*2+1), 16);
            int low = Integer.parseInt(hexStr.substring(i*2+1, i*2+2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }



    /*
     * static method to encrypt a given message using a given key
     * @param message for encrypting
     * @param key used to encrypt the message
     *
     * @return return the encrypted string
     */
    public static byte[] Encrypt(String sMsg, String sKey){

        byte[] encrypted = null;

        byte[] skey = parseHexStr2Byte(sKey);
        // Encrypt first
        Key aesKey = new SecretKeySpec(skey, "AES");
        try {
            // TODO: add padding/ or not??
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//            Cipher cipher = Cipher.getInstance("AES");
            // Perform encryption
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            encrypted = cipher.doFinal(sMsg.getBytes("UTF-8"));

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // encoding using base64
//        return Base64.getEncoder().encodeToString(encrypted);
        return encrypted;
    }

    /*
     * static method to decrypt a given message using a given key
     * @param message for decrypting
     * @param key used to decrypt the message
     *
     * @return return the decrypted bytes
     */
    public static String Decrypt(byte[] sMsg, String sKey){
        byte[] message = null;
        String decryptedMsg=null;
        byte[] key = parseHexStr2Byte(sKey);
        // Decrypt result
        try {
            Key aesKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            message = cipher.doFinal(sMsg);
            decryptedMsg = new String(message, "UTF-8");


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return decryptedMsg;

    }


}