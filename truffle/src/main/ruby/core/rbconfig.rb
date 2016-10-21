# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module RbConfig
  jruby_home = Truffle::Boot.jruby_home_directory

  bindir = if jruby_home.end_with?('/mxbuild/ruby-zip-extracted')
    File.expand_path('../../bin', jruby_home)
  else
    "#{jruby_home}/bin"
  end

  CONFIG = {
    'exeext' => '',
    'EXEEXT' => '',
    'host_os' => Truffle::System.host_os,
    'host_cpu' => Truffle::System.host_cpu,
    'bindir' => bindir,
    'libdir' => "#{jruby_home}/lib/ruby/truffle",
    "sitelibdir"=>"#{jruby_home}/lib/ruby/2.3/site_ruby", # TODO BJF Oct 21, 2016 Need to review these values
    "sitearchdir"=>"#{jruby_home}/lib/ruby/2.3/site_ruby",
    'ruby_install_name' => 'jruby-truffle',
    'RUBY_INSTALL_NAME' => 'jruby-truffle',
    # 'ruby_install_name' => 'jruby',
    # 'RUBY_INSTALL_NAME' => 'jruby',
    'ruby_version' => '2.2.0',
    'OBJEXT' => 'll',
    'DLEXT' => 'su'
  }

  def self.ruby
    # TODO (eregon, 30 Sep 2016): should be the one used by the launcher!
    File.join CONFIG['bindir'], CONFIG['ruby_install_name'], CONFIG['exeext']
  end
end
