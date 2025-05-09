module GC
  def self.stat(all_stats_or_key = {})
    gc_beans = java.lang.management.ManagementFactory.garbage_collector_mx_beans
    pool_beans = {}
    java.lang.management.ManagementFactory.memory_pool_mx_beans.each do |pool_bean|
      pool_beans[pool_bean.name] = pool_bean
    end

    case all_stats_or_key
    when Hash
      all_stats = all_stats_or_key
    when Symbol
      result_key = all_stats_or_key
    else
      raise TypeError.new("non-hash or symbol given")
    end

    count = time = 0
    committed = init = max = used = peak_committed = peak_init = peak_max =
      peak_used = last_committed = last_init = last_max = last_used = 0.0

    gc_beans.each do |gc_bean|
      unless result_key
        gc_stats = all_stats[gc_bean.name] = {}
        gc_stats[:count] = gc_bean.collection_count
        gc_stats[:time] = gc_bean.collection_time
      end

      count += gc_bean.collection_count
      time += gc_bean.collection_time

      gc_bean.memory_pool_names.each do |pool_name|
        pool_bean = pool_beans[pool_name]

        unless result_key
          all_pools = gc_stats[:pools] = {}
          pool_stats = all_pools[pool_name] = {}
        end

        usage = pool_bean.usage
        peak_usage = pool_bean.peak_usage
        last_usage = pool_bean.collection_usage

        unless result_key
          pool_stats[:committed] = usage.committed
          pool_stats[:init] = usage.init
          pool_stats[:max] = usage.max
          pool_stats[:used] = usage.used
          pool_stats[:peak_committed] = peak_usage.committed
          pool_stats[:peak_init] = peak_usage.init
          pool_stats[:peak_max] = peak_usage.max
          pool_stats[:peak_used] = peak_usage.used
          pool_stats[:last_committed] = last_usage.committed
          pool_stats[:last_init] = last_usage.init
          pool_stats[:last_max] = last_usage.max
          pool_stats[:last_used] = last_usage.used
        end

        committed += usage.committed
        init += usage.init
        max += usage.max
        used += usage.used
        peak_committed += peak_usage.committed
        peak_init += peak_usage.init
        peak_max += peak_usage.max
        peak_used += peak_usage.used
        last_committed += last_usage.committed
        last_init += last_usage.init
        last_max += last_usage.max
        last_used += last_usage.used
      end
    end

    case result_key
    when :count
      count
    when :time
      time
    when :committed
      committed
    when :init
      init
    when :max
      max
    when :used
      used
    when :peak_committed
      peak_committed
    when :peak_init
      peak_init
    when :peak_max
      peak_max
    when :peak_used
      peak_used
    when :last_committed
      last_committed
    when :last_init
      last_init
    when :last_max
      last_max
    when :last_used
      last_used
    when Symbol
      raise ArgumentError.new(format("unknown key: %s", result_key).force_encoding("UTF-8"))
    else
      all_stats.merge!({count:, time:, committed:, init:, max:, used:, peak_committed:, peak_init:, peak_max:, peak_used:, last_committed:, last_init:, last_max:, last_used:})
    end
  end

  begin
    module Profiler
      def self.__setup__
        @profiler ||= begin
          NotifiedProfiler.new!
          # class exists and Java dependecies are also present, proceed with GC notification version
        rescue NameError
          NormalProfiler.new
        end
      end

      def self.enabled?
        __setup__
        @profiler.enabled?
      end

      def self.enable
        __setup__
        @profiler.enable
      end

      def self.disable
        __setup__
        @profiler.disable
      end

      def self.clear
        __setup__
        @profiler.clear
      end

      def self.raw_data
        __setup__
        @profiler.raw_data
      end

      def self.result
        __setup__
        @profiler.result
      end

      def self.report
        __setup__
        @profiler.report
      end

      def self.total_time
        __setup__
        @profiler.total_time
      end

      begin
        class NotifiedProfiler
          HEADER = "   ID  Type                      Timestamp(sec)    Before(kB)     After(kB)    Delta(kB)        Heap(kB)          GC Time(ms) \n"
          FORMAT = "%5d  %-20s %19.4f %13i %13i %12i %15i %20.10f\n"

          def self.new!
            javax.management.NotificationListener # try to access
            com.sun.management.GarbageCollectionNotificationInfo
            new
          end

          def enabled?
            @gc_listener != nil
          end

          def enable
            @gc_listener ||= begin
              # delay JI class-loading, generate listener on first use :
              gc_listener = Class.new do # GCListener
                include javax.management.NotificationListener

                def initialize
                  @lines = []
                end

                attr_accessor :lines

                def handleNotification(notification, o)
                  lines << notification
                end

                def clear
                  lines.clear
                end
              end
              gc_listener.new
            end
            java.lang.management.ManagementFactory.garbage_collector_mx_beans.each do |gc_bean|
              gc_bean.add_notification_listener @gc_listener, nil, nil
            end
          end

          def disable
            java.lang.management.ManagementFactory.garbage_collector_mx_beans.each do |gc_bean|
              begin
                gc_bean.remove_notification_listener @gc_listener
              rescue javax.management.ListenerNotFoundException
                # ignore missing listeners
              end
            end
            @gc_listener = nil
          end

          def clear
            @gc_listener.clear if @gc_listener
          end

          def raw_data
            @gc_listener.lines.dup
          end

          def report
            result($stderr)
          end

          def result(out = "")
            return nil unless @gc_listener
            
            lines = raw_data

            counts = Hash.new(0)
            report_lines = []

            lines.each_with_index do |line, i|
              out << HEADER if i % 20 == 0

              gc_notification = com.sun.management.GarbageCollectionNotificationInfo.from(line.user_data)
              gc_info = gc_notification.gc_info

              mem_before = gc_info.memory_usage_before_gc
              mem_after = gc_info.memory_usage_after_gc

              after = 0
              before = 0
              commit = 0

              mem_after.entry_set.each do |entry|
                name = entry.key

                before_usage = mem_before[name]
                before += before_usage.used

                after_usage = entry.value
                after += after_usage.used

                commit += after_usage.committed
              end

              counts[gc_notification.gc_name] += 1

              out << sprintf(
                       FORMAT,
                       gc_info.id,
                       gc_notification.gc_name,
                       gc_info.start_time/1000000.0,
                       before / 1024,
                       after / 1024,
                       (before - after) / 1024,
                       commit / 1024,
                       gc_info.duration/1000.0)
            end

            out << "GC: #{counts.map{|k,v| "#{v} #{k}"}.join(', ')}\n" + report_lines.join("\n")

            out
          end

          def total_time
            duration = 0

            @gc_listener.lines.each_with_index do |line, i|
              gc_notification = com.sun.management.GarbageCollectionNotificationInfo.from(line.user_data)
              gc_info = gc_notification.gc_info

              duration += gc_info.duration
            end if @gc_listener

            return duration / 1000.0
          end
        end
        private_constant :NotifiedProfiler
      rescue Exception
        # ignore, leave it undefined
      end

      # No GC notifications, use polled version
      class NormalProfiler
        def enabled?
          @gc_beans != nil
        end

        def enable
          @gc_beans ||= java.lang.management.ManagementFactory.garbage_collector_mx_beans
          clear
        end

        def disable
          @gc_beans = nil
        end

        def clear
          return unless @gc_beans

          time = 0
          @gc_beans.each do |gc_bean|
            time += gc_bean.collection_time
          end
          @start_time = time
        end

        def result
          nil
        end

        def report
          nil
        end

        def total_time
          time = 0
          @gc_beans.each do |gc_bean|
            time += gc_bean.collection_time
          end
          (time - @start_time) / 1000.0
        end
      end
      private_constant :NormalProfiler
    end
  end
end
