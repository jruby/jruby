# This is duplicated on the filesystem and in the jar (core/src/main/ruby) to allow loading the lib with only the jar
# present while keeping a file for e.g. bootsnap to index. See https://github.com/jruby/jruby/issues/6362.

# Load built-in etc library
JRuby::Util.load_ext("org.jruby.ext.etc.EtcLibrary")
