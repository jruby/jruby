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
    'EXEEXT' => 'rubytruffle',
    'host_os' => Truffle::Primitive.host_os,
    'libdir' => "#{Truffle::Primitive.home_directory}/lib/ruby/truffle",
    'ruby_install_name' => 'rubytruffle',
    'RUBY_INSTALL_NAME' => 'rubytruffle',
    'ruby_version' => '2.2.0',
  }
end
