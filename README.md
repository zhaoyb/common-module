# common-module #

该项目主要记录一些在开源项目、个人学习中看到的一些比较公共的模块或者类，这些模块和类可以作为子模块嵌入到新的项目中。


#### sharp-time 获取整点时间
> 在监控类或者job类的项目中，需要按照指定的间隔运行，比如在监控系统中，客户端SDK需要每隔5秒收集一次数据，如果客户端运行的开始时间并不是0秒 5秒..开头的，则服务端不是很好处理，
> sharp-time类的作用，主要是为了获取下一个整数时间，
>
> 参考源码：spark(org.apache.spark.sql.execution.streaming.TriggerExecutor)
