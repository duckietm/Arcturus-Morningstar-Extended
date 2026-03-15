package com.eu.habbo.crypto;

public class HabboEncryption {

    private final HabboRSACrypto crypto;
    private final HabboDiffieHellman diffie;

    public HabboEncryption(String e, String n, String d) {
        this.crypto = new HabboRSACrypto(e, n, d);
        this.diffie = new HabboDiffieHellman(this.crypto);
    }

    public HabboRSACrypto getCrypto() {
        return crypto;
    }

    public HabboDiffieHellman getDiffie() {
        return diffie;
    }

}
