/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 *
 * Some of the code in this class is modified from org.jruby.util.StringSupport,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 */
package org.jruby.truffle.aot;

import com.oracle.truffle.api.source.Source;
import org.jcodings.exception.InternalException;
import org.joda.time.DateTimeZone;
import org.jruby.Ruby;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.language.loader.SourceLoader;
import org.jruby.util.FileResource;
import org.jruby.truffle.util.SafePropertyAccessor;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormatSymbols;
import java.util.Locale;
import java.util.Map;

// Checkstyle: stop

final class Target_org_jruby_util_unsafe_UnsafeHolder {
    static boolean SUPPORTS_FENCES = false;
    static long ARRAY_OBJECT_BASE_OFFSET;
    static long ARRAY_OBJECT_INDEX_SCALE;

    static void fullFence() {
    }
}

final class Target_org_jcodings_Encoding {
    static org.jcodings.Encoding load(String name) {
        JRubySupport.EncodingInstance instance = JRubySupport.allEncodings.get(name);
        if (instance == null) {
            throw new InternalException(org.jcodings.exception.ErrorMessages.ERR_ENCODING_CLASS_DEF_NOT_FOUND, "org.jcodings.specific." + name + "Encoding");
        }
        return instance.get(false);
    }
}

final class Target_org_jcodings_util_ArrayReader {
    static DataInputStream openStream(String name) {
        byte[] table = JRubySupport.allJCodingsTables.get(name);
        if (table == null) {
            throw new InternalException(("entry: /tables/" + name + ".bin not found"));
        }
        return new DataInputStream(new ByteArrayInputStream(table));
    }
}

@SuppressWarnings("static-method")
final class Target_org_joda_time_tz_ZoneInfoProvider {
    File iFileDir;

    DateTimeZone getZone(String id) {
        return JRubySupport.allTimeZones.get(id);
    }

    InputStream openResource(String name) throws IOException {
        if (iFileDir != null) {
            return new FileInputStream(new File(iFileDir, name));
        } else {
            throw new IllegalArgumentException("Not supported on SubstrateVM");
        }
    }
}

@SuppressWarnings("unused")
final class Target_org_jruby_util_URLResource {
    URL url;

    static FileResource createClassloaderURI(Ruby runtime, String pathname, boolean isFile) {
        throw new RuntimeException("Not supported on Substrate VM");
    }

    InputStream openInputStream() throws IOException {
        return url.openStream();
    }
}

final class Target_org_jruby_RubyInstanceConfig {
    PrintStream error;
    Map<String, String> environment;

    static native String verifyHome(String home, PrintStream error);

    static ClassLoader defaultClassLoader() {
        return null;
    }

    void setupEnvironment(String jrubyHome) {
        if (!new File(jrubyHome).exists() && !environment.containsKey("RUBY")) {
            environment.put("RUBY", "svm_jruby");
        }
    }

    String calculateJRubyHome() {
        String newJRubyHome = null;

        // try the normal property first
        if (!Ruby.isSecurityRestricted()) {
            newJRubyHome = SafePropertyAccessor.getProperty("jruby.home");
        }

        if (newJRubyHome != null) {
            // verify it if it's there
            newJRubyHome = verifyHome(newJRubyHome, error);
        } else {
            try {
                newJRubyHome = SafePropertyAccessor.getenv("JRUBY_HOME");
            } catch (Exception e) {
            }

            if (newJRubyHome != null) {
                // verify it if it's there
                newJRubyHome = verifyHome(newJRubyHome, error);
            } else {
                // otherwise fall back on system temp location
                newJRubyHome = SafePropertyAccessor.getProperty("java.io.tmpdir");
            }
        }

        return newJRubyHome;
    }
}

final class Target_org_joda_time_DateTimeUtils {
    static DateFormatSymbols getDateFormatSymbols(Locale locale) {
        return DateFormatSymbols.getInstance(locale);
    }
}

@SuppressWarnings("unused")
final class Target_org_jruby_util_JarResource {
    static Object create(String pathname) {
        return null;
    }
}

@SuppressWarnings({"static-method", "unused"})
final class Target_org_jruby_RubyBasicObject {
    static long VAR_TABLE_OFFSET;
    static long STAMP_OFFSET;

    <T> T defaultToJava(Class<T> target) {
        return null;
    }
}

final class Target_org_jruby_RubyEncoding {
    static int CHAR_ARRAY_BASE;
    static int BYTE_ARRAY_BASE;
    static long VALUE_FIELD_OFFSET;
}

final class Target_org_jruby_util_StringSupport {
    static int ARRAY_BYTE_BASE_OFFSET;
}

@SuppressWarnings("static-method")
final class Target_org_jruby_truffle_language_loader_SourceLoader {
    Source loadResource(String path) throws IOException {
        if (!(path.startsWith(SourceLoader.TRUFFLE_SCHEME) || path.startsWith(SourceLoader.JRUBY_SCHEME))) {
            throw new UnsupportedOperationException();
        }

        final String canonicalPath = JRubySourceLoaderSupport.canonicalizeResourcePath(path);
        final JRubySourceLoaderSupport.CoreLibraryFile coreFile = JRubySourceLoaderSupport.allCoreLibraryFiles.get(canonicalPath);
        if (coreFile == null) {
            throw new FileNotFoundException(path);
        }

        return Source.newBuilder(new InputStreamReader(new ByteArrayInputStream(coreFile.code), StandardCharsets.UTF_8)).name(path).mimeType(RubyLanguage.MIME_TYPE).build();
    }
}

/** Dummy class to have a class with the file's name. */
public final class JRubySubstitutions {
}
