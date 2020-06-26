package com.commonmodule.dynamicthreadpool;

import com.google.common.collect.Lists;
import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SysInfo {

    private static Logger logger = LoggerFactory.getLogger(SysInfo.class);

    private static SysInfo sysInfo;
    private static final Object lock = new Object();
    private static final TreeMap<Long, List<GCInfo>> lastGCInfo = new TreeMap();


    private static ScheduledExecutorService printScheduler;
    private static OperatingSystemMXBean operatingSystemMXBean;
    private static java.lang.management.OperatingSystemMXBean operatingSystemMXBean2;
    private static List<MemoryPoolMXBean> memoryPoolMXBeanList;
    private static MemoryMXBean memoryMXBean;
    private static List<GarbageCollectorMXBean> garbageCollectorMXBeanList;
    private static ScheduledExecutorService scheduler;
    private static Runtime runtime;

    public static SysInfo getSysInfo() {
        if (sysInfo == null) {
            synchronized (lock) {
                if (sysInfo == null) {
                    try {
                        sysInfo = new SysInfo();
                        printScheduler.scheduleAtFixedRate(() -> {
                            try {
                                printSysInfo();
                            } catch (Exception e) {
                            }
                        }, 0, 60, TimeUnit.SECONDS);


                    } catch (Exception e) {
                        logger.error("SysInfo init failed " + e.getMessage());
                    }
                }
            }
        }
        return sysInfo;
    }

    private SysInfo() {
        operatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        operatingSystemMXBean2 = ManagementFactory.getOperatingSystemMXBean();
        memoryPoolMXBeanList = ManagementFactory.getMemoryPoolMXBeans();
        memoryMXBean = ManagementFactory.getMemoryMXBean();
        garbageCollectorMXBeanList = ManagementFactory.getGarbageCollectorMXBeans();
        runtime = Runtime.getRuntime();
        scheduler = Executors.newScheduledThreadPool(1);
        printScheduler = Executors.newScheduledThreadPool(1);
        refreshGCInfo();
    }

    public double getSystemLoadAverage() {
        return operatingSystemMXBean2.getSystemLoadAverage();
    }

    public double getSystemCpuLoad() {
        return operatingSystemMXBean.getSystemCpuLoad();
    }

    public double getProcessCpuLoad() {
        return operatingSystemMXBean.getProcessCpuLoad();
    }

    public double getTotalPhysicalMemorySize() {
        return operatingSystemMXBean.getTotalPhysicalMemorySize();
    }

    public double getFreePhysicalMemorySize() {
        return operatingSystemMXBean.getFreePhysicalMemorySize();
    }

    public double getPhysicalMemoryUsedPercent() {
        return (double) operatingSystemMXBean.getFreePhysicalMemorySize() / (double) operatingSystemMXBean.getTotalPhysicalMemorySize();
    }

    public long getRuntimeFreeMemory() {
        return runtime.freeMemory();
    }

    private void refreshGCInfo() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                long time = System.currentTimeMillis() / 1000;
                lastGCInfo.put(time, getCurrentGCInfo());

                long inActiveTime = System.currentTimeMillis() / 1000 - 10 * 60;
                lastGCInfo.headMap(inActiveTime).clear();

            } catch (Exception e) {
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public List<GCInfo> getCurrentGCInfo() {

        List<GCInfo> gcInfos = new ArrayList<>();
        GCInfo gcInfo;
        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeanList) {
            gcInfo = new GCInfo(garbageCollectorMXBean.getName(),
                                garbageCollectorMXBean.getCollectionCount(),
                                garbageCollectorMXBean.getCollectionTime());

            gcInfos.add(gcInfo);
        }
        return gcInfos;
    }

    public List<GCInfo> getRecentGCInfo(int seconds) {

        long time = System.currentTimeMillis() / 1000 - seconds;

        long firstKey = lastGCInfo.tailMap(time).firstKey();
        List<GCInfo> lastGCInfos = lastGCInfo.get(firstKey);

        List<GCInfo> currentGCInfos = getCurrentGCInfo();

        List<GCInfo> recent = new ArrayList<>();
        GCInfo recentGCInfo;
        for (GCInfo currentGCInfo : currentGCInfos) {
            for (GCInfo gcInfo : lastGCInfos) {
                if (currentGCInfo.name == gcInfo.name) {
                    recentGCInfo = new GCInfo(currentGCInfo.name,
                                              currentGCInfo.count - gcInfo.count,
                                              currentGCInfo.time - gcInfo.time);
                    recent.add(recentGCInfo);
                }
            }
        }
        return recent;
    }

    public List<JVMMemory> getJVMMemoryPoolInfo() {
        List<JVMMemory> jvmMemoryList = Lists.newArrayList();

        JVMMemory jvmMemory;
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeanList) {
            jvmMemory = new JVMMemory(memoryPoolMXBean.getName(),
                                      memoryPoolMXBean.getUsage().getInit(),
                                      memoryPoolMXBean.getUsage().getMax(),
                                      memoryPoolMXBean.getUsage().getUsed());
            jvmMemoryList.add(jvmMemory);
        }
        return jvmMemoryList;
    }


    public static void printSysInfo() {

        StringBuilder sysInfo = new StringBuilder();
        sysInfo.append(System.lineSeparator());
        sysInfo.append("系统CPU负载:").append(operatingSystemMXBean2.getSystemLoadAverage()).append(System.lineSeparator());

        sysInfo.append("系统CPU利用率:").append(operatingSystemMXBean.getSystemCpuLoad()).append(System.lineSeparator());
        sysInfo.append("进程CPU利用率:").append(operatingSystemMXBean.getProcessCpuLoad()).append(System.lineSeparator());
        sysInfo.append("物理内存:").append(operatingSystemMXBean.getTotalPhysicalMemorySize() / 1024 / 1024).append("MB").append(System.lineSeparator());
        sysInfo.append("物理可用内存:").append(operatingSystemMXBean.getFreePhysicalMemorySize() / 1024 / 1024).append("MB").append(System.lineSeparator());
        sysInfo.append("内存利用率:").append((double) operatingSystemMXBean.getFreePhysicalMemorySize() / (double) operatingSystemMXBean.getTotalPhysicalMemorySize()).append(System.lineSeparator());

        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeanList) {
            sysInfo.append("GC name:").append(garbageCollectorMXBean.getName()).append("\t");
            sysInfo.append("GC count:").append(garbageCollectorMXBean.getCollectionCount()).append("\t");
            sysInfo.append("GC time:").append(garbageCollectorMXBean.getCollectionTime()).append("ms").append(System.lineSeparator());
        }


        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeanList) {
            sysInfo.append("memory name:").append(memoryPoolMXBean.getName()).append("\t");
            sysInfo.append("memory inin:").append(memoryPoolMXBean.getUsage().getInit() / 1024 / 1024).append("MB").append("\t");
            sysInfo.append("memory max:").append(memoryPoolMXBean.getUsage().getMax() / 1024 / 1024).append("MB").append("\t");
            sysInfo.append("memory use:").append(memoryPoolMXBean.getUsage().getUsed() / 1024 / 1024).append("MB").append(System.lineSeparator());
        }

        MemoryUsage memoryUsage = memoryMXBean.getHeapMemoryUsage();
        sysInfo.append("初始内存:").append(memoryUsage.getInit() / 1024 / 1024).append("MB").append(System.lineSeparator());
        Runtime runtime = Runtime.getRuntime();
        sysInfo.append("jvm 总内存").append(runtime.totalMemory() / 1024 / 1024).append("MB").append(System.lineSeparator());
        sysInfo.append("jvm 可用内存").append(runtime.freeMemory() / 1024 / 1024).append("MB").append(System.lineSeparator());
        sysInfo.append("jvm 已用内存").append((runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024).append("MB").append(System.lineSeparator());

        logger.info(sysInfo.toString());
    }


    class GCInfo {

        private String name;
        private long count;
        private long time;

        public GCInfo(String name, long count, long time) {
            this.name = name;
            this.count = count;
            this.time = time;
        }

        public String getName() {
            return name;
        }

        public long getCount() {
            return count;
        }

        public long getTime() {
            return time;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", GCInfo.class.getSimpleName() + "[", "]")
                    .add("name='" + name + "'")
                    .add("count=" + count)
                    .add("time=" + time)
                    .toString();
        }
    }

    class JVMMemory {

        private String name;
        private long init;
        private long max;
        private long used;

        public JVMMemory(String name, long init, long max, long used) {
            this.name = name;
            this.init = init;
            this.max = max;
            this.used = used;
        }

        public String getName() {
            return name;
        }

        public long getInit() {
            return init;
        }

        public long getMax() {
            return max;
        }

        public long getUsed() {
            return used;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", JVMMemory.class.getSimpleName() + "[", "]")
                    .add("name='" + name + "'")
                    .add("init=" + init)
                    .add("max=" + max)
                    .add("used=" + used)
                    .toString();
        }
    }


}
