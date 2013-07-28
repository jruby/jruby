module JRubyJars
  VERSION = Dir[ File.expand_path(File.dirname(File.dirname(__FILE__))) + '/jruby-core-complete-*jar' ].first.gsub( /^.*jruby-core-complete-|.jar$/, '' )
end
