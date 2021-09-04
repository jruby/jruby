# bundler includes Bundler::SharedHelpers into its runtime
# adding the included method allows to monkey patch the runtime
# the moment it is used. i.e. no need to activate the bundler gem
module Bundler
  module Patch
    def clean_load_path
      # nothing to be done for JRuby
    end
  end
  module SharedHelpers
    def included(bundler)
      bundler.send :include, Patch
    end
  end
end
