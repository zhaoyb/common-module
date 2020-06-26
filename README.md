# common-module #

该项目主要记录一些在开源项目、个人学习中看到的一些比较公共的模块或者类，这些模块和类可以作为子模块嵌入到新的项目中。


#### sharp-time 获取整点时间
> 在监控类或者job类的项目中，需要按照指定的间隔运行，比如在监控系统中，客户端SDK需要每隔5秒收集一次数据，如果客户端运行的开始时间并不是0秒 5秒..开头的，则服务端不是很好处理，
> sharp-time类的作用，主要是为了获取下一个整数时间，
>
> 参考源码：spark(org.apache.spark.sql.execution.streaming.TriggerExecutor)

#### dynamic-threadpool 动态线程池
> Java自带的线程池运行过程是 core -> queue -> max, 但是对于一些场景来讲，希望能变为 core -> max -> queue的方式，即先使用核心线程，核心线程处理不过的时候，开始使用最大线程，
> 当最大线程也满的时候，再放入到队列。tomcat的线程池实现了这个过程，另外美团会利用配置系统来动态调整线程池的配置，这里通过获取Java运行时的系统状态，来判断系统是否繁忙，如果系统繁忙，并且
> 线程池中积压任务的长度超过了总长度的70%，则触发自动扩容。

