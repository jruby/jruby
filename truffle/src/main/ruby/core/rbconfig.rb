# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1
#
# Copyright (C) 1993-2013 Yukihiro Matsumoto. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
# 1. Redistributions of source code must retain the above copyright
# notice, this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright
# notice, this list of conditions and the following disclaimer in the
# documentation and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
# OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
# HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
# OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
# SUCH DAMAGE.
#
#  RbConfig.expand method imported from MRI sources
#

module RbConfig
  
  host_os = Truffle::System.host_os
  host_cpu = Truffle::System.host_cpu

  CONFIG = {
    'arch' => "#{host_cpu}-#{host_os}",
    'exeext' => '',
    'EXEEXT' => '',
    'host_os' => host_os,
    'host_cpu' => host_cpu,
    'ruby_install_name' => 'jruby-truffle',
    'RUBY_INSTALL_NAME' => 'jruby-truffle',
    # 'ruby_install_name' => 'jruby',
    # 'RUBY_INSTALL_NAME' => 'jruby',
    'ruby_version' => '2.2.0',
    'OBJEXT' => 'll',
    'DLEXT' => 'su',
    'includedir' => '',
  }

  MAKEFILE_CONFIG = {
      'configure_args' => ' ',
      'ARCH_FLAG' => '',
      'CPPFLAGS' => '',
      'LDFLAGS' => '',
      'DLDFLAGS' => '',
      'DLEXT' => 'su',
      'LIBEXT' => 'c',
      'OBJEXT' => 'bc',
      'EXEEXT' => '',
      'LIBS' => '',
      'DLDLIBS' => '',
      'LIBRUBYARG_STATIC' => '',
      'LIBRUBYARG_SHARED' => '',
      'libdirname' => 'libdir',
      'LIBRUBY' => '',
      'LIBRUBY_A' => '',
      'LIBRUBYARG' => '',
      'prefix' => '',
      'ruby_install_name' => 'jruby-truffle',
      'RUBY_SO_NAME' => '$(RUBY_BASE_NAME)'
  }
  
  ruby_home = Truffle::Boot.ruby_home

  if ruby_home
    bindir = if ruby_home.end_with?('/mxbuild/ruby-zip-extracted')
               File.expand_path('../../bin', ruby_home)
             else
               "#{ruby_home}/bin"
             end
    
    CONFIG.merge!({
      'bindir' => bindir,
      'libdir' => "#{ruby_home}/lib/ruby/truffle",
      "sitelibdir"=>"#{ruby_home}/lib/ruby/2.3/site_ruby", # TODO BJF Oct 21, 2016 Need to review these values
      "sitearchdir"=>"#{ruby_home}/lib/ruby/2.3/site_ruby",
      'rubyhdrdir' => "#{ruby_home}/lib/ruby/truffle/cext",
      'topdir' => "#{ruby_home}/lib/ruby/stdlib",
      "rubyarchhdrdir"=>"#{ruby_home}/lib/ruby/truffle/cext",
    })
    
    MAKEFILE_CONFIG.merge!({
      'hdrdir' => "#{ruby_home}/lib/ruby/truffle/cext",
      'bindir' => bindir
    })
  end

  if Truffle::Safe.memory_safe? && Truffle::Safe.processes_safe?
    clang = ENV['JT_CLANG'] || 'clang'
    opt = ENV['JT_OPT'] || 'opt'
    cc = "#{clang} -I#{ENV['SULONG_HOME']}/include"
    cpp = cc
    
    MAKEFILE_CONFIG.merge!({
        'CC' => cc,
        'CPP' => cpp,
        'COMPILE_C' => "$(CC) $(INCFLAGS) $(CPPFLAGS) $(CFLAGS) $(COUTFLAG)$< -o $@ && #{opt} -always-inline -mem2reg $@ -o $@",
        'CFLAGS' => "-c -emit-llvm",
        'LINK_SO' => "mx -v -p #{ENV['SULONG_HOME']} su-link -o $@ $(OBJS) $(LIBS)",
        'TRY_LINK' => "#{clang} $(src) $(INCFLAGS) $(CFLAGS) -I#{ENV['SULONG_HOME']}/include $(LIBS)"
    })
    
    CONFIG.merge!({
        'CC' => cc,
        'CPP' => cpp
    })
  end

  def self.ruby
    raise unless ruby_home
    # TODO (eregon, 30 Sep 2016): should be the one used by the launcher!
    jruby_truffle = CONFIG['ruby_install_name'] + CONFIG['exeext']
    File.join CONFIG['bindir'], jruby_truffle
  end

  def RbConfig.expand(val, config = CONFIG)
    newval = val.gsub(/\$\$|\$\(([^()]+)\)|\$\{([^{}]+)\}/) {
      var = $&
      if !(v = $1 || $2)
        '$'
      elsif key = config[v = v[/\A[^:]+(?=(?::(.*?)=(.*))?\z)/]]
        pat, sub = $1, $2
        config[v] = false
        config[v] = RbConfig.expand(key, config)
        key = key.gsub(/#{Regexp.quote(pat)}(?=\s|\z)/n) {sub} if pat
        key
      else
        var
      end
    }
    val.replace(newval) unless newval == val
    val
  end
end

CROSS_COMPILING = nil
