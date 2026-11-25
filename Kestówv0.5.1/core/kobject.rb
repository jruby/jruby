# frozen_string_literal: true

# Kestówv 0.5.1 — core/kobject.rb
#
# Base kernel object.
# Provides identity, reference counting, and basic lifecycle.
# All major kernel structures inherit from or compose with KObject.

module Kestowv
  module Core
    class KObject

      attr_reader :id, :created_at, :type_tag

      # -----------------------------------------------------------
      # CLASS-LEVEL ID ALLOCATOR
      # Atomic monotonic counter — same pattern as Linux's ida_alloc.
      # Using Mutex rather than AtomicInteger since we're pre-gem here.
      # -----------------------------------------------------------

      @@next_id  = 0
      @@id_mutex = Mutex.new

      def self.next_id
        @@id_mutex.synchronize { @@next_id += 1 }
      end

      # -----------------------------------------------------------
      # LIFECYCLE
      # -----------------------------------------------------------

      def initialize(type_tag: nil)
        @id         = self.class.next_id
        @created_at = ::Time.now.freeze
        @type_tag   = (type_tag || self.class.name&.split("::")&.last&.downcase&.to_sym || :kobject)
        @refcount   = 1
        @destroyed  = false
        @mutex      = Mutex.new
      end

      # -----------------------------------------------------------
      # REFERENCE COUNTING
      # -----------------------------------------------------------

      def retain
        @mutex.synchronize do
          raise ObjectDestroyedError, "retain on destroyed object #{self}" if @destroyed
          @refcount += 1
        end
        self
      end

      def release
        should_destroy = @mutex.synchronize do
          raise ObjectDestroyedError, "release on destroyed object #{self}" if @destroyed
          @refcount -= 1
          @refcount <= 0
        end

        if should_destroy
          @mutex.synchronize { @destroyed = true }
          destroy
        end

        self
      end

      # Reinitialise lifecycle fields so a Slab::Pool can recycle this object.
      # Called by Pool#acquire after popping from the free list, before the
      # type-specific reuse! method resets the payload fields.
      def slab_reclaim!
        @mutex.synchronize do
          @refcount   = 1
          @destroyed  = false
          @created_at = ::Time.now.freeze
        end
        self
      end

      def refcount
        @mutex.synchronize { @refcount }
      end

      def alive?
        @mutex.synchronize { !@destroyed }
      end

      # -----------------------------------------------------------
      # DESTRUCTION HOOK
      # Subclasses override for cleanup.
      # Always call super to mark destroyed and unregister from Boot.
      # -----------------------------------------------------------

      def destroy
        @mutex.synchronize { @destroyed = true }
        Boot.clear_bit(:"kobject_#{@id}") if defined?(Boot)
      end

      # -----------------------------------------------------------
      # INTROSPECTION
      # -----------------------------------------------------------

      def to_s
        "#<#{self.class}[#{@type_tag}]:#{@id} rc=#{refcount}>"
      end

      def inspect
        "#<#{self.class} id=#{@id} type=#{@type_tag} rc=#{refcount} " \
        "created=#{@created_at.iso8601} alive=#{alive?}>"
      end

      def to_h
        {
          id:         @id,
          type_tag:   @type_tag,
          refcount:   refcount,
          created_at: @created_at,
          alive:      alive?
        }
      end

      # -----------------------------------------------------------
      # ERROR TYPES
      # -----------------------------------------------------------

      class ObjectDestroyedError < RuntimeError; end

      private

      # Kept for backward compat — class method is preferred
      def allocate_id
        self.class.next_id
      end
    end
  end
end

# -----------------------------------------------------------
# AUTO-REGISTER — safe to call at file load time.
# Modules.register is idempotent (no-op if already registered).
# -----------------------------------------------------------
Kestowv::Config::Modules.register(
  :core_kobject,
  __FILE__,
  feature:    :kobject,
  depends_on: []
)
