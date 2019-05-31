package unimelb.bitbox;

import java.io.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/*
 * For client to get and read public and private key from local files
 */

public class ClientKeys {
    private static Map<Integer, String> keyMap = new HashMap<Integer, String>();


    /*
    public static void main(String[] args) throws Exception {

        // Read local private key file and get key strings
//        String priKey = ClientKeys.getKeyContent("/client3.pem");
        String priKey = ClientKeys.getKeyContent("bitboxclient_rsa");

        System.out.println("private key origin str: \n" + priKey);

        // Read local public key file and get key strings
//        String pubKey = ClientKeys.getKeyContent("/client3.pem.pub");
        String pubKey = ClientKeys.getKeyContent("bitboxclient_rsa.pub");
        System.out.println("public key origin str:: \n" + pubKey);

        // convert public key string into compatible key format and get the identity
        PublicKey publicKey = RSAConverter.decodePublicKey(pubKey);
        String identity = RSAConverter.identity;
        System.out.println("identity is :"+identity);

        // convert private key into compatible key format
        PrivateKey privateKey = RSAConverter.convertPriKey(priKey);

        System.out.println("\nTesting RSA :\n");
        String encrypted = RSAEncryption.PubEncrypt("i am message for testing encryption", publicKey);
        System.out.println("Encrypted message : "+encrypted);

        String decrypted = RSAEncryption.PriDecrypt(encrypted, privateKey);
        System.out.println("Decrypted message : "+decrypted);


//        System.out.println("\nTetsing generating keys: \n");
//        genKeyPair();

    }


     */

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
    public static String getKeyContent(String filename) throws IOException {
        InputStream is = new FileInputStream(filename);
//        InputStream is = ClientKeys.class.getResourceAsStream(filename);
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

    private static Map<String, byte[]> clientInfo = new HashMap<String, byte[]>();

    /*
     * from the authorized public keys list to find this client's public key
     * if not exist, then refuse connection request
     * @param authorized public key strings
     * @param current client's identity
     */
    private static Map<String, byte[]> getClientPubKey(String authorized_keys, String ClientIdentity){
        byte[] tmp_key=null;
        String tmp_identity=null;

        // there are more than one key stores
        if (authorized_keys.contains(",")){
            // split the string by comma
            for (String key : authorized_keys.split(",")){

                // retrieve identity and public key
                for (String part : key.split(" ")) {
                    if (part.contains("@")){

                        if (ClientIdentity.equals(part)){
                            tmp_identity = part;
                            clientInfo.put(tmp_identity, tmp_key);
                            return clientInfo;
                        }

//                        Peer.key_identity = part;
                    }
                    if (part.startsWith("AAAA")) {
                        tmp_key = Base64.getDecoder().decode(part);
//                        Peer.public_key = Base64.getDecoder().decode(part);
                    }
                }
            }
        }else{
            for (String part : authorized_keys.split(" ")) {
                if (part.contains("@")){

                    if (ClientIdentity.equals(part)){
                        tmp_identity = part;
                        clientInfo.put(tmp_identity, tmp_key);
                        return clientInfo;
                    }
//                    Peer.key_identity = part;
                }
                if (part.startsWith("AAAA")) {
                    tmp_key = Base64.getDecoder().decode(part);
//                    Peer.public_key = Base64.getDecoder().decode(part);
                }
            }
        }

        return clientInfo;
    }



}
