package org.exoplatform.platform.am.utils

import java.io.*
import java.nio.file.Files
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


 class FileEncrypt {

     private static final Logger LOG = Logger.getInstance()

     void processFile(Cipher ci, InputStream inputStream, OutputStream outputStream) throws Exception {
         byte[] ibuf = new byte[1024];
         int len;
         while ((len = inputStream.read(ibuf)) != -1) {
             byte[] obuf = ci.update(ibuf, 0, len);
             if (obuf != null) {
                 outputStream.write(obuf);
             }
         }
         byte[] obuf = ci.doFinal();
         if (obuf != null) {
             outputStream.write(obuf);
         }
     }

      boolean decryptFile(File publicKeyFile, File inputFile) {
         try {

             byte[] bytes = Files.readAllBytes(publicKeyFile.toPath())
             X509EncodedKeySpec ks = new X509EncodedKeySpec(bytes)
             KeyFactory kf = KeyFactory.getInstance("RSA")
             PublicKey pub = kf.generatePublic(ks)

                 FileInputStream fileInputStream = new FileInputStream(inputFile)

                     Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
                     cipher.init(Cipher.DECRYPT_MODE, pub)
                     byte[] b = new byte[256]
                     fileInputStream.read(b)
                     byte[] keyb = cipher.doFinal(b)
                     SecretKeySpec skey = new SecretKeySpec(keyb, "AES")


                 byte[] iv = new byte[128 / 8]
                 fileInputStream.read(iv)
                 IvParameterSpec ivspec = new IvParameterSpec(iv)

                 Cipher ci = Cipher.getInstance("AES/CBC/PKCS5Padding")
                 ci.init(Cipher.DECRYPT_MODE, skey, ivspec)

                 FileOutputStream out = new FileOutputStream(inputFile.getAbsolutePath().replaceAll(".enc\$", ""))
                 processFile(ci, fileInputStream, out)


         } catch (Exception ex) {
             LOG.error("Could not decrypt ${inputFile.getAbsolutePath()} File",ex)
             return false
         }
         return new File(inputFile.getAbsolutePath().replaceAll(".enc\$", "")).exists()
     }

}

