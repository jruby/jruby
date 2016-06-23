# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

example "Encoding::US_ASCII.ascii_compatible?", true
example "Encoding::UTF_16BE.ascii_compatible?", false

example "Encoding::ISO_2022_JP.dummy?", true
example "Encoding::UTF_8.dummy?", false

example "Encoding.compatible?('abc', 'def')", Encoding::UTF_8
example "Encoding.compatible?(Encoding::UTF_8, Encoding::US_ASCII)", Encoding::UTF_8
example "Encoding.compatible?(Encoding::UTF_8, Encoding::ASCII_8BIT)", nil
