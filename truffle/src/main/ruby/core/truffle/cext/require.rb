# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

if Truffle::CExt.supported?

  # Monkey patch #require, similar to how RubyGems does, in order to load
  # C extensions.

  def require(name)
    # We're getting quite hacky here. A lot of C extensions are required
    # using the format foo/foo, so we need to guess that the real name is
    # foo from that.

    if name =~ /(.+?)\/\1/
      cext_name = $1
    else
      cext_name = name
    end

    # Look in each $JRUBY_TRUFFLE_CEXT_PATH directory for
    # cext_name/ext/cext_name/extconf.rb

    if ENV.include? 'JRUBY_TRUFFLE_CEXT_PATH'
      cext_path = ENV['JRUBY_TRUFFLE_CEXT_PATH'].split(':')
    else
      cext_path = [Dir.pwd]
    end

    cext_path.each do |dir|
      extconf = File.join(dir, cext_name, 'ext', cext_name, 'extconf.rb')

      if File.exist? extconf
        return Truffle::CExt::load_extconf(extconf)
      end
    end

    Kernel.require name
  end

end
