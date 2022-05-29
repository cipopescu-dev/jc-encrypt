package com.cipdev.jcapp.util;

import com.sun.javacard.apduio.Apdu;
import com.sun.javacard.apduio.CadClientInterface;
import com.sun.javacard.apduio.CadDevice;
import com.sun.javacard.apduio.CadTransportException;
import org.objectweb.asm.ByteVector;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
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

    private static byte[] update(byte[] data) throws CadTransportException, IOException {
        apdu = ApduRepository.getUpdateApdu(data);
        cardInterface.exchangeApdu(apdu);
        validateSWs(apdu.getSw1Sw2(), "UPDATE");
        return apdu.getDataOut();
    }

    private static byte[] dofinal(byte[] data) throws CadTransportException, IOException {
        apdu = ApduRepository.getDoFinalApdu(data);
        cardInterface.exchangeApdu(apdu);
        validateSWs(apdu.getSw1Sw2(), "DOFINAL");
        return apdu.getDataOut();
    }

    public static void doCipher(FileInputStream fis, FileOutputStream fos, boolean isDecrypt) {
        try (BufferedInputStream bis = new BufferedInputStream(fis)) {
            byte[] inputBytes = bis.readAllBytes();
            byte[] chunk;
            int extraChunkOffset;
            if (isDecrypt) {
                extraChunkOffset = inputBytes.length - CIPHER_BLOCK_SIZE - 4;
                if (extraChunkOffset == 0 || extraChunkOffset == CIPHER_BLOCK_SIZE)
                    extraChunkOffset = 4;
                for (int i = 0; i < extraChunkOffset / CIPHER_MAX_CHUNK_SIZE; i++) {
                    chunk = Arrays.copyOfRange(inputBytes, i * CIPHER_MAX_CHUNK_SIZE - 4, (i + 1) * CIPHER_MAX_CHUNK_SIZE - CIPHER_BLOCK_SIZE);
                    fos.write(update(chunk));
                }
                int messageSize = ByteBuffer.wrap(Arrays.copyOfRange(inputBytes, 0, 4)).getInt();
                chunk = Arrays.copyOfRange(inputBytes, extraChunkOffset, inputBytes.length);
                fos.write(update(chunk), 0, inputBytes.length - (inputBytes.length - messageSize));


            } else {
                fos.write(ByteBuffer.allocate(4).putInt(inputBytes.length).array());
                for (int i = 0; i < inputBytes.length / CIPHER_MAX_CHUNK_SIZE; i++) {
                    chunk = Arrays.copyOfRange(inputBytes, i * CIPHER_MAX_CHUNK_SIZE, (i + 1) * CIPHER_MAX_CHUNK_SIZE);
                    fos.write(update(chunk));
                }
                extraChunkOffset = inputBytes.length / CIPHER_MAX_CHUNK_SIZE * CIPHER_MAX_CHUNK_SIZE;
                if (extraChunkOffset < inputBytes.length) {
                    chunk = Arrays.copyOfRange(inputBytes, extraChunkOffset, inputBytes.length);
                    fos.write(dofinal(chunk));
                }
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
                exitWithReason("AesApplet error: SW_CIPHER_CHUNK_BAD_SIZE");
            case 0x04:
                exitWithReason("AesApplet error: SW_CIPHER_CHUNK_EMPTY");
            case 0x05:
                exitWithReason("AesApplet error: SW_CIPHER_BLOCK_OVERFLOW");
        }
    }

}
