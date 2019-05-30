package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.crypto.Cipher;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import java.util.Base64;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.junit.Ignore;
import org.junit.Test;

import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

/*
 * For client to get and read public and private key from local files
 */

public class ClientKeys {
    private static Map<Integer, String> keyMap = new HashMap<Integer, String>();

    public static void main(String[] args) throws Exception {

        System.out.println(ClientKeys.class.getResourceAsStream("/client3.pem"));

        String priKey = ClientKeys.getKeyContent("/client3.pem");
        System.out.println("private key: \n" + priKey);
        String pubKey = ClientKeys.getKeyContent("/client3.pem.pub");
        System.out.println("public key: \n" + pubKey);

        PublicKey publicKey = PublicRSAConverter.decodePublicKey(pubKey);
        String identity = PublicRSAConverter.identity;
        System.out.println("identity is :"+identity);

        String newPri;
        newPri = priKey.replaceAll("\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "");
        System.out.println("new Private key\n"+newPri);

        PrivateKey key = convertPriKey(newPri);
        System.out.println("new Private key\n"+key);

        System.out.println("\nTesting RSA :\n");
        String encrypted = PriEncrypt("i am message for testing encryption", key);
        System.out.println("Encrypted message : "+encrypted);

        String decrypted = PubDecrypt(encrypted, publicKey);
        System.out.println("Decrypted message : "+decrypted);

    }

    public static void genKeyPair() throws NoSuchAlgorithmException {
        // KeyPairGenerator to generate pub/pri pair
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        // initialize key pair with given size
        keyPairGen.initialize(1024,new SecureRandom());
        // generate key pair
        KeyPair keyPair = keyPairGen.generateKeyPair();
        // get private key
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        // get private key
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        // get private key string
        String privateKeyString = Base64.getEncoder().encodeToString((privateKey.getEncoded()));
        // save pub and private key into map
        keyMap.put(0,publicKeyString);  //0 for public key
        keyMap.put(1,privateKeyString);  //1 for private key
    }

    /*
     * read key files from local file
     * @param key file name
     * @return pub/pri key
     */
    private static String getKeyContent(String filename) throws IOException {

        InputStream is = ClientKeys.class.getResourceAsStream(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        String lines = "";
        while ((line = reader.readLine()) != null) {
            lines = lines + line;
//            lines += "\n";

        }
        // TODO: remove last \n character
//        lines.substring(0,lines.length() - 1);

        return lines;
    }

    // http://magnus-k-karlsson.blogspot.com/2018/05/how-to-read-pem-pkcs1-or-pkcs8-encoded.html
    public static PrivateKey convertPriKey(String key_str) throws Exception{
//        String content = new String(
//                Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("server.key.pkcs1.pem").toURI())));
//        content = content.replaceAll("\\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "")
//                .replace("-----END RSA PRIVATE KEY-----", "");
//        System.out.println("'" + content + "'");
        byte[] bytes = Base64.getDecoder().decode(key_str);

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


    public static String PriEncrypt( String str, PrivateKey priKey) throws Exception{
//        byte[] decoded = Base64.getDecoder().decode(priKey);
//        RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        // using RSA de encrypt
        Cipher cipher = Cipher.getInstance("RSA");
//        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        cipher.init(Cipher.ENCRYPT_MODE, priKey);
//        String outStr = Base64.encodeBase64String(cipher.doFinal(str.getBytes("UTF-8")));
        String outStr = Base64.getEncoder().encodeToString(cipher.doFinal(str.getBytes("UTF-8")));

        return outStr;
    }

    public static String PubDecrypt( String str, PublicKey pubKey) throws Exception{

        // decode the encrypted string
        byte[] inputByte = Base64.getDecoder().decode(str.getBytes("UTF-8"));
        // use rsa to decode
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, pubKey);
        String outStr = new String(cipher.doFinal(inputByte));
        return outStr;
    }

    public static String PubEncrypt( String str, PublicKey publicKey ) throws Exception{

//        .decodeBase64(publicKey);
//        byte[] decoded = Base64.getDecoder().decode(publicKey);
//        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        // using RSA de encrypt
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
//        String outStr = Base64.encodeBase64String(cipher.doFinal(str.getBytes("UTF-8")));
        String outStr = Base64.getEncoder().encodeToString(cipher.doFinal(str.getBytes("UTF-8")));

        return outStr;
    }
    public static String PriDecrypt(String str, PrivateKey privateKey) throws Exception{
        // decode the encrypted string
        byte[] inputByte = Base64.getDecoder().decode(str.getBytes("UTF-8"));
        // decode the private key
//        byte[] decoded = Base64.getDecoder().decode(privateKey);
//        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        // use rsa to decode
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        String outStr = new String(cipher.doFinal(inputByte));
        return outStr;
    }

}
