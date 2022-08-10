package com.libra.plugin;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;


public class DESUtil {

    private static String key = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

    /*
     * 生成密钥
     */
    public static byte[] initKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
        keyGen.init(56);
        SecretKey secretKey = keyGen.generateKey();
        return secretKey.getEncoded();
    }


    /*
     * DES 加密
     */
    public static String encrypt(String data) throws Exception {
        SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode("8uPxrsRRZ78=".getBytes()), "DES");

        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] cipherBytes = cipher.doFinal(data.getBytes());
        return new String(cipherBytes);
    }


    /*
     * DES 解密
     */
    public static String decrypt(String data) throws Exception {
        SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode("8uPxrsRRZ78=".getBytes()), "DES");

        byte[] decode = Base64.getDecoder().decode(data);

        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] plainBytes = cipher.doFinal(decode);
        return new String(plainBytes);
    }

    //Test
    public static void main(String[] args) throws Exception {
//        byte[] desKey = DESUtil.initKey();
//        System.out.println(">>>>>>>>>>>>>> : "+new String(Base64.getEncoder().encode(desKey)));
        String key = UUID.randomUUID().toString().replace("-","").substring(0,5)+"&20200000000001";

        String encrypt = DESUtil.encrypt(key);
        System.out.println(key + ">>>DES 加密结果>>>" + encrypt);

        System.out.println(key + ">>>DES 解密结果>>>" + DESUtil.decrypt(encrypt));

    }
}