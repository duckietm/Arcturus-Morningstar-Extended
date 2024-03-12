package org.krews.plugin.nitro.websockets.ssl;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class SSLCertificateLoader {
    private static final String filePath = "ssl";
    private static final Logger LOGGER = LoggerFactory.getLogger(SSLCertificateLoader.class);

    public static SslContext getContext() {
        SslContext context;
        try {
            context = SslContextBuilder.forServer(new File( filePath + File.separator + "cert.pem" ), new File( filePath + File.separator + "privkey.pem" )).build();
        } catch ( Exception e ) {
            LOGGER.info("Unable to load ssl: " + e.getMessage());
            context = null;
        }
        return context;
    }
}
