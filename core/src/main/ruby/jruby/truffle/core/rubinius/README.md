`kernel` contains the Ruby component of the Rubinius kernel (core library)
implementation, in some cases modified. We have taken files from version 2.4.1
of Rubinius. This code was written by Evan Phoenix, Brian Shirai, et al.

https://github.com/rubinius/rubinius

`api` contains code from `rubinius-core-api`, the Rubinius API implementation
written in Ruby, in some cases modified. We have taken files from commit
8d01207061518355da9b53274fe8766ecf85fdfe. This code was written by Evan Phoenix,
Charles Nutter, et al.

Some of the code from `rubinius-core-api` is also found at
`core/src/main/java/org/jruby/truffle/runtime/rubinius`, and again may be
modified.

https://github.com/rubinius/rubinius-core-api

We try not to modify files, so that they can easily be merged from upstream in
the future, but we have found it easier to modify inline in a few cases, rather
than somehow patch things from within JRuby+Truffle.

We have also directly attached copyright and license information to each file.
