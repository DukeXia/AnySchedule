前言
---------
  本项目重构于TBSchedule，使用SpringBoot简化开发，美化界面，更容易上手

环境
---------
    JDK 1.8
    MAVEN 3.3.9
    Zookeeper 3.4.9
    MariaDB 10.1.12
    TOMCAT (SpringBoot1.4.2 内嵌，可省略)

开始体验
----------
    1.修改resources/application.yml文件，配置好数据库信息和Zookeeper信息
    2.初始化用于测试的数据库数据，resources/mariadb.sql
    3.启动Zookeeper
    4.启动SpringBoot应用（运行cn.ictgu.Application）
    5.观察日志，显示Zookeeper连接成功后，访问 http://localhost
    6.在“任务管理”页，选择“添加任务”，参数举例：
    
    任务名称：httpTask Bean名称：httpTask 心跳频率：5000 死亡间隔：30000  线程数：5
    每获取数量：50  每次执行数量：0  没有数据休眠时长：180000   处理模式：SLEEP
    每次处理完数据休眠时间：60000 执行开始时间：0 1 1 * * ?  执行结束时间：0 1 23 * * ?
    单线程组最大任务项：0 自定义参数：  任务项：0,1,2,3,4,5,6,7,8,9
    
    7.在“策略管理”页，选择“创建新策略”，参数举例：
    
    策略名称：httpTask-Strategy  任务类型：Schedule 任务名称：httpTask
    单JVM最大线程组数量：0 最大线程组数量：10  IP地址：127.0.0.1
    
自定义任务
----------
    1.参考DbTask.java 或者 HttpTask.java，编写自己的任务类，实现IScheduleTaskDealSingle<T> 接口
      或者 IScheduleTaskDealMulti<T>接口，使用@Component注解，并带上名称，对应任务Bean名称
    2.重启项目，使用控制面板创建任务及策略来调度任务

附加内容
----------
  深入了解AnySchedule可以阅读源码，或者搜索TBSchedule进行学习，本项目也会不断补充文档
