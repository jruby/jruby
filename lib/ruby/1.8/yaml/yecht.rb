#
# YAML::Yecht module
# .. glues yecht and yaml.rb together ..
#
require 'yecht'
require 'yaml/basenode'

module YAML
    module Yecht

        #
        # Mixin BaseNode functionality
        #
        class Node
            include YAML::BaseNode
        end

    end
end
