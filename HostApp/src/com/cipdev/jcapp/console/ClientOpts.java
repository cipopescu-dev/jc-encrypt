/*
 * Copyright (c) 2022. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.cipdev.jcapp.console;

public class ClientOpts {
    String input;
    String output;
    boolean modeDecrypt;
    String host;
    int port;

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public boolean isModeDecrypt() {
        return modeDecrypt;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public static final class ClientOptsBuilder {
        String input;
        String output;
        boolean mode;
        String host;
        int port;

        private ClientOptsBuilder() {
        }

        public static ClientOptsBuilder aClientOpts() {
            return new ClientOptsBuilder();
        }

        public ClientOptsBuilder withInput(String input) {
            this.input = input;
            return this;
        }

        public ClientOptsBuilder withOutput(String output) {
            this.output = output;
            return this;
        }

        public ClientOptsBuilder withMode(boolean mode) {
            this.mode = mode;
            return this;
        }

        public ClientOptsBuilder withHost(String host) {
            this.host = host;
            return this;
        }

        public ClientOptsBuilder withPort(int port) {
            this.port = port;
            return this;
        }

        public ClientOpts build() {
            ClientOpts clientOpts = new ClientOpts();
            clientOpts.output = this.output;
            clientOpts.modeDecrypt = this.mode;
            clientOpts.host = this.host;
            clientOpts.port = this.port;
            clientOpts.input = this.input;
            return clientOpts;
        }
    }
}
