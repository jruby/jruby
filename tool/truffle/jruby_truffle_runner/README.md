# jruby+truffle_runner gem

-   Use `jt install-tool` or add alias to the gem's executable, 
    e.g. `~/Workspace/labs/jruby/tool/truffle/jruby_truffle_runner/bin/jruby+truffle`
-   `git clone foo` project/gem you would like to run in JRuby+Truffle
-   `cd foo`
-   You need other Ruby (MRI or JRuby) to run the tool.
-   `jruby+truffle setup` to install required gems and prepare environment to run on JRuby+Truffle
-   `jruby+truffle setup a_file` to execute files in prepared environment of the gem
-   For more information see `jruby+truffle --help` 

