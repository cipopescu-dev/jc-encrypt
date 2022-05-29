package com.cipdev.jcapp;

import com.cipdev.jcapp.console.ClientInterface;
import com.cipdev.jcapp.console.ClientOpts;
import com.cipdev.jcapp.util.AesAppletService;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class App {

    public static void main(String[] args) {
        ClientOpts clientOpts = ClientInterface.getClientOptions(args);

        AesAppletService.initializeConnection(clientOpts.getHost(), clientOpts.getPort());
        AesAppletService.selectAesApplet();

        AesAppletService.verifyPin(ClientInterface.getClientPassword());
        AesAppletService.initCipher(clientOpts.isModeDecrypt());

        try (FileInputStream fis = new FileInputStream(clientOpts.getInput()); FileOutputStream fos = new FileOutputStream(clientOpts.getOutput())) {
            AesAppletService.doCipher(fis, fos, clientOpts.isModeDecrypt());
        } catch (IOException e) {
            System.out.println("Files problem!" + e.getMessage());
            System.exit(1);
        }

        System.out.println("Done!");
    }
}
