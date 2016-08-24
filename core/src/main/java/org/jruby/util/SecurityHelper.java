package org.jruby.util;

import org.jruby.Ruby;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract class SecurityHelper {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityHelper.class);

    // attempt to enable unlimited-strength crypto on OracleJDK
    public static void checkCryptoRestrictions(final Ruby runtime) {
        if ( isOracleJRE() ) {
            setNonRestricted();
            // NOTE: this is not 'really' enough and there's more to be done :
            // JceSecurity#defaultPolicy should add: javax.crypto.CryptoAllPermission
            //
            // ... but instead of further hacking we shall advise on installing JCE pack
            // JRuby-OpenSSL uses BC thus might not care much for un-limiting the built-in crypto provider
        }
    }

    private static boolean setNonRestricted() {
        try {
            Class jceSecurity = Class.forName("javax.crypto.JceSecurity");
            Field isRestricted = jceSecurity.getDeclaredField("isRestricted");
            if ( Boolean.TRUE.equals(isRestricted.get(null)) ) {
                if ( Modifier.isFinal(isRestricted.getModifiers()) ) {
                    Field modifiers = Field.class.getDeclaredField("modifiers");
                    modifiers.setAccessible(true);
                    modifiers.setInt(isRestricted, isRestricted.getModifiers() & ~Modifier.FINAL);
                }
                isRestricted.setAccessible(true);
                isRestricted.setBoolean(null, false); // isRestricted = false;
                isRestricted.setAccessible(false);
                return true;
            }
        }
        catch (ClassNotFoundException e) {
            LOG.info("unable un-restrict jce security: " + e);
        }
        catch (Exception e) {
            LOG.debug("unable un-restrict jce security: ", e);
        }
        return false;
    }

    private static boolean isOracleJRE() {
        try {
            String name = System.getProperty("java.vendor"); // "Oracle Corporation"
            if ( name == null || ! name.contains("Oracle") ) return false;
            name = System.getProperty("java.runtime.name"); // make sure we're not OpenJDK
            if ( name == null || name.contains("OpenJDK") ) return false;
            return true;
        }
        catch (SecurityException e) {
            return false;
        }
    }

}
