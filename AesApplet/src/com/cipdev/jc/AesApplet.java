package com.cipdev.jc;

import javacard.framework.*;
import javacard.security.AESKey;
import javacard.security.KeyBuilder;
import javacardx.annotations.*;
import javacardx.crypto.Cipher;

import static com.cipdev.jc.AesAppletStrings.*;

/**
 * Applet class
 * 
 * @author Stefan-Ciprian Popescu
 *
 */
@StringPool(value = {
        @StringDef(name = "Package", value = "com.cipdev.jc"),
        @StringDef(name = "AppletName", value = "AesApplet") }, name = "AesAppletStrings")
public class AesApplet extends Applet {
    final static byte CLA_AES_APPLET = (byte) 0x80;

    final static byte INS_VERIFY = (byte) 0x20;
    final static byte INS_INIT = (byte) 0x30;
    final static byte INS_UPDATE = (byte) 0x40;
    final static byte INS_DOFINAL = (byte) 0x50;

    final static byte P1_MODE_ENCRYPT_CIPHER = (byte) 0x01;
    final static byte P1_MODE_DECRYPT_CIPHER = (byte) 0x02;

    final static short SW_PIN_VERIFICATION_FAILED = 0x6300;
    final static short SW_PIN_VERIFICATION_REQUIRED = 0x6301;
    final static short SW_CIPHER_NOMODESET = 0x6302;
    final static short SW_CIPHER_CHUNK_BAD_SIZE = 0x6303;
    final static short SW_CIPHER_CHUNK_EMPTY = 0x6304;
    final static short SW_CIPHER_CHUNK_OVERFLOW = 0x6305;

    final static byte PIN_MAX_SIZE = (byte) 0x0A;
    final static byte PIN_TRY_LIMIT = (byte) 0x03;
    final static short CIPHER_MAX_BLOCK_SIZE = 0x0080;
    final static short CIPHER_CHUNK_SIZE = 0x0010;

    OwnerPIN pin;
    AESKey key;
    Cipher cipher;
    byte cipherMode;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new AesApplet(bArray, bOffset);
    }

    protected AesApplet(byte[] bArray, short bOffset) {
        bOffset -= 1;
        pin = new OwnerPIN(PIN_TRY_LIMIT, PIN_MAX_SIZE);
        byte pinLength = bArray[bOffset++];
        pin.update(bArray, bOffset, pinLength);

        bOffset += pinLength;

        key = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, true);
        key.setKey(bArray, ++bOffset);

        cipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
        cipherMode = 0x00;
        register();
    }

    @Override
    public void process(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        buffer[ISO7816.OFFSET_CLA] = (byte) (buffer[ISO7816.OFFSET_CLA] & (byte) 0xFC);
        if ((buffer[ISO7816.OFFSET_CLA] == 0) && (buffer[ISO7816.OFFSET_INS] == (byte) (0xA4)))
            return;
        if (buffer[ISO7816.OFFSET_CLA] != CLA_AES_APPLET)
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        switch (buffer[ISO7816.OFFSET_INS]) {
            case INS_INIT:
                init(apdu);
                return;
            case INS_UPDATE:
                update(apdu);
                return;
            case INS_DOFINAL:
                dofinal(apdu);
                return;
            case INS_VERIFY:
                verify(apdu);
                return;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    @Override
    public boolean select() {
        if (pin.getTriesRemaining() == 0)
            return false;
        return true;
    }

    @Override
    public void deselect() {
        pin.reset();
        cipherMode = 0x00;
    }

    private void init(APDU apdu) {
        if (!pin.isValidated())
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        byte[] buffer = apdu.getBuffer();
        switch (buffer[ISO7816.OFFSET_P1]) {
            case P1_MODE_ENCRYPT_CIPHER:
                cipher.init(key, Cipher.MODE_ENCRYPT);
                cipherMode = Cipher.MODE_ENCRYPT;
                return;
            case P1_MODE_DECRYPT_CIPHER:
                cipher.init(key, Cipher.MODE_DECRYPT);
                cipherMode = Cipher.MODE_DECRYPT;
                return;
            default:
                ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }

    }

    private void update(APDU apdu) {
        if (!pin.isValidated())
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        if (cipherMode == 0)
            ISOException.throwIt(SW_CIPHER_NOMODESET);
        byte[] buffer = apdu.getBuffer();
        short dataSize = apdu.setIncomingAndReceive();
        if (dataSize == 0)
            ISOException.throwIt(SW_CIPHER_CHUNK_EMPTY);
        if (dataSize % CIPHER_CHUNK_SIZE != 0)
            ISOException.throwIt(SW_CIPHER_CHUNK_BAD_SIZE);
        if (dataSize > CIPHER_MAX_BLOCK_SIZE)
            ISOException.throwIt(SW_CIPHER_CHUNK_OVERFLOW);
        for (short i = 0; i < dataSize / CIPHER_CHUNK_SIZE; i++) {
            cipher.update(buffer, (short) (i * CIPHER_CHUNK_SIZE + ISO7816.OFFSET_CDATA), (short) CIPHER_CHUNK_SIZE,
                    buffer,
                    (short) (i * CIPHER_CHUNK_SIZE + ISO7816.OFFSET_CDATA));
        }
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, dataSize);

    }

    private void dofinal(APDU apdu) {
        if (!pin.isValidated())
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        if (cipherMode == 0)
            ISOException.throwIt(SW_CIPHER_NOMODESET);
        byte[] buffer = apdu.getBuffer();
        byte dataSize = (byte) (apdu.setIncomingAndReceive());
        if (dataSize == 0)
            ISOException.throwIt(SW_CIPHER_CHUNK_EMPTY);
        if (dataSize > CIPHER_MAX_BLOCK_SIZE)
            ISOException.throwIt(SW_CIPHER_CHUNK_OVERFLOW);
        short returnedDataSize;
        if (dataSize < CIPHER_CHUNK_SIZE)
            returnedDataSize = CIPHER_CHUNK_SIZE;
        else
            returnedDataSize = (short) (dataSize + (short) (CIPHER_CHUNK_SIZE - dataSize % CIPHER_CHUNK_SIZE));
        for (short i = (short) (ISO7816.OFFSET_CDATA + dataSize); i < returnedDataSize; i++)
            buffer[i] = 0x00;
        buffer[ISO7816.OFFSET_LC] = (byte) returnedDataSize;
        buffer[(short) (ISO7816.OFFSET_CDATA + returnedDataSize)] = 0x7F;
        cipher.doFinal(buffer, ISO7816.OFFSET_CDATA, returnedDataSize, buffer, ISO7816.OFFSET_CDATA);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, returnedDataSize);
    }

    private void verify(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte byteRead = (byte) (apdu.setIncomingAndReceive());
        if (pin.check(buffer, ISO7816.OFFSET_CDATA, byteRead) == false)
            ISOException.throwIt(SW_PIN_VERIFICATION_FAILED);
    }
}
