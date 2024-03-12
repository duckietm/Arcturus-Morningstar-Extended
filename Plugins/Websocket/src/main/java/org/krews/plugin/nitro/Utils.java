package org.krews.plugin.nitro;

import java.net.URI;

public class Utils {
    public static String getDomainNameFromUrl(String url) throws Exception {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    public static boolean isWhitelisted(String toCheck, String[] whitelist) {
        for(String whitelistEntry : whitelist) {
            if(whitelistEntry.startsWith("*")) {
                if(toCheck.endsWith(whitelistEntry.substring(1)) || ("." + toCheck).equals(whitelistEntry.substring(1))) return true;
            } else {
                if(toCheck.equals(whitelistEntry)) return true;
            }
        }
        return false;
    }
}
