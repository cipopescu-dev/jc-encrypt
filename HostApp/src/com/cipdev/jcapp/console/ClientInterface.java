/*
 * Copyright (c) 2022. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.cipdev.jcapp.console;

import org.apache.commons.cli.*;

import java.io.Console;
import java.util.Scanner;

public class ClientInterface {
    private static ClientOpts clientOpts;
    private static Scanner scanner;

    public static ClientOpts getClientOptions(String[] args) {
        Options options = new Options();

        Option input = new Option("i", "input", true, "Input file path. Will be send to the device running the JCApplet");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("o", "output", true, "Output file path. Will store the result of the device running the JCApplet process. Defaults to \"output.enc\"");
        output.setRequired(false);
        options.addOption(output);

        Option mode = new Option("d", "decrypt", false, "Decrypt the give input file. If not given, will default to encryption mode");
        mode.setRequired(false);
        options.addOption(mode);

        Option host = new Option("h", "host", true, "Host of the device running the JCApplet. Defaults to \"localhost\"");
        host.setRequired(false);
        options.addOption(host);

        Option port = new Option("p", "port", true, "Port of the device running the JCApplet. Defaults to \"9025\"");
        port.setRequired(false);
        options.addOption(port);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);
            clientOpts = ClientOpts.ClientOptsBuilder.aClientOpts()
                    .withInput(cmd.getOptionValue("input"))
                    .withOutput(cmd.getOptionValue("output", "output.enc"))
                    .withMode(cmd.hasOption("d"))
                    .withHost(cmd.getOptionValue("host", "localhost"))
                    .withPort(Integer.parseInt(cmd.getOptionValue("port", "9025")))
                    .build();
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java com.cipdev.jcapp.App -i inputFile", options);
            System.exit(1);
        }
        return clientOpts;
    }

    public static String getClientPassword() {
        if (scanner == null)
            scanner = new Scanner(System.in);
        System.out.print("Applet password:");
        return scanner.nextLine();
    }
}
