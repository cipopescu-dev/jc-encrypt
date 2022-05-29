package com.cipdev.jcapp.util;

import com.sun.javacard.apduio.Apdu;
import com.sun.javacard.apduio.CadClientInterface;
import com.sun.javacard.apduio.CadDevice;
import com.sun.javacard.apduio.CadTransportException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class AesAppletService {
    private static Socket sock;
    private static final int CIPHER_MAX_CHUNK_SIZE = 128;
    private static final int CIPHER_BLOCK_SIZE = 16; //AES 128
    private static CadClientInterface cardInterface;

    private static Apdu apdu;

    public static void initializeConnection(String host, int port) {
        try {
            sock = new Socket(host, port);
            if (cardInterface == null)
                cardInterface = CadDevice.getCadClientInstance(CadDevice.PROTOCOL_T1, sock.getInputStream(), sock.getOutputStream());
            cardInterface.powerUp();
        } catch (IOException | CadTransportException e) {
            exitWithReason(String.format("Cannot open connection to %s:%d. %s", host, port, e.getMessage()));
        }
    }

    public static void selectAesApplet() {
        try {
            apdu = ApduRepository.getSelectApdu();
            cardInterface.exchangeApdu(apdu);
            validateSWs(apdu.getSw1Sw2(), "SELECT");
        } catch (IOException | CadTransportException e) {
            exitWithReason("Cannot select the AesApplet!" + e.getMessage());
        }
    }

    public static void verifyPin(String password) {
        try {
            apdu = ApduRepository.getVerifyPinApdu(password);
            cardInterface.exchangeApdu(apdu);
            validateSWs(apdu.getSw1Sw2(), "VERIFY");
        } catch (IOException | CadTransportException e) {
            exitWithReason("Cannot validate the password!" + e.getMessage());
        }
    }

    public static void initCipher(boolean isDecrypt) {
        try {
            apdu = ApduRepository.getInitCipherApdu(isDecrypt ? ApduRepository.AesAppletCipherMode.DECRYPT : ApduRepository.AesAppletCipherMode.ENCRYPT);
            cardInterface.exchangeApdu(apdu);
            validateSWs(apdu.getSw1Sw2(), "INIT CIPHER");
        } catch (IOException | CadTransportException e) {
            exitWithReason("Cannot init the cipher!" + e.getMessage());
        }
    }

    public static void doCipher(FileInputStream fis, FileOutputStream fos, boolean isDecrypt) {
        try {

            int fileSize = 0;
            byte[] data = fis.readNBytes(CIPHER_MAX_CHUNK_SIZE);
            byte[] finalChunk;
            while (data.length > 0) {
                fileSize += data.length;

                if (isDecrypt) {

                } else {
                    if (data.length == CIPHER_MAX_CHUNK_SIZE) {
                        apdu = ApduRepository.getUpdateApdu(data);
                        cardInterface.exchangeApdu(apdu);
                        validateSWs(apdu.getSw1Sw2(), "UPDATE");
                        fos.write(apdu.getDataOut());
                    } else {
                        byte[] chunk = Arrays.copyOfRange(data, 0, data.length - (data.length % CIPHER_BLOCK_SIZE));
                        finalChunk = Arrays.copyOfRange(data, data.length - (data.length % CIPHER_BLOCK_SIZE), data.length);

                        apdu = ApduRepository.getUpdateApdu(chunk);
                        cardInterface.exchangeApdu(apdu);
                        validateSWs(apdu.getSw1Sw2(), "UPDATE");
                        fos.write(apdu.getDataOut());

                        apdu = ApduRepository.getDoFinalApdu(finalChunk);
                        cardInterface.exchangeApdu(apdu);
                        validateSWs(apdu.getSw1Sw2(), "DOFINAL");
                        fos.write(apdu.getDataOut());

                        fos.write(fileSize); // size of message. No padding, remember?
                    }
                }

                data = fis.readNBytes(CIPHER_MAX_CHUNK_SIZE);
            }

            cardInterface.powerDown();
            sock.close();
        } catch (IOException | CadTransportException e) {
            exitWithReason("Cannot decrypt the file!" + e.getMessage());
        }
    }

    private static void exitWithReason(String reason) {
        System.out.println(reason);
        System.exit(1);
    }

    private static void validateSWs(byte[] sw1sw2, String traceApdu) {
        if (sw1sw2[0] == (byte) 0x90 && sw1sw2[1] == (byte) 0x00) return;
        if (sw1sw2[0] != 0x63)
            exitWithReason(String.format("Unknown APDU response! %s SW1:%02X SW2:0%02X", traceApdu, sw1sw2[0], sw1sw2[1]));
        switch (sw1sw2[1]) {
            case 0x00:
                exitWithReason("AesApplet error: SW_PIN_VERIFICATION_FAILED");
            case 0x01:
                exitWithReason("AesApplet error: SW_PIN_VERIFICATION_REQUIRED");
            case 0x02:
                exitWithReason("AesApplet error: SW_CIPHER_NOMODESET");
            case 0x03:
                exitWithReason("AesApplet error: SW_CIPHER_BLOCK_NOALLIGN");
            case 0x04:
                exitWithReason("AesApplet error: SW_CIPHER_BLOCK_NEEDUPDATE");
            case 0x05:
                exitWithReason("AesApplet error: SW_CIPHER_BLOCK_OVERFLOW");
        }
    }

}
