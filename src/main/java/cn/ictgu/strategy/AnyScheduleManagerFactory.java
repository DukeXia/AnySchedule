package cn.ictgu.strategy;

import cn.ictgu.commen.IScheduleTaskDeal;
import cn.ictgu.commen.ScheduleUtil;
import cn.ictgu.config.ZookeeperProperties;
import cn.ictgu.taskmanager.AnyScheduleManagerStatic;
import cn.ictgu.taskmanager.IScheduleDataManager;
import cn.ictgu.zk.ScheduleDataManager4ZK;
import cn.ictgu.zk.ScheduleStrategyDataManager4ZK;
import cn.ictgu.zk.ZKManager;
import lombok.extern.log4j.Log4j;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 调度服务器构造器
 * Created by Silence on 2016/12/19.
 */
@Log4j
@Component
public class AnyScheduleManagerFactory implements ApplicationContextAware {

  @Autowired
  private ZookeeperProperties properties;

  // Zookeeper管理
  ZKManager zkManager;

  // 是否启动调度管理，如果只是做系统管理，应该设置为false
  private boolean start = true;

  /**
   * ManagerFactoryTimerTask上次执行的时间戳。<br/>
   * zk环境不稳定，可能导致所有task自循环丢失，调度停止。<br/>
   * 外层应用，通过jmx暴露心跳时间，监控这个AnySchedule最重要的大循环。<br/>
   */
  volatile long timerTaskHeartBeatTS = System.currentTimeMillis();

  // 调度配置中心客服端
  private IScheduleDataManager scheduleDataManager;

  private ScheduleStrategyDataManager4ZK scheduleStrategyManager;

  private Map<String, List<IStrategyTask>> managerMap = new ConcurrentHashMap<>();

  private ApplicationContext applicationcontext;

  private String uuid;
  private String ip;
  private String hostName;

  private Timer timer;
  private ManagerFactoryTimerTask timerTask;
  Lock lock = new ReentrantLock();

  volatile String errorMessage = "没有配置Zookeeper的连接信息";

  private InitialThread initialThread;

  public AnyScheduleManagerFactory() {
    this.ip = ScheduleUtil.getLocalIP();
    this.hostName = ScheduleUtil.getLocalHostName();
  }

  public void init() throws Exception {
    this.init(this.properties);
  }

  //初始化ZKManager 和 InitialThread
  private void init(ZookeeperProperties p) throws Exception {
    if (this.initialThread != null) {
      this.initialThread.stopThread();
    }
    this.lock.lock();
    try {
      this.scheduleDataManager = null;
      this.scheduleStrategyManager = null;
      if (this.zkManager != null) {
        this.zkManager.close();
      }
      this.zkManager = new ZKManager(p);
      this.errorMessage = "Zookeeper connecting ......" + this.zkManager.getConnectStr();
      initialThread = new InitialThread(this);
      initialThread.setName("AnyScheduleManagerFactory-initialThread");
      initialThread.start();
    } finally {
      this.lock.unlock();
    }
  }

  //在Zk状态正常后回调数据初始化
  void initialData() throws Exception {
    // 定时器时间间隔
    int timerInterval = 2 * 1000;
    // 延时执行
    int timerWait = 2 * 1000;
    this.zkManager.initial();
    this.zkManager.getZooKeeper();
    this.scheduleDataManager = new ScheduleDataManager4ZK(this.zkManager);
    this.scheduleStrategyManager = new ScheduleStrategyDataManager4ZK(this.zkManager);
    if (this.start) {
      // 注册调度管理器
      this.scheduleStrategyManager.registerManagerFactory(this);
      if (timer == null) {
        timer = new Timer("AnyScheduleManagerFactory-Timer");
      }
      if (timerTask == null) {
        timerTask = new ManagerFactoryTimerTask(this);
        timer.schedule(timerTask, timerWait, timerInterval);
      }
    }
  }

  // 创建调度服务器
  private IStrategyTask createStrategyTask(ScheduleStrategy strategy) throws Exception {
    IStrategyTask result = null;
    try {
      if (ScheduleStrategy.Kind.Schedule == strategy.getKind()) {
        String baseTaskType = ScheduleUtil.splitBaseTaskTypeFromTaskType(strategy.getTaskName());
        String ownSign = ScheduleUtil.splitOwnsignFromTaskType(strategy.getTaskName());
        result = new AnyScheduleManagerStatic(this, baseTaskType, ownSign, scheduleDataManager);
      } else if (ScheduleStrategy.Kind.Java == strategy.getKind()) {
        result = (IStrategyTask) Class.forName(strategy.getTaskName()).newInstance();
        result.initialTaskParameter(strategy.getStrategyName(), strategy.getTaskParameter());
      } else if (ScheduleStrategy.Kind.Bean == strategy.getKind()) {
        result = (IStrategyTask) this.getBean(strategy.getTaskName());
        result.initialTaskParameter(strategy.getStrategyName(), strategy.getTaskParameter());
      }
    } catch (Exception e) {
      log.error("strategy 获取对应的java or bean 出错,schedule并没有加载该任务,请确认" + strategy.getStrategyName(), e);
    }
    return result;
  }

  void refresh() throws Exception {
    this.lock.lock();
    try {
      // 判断状态是否终止
      ManagerFactoryInfo stsInfo = null;
      boolean isException = false;
      try {
        stsInfo = this.getScheduleStrategyManager().loadManagerFactoryInfo(this.getUuid());
      } catch (Exception e) {
        isException = true;
        log.error("获取服务器信息有误：uuid=" + this.getUuid(), e);
      }
      if (isException) {
        try {
          stopServer(null);         // 停止所有的调度任务
          this.getScheduleStrategyManager().unRegisterManagerFactory(this);
        } finally {
          reRegisterManagerFactory();
        }
      } else if (!stsInfo.isStart()) {
        stopServer(null);           // 停止所有的调度任务
        this.getScheduleStrategyManager().unRegisterManagerFactory(
          this);
      } else {
        reRegisterManagerFactory();
      }
    } finally {
      this.lock.unlock();
    }
  }

  private void reRegisterManagerFactory() throws Exception {
    //重新分配调度器
    List<String> stopList = this.getScheduleStrategyManager().registerManagerFactory(this);
    for (String strategyName : stopList) {
      this.stopServer(strategyName);
    }
    this.assignScheduleServer();
    this.reRunScheduleServer();
  }

  // 根据策略重新分配调度任务的机器
  private void assignScheduleServer() throws Exception {
    for (ScheduleStrategyRuntime run : this.scheduleStrategyManager.loadAllScheduleStrategyRuntimeByUUID(this.uuid)) {
      List<ScheduleStrategyRuntime> factoryList = this.scheduleStrategyManager.loadAllScheduleStrategyRuntimeByStrategyName(
        run.getStrategyName());
      if (factoryList.size() == 0 || !this.isLeader(this.uuid, factoryList)) {
        continue;
      }
      ScheduleStrategy scheduleStrategy = this.scheduleStrategyManager.loadStrategy(run.getStrategyName());
      int[] nums = ScheduleUtil.assignTaskNumber(factoryList.size(), scheduleStrategy.getAssignNum());
      for (int i = 0; i < factoryList.size(); i++) {
        ScheduleStrategyRuntime factory = factoryList.get(i);
        //更新请求的服务器数量
        this.scheduleStrategyManager.updateStrategyRuntimeReqestNum(run.getStrategyName(), factory.getUuid(), nums[i]);
      }
    }
  }

  private boolean isLeader(String uuid, List<ScheduleStrategyRuntime> factoryList) {
    try {
      long no = Long.parseLong(uuid.substring(uuid.lastIndexOf("$") + 1));
      for (ScheduleStrategyRuntime server : factoryList) {
        if (no > Long.parseLong(server.getUuid().substring(
          server.getUuid().lastIndexOf("$") + 1))) {
          return false;
        }
      }
      return true;
    } catch (Exception e) {
      log.error("判断Leader出错：uuid=" + uuid, e);
      return true;
    }
  }

  private void reRunScheduleServer() throws Exception {
    for (ScheduleStrategyRuntime run : this.scheduleStrategyManager.loadAllScheduleStrategyRuntimeByUUID(this.uuid)) {
      List<IStrategyTask> list = this.managerMap.get(run.getStrategyName());
      if (list == null) {
        list = new ArrayList<>();
        this.managerMap.put(run.getStrategyName(), list);
      }
      while (list.size() > run.getRequestNum() && list.size() > 0) {
        IStrategyTask task = list.remove(list.size() - 1);
        try {
          task.stop(run.getStrategyName());
        } catch (Throwable e) {
          log.error("注销任务错误：strategyName=" + run.getStrategyName(), e);
        }
      }
      //不足，增加调度器
      ScheduleStrategy strategy = this.scheduleStrategyManager.loadStrategy(run.getStrategyName());
      while (list.size() < run.getRequestNum()) {
        IStrategyTask result = this.createStrategyTask(strategy);
        if (null == result) {
          log.error("strategy 对应的配置有问题。strategy name=" + strategy.getStrategyName());
        }
        list.add(result);
      }
    }
  }

  /**
   * 终止一类任务
   *
   * @param strategyName 策略名称
   */
  private void stopServer(String strategyName) throws Exception {
    if (strategyName == null) {
      String[] nameList = this.managerMap.keySet().toArray(new String[0]);
      for (String name : nameList) {
        for (IStrategyTask task : this.managerMap.get(name)) {
          try {
            task.stop(name);
          } catch (Throwable e) {
            log.error("注销任务错误：strategyName=" + name, e);
          }
        }
        this.managerMap.remove(name);
      }
    } else {
      List<IStrategyTask> list = this.managerMap.get(strategyName);
      if (list != null) {
        for (IStrategyTask task : list) {
          try {
            task.stop(strategyName);
          } catch (Throwable e) {
            log.error("注销任务错误：strategyName=" + strategyName, e);
          }
        }
        this.managerMap.remove(strategyName);
      }

    }
  }

  // 停止所有调度资源
  public void stopAll() throws Exception {
    try {
      lock.lock();
      this.start = false;
      if (this.initialThread != null) {
        this.initialThread.stopThread();
      }
      if (this.timer != null) {
        if (this.timerTask != null) {
          this.timerTask.cancel();
          this.timerTask = null;
        }
        this.timer.cancel();
        this.timer = null;
      }
      this.stopServer(null);
      if (this.zkManager != null) {
        this.zkManager.close();
      }
      if (this.scheduleStrategyManager != null) {
        try {
          ZooKeeper zk = this.scheduleStrategyManager.getZooKeeper();
          if (zk != null) {
            zk.close();
          }
        } catch (Exception e) {
          log.error("stopAll zk getZooKeeper异常！", e);
        }
      }
      this.uuid = null;
      log.info("stopAll 停止服务成功！");
    } catch (Throwable e) {
      log.error("stopAll 停止服务失败：" + e.getMessage(), e);
    } finally {
      lock.unlock();
    }
  }

  // 重启所有的服务
  void reStart() throws Exception {
    try {
      if (this.timer != null) {
        if (this.timerTask != null) {
          this.timerTask.cancel();
          this.timerTask = null;
        }
        this.timer.purge();
      }
      this.stopServer(null);
      if (this.zkManager != null) {
        this.zkManager.close();
      }
      this.uuid = null;
      this.init();
    } catch (Throwable e) {
      log.error("重启服务失败：" + e.getMessage(), e);
    }
  }

  //检查Zookeeper是否初始化成功
  public boolean isZookeeperInitialSucess() throws Exception {
    return this.zkManager.checkZookeeperState();
  }

  public IScheduleDataManager getScheduleDataManager() {
    if (this.scheduleDataManager == null) {
      throw new RuntimeException(this.errorMessage);
    }
    return scheduleDataManager;
  }

  public ScheduleStrategyDataManager4ZK getScheduleStrategyManager() {
    if (this.scheduleStrategyManager == null) {
      throw new RuntimeException(this.errorMessage);
    }
    return scheduleStrategyManager;
  }

  public ZKManager getZkManager(){
    return this.zkManager;
  }

  public void setApplicationContext(ApplicationContext aApplicationcontext) throws BeansException {
    applicationcontext = aApplicationcontext;
  }

  public Object getBean(String beanName) {
    return applicationcontext.getBean(beanName);
  }

  public String getUuid() {
    return uuid;
  }

  public String getIp() {
    return ip;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getHostName() {
    return hostName;
  }

}

@Log4j
class ManagerFactoryTimerTask extends java.util.TimerTask {
  private AnyScheduleManagerFactory factory;
  private int count = 0;

  ManagerFactoryTimerTask(AnyScheduleManagerFactory factory) {
    this.factory = factory;
  }

  public void run() {
    try {
      Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
      if (!this.factory.zkManager.checkZookeeperState()) {
        if (count > 5) {
          log.error("Zookeeper连接失败，关闭所有的任务后，重新连接Zookeeper服务器......");
          this.factory.reStart();
        } else {
          count = count + 1;
        }
      } else {
        count = 0;
        this.factory.refresh();
      }
    } catch (Throwable ex) {
      log.error(ex.getMessage(), ex);
    } finally {
      factory.timerTaskHeartBeatTS = System.currentTimeMillis();
    }
  }
}

@Log4j
class InitialThread extends Thread {
  private AnyScheduleManagerFactory factory;
  private boolean isStop = false;

  InitialThread(AnyScheduleManagerFactory aFactory) {
    this.factory = aFactory;
  }

  void stopThread() {
    this.isStop = true;
  }

  @Override
  public void run() {
    factory.lock.lock();
    try {
      int count = 0;
      while (!factory.zkManager.checkZookeeperState()) {
        count = count + 1;
        if (count % 50 == 0) {
          factory.errorMessage = "Zookeeper 连接中......" + factory.zkManager.getConnectStr() + " 耗时:" + count * 20 + "(ms)";
          log.error(factory.errorMessage);
        }
        Thread.sleep(20);
        if (this.isStop) {
          return;
        }
      }
      factory.initialData();
    } catch (Throwable e) {
      log.error(e.getMessage());
    } finally {
      factory.lock.unlock();
    }
  }
}
