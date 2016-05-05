# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module RbConfig
  CONFIG = {
    'exeext' => '',
    'EXEEXT' => '',
    'host_os' => Truffle::System.host_os,
    'host_cpu' => Truffle::System.host_cpu,
    'bindir' => "#{Truffle::Boot.jruby_home_directory}/bin",
    'libdir' => "#{Truffle::Boot.jruby_home_directory}/lib/ruby/truffle",
    'ruby_install_name' => 'jruby',
    'RUBY_INSTALL_NAME' => 'jruby',
    'ruby_version' => '2.2.0',
  }

  def self.ruby
    # TODO CS 19-May-15 should return the original command? Not sure that's possible
    "ruby"
  end
end
