# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Copyright (c) 2007-2015, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

module Rubinius
  module Type
    def self.const_get(mod, name, inherit=true, resolve=true)
      raise "unsupported" unless resolve
      mod.const_get name, inherit
    end

    def self.const_exists?(mod, name, inherit = true)
      mod.const_defined? name, inherit
    end
  end
end

ARGV.push *Truffle::Boot.original_argv

$LOAD_PATH.push *Truffle::Boot.original_load_path

home = Truffle::Boot.jruby_home_directory_protocol
# Does not exist but it's used by rubygems to determine index where to insert gem lib directories, as a result
# paths supplied by -I will stay before gem lib directories.
$LOAD_PATH.push home + '/lib/ruby/2.3/site_ruby'

$LOAD_PATH.push home + '/lib/ruby/truffle/mri'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-strscan/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-stringio/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-complex/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-date/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-pathname/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-tempfile/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-socket/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-securerandom/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-timeout/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/rubysl/rubysl-webrick/lib'
$LOAD_PATH.push home + '/lib/ruby/truffle/openssl'
$LOAD_PATH.push home + '/lib/ruby/truffle/truffle'

# We defined Psych at the top level because several things depend on its name.
# Here we fix that up and put it back into Truffle.

Truffle::Psych = Psych

class Object
  remove_const :Psych
end

class Module

  # Invokes <code>Module#append_features</code> and
  # <code>Module#included</code> on each argument, passing in self.
  #
  def include(*modules)
    modules.reverse_each do |mod|
      if !mod.kind_of?(Module) or mod.kind_of?(Class)
        raise TypeError, "wrong argument type #{mod.class} (expected Module)"
      end

      Truffle.privately do
        mod.append_features self
        mod.included self
      end
    end
    self
  end

end
