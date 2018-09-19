package org.jruby.util;

import org.jruby.Ruby;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Security;

import static org.jruby.RubyInstanceConfig.JAVA_VERSION;

public abstract class SecurityHelper {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityHelper.class);
    private static boolean attempted = false;

    // attempt to enable unlimited-strength crypto on OracleJDK
    public static void checkCryptoRestrictions(final Ruby runtime) {
        if ( ! attempted ) {
            attempted = true;
            if ( JAVA_VERSION >= 53 ) {
                setNonRestrictedJava9();
            }
            else {
                setNonRestrictedJava8();
            }
            // NOTE: this is not 'really' enough and there's more to be done :
            // JceSecurity#defaultPolicy should add: javax.crypto.CryptoAllPermission
            //
            // ... but instead of further hacking we shall advise on installing JCE pack
            // JRuby-OpenSSL uses BC thus might not care much for un-limiting the built-in crypto provider
        }
    }

    private static boolean setNonRestrictedJava9() {
        try {
            if (Security.getProperty("crypto.policy") == null) {
                Security.setProperty("crypto.policy", "unlimited");
            }
            return true;
        }
        catch (Exception e) {
            if (LOG.isDebugEnabled()) LOG.debug("unable un-restrict jce security: ", e);
        }
        return false;
    }

    private static boolean setNonRestrictedJava8() {
        try {
            Class jceSecurity = Class.forName("javax.crypto.JceSecurity");
            Field isRestricted = jceSecurity.getDeclaredField("isRestricted");

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
        catch (ClassNotFoundException e) {
            if (LOG.isDebugEnabled()) LOG.debug("unable un-restrict jce security: " + e);
        }
        catch (Exception e) {
            if (LOG.isDebugEnabled()) LOG.debug("unable un-restrict jce security: ", e);
        }
        return false;
    }

}
