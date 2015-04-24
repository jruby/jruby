# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module YAML

  def load(string)
    raise "YAML.load not implemented"
  end
  module_function :load

  def dump(object)
    raise "YAML.dump not implemented"
  end
  module_function :dump

end

class Object

  def to_yaml
    raise "Object#to_yaml not implemented"
  end

end
