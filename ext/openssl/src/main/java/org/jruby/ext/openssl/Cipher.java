/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006, 2007 Ola Bini <ola@ologix.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.openssl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;
import org.jruby.util.ByteList;

import static org.jruby.ext.openssl.OpenSSLReal.isDebug;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Cipher extends RubyObject {
    private static final long serialVersionUID = 7727377435222646536L;

    private static ObjectAllocator CIPHER_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new Cipher(runtime, klass);
        }
    };

    public static void createCipher(final Ruby runtime, final RubyModule mOSSL) {
        RubyClass cCipher = mOSSL.defineClassUnder("Cipher", runtime.getObject(), CIPHER_ALLOCATOR);
        cCipher.defineAnnotatedMethods(Cipher.class);
        cCipher.defineAnnotatedMethods(CipherModule.class);
        RubyClass openSSLError = mOSSL.getClass("OpenSSLError");
        cCipher.defineClassUnder("CipherError", openSSLError, openSSLError.getAllocator());
    }

    public static boolean isSupportedCipher(final String name) {
        return CipherModule.isSupportedCipher(name);
    }

    @JRubyModule(name = "OpenSSL::Cipher")
    public static class CipherModule {

        @JRubyMethod(meta = true)
        public static IRubyObject ciphers(final ThreadContext context, final IRubyObject self) {
            initializeCiphers();
            final Ruby runtime = context.runtime;

            List<IRubyObject> result = new ArrayList<IRubyObject>(CIPHERS.size() * 2);
            for (String cipher : CIPHERS) {
                result.add( runtime.newString(cipher) );
                result.add( runtime.newString(cipher.toLowerCase()) );
            }
            return runtime.newArray(result);
        }

        private static boolean isSupportedCipher(final String name) {
            initializeCiphers();
            return CIPHERS.contains( name.toUpperCase() );
        }

        private static boolean initialized = false;
        static final List<String> CIPHERS = new ArrayList<String>();

        private static void initializeCiphers() {
            if ( initialized ) return;
            synchronized (CIPHERS) {
                if ( initialized ) return;
                final String[] other = {
                    "AES128", "AES192", "AES256",
                    "BLOWFISH",
                    "RC2-40-CBC", "RC2-64-CBC",
                    "RC4", "RC4-40",
                    "CAST", "CAST-CBC"
                };
                final String[] bases = {
                    "AES-128", "AES-192", "AES-256",
                    "BF", "DES", "DES-EDE", "DES-EDE3",
                    "RC2", "CAST5"
                };
                final String[] suffixes = {
                    "", "-CBC", "-CFB", "-CFB1", "-CFB8", "-ECB", "-OFB"
                };
                for ( int i = 0; i < bases.length; i++ ) {
                    for ( int k = 0; k < suffixes.length; k++ ) {
                        final String cipher;
                        if ( tryCipher( cipher = bases[i] + suffixes[k]) ) {
                            CIPHERS.add( cipher.toUpperCase() );
                        }
                    }
                }
                for ( int i = 0; i < other.length; i++ ) {
                    final String cipher = other[i];
                    if ( tryCipher( cipher ) ) {
                        CIPHERS.add( cipher.toUpperCase() );
                    }
                }
                initialized = true;
            }
        }
    }

    public static class Algorithm {

        private static final Set<String> BLOCK_MODES;

        static {
            BLOCK_MODES = new HashSet<String>();

            BLOCK_MODES.add("CBC");
            BLOCK_MODES.add("CFB");
            BLOCK_MODES.add("CFB1");
            BLOCK_MODES.add("CFB8");
            BLOCK_MODES.add("ECB");
            BLOCK_MODES.add("OFB");
        }

        public static String jsseToOssl(String inName, int keyLen) {
            String cryptoBase;
            String cryptoVersion = null;
            String cryptoMode = null;
            String[] parts = inName.split("/");
            if (parts.length != 1 && parts.length != 3) {
                return null;
            }
            cryptoBase = parts[0];
            if (parts.length > 2) {
                cryptoMode = parts[1];
                // padding: parts[2] is not used
            }
            if (!BLOCK_MODES.contains(cryptoMode)) {
                cryptoVersion = cryptoMode;
                cryptoMode = "CBC";
            }
            if (cryptoMode == null) {
                cryptoMode = "CBC";
            }
            if (cryptoBase.equals("DESede")) {
                cryptoBase = "DES";
                cryptoVersion = "EDE3";
            } else if (cryptoBase.equals("Blowfish")) {
                cryptoBase = "BF";
            }
            if (cryptoVersion == null) {
                cryptoVersion = String.valueOf(keyLen);
            }
            return cryptoBase + "-" + cryptoVersion + "-" + cryptoMode;
        }

        public static String getAlgorithmBase(javax.crypto.Cipher cipher) {
            String algoBase = cipher.getAlgorithm();
            if (algoBase.indexOf('/') != -1) {
                algoBase = algoBase.split("/")[0];
            }
            return algoBase;
        }

        public static String[] osslToJsse(String inName) {
            // assume PKCS5Padding
            return osslToJsse(inName, null);
        }

        public static String[] osslToJsse(String inName, String padding) {
            String[] split = inName.split("-");
            String cryptoBase = split[0];
            String cryptoVersion = null;
            String cryptoMode;
            String realName;

            String paddingType;
            if (padding == null || padding.equalsIgnoreCase("PKCS5Padding")) {
                paddingType = "PKCS5Padding";
            } else if (padding.equals("0") || padding.equalsIgnoreCase("NoPadding")) {
                paddingType = "NoPadding";
            } else if (padding.equalsIgnoreCase("ISO10126Padding")) {
                paddingType = "ISO10126Padding";
            } else {
                paddingType = "PKCS5Padding";
            }

            if ("bf".equalsIgnoreCase(cryptoBase)) {
                cryptoBase = "Blowfish";
            }

            if (split.length == 3) {
                cryptoVersion = split[1];
                cryptoMode = split[2];
            } else if (split.length == 2) {
                cryptoMode = split[1];
            } else {
                cryptoMode = "CBC";
            }

            if (cryptoBase.equalsIgnoreCase("CAST")) {
                realName = "CAST5";
            } else if (cryptoBase.equalsIgnoreCase("DES") && "EDE3".equalsIgnoreCase(cryptoVersion)) {
                realName = "DESede";
            } else {
                realName = cryptoBase;
            }

            if (!BLOCK_MODES.contains(cryptoMode.toUpperCase())) {
                cryptoVersion = cryptoMode;
                cryptoMode = "CBC";
            } else if (cryptoMode.equalsIgnoreCase("CFB1")) {
                // uglish SunJCE cryptoMode normalization.
                cryptoMode = "CFB";
            }

            if (realName.equalsIgnoreCase("RC4")) {
                realName = "RC4";
                cryptoMode = "NONE";
                paddingType = "NoPadding";
            } else {
                realName = realName + "/" + cryptoMode + "/" + paddingType;
            }

            return new String[]{cryptoBase, cryptoVersion, cryptoMode, realName, paddingType};
        }

        public static int[] osslKeyIvLength(String name) {
            String[] values = Algorithm.osslToJsse(name);
            String cryptoBase = values[0];
            String cryptoVersion = values[1];
            //String cryptoMode = values[2];
            //String realName = values[3];

            int keyLen = -1;
            int ivLen = -1;

            if (hasLen(cryptoBase) && null != cryptoVersion) {
                try {
                    keyLen = Integer.parseInt(cryptoVersion) / 8;
                } catch (NumberFormatException e) {
                    keyLen = -1;
                }
            }
            if (keyLen == -1) {
                if ("DES".equalsIgnoreCase(cryptoBase)) {
                    ivLen = 8;
                    if ("EDE3".equalsIgnoreCase(cryptoVersion)) {
                        keyLen = 24;
                    } else {
                        keyLen = 8;
                    }
                } else if ("RC4".equalsIgnoreCase(cryptoBase)) {
                    ivLen = 0;
                    keyLen = 16;
                } else {
                    keyLen = 16;
                    try {
                        int maxLen = javax.crypto.Cipher.getMaxAllowedKeyLength(name) / 8;
                        if (maxLen < keyLen) keyLen = maxLen;
                    }
                    catch (NoSuchAlgorithmException e) { }
                }
            }

            if (ivLen == -1) {
                if ("AES".equalsIgnoreCase(cryptoBase)) {
                    ivLen = 16;
                } else {
                    ivLen = 8;
                }
            }
            return new int[] { keyLen, ivLen };
        }

        public static boolean hasLen(String cryptoBase) {
            return "AES".equalsIgnoreCase(cryptoBase) || "RC2".equalsIgnoreCase(cryptoBase) || "RC4".equalsIgnoreCase(cryptoBase);
        }
    }

    private static boolean tryCipher(final String rubyName) {
        String transformation = Algorithm.osslToJsse(rubyName, null)[3];
        try {
            return getCipher(transformation, true) != null;
        }
        catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static javax.crypto.Cipher getCipher(final String transformation, boolean silent)
        throws NoSuchAlgorithmException, NoSuchPaddingException {
        try {
            return SecurityHelper.getCipher(transformation); // tries BC if it's available
        }
        catch (NoSuchAlgorithmException e) {
            if ( silent ) return null;
            throw e;
        }
        catch (NoSuchPaddingException e) {
            if ( silent ) return null;
            throw e;
        }
    }

    public Cipher(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    private javax.crypto.Cipher cipher;
    private String name;
    private String cryptoBase;
    private String cryptoVersion;
    private String cryptoMode;
    private String padding_type;
    private String realName;
    private int keyLen = -1;
    private int generateKeyLen = -1;
    private int ivLen = -1;
    private boolean encryptMode = true;
    //private IRubyObject[] modeParams;
    private boolean ciphInited = false;
    private byte[] key;
    private byte[] realIV;
    private byte[] orgIV;
    private String padding;

    private void dumpVars(final PrintStream out) {
        out.println("***** Cipher instance vars ****");
        out.println("name = " + name);
        out.println("cryptoBase = " + cryptoBase);
        out.println("cryptoVersion = " + cryptoVersion);
        out.println("cryptoMode = " + cryptoMode);
        out.println("padding_type = " + padding_type);
        out.println("realName = " + realName);
        out.println("keyLen = " + keyLen);
        out.println("ivLen = " + ivLen);
        out.println("ciph block size = " + cipher.getBlockSize());
        out.println("encryptMode = " + encryptMode);
        out.println("ciphInited = " + ciphInited);
        out.println("key.length = " + (key == null ? 0 : key.length));
        out.println("iv.length = " + (this.realIV == null ? 0 : this.realIV.length));
        out.println("padding = " + padding);
        out.println("ciphAlgo = " + cipher.getAlgorithm());
        out.println("*******************************");
    }

    @JRubyMethod(required = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(final ThreadContext context, final IRubyObject name) {
        final String nameStr = name.toString();
        if ( ! CipherModule.isSupportedCipher(nameStr) ) {
            throw newCipherError(context.runtime, String.format("unsupported cipher algorithm (%s)", nameStr));
        }
        if (cipher != null) {
            throw context.runtime.newRuntimeError("Cipher already inititalized!");
        }
        updateCipher(nameStr, padding);
        return this;
    }

    @Override
    @JRubyMethod(required = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(final IRubyObject obj) {
        if (this == obj) return this;

        checkFrozen();

        final Cipher other = (Cipher) obj;
        cryptoBase = other.cryptoBase;
        cryptoVersion = other.cryptoVersion;
        cryptoMode = other.cryptoMode;
        padding_type = other.padding_type;
        realName = other.realName;
        name = other.name;
        keyLen = other.keyLen;
        ivLen = other.ivLen;
        encryptMode = other.encryptMode;
        ciphInited = false;
        if ( other.key != null ) {
            key = new byte[other.key.length];
            System.arraycopy(other.key, 0, key, 0, key.length);
        } else {
            key = null;
        }
        if (other.realIV != null) {
            this.realIV = new byte[other.realIV.length];
            System.arraycopy(other.realIV, 0, this.realIV, 0, this.realIV.length);
        } else {
            this.realIV = null;
        }
        this.orgIV = this.realIV;
        padding = other.padding;

        cipher = getCipher();

        return this;
    }

    @JRubyMethod
    public IRubyObject name() {
        return getRuntime().newString(name);
    }

    @JRubyMethod
    public IRubyObject key_len() {
        return getRuntime().newFixnum(keyLen);
    }

    @JRubyMethod
    public IRubyObject iv_len() {
        return getRuntime().newFixnum(ivLen);
    }

    @JRubyMethod(name = "key_len=", required = 1)
    public IRubyObject set_key_len(IRubyObject len) {
        this.keyLen = RubyNumeric.fix2int(len);
        return len;
    }

    @JRubyMethod(name = "key=", required = 1)
    public IRubyObject set_key(final ThreadContext context, final IRubyObject key) {
        byte[] keyBytes;
        try {
            keyBytes = key.convertToString().getBytes();
        }
        catch (Exception e) {
            final Ruby runtime = context.runtime;
            if ( isDebug(runtime) ) e.printStackTrace( runtime.getOut() );
            throw newCipherError(runtime, e.getMessage());
        }
        if (keyBytes.length < keyLen) {
            throw newCipherError(context.runtime, "key length too short");
        }

        if (keyBytes.length > keyLen) {
            byte[] keys = new byte[keyLen];
            System.arraycopy(keyBytes, 0, keys, 0, keyLen);
            keyBytes = keys;
        }

        this.key = keyBytes;
        return key;
    }

    @JRubyMethod(name = "iv=", required = 1)
    public IRubyObject set_iv(final ThreadContext context, final IRubyObject iv) {
        byte[] ivBytes;
        try {
            ivBytes = iv.convertToString().getBytes();
        }
        catch (Exception e) {
            final Ruby runtime = context.runtime;
            if ( isDebug(runtime) ) e.printStackTrace( runtime.getOut() );
            throw newCipherError(getRuntime(), e.getMessage());
        }
        if (ivBytes.length < ivLen) {
            throw newCipherError(getRuntime(), "iv length to short");
        } else {
            // EVP_CipherInit_ex uses leading IV length of given sequence.
            byte[] iv2 = new byte[ivLen];
            System.arraycopy(ivBytes, 0, iv2, 0, ivLen);
            this.realIV = iv2;
        }
        this.orgIV = this.realIV;
        if (!isStreamCipher()) {
            ciphInited = false;
        }
        return iv;
    }

    @JRubyMethod
    public IRubyObject block_size(final ThreadContext context) {
        checkInitialized();
        if ( isStreamCipher() ) {
            // getBlockSize() returns 0 for stream cipher in JCE
            // OpenSSL returns 1 for RC4.
            return context.runtime.newFixnum(1);
        }
        return context.runtime.newFixnum(cipher.getBlockSize());
    }

    private void init(final ThreadContext context, final IRubyObject[] args, final boolean encrypt) {
        final Ruby runtime = context.runtime;
        Arity.checkArgumentCount(runtime, args, 0, 2);

        encryptMode = encrypt;
        ciphInited = false;

        if ( args.length > 0 ) {
            /*
             * oops. this code mistakes salt for IV.
             * We deprecated the arguments for this method, but we decided
             * keeping this behaviour for backward compatibility.
             */
            byte[] pass = args[0].convertToString().getBytes();
            byte[] iv = null;
            try {
                iv = "OpenSSL for Ruby rulez!".getBytes("ISO8859-1");
                byte[] iv2 = new byte[this.ivLen];
                System.arraycopy(iv, 0, iv2, 0, this.ivLen);
                iv = iv2;
            } catch (Exception e) {
            }

            if ( args.length > 1 && ! args[1].isNil() ) {
                runtime.getWarnings().warning(ID.MISCELLANEOUS, "key derivation by " + getMetaClass().getRealClass().getName() + "#encrypt is deprecated; use " + getMetaClass().getRealClass().getName() + "::pkcs5_keyivgen instead");
                iv = args[1].convertToString().getBytes();
                if (iv.length > this.ivLen) {
                    byte[] iv2 = new byte[this.ivLen];
                    System.arraycopy(iv, 0, iv2, 0, this.ivLen);
                    iv = iv2;
                }
            }

            MessageDigest digest = Digest.getDigest("MD5", runtime);
            OpenSSLImpl.KeyAndIv result = OpenSSLImpl.EVP_BytesToKey(keyLen, ivLen, digest, iv, pass, 2048);
            this.key = result.getKey();
            this.realIV = iv;
            this.orgIV = this.realIV;
        }
    }

    @JRubyMethod(optional = 2)
    public IRubyObject encrypt(final ThreadContext context, IRubyObject[] args) {
        this.realIV = orgIV;
        init(context, args, true);
        return this;
    }

    @JRubyMethod(optional = 2)
    public IRubyObject decrypt(final ThreadContext context, IRubyObject[] args) {
        this.realIV = orgIV;
        init(context, args, false);
        return this;
    }

    @JRubyMethod
    public IRubyObject reset(final ThreadContext context) {
        checkInitialized();
        if ( ! isStreamCipher() ) {
            this.realIV = orgIV;
            doInitialize(context.runtime);
        }
        return this;
    }

    private void updateCipher(String name, String padding) {
        // given 'rc4' must be 'RC4' here. OpenSSL checks it as a LN of object
        // ID and set SN. We don't check 'name' is allowed as a LN in ASN.1 for
        // the possibility of JCE specific algorithm so just do upperCase here
        // for OpenSSL compatibility.
        this.name = name.toUpperCase();
        this.padding = padding;

        String[] values = Algorithm.osslToJsse(name, padding);
        cryptoBase = values[0];
        cryptoVersion = values[1];
        cryptoMode = values[2];
        realName = values[3];
        padding_type = values[4];

        int[] lengths = Algorithm.osslKeyIvLength(name);
        keyLen = lengths[0];
        ivLen = lengths[1];
        if ("DES".equalsIgnoreCase(cryptoBase)) {
            generateKeyLen = keyLen / 8 * 7;
        }

        cipher = getCipher();
    }

    javax.crypto.Cipher getCipher() {
        try {
            return getCipher(realName, false);
        }
        catch (NoSuchAlgorithmException e) {
            throw newCipherError(getRuntime(), "unsupported cipher algorithm (" + realName + ")");
        }
        catch (NoSuchPaddingException e) {
            throw newCipherError(getRuntime(), "unsupported cipher padding (" + realName + ")");
        }
    }

    @JRubyMethod(required = 1, optional = 3)
    public IRubyObject pkcs5_keyivgen(final ThreadContext context, final IRubyObject[] args) {
        final Ruby runtime = context.runtime;
        Arity.checkArgumentCount(runtime, args, 1, 4);
        byte[] pass = args[0].convertToString().getBytes();
        byte[] salt = null;
        int iter = 2048;
        IRubyObject vdigest = runtime.getNil();
        if ( args.length > 1 ) {
            if ( ! args[1].isNil() ) {
                salt = args[1].convertToString().getBytes();
            }
            if ( args.length > 2 ) {
                if ( ! args[2].isNil() ) {
                    iter = RubyNumeric.fix2int(args[2]);
                }
                if ( args.length > 3 ) {
                    vdigest = args[3];
                }
            }
        }
        if ( salt != null && salt.length != 8 ) {
            throw newCipherError(runtime, "salt must be an 8-octet string");
        }

        final String algorithm = vdigest.isNil() ? "MD5" : ((Digest) vdigest).getAlgorithm();
        final MessageDigest digest = Digest.getDigest(algorithm, runtime);
        OpenSSLImpl.KeyAndIv result = OpenSSLImpl.EVP_BytesToKey(keyLen, ivLen, digest, salt, pass, iter);
        this.key = result.getKey();
        this.realIV = result.getIv();
        this.orgIV = this.realIV;

        doInitialize(runtime);

        return runtime.getNil();
    }

    private void doInitialize(final Ruby runtime) {
        if ( isDebug(runtime) ) {
            runtime.getOut().println("*** doInitialize");
            dumpVars( runtime.getOut() );
        }
        checkInitialized();
        if (key == null) {
            throw newCipherError(runtime, "key not specified");
        }
        try {
            if ( ! "ECB".equalsIgnoreCase(cryptoMode) ) {
                if ( this.realIV == null ) {
                    // no IV yet, start out with all zeros
                    this.realIV = new byte[ivLen];
                }
                if ( "RC2".equalsIgnoreCase(cryptoBase) ) {
                    this.cipher.init(encryptMode ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE, new SimpleSecretKey("RC2", this.key), new RC2ParameterSpec(this.key.length * 8, this.realIV));
                } else if ( "RC4".equalsIgnoreCase(cryptoBase) ) {
                    this.cipher.init(encryptMode ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE, new SimpleSecretKey("RC4", this.key));
                } else {
                    this.cipher.init(encryptMode ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE, new SimpleSecretKey(realName.split("/")[0], this.key), new IvParameterSpec(this.realIV));
                }
            } else {
                this.cipher.init(encryptMode ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE, new SimpleSecretKey(realName.split("/")[0], this.key));
            }
        }
        catch (InvalidKeyException e) {
            throw newCipherError(runtime, e.getMessage() + ": possibly you need to install Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files for your JRE");
        }
        catch (Exception e) {
            if ( isDebug(runtime) ) e.printStackTrace( runtime.getOut() );
            throw newCipherError(runtime, e.getMessage());
        }
        ciphInited = true;
    }
    private byte[] lastIv = null;

    @JRubyMethod
    public IRubyObject update(final ThreadContext context, final IRubyObject data) {
        final Ruby runtime = context.runtime;
        final boolean debug = isDebug(runtime);
        if ( debug ) {
            runtime.getOut().println("*** update [" + data + "]");
        }
        checkInitialized();
        byte[] val = data.convertToString().getBytes();
        if (val.length == 0) {
            throw getRuntime().newArgumentError("data must not be empty");
        }

        if ( ! ciphInited ) {
            //if ( debug ) runtime.getOut().println("BEFORE INITING");
            doInitialize(runtime);
            //if ( debug ) runtime.getOut().println("AFTER INITING");
        }

        byte[] str = new byte[0];
        try {
            byte[] out = cipher.update(val);
            if (out != null) {
                str = out;

                if (this.realIV != null) {
                    if (lastIv == null) {
                        lastIv = new byte[ivLen];
                    }
                    byte[] tmpIv = encryptMode ? out : val;
                    if (tmpIv.length >= ivLen) {
                        System.arraycopy(tmpIv, tmpIv.length - ivLen, lastIv, 0, ivLen);
                    }
                }
            }
        }
        catch (Exception e) {
            if ( isDebug(runtime) ) e.printStackTrace( runtime.getOut() );
            throw newCipherError(runtime, e.getMessage());
        }

        return getRuntime().newString(new ByteList(str, false));
    }

    @JRubyMethod(name = "<<")
    public IRubyObject update_deprecated(final ThreadContext context, final IRubyObject data) {
        context.runtime.getWarnings().warn(IRubyWarnings.ID.DEPRECATED_METHOD, "" + this.getMetaClass().getRealClass().getName() + "#<< is deprecated; use " + this.getMetaClass().getRealClass().getName() + "#update instead");
        return update(context, data);
    }

    @JRubyMethod(name = "final")
    public IRubyObject _final(final ThreadContext context) {
        final Ruby runtime = context.runtime;
        checkInitialized();
        if ( ! ciphInited ) {
            doInitialize(runtime);
        }
        // trying to allow update after final like cruby-openssl. Bad idea.
        if ( "RC4".equalsIgnoreCase(cryptoBase) ) {
            return runtime.newString("");
        }
        ByteList str = new ByteList(ByteList.NULL_ARRAY);
        try {
            byte[] out = cipher.doFinal();
            if (out != null) {
                str = new ByteList(out, false);
                // TODO: Modifying this line appears to fix the issue, but I do
                // not have a good reason for why. Best I can tell, lastIv needs
                // to be set regardless of encryptMode, so we'll go with this
                // for now. JRUBY-3335.
                //if(this.realIV != null && encryptMode) {
                if (this.realIV != null) {
                    if (lastIv == null) {
                        lastIv = new byte[ivLen];
                    }
                    byte[] tmpIv = out;
                    if (tmpIv.length >= ivLen) {
                        System.arraycopy(tmpIv, tmpIv.length - ivLen, lastIv, 0, ivLen);
                    }
                }
            }
            if (this.realIV != null) {
                this.realIV = lastIv;
                doInitialize(runtime);
            }
        }
        catch (Exception e) {
            throw newCipherError(runtime, e.getMessage());
        }
        return runtime.newString(str);
    }

    @JRubyMethod(name = "padding=")
    public IRubyObject set_padding(IRubyObject padding) {
        updateCipher(name, padding.toString());
        return padding;
    }

    String getAlgorithm() {
        return this.cipher.getAlgorithm();
    }

    String getName() {
        return this.name;
    }

    String getCryptoBase() {
        return this.cryptoBase;
    }

    String getCryptoMode() {
        return this.cryptoMode;
    }

    int getKeyLen() {
        return keyLen;
    }

    int getGenerateKeyLen() {
        return (generateKeyLen == -1) ? keyLen : generateKeyLen;
    }

    private void checkInitialized() {
        if (cipher == null) {
            throw getRuntime().newRuntimeError("Cipher not inititalized!");
        }
    }

    private boolean isStreamCipher() {
        return cipher.getBlockSize() == 0;
    }

    private static RaiseException newCipherError(Ruby runtime, String message) {
        return Utils.newError(runtime, "OpenSSL::Cipher::CipherError", message);
    }
}
