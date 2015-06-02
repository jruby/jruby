Contributions are much appreciated.  
Please open a pull request or add an issue to discuss what you intend to work on.  
If the pull requests passes the CI and conforms to the existing style of specs, it will be merged.

### Creating files for currently unspecified modules or classes

If you want to create specs for a module or class and the files do not exist yet,
you should use `mkspec`, part of [MSpec](http://github.com/ruby/mspec):

    $ path/to/mspec/bin/mkspec -h

Specify `core` or `library` as the `base`.
