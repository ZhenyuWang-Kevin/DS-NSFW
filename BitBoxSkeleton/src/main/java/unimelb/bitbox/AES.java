package unimelb.bitbox;

import java.io.IOException;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;


import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
/*
 * Class for using AES128 to encrypt and decrypt
 */
public class AES {

    private static Logger log = Logger.getLogger(AES.class.getName());


    public static void main(String[] args) throws Exception {

        String cKey = "1234567890123456";

        String cSrc = "testAES";
        System.out.println(cSrc);

        String enString = AES.Encrypt(cSrc, cKey);
        System.out.println("encerypted string: " + enString);


        String DeString = AES.Decrypt(enString, cKey);
        System.out.println("decrypted string: " + DeString);
    }

    /*
     * static method to encrypt a given messgage using a given key
     * @param message for encrypting
     * @param key used to encrypt the message
     *
     * @return return the encrypted string
     */
    public static String Encrypt(String sMsg, String sKey) throws Exception {

        if (sKey == null) {
            log.warning("key cannot be null!")
            return null;
        }

        if (sKey.length() != 16) {
            log.warning("length of key is not 16!")
            return null;
        }

        byte[] raw = sKey.getBytes("utf-8");
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");

        // cipher method in "alogorithm/mode/padding"
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(sMsg.getBytes("utf-8"));

        // encoding using base64
        return new Base64().encodeToString(encrypted);

    }

    public static String Decrypt(String sMsg, String sKey) throws Exception {
        try {
            if (sKey == null) {
                log.warning("key cannot be null!")
                return null;
            }

            if (sKey.length() != 16) {
                log.warning("length of key is not 16!")
                return null;
            }
            byte[] raw = sKey.getBytes("utf-8");
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);

            // use base64 to decode first
            byte[] encrypted1 = new Base64().decode(sMsg);
            try {
                byte[] original = cipher.doFinal(encrypted1);
                String originalString = new String(original,"utf-8");
                return originalString;
            } catch (Exception e) {
                System.out.println(e.toString());
                return null;
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
            return null;
        }
//        catch(IOException e){
//            log.warning(e.getMessage());
//        }
    }


}