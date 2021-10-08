# This is duplicated on the filesystem and in the jar (core/src/main/ruby) to allow loading the lib with only the jar
# present while keeping a file for e.g. bootsnap to index. See https://github.com/jruby/jruby/issues/6362.

# JRuby: load built-in tempfile library
JRuby::Util.load_ext("org.jruby.ext.tempfile.TempfileLibrary")

require 'jruby/stdlib/tempfile'
