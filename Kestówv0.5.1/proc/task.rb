# frozen_string_literal: true

# Kestówv 0.5.1 — proc/task.rb
#
# Task (lightweight process / thread group entry).
# Integrates with KThread, mm::VmSpace, and KObject lifecycle.

module Kestowv
  module Proc
    class Task < Kestowv::Core::KObject

      attr_reader :tid, :name

      # Task states — mirrors Linux task_struct states.
      STATES = %i[ready running blocked zombie].freeze

      # Class-level TID allocator — promoted from @@ to class ivar
      # so it doesn't leak across subclasses via Ruby's @@ inheritance.
      @next_tid  = 0
      @tid_mutex = Mutex.new

      def self.next_tid
        @tid_mutex.synchronize { @next_tid += 1 }
      end

      # --------------------------------------------------------
      # SLAB POOL — class-level object recycler
      # --------------------------------------------------------

      POOL_CAPACITY = 128

      @pool         = nil
      @pool_mu      = Mutex.new
      @in_pool_init = false

      class << self
        def init_pool(capacity: POOL_CAPACITY)
          @pool_mu.synchronize do
            return if @pool
            @in_pool_init = true
            @pool = Mm::Slab.create_pool(:task, capacity: capacity) do
              new(name: :_slab)
            end
            @in_pool_init = false
          end
          self
        end

        def slab_pool
          @pool
        end

        def slab_acquire(name: nil)
          obj = @pool&.acquire
          obj ? obj.reuse!(name: name) : new(name: name)
        end
      end

      # --------------------------------------------------------
      # LIFECYCLE
      # --------------------------------------------------------

      def initialize(name: nil, vm_space: nil)
        super(type_tag: :task)

        @tid      = self.class.next_tid
        @name     = (name || :"task_#{@tid}").to_sym
        @state    = :ready
        @vm_space = vm_space
        @cred     = nil
        @limits   = nil
        @ns_set   = nil

        # Signal state — all protected by @mutex
        @sig_mask     = 0          # blocked signals (bitmask)
        @sig_pending  = 0          # pending signals (bitmask)
        @sig_handlers = {}         # signo => :default | :ignore | Proc
        @sig_infos    = {}         # signo => SigInfo

        Boot.set_bit_raw(Boot.register(task_bit))
      end

      # --------------------------------------------------------
      # STATE MANAGEMENT — through inherited @mutex
      # --------------------------------------------------------

      def state
        @mutex.synchronize { @state }
      end

      def state=(new_state)
        raise ArgumentError, "Invalid state: #{new_state}" unless STATES.include?(new_state)

        @mutex.synchronize { @state = new_state }

        # Reflect terminal state in bit vector
        if new_state == :zombie
          pos = Boot.bit_position(task_bit)
          Boot.clear_bit_raw(pos) if pos
        end
      end

      def transition(new_state)
        self.state = new_state
        self
      end

      def runnable?
        @mutex.synchronize { @state == :ready || @state == :running }
      end

      def blocked?
        @mutex.synchronize { @state == :blocked }
      end

      def zombie?
        @mutex.synchronize { @state == :zombie }
      end

      # --------------------------------------------------------
      # VM SPACE
      # --------------------------------------------------------

      def vm_space
        @mutex.synchronize { @vm_space }
      end

      def assign_vm_space(space)
        raise ArgumentError, "Expected VmSpace" unless space.is_a?(Mm::VmSpace)
        @mutex.synchronize { @vm_space = space }
        self
      end

      # --------------------------------------------------------
      # CREDENTIALS
      # --------------------------------------------------------

      def cred
        @mutex.synchronize { @cred }
      end

      def assign_credentials(cred)
        raise ArgumentError, "Expected Cred" unless cred.is_a?(Proc::Credentials::Cred)
        @mutex.synchronize { @cred = cred }
        self
      end

      # --------------------------------------------------------
      # LIMITS
      # --------------------------------------------------------

      def limits
        @mutex.synchronize { @limits }
      end

      def assign_limits(limits)
        @mutex.synchronize { @limits = limits }
        self
      end

      # --------------------------------------------------------
      # NAMESPACE SET
      # --------------------------------------------------------

      def ns_set
        @mutex.synchronize { @ns_set }
      end

      def assign_ns_set(ns_set)
        raise ArgumentError, "Expected NamespaceSet" unless ns_set.is_a?(Proc::Namespace::NamespaceSet)
        @mutex.synchronize { @ns_set = ns_set }
        self
      end

      # --------------------------------------------------------
      # SIGNAL STATE (all reads/writes through @mutex)
      # --------------------------------------------------------

      def sig_mask
        @mutex.synchronize { @sig_mask }
      end

      def sig_pending
        @mutex.synchronize { @sig_pending }
      end

      def sig_handlers
        @mutex.synchronize { @sig_handlers.dup }
      end

      # Post a signal (called by SignalDelivery)
      def post_signal(info)
        return unless info.is_a?(Signal::SigInfo)

        @mutex.synchronize do
          @sig_pending |= Signal.bit(info.signo)
          @sig_infos[info.signo] = info
        end
        self
      end

      # Clear a specific signal from pending state
      def clear_signal(signo)
        @mutex.synchronize do
          @sig_pending &= ~Signal.bit(signo)
          @sig_infos.delete(signo)
        end
        self
      end

      # Remove and return the SigInfo for a signal (used during delivery)
      def dequeue_signal(signo)
        @mutex.synchronize do
          @sig_pending &= ~Signal.bit(signo)
          @sig_infos.delete(signo)
        end
      end

      # Set signal handler (called by user code or exec reset)
      def set_signal_handler(signo, handler)
        unless Signal.valid_handler?(handler)
          raise ArgumentError, "Invalid signal handler"
        end
        @mutex.synchronize { @sig_handlers[signo] = handler }
        self
      end

      # --------------------------------------------------------
      # SLAB REUSE — reinitialise payload after pool acquire
      # --------------------------------------------------------

      def reuse!(name: nil)
        # slab_reclaim! already called by Pool#acquire (resets @refcount + @destroyed)
        @mutex.synchronize do
          @name         = (name || :"task_#{@tid}").to_sym
          @state        = :ready
          @vm_space     = nil
          @cred         = nil
          @limits       = nil
          @ns_set       = nil
          @sig_mask     = 0
          @sig_pending  = 0
          @sig_handlers = {}
          @sig_infos    = {}
        end
        Boot.set_bit_raw(Boot.register(task_bit))
        self
      end

      # Return to slab pool instead of letting GC reclaim.
      def slab_release
        @mutex.synchronize do
          @refcount -= 1
          return self if @refcount > 0
          @state    = :zombie
          @destroyed = true
        end
        pos = Boot.bit_position(task_bit)
        Boot.clear_bit_raw(pos) if pos
        self.class.slab_pool&.release(self)
        self
      end

      # --------------------------------------------------------
      # LIFECYCLE OVERRIDE
      # --------------------------------------------------------

      def destroy
        @mutex.synchronize { @state = :zombie }
        pos = Boot.bit_position(task_bit)
        Boot.clear_bit_raw(pos) if pos
        super
      end

      # --------------------------------------------------------
      # INTROSPECTION
      # --------------------------------------------------------

      def to_h
        @mutex.synchronize do
          super.merge(
            tid:         @tid,
            name:        @name,
            state:       @state,
            vm_space:    @vm_space&.to_s,
            cred:        @cred&.to_s,
            ns_set:      @ns_set&.to_h,
            sig_pending: Signal.from_mask(@sig_pending),
            sig_mask:    Signal.from_mask(@sig_mask)
          )
        end
      end

      def to_s
        "#<Task #{@name}[#{@tid}] state=#{state} rc=#{refcount}>"
      end

      private

      def task_bit
        :"proc_task_#{@tid}"
      end
    end
  end
end

# --------------------------------------------------------
# AUTO-REGISTER
# Remove :core_scheduler until that module exists.
# --------------------------------------------------------
Kestowv::Config::Modules.register(
  :proc_task,
  __FILE__,
  feature:    :proc_task,
  depends_on: [:core_kobject, :core_thread, :mm_vm_space, :core_signals]
)
