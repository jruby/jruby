# frozen_string_literal: true

# Kestówv 0.5.1 — proc/cgroup.rb
#
# Control group (cgroup v2-style) management.
# Organizes tasks into a hierarchy with per-controller resource limits.
# Mirrors Linux cgroup v2: unified hierarchy, one tree, controller delegation.

module Kestowv
  module Proc
    module Cgroup

      # --------------------------------------------------------
      # CONTROLLERS — resource subsystems
      # --------------------------------------------------------

      CONTROLLERS = %i[cpu memory io pids].freeze

      # Default controller limits — override per cgroup.
      CONTROLLER_DEFAULTS = {
        cpu:    { weight: 100, max: :unlimited },
        memory: { min: 0, max: :unlimited, swap_max: 0 },
        io:     { rbps: :unlimited, wbps: :unlimited },
        pids:   { max: :unlimited }
      }.freeze

      # --------------------------------------------------------
      # CGROUP NODE
      # --------------------------------------------------------

      class CgroupNode
        attr_reader :name, :id, :parent_id, :controllers, :created_at

        def initialize(id:, name:, parent_id: nil, controllers: [])
          @id          = id
          @name        = name.to_sym
          @parent_id   = parent_id
          @controllers = (controllers & CONTROLLERS).freeze
          @limits      = build_default_limits
          @members     = Set.new   # PIDs
          @children    = Set.new   # child cgroup IDs
          @created_at  = Time.now.freeze
          @mutex       = Mutex.new
        end

        def root?
          @parent_id.nil?
        end

        # --------------------------------------------------------
        # MEMBERSHIP
        # --------------------------------------------------------

        def add_task(pid)
          @mutex.synchronize { @members.add(pid) }
        end

        def remove_task(pid)
          @mutex.synchronize { @members.delete(pid) }
        end

        def members
          @mutex.synchronize { @members.to_a.sort }
        end

        def member?(pid)
          @mutex.synchronize { @members.include?(pid) }
        end

        def task_count
          @mutex.synchronize { @members.size }
        end

        # --------------------------------------------------------
        # CHILDREN
        # --------------------------------------------------------

        def add_child(id)
          @mutex.synchronize { @children.add(id) }
        end

        def remove_child(id)
          @mutex.synchronize { @children.delete(id) }
        end

        def child_ids
          @mutex.synchronize { @children.to_a.sort }
        end

        def leaf?
          @mutex.synchronize { @children.empty? }
        end

        # --------------------------------------------------------
        # LIMITS
        # --------------------------------------------------------

        def set_limit(controller, key, value)
          return false unless @controllers.include?(controller)
          @mutex.synchronize do
            @limits[controller] ||= {}
            @limits[controller][key] = value
          end
          true
        end

        def get_limit(controller, key)
          @mutex.synchronize { @limits.dig(controller, key) }
        end

        def limits
          @mutex.synchronize { @limits.dup }
        end

        # Check if a task would exceed pids.max by adding one more.
        def pids_exceeded?
          @mutex.synchronize do
            max = @limits.dig(:pids, :max)
            next false if max == :unlimited
            @members.size >= max
          end
        end

        # --------------------------------------------------------
        # INTROSPECTION
        # --------------------------------------------------------

        def to_h
          @mutex.synchronize do
            {
              id:          @id,
              name:        @name,
              parent_id:   @parent_id,
              root:        root?,
              controllers: @controllers,
              limits:      @limits.dup,
              members:     @members.to_a.sort,
              children:    @children.to_a.sort,
              created_at:  @created_at
            }
          end
        end

        def to_s
          "#<Cgroup #{@name}[#{@id}] tasks=#{task_count} ctrl=#{@controllers.join(',')}>"
        end

        private

        def build_default_limits
          @controllers.each_with_object({}) do |ctrl, h|
            h[ctrl] = CONTROLLER_DEFAULTS[ctrl].dup
          end
        end
      end

      # --------------------------------------------------------
      # REGISTRY
      # --------------------------------------------------------

      @cgroups    = {}   # id => CgroupNode
      @task_map   = {}   # pid => cgroup id
      @next_id    = 0
      @root_id    = nil
      @mutex      = Mutex.new

      class << self

        # --------------------------------------------------------
        # BOOT REGISTRATION
        # --------------------------------------------------------

        def register_with_boot
          Boot.register(:proc_cgroup)
          Boot.set_bit(:proc_cgroup)
          init_root
          self
        end

        def init_root
          root = create("/", parent_id: nil, controllers: CONTROLLERS)
          @root_id = root.id
          self
        end

        # --------------------------------------------------------
        # CGROUP MANAGEMENT
        # --------------------------------------------------------

        def create(name, parent_id: @root_id, controllers: [])
          id = allocate_id

          node = CgroupNode.new(
            id:          id,
            name:        name,
            parent_id:   parent_id,
            controllers: controllers
          )

          @mutex.synchronize do
            @cgroups[id] = node
            @cgroups[parent_id]&.add_child(id) if parent_id
          end

          node
        end

        def destroy(id)
          return false if id == @root_id   # cannot destroy root

          node = @mutex.synchronize { @cgroups[id] }
          return false unless node
          return false unless node.leaf?
          return false unless node.task_count.zero?

          @mutex.synchronize do
            @cgroups.delete(id)
            @cgroups[node.parent_id]&.remove_child(id)
          end
          true
        end

        # --------------------------------------------------------
        # TASK MANAGEMENT
        # --------------------------------------------------------

        # Move a task into a cgroup. Removes from previous cgroup.
        def assign(pid, cgroup_id)
          @mutex.synchronize do
            node = @cgroups[cgroup_id]
            next :not_found unless node
            next :pids_exceeded if node.pids_exceeded?

            old_id = @task_map[pid]
            @cgroups[old_id]&.remove_task(pid) if old_id

            node.add_task(pid)
            @task_map[pid] = cgroup_id
            :ok
          end
        end

        def unassign(pid)
          @mutex.synchronize do
            cid = @task_map.delete(pid)
            @cgroups[cid]&.remove_task(pid) if cid
          end
        end

        def cgroup_for(pid)
          @mutex.synchronize do
            cid = @task_map[pid]
            @cgroups[cid] if cid
          end
        end

        # --------------------------------------------------------
        # QUERY
        # --------------------------------------------------------

        def get(id)
          @mutex.synchronize { @cgroups[id] }
        end

        def root
          @mutex.synchronize { @cgroups[@root_id] }
        end

        def all
          @mutex.synchronize { @cgroups.values.dup }
        end

        def to_a
          @mutex.synchronize { @cgroups.values.map(&:to_h) }
        end

        def stats
          @mutex.synchronize do
            by_ctrl = CONTROLLERS.each_with_object({}) do |ctrl, h|
              h[ctrl] = @cgroups.count { |_, n| n.controllers.include?(ctrl) }
            end

            {
              feature:  :proc_cgroup,
              total:    @cgroups.size,
              tasks:    @task_map.size,
              by_ctrl:  by_ctrl
            }
          end
        end

        private

        def allocate_id
          @mutex.synchronize { @next_id += 1 }
        end

      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :proc_cgroup,
  __FILE__,
  feature:    :proc_cgroup,
  depends_on: [:proc_pid, :proc_task, :proc_limits]
)
