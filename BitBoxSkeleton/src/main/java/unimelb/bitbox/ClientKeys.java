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
        }

        return lines;
    }


}
