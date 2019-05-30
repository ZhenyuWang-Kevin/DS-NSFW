package unimelb.bitbox;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;

import java.security.Key;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

//import org.apache.commons.codec.binary.Base64;
import java.util.Base64;
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

        System.out.println(AES.class.getResourceAsStream("/client3.pem"));

        String priKey = getKeyContent("/client3.pem");
        System.out.println("private key: \n" + priKey);
        String pubKey = getKeyContent("/client3.pem.pub");
        System.out.println("public key: \n" + pubKey);

    }

    public static String getKeyContent(String filename) throws IOException {

        InputStream is = AES.class.getResourceAsStream(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        String lines = "";
        while ((line = reader.readLine()) != null) {
            lines = lines + line;
            lines += "\n";

        }

        return lines;
    }

    /*
     * read and transform local private key file into readable format
     */
    public static String getPrivateKey(String filename)throws IOException{
//        String priKey = null;

//        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));
//        priKey = Base64.getEncoder().encodeToString(keyBytes);
//        System.out.println("keyBytes"+priKey);

        File f = new File(filename);
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        byte[] keyBytes = new byte[(int) f.length()];
        dis.readFully(keyBytes);
        dis.close();

        String temp = new String(keyBytes);
        System.out.println("Private key\n"+temp);
//        String privKeyPEM = temp.replace("-----BEGIN RSA PRIVATE KEY-----\n", "");
//        privKeyPEM = privKeyPEM.replace("-----END RSA PRIVATE KEY-----", "");
        //System.out.println("Private key\n"+privKeyPEM);

//        Base64 b64 = new Base64();
//        byte [] decoded = b64.decode(privKeyPEM);
//
//        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
//        KeyFactory kf = KeyFactory.getInstance(algorithm);
//        return kf.generatePrivate(spec);
        return temp;
    }

    /*
     * static method to encrypt a given messgage using a given key
     * @param message for encrypting
     * @param key used to encrypt the message
     *
     * @return return the encrypted string
     */
    public static String Encrypt(String sMsg, String sKey) throws Exception {


        byte[] encrypted = null;
        // Encrypt first
        Key aesKey = new SecretKeySpec(sKey.getBytes(), "AES");
        try {
            // TODO: add padding/ or not??
//            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            Cipher cipher = Cipher.getInstance("AES");
            // Perform encryption
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            encrypted = cipher.doFinal(sMsg.getBytes("UTF-8"));

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // encoding using base64
        return Base64.getEncoder().encodeToString(encrypted);

    }

    public static String Decrypt(String sMsg, String sKey) throws Exception {
        String message = null;
        // Decrypt result
        try {
            Key aesKey = new SecretKeySpec(sKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            message = new String(cipher.doFinal(Base64.getDecoder().decode(sMsg.getBytes())));


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return message;

    }


}