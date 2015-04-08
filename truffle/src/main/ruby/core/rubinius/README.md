`bootstrap`, `common`, `delta` and `platform` contains the Ruby component of the
Rubinius kernel (core library) implementation, in some cases modified. We have
taken files from version 2.4.1 of Rubinius. This code was written by Evan
Phoenix, Brian Shirai, et al.

https://github.com/rubinius/rubinius

Some files in the parent directory contain small snippets of code from the same
repository. These are individually annotated.

`api` contains code from `rubinius-core-api`, the Rubinius API implementation
written in Ruby, in some cases modified. We have taken files from commit
8d01207061518355da9b53274fe8766ecf85fdfe. This code was written by Evan Phoenix,
Charles Nutter, et al.

https://github.com/rubinius/rubinius-core-api

`api/shims` is our own code.

We try not to modify files from Rubinius, so that they can easily be merged from
upstream in the future. In some cases there are shims that patch up Rubinius
code after it has been loaded, such as `api/shims`. We have also tried to keep
files from `rubinius-core-api` and Rubinius itself separate and to be consistent
about what we're using from where and why. In the end it's a bit of a mix from
the two projects, and a mix of modification and shimming that works for us.

The only file the VM loads is `core.rb` - everything else is loaded from there.

We have directly attached copyright and license information to each file.
