#
# Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
#
# JRuby - http://jruby.sourceforge.net
#
# This file is part of JRuby
#
# JRuby is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License as
# published by the Free Software Foundation; either version 2 of the
# License, or (at your option) any later version.
#
# JRuby is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public
# License along with JRuby; if not, write to
# the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
# Boston, MA  02111-1307 USA

module JRuby
  module Compiler
    module Bytecode

      class AssignLocal
        attr_reader :index

        def initialize(index)
          @index = index
        end
      end

      class PushFixnum
        attr_reader :value

        def initialize(value)
          @value = value
        end
      end

      class PushSelf
      end

      class Call
        attr_reader :name
        attr_reader :arity

        def initialize(name, arity, type)
          @name, @arity, @type = name, arity, type
        end
      end

      class PushString
        attr_reader :value

        def initialize(value)
          @value = value
        end
      end

    end
  end
end
