# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{jruby-win32ole}
  s.version = "0.8.3"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Thomas E. Enebo"]
  s.date = %q{2010-12-27}
  s.description = %q{A Gem for win32ole support on JRuby}
  s.email = %q{tom.enebo@gmail.com}
  s.executables = ["make_data.rb", "sample"]
  s.files = [".gitignore", "Gemfile", "README", "Rakefile", "VERSION", "bin/make_data.rb", "bin/sample", "build.xml", "jruby-win32ole.gemspec", "lib/jruby-win32ole.rb", "lib/jruby-win32ole/version.rb", "lib/racob-x64.dll", "lib/racob-x86.dll", "lib/racob.jar", "lib/win32ole/utils.rb", "lib/win32ole/win32ole.jar", "lib/win32ole/win32ole_error.rb", "lib/win32ole/win32ole_event.rb", "lib/win32ole/win32ole_method.rb", "lib/win32ole/win32ole_param.rb", "lib/win32ole/win32ole_ruby.rb", "lib/win32ole/win32ole_type.rb", "lib/win32ole/win32ole_typelib.rb", "lib/win32ole/win32ole_variable.rb", "lib/win32ole/win32ole_variant.rb", "nbproject/build-impl.xml", "nbproject/genfiles.properties", "nbproject/private/config.properties", "nbproject/private/private.properties", "nbproject/private/private.xml", "nbproject/project.properties", "nbproject/project.xml", "samples/browser_connect.rb", "samples/const_load.rb", "samples/dir_enum_bench.rb", "samples/dispatch_bench.rb", "samples/dump.rb", "samples/file_system_object.rb", "samples/fs.rb", "samples/ie_plus_events.rb", "samples/ie_simple.rb", "samples/ie_simple_clsid.rb", "samples/sbem.rb", "samples/small_enum_bench.rb", "src/org/jruby/ext/win32ole/RubyInvocationProxy.java", "src/org/jruby/ext/win32ole/RubyWIN32OLE.java", "src/win32ole/Win32oleService.java"]
  s.homepage = %q{http://github.com/enebo/jruby-win32ole}
  s.require_paths = ["lib"]
  s.rubyforge_project = %q{jruby-win32ole}
  s.rubygems_version = %q{1.5.1}
  s.summary = %q{A Gem for win32ole support on JRuby}

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end
