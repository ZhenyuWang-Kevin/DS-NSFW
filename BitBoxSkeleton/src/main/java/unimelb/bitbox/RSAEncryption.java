package unimelb.bitbox;

import javax.crypto.Cipher;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;


/*
 * for both client and peer to use to encrypt and decrypt given their key and messages
 */
public class RSAEncryption {

    /*
     * use public key to encrypt the string
     * @param string that needs to be encrypted
     * @param public key that is used to encrypt
     * @return encrypted string
     */
    public static String PubEncrypt( String str, PublicKey publicKey ) throws Exception{

        // using RSA de encrypt
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        String outStr = Base64.getEncoder().encodeToString(cipher.doFinal(str.getBytes("UTF-8")));

        return outStr;
    }

    /*
     * use private key to decrypt the string
     * @param string that needs to be decrypted
     * @param private key that is used to decrypt
     * @return decrypted string
     */
    public static String PriDecrypt(String str, PrivateKey privateKey) throws Exception{
        // decode the encrypted string
        byte[] inputByte = Base64.getDecoder().decode(str.getBytes("UTF-8"));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        String outStr = new String(cipher.doFinal(inputByte));
        return outStr;
    }


}
