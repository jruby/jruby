Contributions are much appreciated.  
Please open a pull request or add an issue to discuss what you intend to work on.  
If the pull requests passes the CI and conforms to the existing style of specs, it will be merged.

### Creating files for currently unspecified modules or classes

If you want to create specs for a module or class and the files do not exist yet,
you should use `mkspec`, part of [MSpec](http://github.com/ruby/mspec):

    $ path/to/mspec/bin/mkspec -h

Specify `core` or `library` as the `base`.

### Guards

Different guards are available as defined by mspec.
In general, the usage of guards shuld be minimized as possible.

The following guards are deprecated and should not be used in new code:
* `not_compliant_on`: Simply tag the spec as failing instead.  
  If it makes sense to test part of the example, split it (an example should have only one or a couple `should`).
* `compliant_on` / `deviates_on`: RubySpec defines common behavior and not implementation details.  
  Use the implementation suite of tests/specs for these.

### Style

Do not leave any trailing space and respect the existing style.
