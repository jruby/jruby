package org.jruby.util;

import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.load.ExtendedFileResource;

public class FileResourceFactory {
    public static ExtendedFileResource createResource(ThreadContext context, String pathname) {
        return createResource(context.runtime, pathname);
      }

      public static ExtendedFileResource createResource(Ruby runtime, String pathname) {
        return createResource(runtime.getCurrentDirectory(), pathname);
      }

      public static ExtendedFileResource createResource(String cwd, String pathname) {
          ExtendedFileResource emptyResource = EmptyFileResource.create(pathname);
          if (emptyResource != null) {
              return emptyResource;
          }

          ExtendedFileResource jarResource = JarResource.create(pathname);
          if (jarResource != null) {
              return jarResource;
          }

          // HACK turn the pathname into something meaningful in case of being an URI
          ExtendedFileResource cpResource = ClasspathResource.create(pathname.replace(cwd == null ? "" : cwd, "" ));
          if (cpResource != null) {
              return cpResource;
          }

          // HACK this codes get triggers by LoadService via findOnClasspath, so remove the prefix to get the uri
          ExtendedFileResource urlResource = URLResource.create(pathname.replace("classpath:/", ""));
          if (urlResource != null) {
              return urlResource;
          }

          if (pathname.startsWith("file:")) {
              pathname = pathname.substring(5);
          }

          // If any other special resource types fail, count it as a filesystem backed resource.
          return new RegularFileResource(JRubyFile.create(cwd, pathname));
      }
}