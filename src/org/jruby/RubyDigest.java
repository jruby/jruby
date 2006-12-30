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
package org.jruby;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyDigest {
    public static void createDigest(IRuby runtime) {
        RubyModule mDigest = runtime.defineModule("Digest");
        RubyClass cDigestBase = mDigest.defineClassUnder("Base",runtime.getObject());

        CallbackFactory basecb = runtime.callbackFactory(Base.class);
        
        cDigestBase.defineFastSingletonMethod("new",basecb.getOptSingletonMethod("newInstance"));
        cDigestBase.defineFastSingletonMethod("digest",basecb.getSingletonMethod("s_digest",IRubyObject.class));
        cDigestBase.defineFastSingletonMethod("hexdigest",basecb.getSingletonMethod("s_hexdigest",IRubyObject.class));

        cDigestBase.defineMethod("initialize",basecb.getOptMethod("initialize"));
        cDigestBase.defineMethod("initialize_copy",basecb.getMethod("initialize_copy",IRubyObject.class));
        cDigestBase.defineFastMethod("clone",basecb.getMethod("rbClone"));
        cDigestBase.defineFastMethod("update",basecb.getMethod("update",IRubyObject.class));
        cDigestBase.defineFastMethod("<<",basecb.getMethod("update",IRubyObject.class));
        cDigestBase.defineFastMethod("digest",basecb.getMethod("digest"));
        cDigestBase.defineFastMethod("hexdigest",basecb.getMethod("hexdigest"));
        cDigestBase.defineFastMethod("to_s",basecb.getMethod("hexdigest"));
        cDigestBase.defineFastMethod("==",basecb.getMethod("eq",IRubyObject.class));
    }

    public static void createDigestMD5(IRuby runtime) {
        runtime.getLoadService().require("digest.so");
        RubyModule mDigest = runtime.getModule("Digest");
        RubyClass cDigestBase = mDigest.getClass("Base");
        RubyClass cDigest_MD5 = mDigest.defineClassUnder("MD5",cDigestBase);
        cDigest_MD5.setClassVar("metadata",runtime.newString("MD5"));
    }

    public static void createDigestRMD160(IRuby runtime) {
        runtime.getLoadService().require("digest.so");
        RubyModule mDigest = runtime.getModule("Digest");
        RubyClass cDigestBase = mDigest.getClass("Base");
        RubyClass cDigest_RMD160 = mDigest.defineClassUnder("RMD160",cDigestBase);
        cDigest_RMD160.setClassVar("metadata",runtime.newString("RIPEMD160"));
    }

    public static void createDigestSHA1(IRuby runtime) {
        runtime.getLoadService().require("digest.so");
        RubyModule mDigest = runtime.getModule("Digest");
        RubyClass cDigestBase = mDigest.getClass("Base");
        RubyClass cDigest_SHA1 = mDigest.defineClassUnder("SHA1",cDigestBase);
        cDigest_SHA1.setClassVar("metadata",runtime.newString("SHA1"));
    }

    public static void createDigestSHA2(IRuby runtime) {
        try {
            MessageDigest.getInstance("SHA-256");
        } catch(NoSuchAlgorithmException e) {
            throw runtime.newLoadError("SHA2 not supported");
        }
        runtime.getLoadService().require("digest.so");
        RubyModule mDigest = runtime.getModule("Digest");
        RubyClass cDigestBase = mDigest.getClass("Base");
        RubyClass cDigest_SHA2_256 = mDigest.defineClassUnder("SHA256",cDigestBase);
        cDigest_SHA2_256.setClassVar("metadata",runtime.newString("SHA-256"));
        RubyClass cDigest_SHA2_384 = mDigest.defineClassUnder("SHA384",cDigestBase);
        cDigest_SHA2_384.setClassVar("metadata",runtime.newString("SHA-384"));
        RubyClass cDigest_SHA2_512 = mDigest.defineClassUnder("SHA512",cDigestBase);
        cDigest_SHA2_512.setClassVar("metadata",runtime.newString("SHA-512"));
    }

    public static class Base extends RubyObject {
        public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
            if(recv == recv.getRuntime().getModule("Digest").getClass("Base")) {
                throw recv.getRuntime().newNotImplementedError("Digest::Base is an abstract class");
            }

            if(!((RubyClass)recv).isClassVarDefined("metadata")) {
                throw recv.getRuntime().newNotImplementedError("the " + recv + "() function is unimplemented on this machine");
            }

            Base result = new Base(recv.getRuntime(), (RubyClass)recv);
            try {
                result.setAlgorithm(((RubyClass)recv).getClassVar("metadata"));
            } catch(NoSuchAlgorithmException e) {
                throw recv.getRuntime().newNotImplementedError("the " + recv + "() function is unimplemented on this machine");
            }
            result.callInit(args);
            return result;
        }
        public static IRubyObject s_digest(IRubyObject recv, IRubyObject str) {
            String name = ((RubyClass)recv).getClassVar("metadata").toString();
            try {
                MessageDigest md = MessageDigest.getInstance(name);
                return recv.getRuntime().newString(new String(md.digest(str.toString().getBytes("PLAIN")),"ISO8859_1"));
            } catch(NoSuchAlgorithmException e) {
                throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            } catch(java.io.UnsupportedEncodingException e) {
                throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            }
        }
        public static IRubyObject s_hexdigest(IRubyObject recv, IRubyObject str) {
            String name = ((RubyClass)recv).getClassVar("metadata").toString();
            try {
                MessageDigest md = MessageDigest.getInstance(name);
                return recv.getRuntime().newString(toHex(md.digest(str.toString().getBytes("PLAIN"))));
            } catch(NoSuchAlgorithmException e) {
                throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            } catch(java.io.UnsupportedEncodingException e) {
                throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            }
        }

        private MessageDigest algo;
        private StringBuffer data;

        public Base(IRuby runtime, RubyClass type) {
            super(runtime,type);
            data = new StringBuffer();
        }
        
        public IRubyObject initialize(IRubyObject[] args) {
            if(args.length > 0 && !args[0].isNil()) {
                update(args[0]);
            }
            return this;
        }

        public IRubyObject initialize_copy(IRubyObject obj) {
            if(this == obj) {
                return this;
            }
            ((RubyObject)obj).checkFrozen();

            data = new StringBuffer(((Base)obj).data.toString());
            String name = ((Base)obj).algo.getAlgorithm();
            try {
                algo = MessageDigest.getInstance(name);
            } catch(NoSuchAlgorithmException e) {
                throw getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            }
            return this;
        }

        public IRubyObject update(IRubyObject obj) {
            try {
                data.append(obj);
                algo.update(obj.toString().getBytes("PLAIN"));
            } catch(java.io.UnsupportedEncodingException e) {}
            return this;
        }

        public IRubyObject digest() {
            try {
                algo.reset();
                return getRuntime().newString(new String(algo.digest(data.toString().getBytes("PLAIN")),"ISO8859_1"));
            } catch(java.io.UnsupportedEncodingException e) {
                return getRuntime().getNil();
            }
        }

        public IRubyObject hexdigest() {
            try {
                algo.reset();
                return getRuntime().newString(toHex(algo.digest(data.toString().getBytes("PLAIN"))));
            } catch(java.io.UnsupportedEncodingException e) {
                return getRuntime().getNil();
            }
        }

        public IRubyObject eq(IRubyObject oth) {
            boolean ret = this == oth;
            if(!ret && oth instanceof Base) {
                Base b = (Base)oth;
                ret = this.algo.getAlgorithm().equals(b.algo.getAlgorithm()) &&
                    this.digest().equals(b.digest());
            }

            return ret ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        public IRubyObject rbClone() {
            IRubyObject clone = new Base(getRuntime(),getMetaClass().getRealClass());
            clone.setMetaClass(getMetaClass().getSingletonClassClone());
            clone.setTaint(this.isTaint());
            clone.initCopy(this);
            clone.setFrozen(isFrozen());
            return clone;
        }

        private void setAlgorithm(IRubyObject algo) throws NoSuchAlgorithmException {
            this.algo = MessageDigest.getInstance(algo.toString());
        }

        private static String toHex(byte[] val) {
            StringBuffer out = new StringBuffer();
            for(int i=0,j=val.length;i<j;i++) {
                String ve = Integer.toString((((int)((char)val[i])) & 0xFF),16);
                if(ve.length() == 1) {
                    ve = "0" + ve;
                }
                out.append(ve);
            }
            return out.toString();
        }
    }
}// RubyDigest
