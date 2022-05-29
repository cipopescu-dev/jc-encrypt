package com.cipdev.jcapp.util;

import com.sun.javacard.apduio.Apdu;

import java.nio.charset.StandardCharsets;

public class ApduRepository {

    private static Apdu SELECT_APDU;
    private static Apdu VERIFY_PIN_APDU;
    private static Apdu INIT_CIPHER_APDU;
    private static Apdu UPDATE_APDU;
    private static Apdu DOFINAL_APDU;

    private static final byte[] COMMAND_SELECT_APPLET = new byte[]{(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00};
    private static final byte[] COMMAND_VERIFY_PIN = new byte[]{(byte) 0x80, 0x20, 0x00, 0x00};
    private static final byte[] COMMAND_INIT_ENCRYPT = new byte[]{(byte) 0x80, 0x30, 0x01, 0x00};
    private static final byte[] COMMAND_INIT_DECRYPT = new byte[]{(byte) 0x80, 0x30, 0x02, 0x00};
    private static final byte[] COMMAND_UPDATE = new byte[]{(byte) 0x80, 0x40, 0x00, 0x00};
    private static final byte[] COMMAND_DOFINAL = new byte[]{(byte) 0x80, 0x50, 0x00, 0x00};

    private static final byte[] DATA_SELECT_APPLET = new byte[]{(byte) 0xA1, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5, (byte) 0x01, (byte) 0x01};
    private static final byte[] DATA_INIT_CIPHER = new byte[]{};

    private static final byte DEFAULT_LE = 0x7F;

    public enum AesAppletCipherMode {
        ENCRYPT, DECRYPT
    }


    public static Apdu getSelectApdu() {
        if (SELECT_APDU == null) {
            SELECT_APDU = new Apdu();
            SELECT_APDU.command = COMMAND_SELECT_APPLET;
            SELECT_APDU.setDataIn(DATA_SELECT_APPLET, DATA_SELECT_APPLET.length);
            SELECT_APDU.Le = DEFAULT_LE;
        }
        return SELECT_APDU;
    }

    public static Apdu getVerifyPinApdu(String password) {
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        if (VERIFY_PIN_APDU == null) {
            VERIFY_PIN_APDU = new Apdu();
            VERIFY_PIN_APDU.command = COMMAND_VERIFY_PIN;
            VERIFY_PIN_APDU.Le = DEFAULT_LE;
        }
        VERIFY_PIN_APDU.setDataIn(passwordBytes, passwordBytes.length);
        return VERIFY_PIN_APDU;
    }

    public static Apdu getInitCipherApdu(AesAppletCipherMode mode) {
        if (INIT_CIPHER_APDU == null) {
            INIT_CIPHER_APDU = new Apdu();
            INIT_CIPHER_APDU.setDataIn(DATA_INIT_CIPHER, DATA_INIT_CIPHER.length);
            INIT_CIPHER_APDU.Le = DEFAULT_LE;
        }
        switch (mode) {
            case ENCRYPT -> INIT_CIPHER_APDU.command = COMMAND_INIT_ENCRYPT;
            case DECRYPT -> INIT_CIPHER_APDU.command = COMMAND_INIT_DECRYPT;
        }
        return INIT_CIPHER_APDU;
    }

    public static Apdu getUpdateApdu(byte[] data) {
        if (UPDATE_APDU == null) {
            UPDATE_APDU = new Apdu();
            UPDATE_APDU.command = COMMAND_UPDATE;
            UPDATE_APDU.Le = DEFAULT_LE;
        }
        UPDATE_APDU.setDataIn(data, data.length);
        return UPDATE_APDU;
    }

    public static Apdu getDoFinalApdu(byte[] data) {
        if (DOFINAL_APDU == null) {
            DOFINAL_APDU = new Apdu();
            DOFINAL_APDU.command = COMMAND_DOFINAL;
            DOFINAL_APDU.Le = DEFAULT_LE;
        }
        DOFINAL_APDU.setDataIn(data, data.length);
        return DOFINAL_APDU;
    }
}
