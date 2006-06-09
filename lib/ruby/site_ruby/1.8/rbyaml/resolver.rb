require 'rbyaml/util'
require 'rbyaml/nodes'
require 'rbyaml/error'
  
module RbYAML
  class ResolverError < MarkedYAMLError
  end

  DEFAULT_SCALAR_TAG = 'tag:yaml.org,2002:str'
  DEFAULT_SEQUENCE_TAG = 'tag:yaml.org,2002:seq'
  DEFAULT_MAPPING_TAG = 'tag:yaml.org,2002:map'
  
  class BaseResolver
    @@yaml_implicit_resolvers = {}
    @@yaml_path_resolvers = {}

    def initialize
      @resolver_exact_paths = []
      @resolver_prefix_paths = []
    end
    
    def self.add_implicit_resolver(tag, regexp, first)
      if first.nil?
        first = ""
      end
      first.each_byte { |ch|
        @@yaml_implicit_resolvers[ch] ||= []
        @@yaml_implicit_resolvers[ch] << [tag,regexp]
      }
    end

    def self.add_path_resolver(tag, path, kind=nil)
      new_path = []
      for element in path
        if element.__is_a
          if element.length == 2
            node_check, index_check = element
          elsif element.length == 1
            node_check = element[0]
            index_check = true
          else
            raise ResolverError.new("Invalid path element: #{element}")
          end
        else
          node_check = nil
          index_check = element
        end
        if String == node_check
          node_check = ScalarNode
        elsif Array == node_check
          node_check = SequenceNode
        elsif Hash == node_check
          node_check = MappingNode
        elsif ![ScalarNode, SequenceNode, MappingNode].include?(node_check) && !node_check.__is_sym && !node_check.nil?
          raise ResolverError.new("Invalid node checker: #{node_check}")
        end
        if !(index_check.__is_str || index_check.__is_int) && !index_check.nil?
          raise ResolverError.new("Invalid index checker: #{index_check}")
        end
        new_path << [node_check, index_check]
      end
      if String == kind
        kind = ScalarNode
      elsif Array == kind
        kind = SequenceNode
      elsif Hash == kind
        kind = MappingNode
      elsif ![ScalarNode, SequenceNode, MappingNode].include?(kind) && !kind.nil?
        raise ResolverError.new("Invalid node kind: #{kind}")
      end
      @@yaml_path_resolvers[[[new_path], kind]] = tag
    end

    def descend_resolver(current_node, current_index)
      exact_paths = {}
      prefix_paths = []
      if current_node
        depth = @resolver_prefix_paths.length
        for path, kind in @resolver_prefix_paths[-1]
          if check_resolver_prefix(depth, path, kind,current_node, current_index)
            if path.length > depth
              prefix_paths << [path, kind]
            else
              exact_paths[kind] = @@yaml_path_resolvers[[path, kind]]
            end
          end
        end
      else
        for path, kind in @@yaml_path_resolvers.keys
          if !path
            exact_paths[kind] = @@yaml_path_resolvers[[path, kind]]
          else
            prefix_paths << [path, kind]
          end
        end
      end
      @resolver_exact_paths << exact_paths
      @resolver_prefix_paths << prefix_paths
    end

    def ascend_resolver
      @resolver_exact_paths.pop
      @resolver_prefix_paths.pop
    end

    def check_resolver_prefix(depth, path, kind, current_node, current_index)
      node_check, index_check = path[depth-1]
      if node_check.__is_str
        return false if current_node.tag != node_check
      elsif !node_check.nil?
        return false if !node_check === current_node
      end
      return false if index_check==true && !current_index.nil?
      return false if !index_check && current_index.nil?
      if index_check.__is_str
        return false if !(current_index.__is_scalar && index_check == current_index.value)
      elsif index_check.__is_int
        return false if index_check != current_index
      end
      true
    end

    def resolve(kind, value, implicit)
      if ScalarNode == kind && implicit[0]
        if value == ""
          resolvers = @@yaml_implicit_resolvers.fetch("",[])
        else
          resolvers = @@yaml_implicit_resolvers.fetch(value[0],[])
        end
        resolvers += @@yaml_implicit_resolvers.fetch(nil,[])
        for tag, regexp in resolvers
          return tag if regexp =~ value
        end
        implicit = implicit[1]
      end
      exact_paths = @resolver_exact_paths[-1]
      return exact_paths[kind] if exact_paths.include?(kind) 
      return exact_paths[nil] if exact_paths.include?(nil)
      if ScalarNode == kind
        return RbYAML::DEFAULT_SCALAR_TAG
      elsif SequenceNode == kind
        return RbYAML::DEFAULT_SEQUENCE_TAG
      elsif MappingNode == kind
        return RbYAML::DEFAULT_MAPPING_TAG
      end
    end
  end

  class Resolver < BaseResolver
  end
  
  BaseResolver.add_implicit_resolver('tag:yaml.org,2002:bool',/^(?:y|Y|yes|Yes|YES|n|N|no|No|NO|true|True|TRUE|false|False|FALSE|on|On|ON|off|Off|OFF)$/,'yYnNtTfFoO')
  BaseResolver.add_implicit_resolver('tag:yaml.org,2002:float',/^(?:[-+]?(?:[0-9][0-9_]*)\.[0-9_]*(?:[eE][-+][0-9]+)?|[-+]?(?:[0-9][0-9_]*)?\.[0-9_]+(?:[eE][-+][0-9]+)?|[-+]?[0-9][0-9_]*(?::[0-5]?[0-9])+\.[0-9_]*|[-+]?\.(?:inf|Inf|INF)|\.(?:nan|NaN|NAN))$/,'-+0123456789.')
  BaseResolver.add_implicit_resolver('tag:yaml.org,2002:int',/^(?:[-+]?0b[0-1_]+|[-+]?0[0-7_]+|[-+]?(?:0|[1-9][0-9_]*)|[-+]?0x[0-9a-fA-F_]+|[-+]?[1-9][0-9_]*(?::[0-5]?[0-9])+)$/,'-+0123456789')
  BaseResolver.add_implicit_resolver('tag:yaml.org,2002:merge',/^(?:<<)$/,'<')
  BaseResolver.add_implicit_resolver('tag:yaml.org,2002:null',/^(?: ~|null|Null|NULL| )$/,'~nN' + ?\0.chr)
  BaseResolver.add_implicit_resolver('tag:yaml.org,2002:timestamp',/^(?:[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]|[0-9][0-9][0-9][0-9]-[0-9][0-9]?-[0-9][0-9]?(?:[Tt]|[ \t]+)[0-9][0-9]?:[0-9][0-9]:[0-9][0-9](?:\.[0-9]*)?(?:[ \t]*(?:Z|[-+][0-9][0-9]?(?::[0-9][0-9])?))?)$/,'0123456789')
  BaseResolver.add_implicit_resolver('tag:yaml.org,2002:value',/^(?:=)$/,'=')
  # The following implicit resolver is only for documentation purposes. It cannot work
  # because plain scalars cannot start with '!', '&', or '*'.
  BaseResolver.add_implicit_resolver('tag:yaml.org,2002:yaml',/^(?:!|&|\*)$/,'!&*')
end

