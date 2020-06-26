package com.commonmodule.dynamicthreadpool;

import com.commonmodule.dynamicthreadpool.SysInfo.GCInfo;
import com.commonmodule.dynamicthreadpool.SysInfo.JVMMemory;
import org.apache.tomcat.util.threads.TaskQueue;
import org.apache.tomcat.util.threads.TaskThreadFactory;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class DynamicThreadPoolBuilder {

    private static Logger logger = LoggerFactory.getLogger(DynamicThreadPoolBuilder.class);

    private DynamicThreadPoolBuilder() {

    }

    public static DynamicThreadPoolBuilder custom() {
        return new DynamicThreadPoolBuilder();
    }

    public static ThreadPoolExecutor defaultBuild() {
        return new DynamicThreadPoolBuilder().Builder();
    }

    /**
     * 默认核心并发数<br>
     */
    private int defaultCorePoolSize = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 默认队列大小
     */
    private int defaultQueueSize = 500;

    /**
     * 默认线程存活时间
     */
    private int defaultKeepAliveTime = 60;

    /**
     * 默认最大并发数<br>
     */
    private int defaultMaximumPoolSize = Runtime.getRuntime().availableProcessors() * 2 * 4;

    /**
     * 是否自动扩容
     */
    private boolean autoScale = false;

    /**
     * 线程池名称格式
     */
    private String defaultThreadPoolName = "ThreadPoolBuilder-%d";

    public DynamicThreadPoolBuilder setDefaultCorePoolSize(int corePoolSize) {
        this.defaultCorePoolSize = corePoolSize;
        return this;
    }

    public DynamicThreadPoolBuilder setDefaultQueueSize(int queueSize) {
        this.defaultQueueSize = queueSize;
        return this;
    }

    public DynamicThreadPoolBuilder setDefaultKeepAliveTime(int keepAliveTime) {
        this.defaultKeepAliveTime = keepAliveTime;
        return this;
    }

    public DynamicThreadPoolBuilder setDefaultMaximumPoolSize(int maximumPoolSize) {
        this.defaultMaximumPoolSize = maximumPoolSize;
        return this;
    }

    public DynamicThreadPoolBuilder setDefaultThreadPoolName(String threadPoolName) {
        this.defaultThreadPoolName = threadPoolName;
        return this;
    }

    public DynamicThreadPoolBuilder setAutoScale(boolean autoScale) {
        this.autoScale = autoScale;
        return this;
    }

    /**
     * tomcat 线程池
     *
     * @return
     */
    public ThreadPoolExecutor Builder() {
        try {
            TaskQueue taskqueue = new TaskQueue(defaultQueueSize);
            TaskThreadFactory tf = new TaskThreadFactory(defaultThreadPoolName, true, Thread.NORM_PRIORITY);
            ThreadPoolExecutor executor = new ThreadPoolExecutor(defaultCorePoolSize, defaultMaximumPoolSize, defaultKeepAliveTime, TimeUnit.SECONDS, taskqueue, tf);
            executor.setThreadRenewalDelay(1000L);
            taskqueue.setParent(executor);
            if (autoScale) {
                new Adjust(executor, defaultQueueSize, defaultThreadPoolName);
            }
            return executor;
        } catch (Exception e) {
            logger.error("ThreadPoolBuilder init error.", e);
            throw new ExceptionInInitializerError(e);
        }
    }


    class Adjust {

        private ThreadPoolExecutor executor;
        private int queueSize;
        private String defaultThreadPoolName;

        //当超过了队列指定长度后，触发扩容
        private static final double QUEUESCALE_PERCNET = 0.7;
        // 最大线程数
        private static final int MAX_POOLSIZE = 2000;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        public Adjust(ThreadPoolExecutor executor, int queueSize, String defaultThreadPoolName) {
            this.executor = executor;
            this.queueSize = queueSize;
            this.defaultThreadPoolName = defaultThreadPoolName;

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    boolean scaleSuccess = scale();
                    //扩容成功后，休眠60S，避免在短时间内连续扩容
                    if (scaleSuccess) {
                        Thread.sleep(60 * 1000);
                    }
                } catch (Exception e) {
                }
            }, 60, 5, TimeUnit.SECONDS);

        }

        private boolean scale() {
            if (executor.getMaximumPoolSize() < MAX_POOLSIZE && executor.getQueue().size() > queueSize * QUEUESCALE_PERCNET) {
                if (systemFree()) {
                    executor.setMaximumPoolSize(Math.min((int) (executor.getMaximumPoolSize() * 1.5), MAX_POOLSIZE));
                    return true;
                }
            }
            return false;
        }


        /**
         * 这里注意  CMS Old Gen   ConcurrentMarkSweep，要根据实际配置的GC来调整
         */
        private boolean systemFree() {
            SysInfo sysInfo = SysInfo.getSysInfo();

            double oldGenUsedPercent = .0;
            List<JVMMemory> jvmMemoryList = sysInfo.getJVMMemoryPoolInfo();
            for (JVMMemory jvmMemory : jvmMemoryList) {
                if (jvmMemory.getName().equals("CMS Old Gen")) {
                    oldGenUsedPercent = (double) jvmMemory.getUsed() / (double) jvmMemory.getInit();
                }
            }

            long oldGCCount = 0;
            long oldGCTime = 0;
            List<GCInfo> gcInfos = sysInfo.getRecentGCInfo(60);
            for (GCInfo gcInfo : gcInfos) {
                if (gcInfo.getName().equals("ConcurrentMarkSweep")) {
                    oldGCCount = gcInfo.getCount();
                    oldGCTime = gcInfo.getTime();
                }
            }

            long runtimeFreeMemoryMB = sysInfo.getRuntimeFreeMemory() / 1024 / 1024;

            return sysInfo.getSystemLoadAverage() < 15
                    && sysInfo.getSystemCpuLoad() < 0.6
                    && sysInfo.getPhysicalMemoryUsedPercent() < 0.7
                    && oldGenUsedPercent < 0.7
                    && (oldGCCount < 5 && (oldGCTime / 1000 < 10))
                    && runtimeFreeMemoryMB < 1024;
        }
    }


}
