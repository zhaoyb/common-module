# common-module #

该项目主要记录一些在开源项目、个人工作中看到或者用到的一些比较公共的模块或者类，这些模块和类可以作为子模块嵌入到新的项目中。

#### loadbalance 负载均衡算法
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
> 因为dubbo最少活跃负载算法和项目本身耦合比较高，需要在具体的调用入口和出口处做计数，所以这里并没有实现。 <br>
> 项目中最常用的是随机负载和轮询负载， 一致性hash负载在需要做服务器亲缘的时候会用到。<br>
>> 举个例子：在做商品推荐的时候，用到一个在线模型(基于逻辑回归的CTR模型)，用户访问时，需要在程序中组装特征向量，特征向量来源于mongodb数据，数据量大，而且使用频繁，如果缓存到redis中，
>> 性能可能不是很好，这里采用了本地缓存，然后在应用前端加了一个网关，网关根据userid来做一致性hash负载。保证同一个用户命中到同一个机器上，保证缓存的命中率。


#### dynamic-threadpool 动态线程池
> Java自带的线程池运行过程是:
>>core -> queue -> max
>
>但是对于一些场景来讲，希望能变为
>> core -> max -> queue
>
> 即先使用核心线程，核心线程处理不过的时候，开始使用最大线程，
> 当最大线程也满的时候，再放入到队列。tomcat的线程池就实现了这个过程。<br>
> 另外[美团技术团队](https://tech.meituan.com/2020/04/02/java-pooling-pratice-in-meituan.html) 会利用配置系统来动态调整线程池的配置, 这里通过获取Java运行时的系统状态，来判断系统是否繁忙，如果系统繁忙，并且
> 线程池中积压任务的长度超过了总长度的70%，则触发自动扩容。

#### sharp-time 获取整点时间
> 在监控类或者job类的项目中，需要按照指定的间隔运行，比如在监控系统中，客户端SDK需要每隔5秒收集一次数据，如果客户端运行的开始时间并不是0秒 5秒..开头的，则服务端不是很好处理， <br>
> sharp-time类的作用，主要是为了获取下一个整数时间<br>
> 参考源码：spark(org.apache.spark.sql.execution.streaming.TriggerExecutor)



