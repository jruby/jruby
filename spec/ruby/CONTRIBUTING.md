Contributions are much appreciated.  
Please open a pull request or add an issue to discuss what you intend to work on.  
If the pull requests passes the CI and conforms to the existing style of specs, it will be merged.

### MkSpec - a tool to generate the spec structure

If you want to create new specs, you should use `mkspec`, part of [MSpec](http://github.com/ruby/mspec).

    $ ../mspec/bin/mkspec -h

#### Creating files for unspecified modules or classes

For instance, to create specs for `forwardable`:

    $ ../mspec/bin/mkspec -b library -rforwardable -c Forwardable

Specify `core` or `library` as the `base`.

#### Finding unspecified core methods

This is very easy, just run the command below in your `rubyspec` directory.  
`ruby` must be a recent version of MRI.

    $ ruby --disable-gem ../mspec/bin/mkspec 

You might also want to search for:

    it "needs to be reviewed for spec completeness"

which indicates the file was generated but the method unspecified.

### Guards

Different guards are available as defined by mspec.
In general, the usage of guards should be minimized as possible.

The following guards are deprecated and should not be used in new code:
* `not_compliant_on`: Simply tag the spec as failing instead.  
  If it makes sense to test part of the example, split it (an example should have only one or a few `should`).
* `compliant_on` / `deviates_on`: RubySpec defines common behavior and not implementation details.  
  Use the implementation suite of tests/specs for these.

### Style

Do not leave any trailing space and respect the existing style.
