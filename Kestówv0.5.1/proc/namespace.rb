# frozen_string_literal: true

# Kestówv 0.5.1 — proc/namespace.rb
#
# Namespace isolation.
# Each namespace type maintains its own view of a resource.
# Tasks belong to a set of namespaces.

module Kestowv
  module Proc
    module Namespace

      # Namespace types we support
      TYPES = %i[
        mount
        pid
        net
        ipc
        uts
        user
        time
        cgroup
      ].freeze

      class NamespaceSet
        attr_reader :id, :namespaces

        def initialize(id: nil)
          @id = id || SecureRandom.uuid
          @namespaces = {}
          @mutex = Mutex.new
        end

        def [](type)
          @mutex.synchronize { @namespaces[type] }
        end

        def []=(type, ns)
          raise ArgumentError, "Unknown namespace type: #{type}" unless TYPES.include?(type)
          @mutex.synchronize { @namespaces[type] = ns }
        end

        def clone
          new_set = NamespaceSet.new
          @mutex.synchronize do
            @namespaces.each do |type, ns|
              new_set[type] = ns.clone if ns.respond_to?(:clone)
            end
          end
          new_set
        end

        def to_h
          @mutex.synchronize { @namespaces.transform_values(&:to_h) }
        end
      end

      # --------------------------------------------------------
      # Concrete Namespace Types
      # --------------------------------------------------------

      class PidNamespace
        attr_reader :id, :pid_map

        def initialize(id: nil)
          @id = id || "pidns_#{SecureRandom.hex(4)}"
          @pid_map = {}          # local_pid => global Task
          @next_local_pid = 1
          @mutex = Mutex.new
        end

        def allocate_local_pid(task)
          @mutex.synchronize do
            pid = @next_local_pid
            @next_local_pid += 1
            @pid_map[pid] = task
            pid
          end
        end

        def lookup(local_pid)
          @mutex.synchronize { @pid_map[local_pid] }
        end

        def to_h
          { id: @id, pids: @pid_map.keys }
        end
      end

      class MountNamespace
        attr_reader :id, :root

        def initialize(id: nil, root: "/")
          @id = id || "mntns_#{SecureRandom.hex(4)}"
          @root = root
          @mounts = {}           # mountpoint => target
          @mutex = Mutex.new
        end

        def mount(source, target, type: nil)
          @mutex.synchronize { @mounts[target] = { source: source, type: type } }
        end

        def unmount(target)
          @mutex.synchronize { @mounts.delete(target) }
        end

        def resolve(path)
          # Very simplified — real implementation would walk mounts
          File.join(@root, path)
        end

        def to_h
          { id: @id, root: @root, mounts: @mounts }
        end
      end

      # --------------------------------------------------------
      # Module Interface
      # --------------------------------------------------------

      class << self

        def register
          Boot.register(:proc_namespace)
          Boot.set_bit(:proc_namespace)
        end
        alias register_with_boot register

        def create_set(types: TYPES)
          set = NamespaceSet.new
          types.each do |t|
            case t
            when :pid   then set[t] = PidNamespace.new
            when :mount then set[t] = MountNamespace.new
            else
              set[t] = BasicNamespace.new(t)
            end
          end
          set
        end

        def stats
          {
            feature: :proc_namespace,
            types:   TYPES.size
          }
        end
      end

      # Fallback for namespace types we haven't specialized yet
      class BasicNamespace
        attr_reader :type, :id

        def initialize(type)
          @type = type
          @id = "#{type}ns_#{SecureRandom.hex(4)}"
        end

        def to_h
          { id: @id, type: @type }
        end
      end
    end
  end
end

Kestowv::Config::Modules.register(
  :proc_namespace,
  __FILE__,
  feature:    :proc_namespace,
  depends_on: [:proc_task]
)
