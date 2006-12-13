/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.openssl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.spec.IvParameterSpec;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Cipher extends RubyObject {
    public static void createCipher(IRuby runtime, RubyModule ossl) {
        RubyModule mCipher = ossl.defineModuleUnder("Cipher");
        RubyClass cCipher = mCipher.defineClassUnder("Cipher",runtime.getObject());

        mCipher.defineClassUnder("CipherError",ossl.getClass("OpenSSLError"));

        CallbackFactory ciphercb = runtime.callbackFactory(Cipher.class);

        mCipher.defineSingletonMethod("ciphers",ciphercb.getSingletonMethod("ciphers"));
        cCipher.defineSingletonMethod("new",ciphercb.getOptSingletonMethod("newInstance"));
        cCipher.defineMethod("initialize",ciphercb.getMethod("initialize",IRubyObject.class));
        cCipher.defineMethod("initialize_copy",ciphercb.getMethod("initialize_copy",IRubyObject.class));
        cCipher.defineMethod("clone",ciphercb.getMethod("rbClone"));
        cCipher.defineMethod("name",ciphercb.getMethod("name"));
        cCipher.defineMethod("key_len",ciphercb.getMethod("key_len"));
        cCipher.defineMethod("key_len=",ciphercb.getMethod("set_key_len",IRubyObject.class));
        cCipher.defineMethod("iv_len",ciphercb.getMethod("iv_len"));
        cCipher.defineMethod("block_size",ciphercb.getMethod("block_size"));
        cCipher.defineMethod("encrypt",ciphercb.getOptMethod("encrypt"));
        cCipher.defineMethod("decrypt",ciphercb.getOptMethod("decrypt"));
        cCipher.defineMethod("key=",ciphercb.getMethod("set_key",IRubyObject.class));
        cCipher.defineMethod("iv=",ciphercb.getMethod("set_iv",IRubyObject.class));
        cCipher.defineMethod("reset",ciphercb.getMethod("reset"));
        cCipher.defineMethod("pkcs5_keyivgen",ciphercb.getOptMethod("pkcs5_keyivgen"));
        cCipher.defineMethod("update",ciphercb.getMethod("update",IRubyObject.class));
        cCipher.defineMethod("<<",ciphercb.getMethod("update_deprecated",IRubyObject.class));
        cCipher.defineMethod("final",ciphercb.getMethod("_final"));
        cCipher.defineMethod("padding=",ciphercb.getMethod("set_padding",IRubyObject.class));
    }

    private static final Set BLOCK_MODES = new HashSet();
    static {
        BLOCK_MODES.add("CBC");
        BLOCK_MODES.add("CFB");
        BLOCK_MODES.add("CFB1");
        BLOCK_MODES.add("CFB8");
        BLOCK_MODES.add("ECB");
        BLOCK_MODES.add("OFB");
    }

    private static String[] rubyToJavaCipher(String inName) {
        String[] split = inName.split("-");
        String cryptoBase = split[0];
        String cryptoVersion = null;
        String cryptoMode = null;
        String realName = null;
        String padding_type = "PKCS5Padding";

        if("bf".equalsIgnoreCase(cryptoBase)) {
            cryptoBase = "Blowfish";
        }

        if(split.length == 3) {
            cryptoVersion = split[1];
            cryptoMode = split[2];
        } else {
            if(split.length == 2) {
                cryptoMode = split[1];
            } else {
                cryptoMode = "ECB";
            }
        }

        if(cryptoBase.equalsIgnoreCase("DES") && "EDE3".equalsIgnoreCase(cryptoVersion)) {
            realName = "DESede";
        } else {
            realName = cryptoBase;
        }

        if(!BLOCK_MODES.contains(cryptoMode.toUpperCase())) {
            cryptoVersion = cryptoMode;
            cryptoMode = "CBC";
        }

        realName = realName + "/" + cryptoMode + "/" + padding_type;

        return new String[]{cryptoBase,cryptoVersion,cryptoMode,realName,padding_type};
    }

    private static boolean tryCipher(String rubyName) {
        try {
            javax.crypto.Cipher.getInstance(rubyToJavaCipher(rubyName)[3],"BC");
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    public static IRubyObject ciphers(IRubyObject recv) {
        List ciphers = new ArrayList();
        String[] other = {"AES128","AES192","AES256","BLOWFISH", "RC2-40-CBC", "RC2-64-CBC","RC4","RC4-40", "CAST","CAST-CBC"};
        String[] bases = {"AES-128","AES-192","AES-256","BF", "DES", "DES-EDE","DES-EDE3", "RC2","CAST5"};
        String[] suffixes = {"","-CBC","-CFB","-CFB1","-CFB8","-ECB","-OFB"};
        for(int i=0,j=bases.length;i<j;i++) {
            for(int k=0,l=suffixes.length;k<l;k++) {
                String val = bases[i]+suffixes[k];
                if(tryCipher(val)) {
                    ciphers.add(recv.getRuntime().newString(val));
                    ciphers.add(recv.getRuntime().newString((val).toLowerCase()));
                }
            }
        }
        for(int i=0,j=other.length;i<j;i++) {
            if(tryCipher(other[i])) {
                ciphers.add(recv.getRuntime().newString(other[i]));
                ciphers.add(recv.getRuntime().newString(other[i].toLowerCase()));
            }
        }
        return recv.getRuntime().newArray(ciphers);
    }

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        Cipher result = new Cipher(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    private RubyClass ciphErr;
    public Cipher(IRuby runtime, RubyClass type) {
        super(runtime,type);
        ciphErr = (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("Cipher"))).getConstant("CipherError"));
    }

    private javax.crypto.Cipher ciph;
    private String name;
    private String cryptoBase;
    private String cryptoVersion;
    private String cryptoMode;
    private String padding_type;
    private String realName;
    private int keyLen = -1;
    private int ivLen = -1;
    private boolean encryptMode = true;
    private IRubyObject[] modeParams;
    private boolean ciphInited = false;
    private byte[] key;
    private byte[] iv;
    private String padding;

    public IRubyObject initialize(IRubyObject str) {
        name = str.toString();
        String[] values = rubyToJavaCipher(name);
        cryptoBase = values[0];
        cryptoVersion = values[1];
        cryptoMode = values[2];
        realName = values[3];
        padding_type = values[4];

        try {
            ciph = javax.crypto.Cipher.getInstance(realName,"BC");
        } catch(NoSuchAlgorithmException e) {
            throw getRuntime().newLoadError("unsupported cipher algorithm (" + realName + ")");
        } catch(NoSuchProviderException e) {
            throw getRuntime().newLoadError("unsupported cipher algorithm (" + realName + ")");
        } catch(javax.crypto.NoSuchPaddingException e) {
            throw getRuntime().newLoadError("unsupported cipher padding (" + realName + ")");
        }

        if(hasLen() && null != cryptoVersion) {
            try {
                keyLen = Integer.parseInt(cryptoVersion);
            } catch(NumberFormatException e) {
                keyLen = -1;
            }
        }
        if(keyLen == -1) {
            if("DES".equalsIgnoreCase(cryptoBase)) {
                if("EDE3".equalsIgnoreCase(cryptoVersion)) {
                    keyLen = 168;
                } else {
                    keyLen = 56;
                }
            } else {
                keyLen = 128;
            }
        }

        if(ivLen == -1) {
            if("AES".equalsIgnoreCase(cryptoBase)) {
                ivLen = 16*8;
            } else {
                ivLen = 8*8;
            }
        }
        return this;
    }

    public IRubyObject initialize_copy(IRubyObject obj) {
        if(this == obj) {
            return this;
        }

        checkFrozen();

        cryptoBase = ((Cipher)obj).cryptoBase;
        cryptoVersion = ((Cipher)obj).cryptoVersion;
        cryptoMode = ((Cipher)obj).cryptoMode;
        padding_type = ((Cipher)obj).padding_type;
        realName = ((Cipher)obj).realName;
        name = ((Cipher)obj).name;
        keyLen = ((Cipher)obj).keyLen;
        ivLen = ((Cipher)obj).ivLen;
        encryptMode = ((Cipher)obj).encryptMode;
        ciphInited = false;
        if(((Cipher)obj).key != null) {
            key = new byte[((Cipher)obj).key.length];
            System.arraycopy(((Cipher)obj).key,0,key,0,key.length);
        } else {
            key = null;
        }
        if(((Cipher)obj).iv != null) {
            iv = new byte[((Cipher)obj).iv.length];
            System.arraycopy(((Cipher)obj).iv,0,iv,0,iv.length);
        } else {
            iv = null;
        }
        padding = ((Cipher)obj).padding;

        try {
            ciph = javax.crypto.Cipher.getInstance(realName,"BC");
        } catch(NoSuchAlgorithmException e) {
            throw getRuntime().newLoadError("unsupported cipher algorithm (" + realName + ")");
        } catch(NoSuchProviderException e) {
            throw getRuntime().newLoadError("unsupported cipher algorithm (" + realName + ")");
        } catch(javax.crypto.NoSuchPaddingException e) {
            throw getRuntime().newLoadError("unsupported cipher padding (" + realName + ")");
        }

        return this;
    }

    public IRubyObject name() {
        return getRuntime().newString(name);
    }

    public IRubyObject key_len() {
        return getRuntime().newFixnum(keyLen);
    }

    public IRubyObject iv_len() {
        return getRuntime().newFixnum(ivLen);
    }

    public IRubyObject set_key_len(IRubyObject len) {
        this.keyLen = RubyNumeric.fix2int(len);
        return len;
    }

    public IRubyObject set_key(IRubyObject key) {
        if(key.toString().length()*8 < keyLen) {
            throw new RaiseException(getRuntime(), ciphErr, "key length to short", true);
        }
        try {
            this.key = key.toString().getBytes("PLAIN");
        } catch(Exception e) {
            throw new RaiseException(getRuntime(), ciphErr, null, true);
        }
        return key;
    }

    public IRubyObject set_iv(IRubyObject iv) {
        if(iv.toString().length()*8 < ivLen) {
            throw new RaiseException(getRuntime(), ciphErr, "iv length to short", true);
        }
        try {
            this.iv = iv.toString().getBytes("PLAIN");
        } catch(Exception e) {
            throw new RaiseException(getRuntime(), ciphErr, null, true);
        }
        return iv;
    }

    public IRubyObject block_size() {
        return getRuntime().newFixnum(ciph.getBlockSize());
    }

    public IRubyObject encrypt(IRubyObject[] args) {
        //TODO: implement backwards compat
        checkArgumentCount(args,0,2);
        encryptMode = true;
        modeParams = args;
        ciphInited = false;
        return this;
    }

    public IRubyObject decrypt(IRubyObject[] args) {
        //TODO: implement backwards compat
        checkArgumentCount(args,0,2);
        encryptMode = false;
        modeParams = args;
        ciphInited = false;
        return this;
    }

    public IRubyObject reset() {
        doInitialize();
        return this;
    }

    private boolean hasLen() {
        return hasLen(this.cryptoBase);
    }

    private static boolean hasLen(String cryptoBase) {
        return "AES".equalsIgnoreCase(cryptoBase) || "RC2".equalsIgnoreCase(cryptoBase) || "RC4".equalsIgnoreCase(cryptoBase);
    }

    public IRubyObject pkcs5_keyivgen(IRubyObject[] args) {
        checkArgumentCount(args,1,4);
        String pass = args[0].toString();
        String salt = null;
        byte[] ssalt = null;
        int iter = 2048;
        IRubyObject vdigest = getRuntime().getNil();
        MessageDigest digest = null;
        if(args.length>1) {
            if(!args[1].isNil()) {
                salt = args[1].toString();
            }
            if(args.length>2) {
                if(!args[2].isNil()) {
                    iter = RubyNumeric.fix2int(args[2]);
                }
                if(args.length>3) {
                    vdigest = args[3];
                }
            }
        }
        try {
            if(null != salt) {
                if(salt.length() != 8) {
                    throw new RaiseException(getRuntime(), ciphErr, "salt must be an 8-octet string", true);
                }
                ssalt = salt.getBytes("PLAIN");
            }
            if(vdigest.isNil()) {
                digest = MessageDigest.getInstance("MD5","BC");
            } else {
                digest = MessageDigest.getInstance(((Digest)vdigest).getAlgorithm(),"BC");
            }

            OpenSSLImpl.KeyAndIv result = OpenSSLImpl.EVP_BytesToKey(keyLen/8,ivLen/8,digest,ssalt,pass.getBytes("PLAIN"),iter);
            this.key = result.getKey();
            this.iv = result.getIv();
        } catch(Exception e) {
            throw new RaiseException(getRuntime(), ciphErr, null, true);
        }

        doInitialize();

        return getRuntime().getNil();
    }

    private void doInitialize() {
        ciphInited = true;
        try {
            if(!"ECB".equalsIgnoreCase(cryptoMode) && this.iv != null) {
                this.ciph.init(encryptMode ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE, new SimpleSecretKey(this.key), new IvParameterSpec(this.iv));
            } else {
                this.ciph.init(encryptMode ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE, new SimpleSecretKey(this.key));
            }
        } catch(Exception e) {
            throw new RaiseException(getRuntime(), ciphErr, null, true);
        }
    }

    public IRubyObject update(IRubyObject data) {
        //TODO: implement correctly
        String val = data.toString();
        if(val.length() == 0) {
            throw getRuntime().newArgumentError("data must not be empty");
        }

        if(!ciphInited) {
            doInitialize();
        }

        String str = "";
        try {
            byte[] out = ciph.update(val.toString().getBytes("PLAIN"));
            if(out != null) {
                str = new String(out,"ISO8859_1");
            }
        } catch(Exception e) {
            throw new RaiseException(getRuntime(), ciphErr, null, true);
        }

        return getRuntime().newString(str);
    }

    public IRubyObject update_deprecated(IRubyObject data) {
        getRuntime().getWarnings().warn("" + this.getMetaClass().getRealClass().getName() + "#<< is deprecated; use " + this.getMetaClass().getRealClass().getName() + "#update instead");
        return update(data);
    }

    public IRubyObject _final() {
        if(!ciphInited) {
            doInitialize();
        }

        //TODO: implement correctly
        String str = "";
        try {
            byte[] out = ciph.doFinal();
            if(out != null) {
                str = new String(out,"ISO8859_1");
            }
        } catch(Exception e) {
            throw new RaiseException(getRuntime(), ciphErr, null, true);
        }

        return getRuntime().newString(str);
    }

    public IRubyObject set_padding(IRubyObject padding) {
        this.padding = padding.toString();
        return padding;
    }

    public IRubyObject rbClone() {
        IRubyObject clone = new Cipher(getRuntime(),getMetaClass().getRealClass());
        clone.setMetaClass(getMetaClass().getSingletonClassClone());
        clone.setTaint(this.isTaint());
        clone.initCopy(this);
        clone.setFrozen(isFrozen());
        return clone;
    }

    String getAlgorithm() {
        return this.ciph.getAlgorithm();
    }
}

