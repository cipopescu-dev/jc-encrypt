package com.cipdev.jc;

import javacard.framework.*;
import javacardx.annotations.*;

//import static com.cipdev.jc.JcEcpsAppletStrings.*;

@StringPool(value = {
        @StringDef(name = "Package", value = "com.cipdev.jc"),
        @StringDef(name = "AppletName", value = "AesApplet")
}, name = "AesAppletStrings")
public class AesApplet extends Applet {

    // code of CLA byte in the command APDU header
    final static byte Wallet_CLA = (byte) 0x80;

    // codes of INS byte in the command APDU header
    final static byte VERIFY = (byte) 0x20;
    final static byte CREDIT = (byte) 0x30;
    final static byte DEBIT = (byte) 0x40;
    final static byte GET_BALANCE = (byte) 0x50;

    // maximum balance
    final static short MAX_BALANCE = 0x7FFF;
    // maximum transaction amount
    final static byte MAX_TRANSACTION_AMOUNT = 127;

    // maximum number of incorrect tries before the
    // PIN is blocked
    final static byte PIN_TRY_LIMIT = (byte) 0x03;
    // maximum size PIN
    final static byte MAX_PIN_SIZE = (byte) 0x08;

    // signal that the PIN verification failed
    final static short SW_VERIFICATION_FAILED = 0x6300;
    // signal the the PIN validation is required
    // for a credit or a debit transaction
    final static short SW_PIN_VERIFICATION_REQUIRED = 0x6301;
    // signal invalid transaction amount
    // amount > MAX_TRANSACTION_AMOUNT or amount < 0
    final static short SW_INVALID_TRANSACTION_AMOUNT = 0x6A83;

    // signal that the balance exceed the maximum
    final static short SW_EXCEED_MAXIMUM_BALANCE = 0x6A84;
    // signal the the balance becomes negative
    final static short SW_NEGATIVE_BALANCE = 0x6A85;

    OwnerPIN pin;
    short balance;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new AesApplet(bArray, bOffset, bLength);
    }

    private AesApplet(byte[] bArray, short bOffset, byte bLength) {

        // It is good programming practice to allocate
        // all the memory that an applet needs during
        // its lifetime inside the constructor
        pin = new OwnerPIN(PIN_TRY_LIMIT, MAX_PIN_SIZE);

        byte iLen = bArray[bOffset]; // aid length
        bOffset = (short) (bOffset + iLen + 1);
        byte cLen = bArray[bOffset]; // info length
        bOffset = (short) (bOffset + cLen + 1);
        byte aLen = bArray[bOffset]; // applet data length

        // The installation parameters contain the PIN
        // initialization value
        pin.update(bArray, (short) (bOffset + 1), aLen);
        register();

    }

    // protected JcEcpsApplet() {
    // register();
    // }
    @Override
    public boolean select() {
        if (pin.getTriesRemaining() == 0)
            return false;
        return true;
    }

    @Override
    public void deselect() {
        pin.reset();

    }

    @Override
    public void process(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        buffer[ISO7816.OFFSET_CLA] = (byte) (buffer[ISO7816.OFFSET_CLA] & (byte) 0xFC);
        if ((buffer[ISO7816.OFFSET_CLA] == 0) && (buffer[ISO7816.OFFSET_INS] == (byte) (0xA4)))
            return;
        if (buffer[ISO7816.OFFSET_CLA] != Wallet_CLA)
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        switch (buffer[ISO7816.OFFSET_INS]) {
            case GET_BALANCE:
                getBalance(apdu);
                return;
            case DEBIT:
                debit(apdu);
                return;
            case CREDIT:
                credit(apdu);
                return;
            case VERIFY:
                verify(apdu);
                return;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void credit(APDU apdu) {
        if (!pin.isValidated())
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        byte[] buffer = apdu.getBuffer();
        byte numBytes = buffer[ISO7816.OFFSET_LC];
        byte byteRead = (byte) (apdu.setIncomingAndReceive());
        if ((numBytes != 1) || (byteRead != 1))
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        byte creditAmount = buffer[ISO7816.OFFSET_CDATA];
        if ((creditAmount > MAX_TRANSACTION_AMOUNT) || (creditAmount < 0))
            ISOException.throwIt(SW_INVALID_TRANSACTION_AMOUNT);
        if ((short) (balance + creditAmount) > MAX_BALANCE)
            ISOException.throwIt(SW_EXCEED_MAXIMUM_BALANCE);
        balance = (short) (balance + creditAmount);
    }

    private void debit(APDU apdu) {
        if (!pin.isValidated())
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        byte[] buffer = apdu.getBuffer();
        byte numBytes = (byte) (buffer[ISO7816.OFFSET_LC]);
        byte byteRead = (byte) (apdu.setIncomingAndReceive());
        if ((numBytes != 1) || (byteRead != 1))
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        byte debitAmount = buffer[ISO7816.OFFSET_CDATA];
        if ((debitAmount > MAX_TRANSACTION_AMOUNT) || (debitAmount < 0))
            ISOException.throwIt(SW_INVALID_TRANSACTION_AMOUNT);
        if ((short) (balance - debitAmount) < (short) 0)
            ISOException.throwIt(SW_NEGATIVE_BALANCE);
        balance = (short) (balance - debitAmount);
    }

    private void getBalance(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short le = apdu.setOutgoing();
        if (le < 2)
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        apdu.setOutgoingLength((byte) 2);
        buffer[0] = (byte) (balance >> 8);
        buffer[1] = (byte) (balance & 0xFF);
        apdu.sendBytes((short) 0, (short) 2);

    }

    private void verify(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte byteRead = (byte) (apdu.setIncomingAndReceive());
        if (pin.check(buffer, ISO7816.OFFSET_CDATA, byteRead) == false)
            ISOException.throwIt(SW_VERIFICATION_FAILED);

    }
}
