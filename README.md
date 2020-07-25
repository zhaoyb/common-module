# common-module #

该项目主要记录一些在个人工作中写的，或者开源项目看到的一些比较公共的模块或者类，这些模块和类可以作为子模块嵌入到新的项目中。


#### [disruptor 内存队列](https://github.com/zhaoyb/common-module/tree/master/disruptor)
> disruptor是一个内存队列，相比于Java自带的ArrayBlockingQueue性能上更有优势，可用于日志传输等。<br>
> 参考：<br>
> https://www.cnblogs.com/pku-liuqiang/p/8544700.html <br>
> https://www.jianshu.com/p/f0d4ff1f8ec9
>


#### [loadbalance 负载均衡算法](https://github.com/zhaoyb/common-module/tree/master/loadbalance)
> 在做RPC框架或者网关项目，往往需要一个负载均衡算法， 该模块主要参考了[soul](https://github.com/Dromara/soul) 和[dubbo](https://github.com/apache/dubbo) 的负载均衡算法 <br>
> soul主要实现了下面三个[算法](https://github.com/Dromara/soul/tree/master/soul-plugin/soul-plugin-divide/src/main/java/org/dromara/soul/plugin/divide/balance) ： 
>> - 一致性hash负载
>> - 随机负载
>> - 轮询算法
>
> dubbo 实现了下面的4个[算法](https://github.com/apache/dubbo/tree/master/dubbo-cluster/src/main/java/org/apache/dubbo/rpc/cluster/loadbalance) 
>> - 一致性hash负载
>> - 随机负载
>> - 轮询算法
>> - 最少活跃负载
>
> 因为dubbo最少活跃负载算法和框架本身耦合比较高，需要在具体的调用入口和出口处做计数，所以这里并没有实现。 <br>
> 项目中最常用的是随机负载和轮询负载， 一致性hash负载在需要做服务器亲缘的时候会用到。<br>
>> 举个例子：在做商品推荐的时候，用到一个在线模型(基于逻辑回归的CTR模型)，用户访问时，需要在程序中组装特征向量，特征向量来源于mongodb数据，数据量大，而且使用频繁，如果缓存到redis中，
>> 性能可能不是很好，这里采用了本地缓存，然后在应用前端加了一个网关，网关根据userid来做一致性hash负载。保证同一个用户命中到同一个机器上，保证缓存的命中率。


#### [dynamic-threadpool 动态线程池](https://github.com/zhaoyb/common-module/tree/master/dynamic-threadpool)
> Java自带的线程池运行过程是:
>>core -> queue -> max
>
>但是对于一些场景来讲，希望能变为
>> core -> max -> queue
>
> 即先使用核心线程，核心线程处理不过的时候，开始使用最大线程，
> 当最大线程也满的时候，再放入到队列。当时参考了tomcat的线程池的实现方式。这里实现上就直接使用了tomcat线程池。<br>
> 另外[美团技术团队](https://tech.meituan.com/2020/04/02/java-pooling-pratice-in-meituan.html) 会利用配置系统来动态调整线程池的配置，需要手动调整。这里实现的时候，直接通过获取Java运行时的系统状态(内存，CPU，GC等)，来判断系统是否繁忙，如果系统繁忙，并且
> 线程池队列积压任务的长度超过了总长度的70%，则触发自动扩容。

#### [分布式限流](https://github.com/zhaoyb/common-module/tree/master/rate_limiter)
> 作为高可用的解决方案，限流是一种托底的手段，通过限制请求数量，保证服务站点不会资源过载，导致自身出现不可预知的一些错误。 同时可以保护后端资源。 <br>
> 限流有多种，有本地限流，分布式限流， 限流算法包括令牌桶，漏铜，计数，滑动窗口 <br>
> 这里采用基于分布式限流，存储用的是redis， 算法用的是令牌桶。 
> [参考](https://mp.weixin.qq.com/s/qb3rg_ZpcMcvyaIRsvc1fw) 为方便大家了解，我对脚本加了注释。 

#### [DistributedMultiLock 分布式有限并发锁(python)](https://github.com/zhaoyb/common-module/tree/master/distributedmultilock)
> 现在关于分布式锁的解决方案，多是单锁，比如像借助数据库，redis, zookeeper等来实现的锁， 
> 但在一些项目中，我们需要有限个并发，即对于一个资源，我们希望可以最多有5个请求方。比如像缓存回源控制，我们希望只有5个并发去读数据库。<br>
> 在单机环境下，我们可以借助信号量来实现，对于分布式, 我们可以借助redis.<br>
> 该模块最早用于一个spark程序，当spark程序在计算的时候，一个RDD可能会有1000个partition，一个partition对应一个task,在计算完成后，要写入mongodb的时候，因为是持续写入，mongodb在这个场景下处理能力有限，
> 我们一般会让rdd repartition成一个较小的数字。 但是repartition需要shuffle，开销较大，所以写了这个分布式锁，partiton不变，但是在写mongodb的时候，需要获取写入锁。这样保证即使有1000个partition，真正能写入的只有2(可设置)个。<br>
> 补充：在spark中，如果要重新切分partition，其实可以借助coalesce
> 

#### [mongo_currentop_monitor MongoDB访问监控(python)](https://github.com/zhaoyb/common-module/tree/master/mongo_currentop_monitor)
> mongotop 、mongostat命令可以获取表或者集合的访问情况，线上如果mongodb出现了压力，可以通过这两个命令定位问题，但是这两个命令只能从表
> 维度或者库维度获取访问情况，如果一个表被多个业务端访问，想看看具体是由哪个业务引起的，就需要借助db.current_OP()命令，但是这个命令
> 只是获取当前时刻的请求情况，没有办法一段时间的，该脚本就是在一个循环中不断调用db.current_OP()命令，并带有统计功能。<br>
> 使用pyton命令直接运行，ctrl+C结束运行，并打印报表




#### [sharp-time 时间对齐](https://github.com/zhaoyb/common-module/tree/master/sharp-time)
> 在监控类或者job类的项目中，需要按照指定的间隔运行，比如在监控系统中，客户端SDK需要每隔5秒收集一次数据，如果客户端运行的开始时间并不是0秒 5秒..开头的，则服务端不是很好处理， <br>
> sharp-time类的作用，主要是为了获取下一个整数时间<br>
> 参考源码：spark(org.apache.spark.sql.execution.streaming.TriggerExecutor)



