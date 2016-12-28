package cn.ictgu.taskmanager;

  import cn.ictgu.commen.CronExpression;
  import cn.ictgu.commen.IScheduleTaskDeal;
  import cn.ictgu.commen.ScheduleUtil;
  import cn.ictgu.commen.TaskItemDefine;
  import cn.ictgu.strategy.IStrategyTask;
  import cn.ictgu.strategy.AnyScheduleManagerFactory;
  import lombok.extern.log4j.Log4j;

  import java.util.Date;
  import java.util.List;
  import java.util.Timer;
  import java.util.concurrent.CopyOnWriteArrayList;
  import java.util.concurrent.atomic.AtomicLong;
  import java.util.concurrent.locks.Lock;
  import java.util.concurrent.locks.ReentrantLock;

/**
 * 1、任务调度分配器的目标：	让所有的任务不重复，不遗漏的被快速处理。
 * 2、一个Manager只管理一种任务类型的一组工作线程。
 * 3、在一个JVM里面可能存在多个处理相同任务类型的Manager，也可能存在处理不同任务类型的Manager。
 * 4、在不同的JVM里面可以存在处理相同任务的Manager
 * 5、调度的Manager可以动态的随意增加和停止
 * <p>
 * 主要的职责：
 * 1、定时向集中的数据配置中心更新当前调度服务器的心跳状态
 * 2、向数据配置中心获取所有服务器的状态来重新计算任务的分配。这么做的目标是避免集中任务调度中心的单点问题。
 * 3、在每个批次数据处理完毕后，检查是否有其它处理服务器申请自己把持的任务队列，如果有，则释放给相关处理服务器。
 * <p>
 * 其它：
 * 如果当前服务器在处理当前任务的时候超时，需要清除当前队列，并释放已经把持的任务。并向控制主动中心报警。
 * <p>
 * Created by Silence on 2016/12/20.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Log4j
abstract class AnyScheduleManager implements IStrategyTask {
  /**
   * 用户标识不同线程的序号
   */
  private static int nextSerialNumber = 0;

  /**
   * 当前线程组编号
   */
  int currentSerialNumber = 0;

  /**
   * 调度任务类型信息
   */
  ScheduleTaskType taskTypeInfo;

  /**
   * 当前调度服务的信息
   */
  ScheduleServer currentScheduleServer;

  /**
   * 队列处理器
   */
  private IScheduleTaskDeal taskDealBean;

  /**
   * 多线程任务处理器
   */
  IScheduleProcessor processor;

  private StatisticsInfo statisticsInfo = new StatisticsInfo();

  boolean isPauseSchedule = true;
  private String pauseMessage = "";
  /**
   * 当前处理任务队列清单
   * ArrayList实现不是同步的。因多线程操作修改该列表，会造成ConcurrentModificationException
   */
  List<TaskItemDefine> currentTaskItemList = new CopyOnWriteArrayList<>();

  /**
   * 最近一起重新装载调度任务的时间。
   * 当前实际  - 上此装载时间  > intervalReloadTaskItemList，则向配置中心请求最新的任务分配情况
   */
  long lastReloadTaskItemListTime = 0;
  boolean isNeedReloadTaskItem = true;

  /**
   * 向配置中心更新信息的定时器
   */
  private Timer heartBeatTimer;

  IScheduleDataManager scheduleCenter;

  String startErrorInfo = null;

  boolean isStopSchedule = false;
  private Lock registerLock = new ReentrantLock();

  /**
   * 运行期信息是否初始化成功
   */
  boolean isRuntimeInfoInitial = false;

  private AnyScheduleManagerFactory factory;

  AnyScheduleManager(
    AnyScheduleManagerFactory aFactory,
    String baseTaskType,
    String ownSign,
    IScheduleDataManager aScheduleCenter
  ) throws Exception {
    this.factory = aFactory;
    this.currentSerialNumber = serialNumber();
    this.scheduleCenter = aScheduleCenter;
    this.taskTypeInfo = this.scheduleCenter.loadTaskTypeBaseInfo(baseTaskType);
    log.info("create AnyScheduleManager for taskType:" + baseTaskType);
    //清除已经过期1天的TASK,OWN_SIGN的组合。超过一天没有活动server的视为过期
    this.scheduleCenter.clearExpireTaskTypeRunningInfo(
      baseTaskType,
      ScheduleUtil.getLocalIP() + "清除过期OWN_SIGN信息",
      this.taskTypeInfo.getExpireOwnSignInterval()
    );

    Object dealBean = aFactory.getBean(this.taskTypeInfo.getDealBeanName());
    if (dealBean == null) {
      throw new Exception("SpringBean " + this.taskTypeInfo.getDealBeanName() + " 不存在");
    }
    if (!(dealBean instanceof IScheduleTaskDeal)) {
      throw new Exception("SpringBean " + this.taskTypeInfo.getDealBeanName() + " 没有实现 IScheduleTaskDeal接口");
    }
    this.taskDealBean = (IScheduleTaskDeal) dealBean;

    if (this.taskTypeInfo.getJudgeDeadInterval() < this.taskTypeInfo.getHeartBeatRate() * 5) {
      throw new Exception("数据配置存在问题，死亡的时间间隔，至少要大于心跳线程的5倍。当前配置数据：JudgeDeadInterval = "
                          + this.taskTypeInfo.getJudgeDeadInterval()
                          + ",HeartBeatRate = " + this.taskTypeInfo.getHeartBeatRate());
    }
    this.currentScheduleServer = ScheduleServer.createScheduleServer(
      this.scheduleCenter,
      baseTaskType,
      ownSign,
      this.taskTypeInfo.getThreadNumber()
    );
    this.currentScheduleServer.setManagerFactoryUUID(this.factory.getUuid());
    this.scheduleCenter.registerScheduleServer(this.currentScheduleServer);
    this.heartBeatTimer = new Timer(this.currentScheduleServer.getTaskType()
                                    + "-"
                                    + this.currentSerialNumber
                                    + "-HeartBeat");
    this.heartBeatTimer.schedule(
      new HeartBeatTimerTask(this),
      new java.util.Date(System.currentTimeMillis() + 500),
      this.taskTypeInfo.getHeartBeatRate()
    );
    initial();
  }

  /**
   * 对象创建时需要做的初始化工作
   */
  public abstract void initial() throws Exception;

  public abstract void refreshScheduleServerInfo() throws Exception;

  public abstract void assignScheduleTask() throws Exception;

  public abstract List<TaskItemDefine> getCurrentScheduleTaskItemList();

  public abstract int getTaskItemCount();

  public String getTaskType() {
    return this.currentScheduleServer.getTaskType();
  }

  public void initialTaskParameter(String strategyName, String taskParameter) {
    //没有实现的方法，需要的参数直接从任务配置中读取
  }

  private static synchronized int serialNumber() {
    return nextSerialNumber++;
  }

  int getCurrentSerialNumber() {
    return this.currentSerialNumber;
  }

  /**
   * 清除内存中所有的已经取得的数据和任务队列,在心态更新失败，或者发现注册中心的调度信息被删除
   */
  void clearMemoInfo() {
    try {
      // 清除内存中所有的已经取得的数据和任务队列,在心态更新失败，或者发现注册中心的调度信息被删除
      this.currentTaskItemList.clear();
      if (this.processor != null) {
        this.processor.clearAllHasFetchData();
      }
    } finally {
      //设置内存里面的任务数据需要重新装载
      this.isNeedReloadTaskItem = true;
    }

  }

  void rewriteScheduleInfo() throws Exception {
    registerLock.lock();
    try {
      if (this.isStopSchedule) {
        if (log.isDebugEnabled()) {
          log.debug("外部命令终止调度,不在注册调度服务，避免遗留垃圾数据：" + currentScheduleServer.getUuid());
        }
        return;
      }
      //先发送心跳信息
      if (startErrorInfo == null) {
        this.currentScheduleServer.setDealInfoDesc(this.pauseMessage + ":" + this.statisticsInfo.getDealDescription());
      } else {
        this.currentScheduleServer.setDealInfoDesc(startErrorInfo);
      }
      if (!this.scheduleCenter.refreshScheduleServer(this.currentScheduleServer)) {
        //更新信息失败，清除内存数据后重新注册
        this.clearMemoInfo();
        this.scheduleCenter.registerScheduleServer(this.currentScheduleServer);
      }
    } finally {
      registerLock.unlock();
    }
  }

  /**
   * 开始的时候，计算第一次执行时间
   */
  void computerStart() throws Exception {
    //只有当存在可执行队列后再开始启动队列
    boolean isRunNow = false;
    if (this.taskTypeInfo.getPermitRunStartTime() == null) {
      isRunNow = true;
    } else {
      String tmpStr = this.taskTypeInfo.getPermitRunStartTime();
      if (tmpStr.toLowerCase().startsWith("startrun:")) {
        isRunNow = true;
        tmpStr = tmpStr.substring("startrun:".length());
      }
      CronExpression cexpStart = new CronExpression(tmpStr);
      Date current = new Date(this.scheduleCenter.getSystemTime());
      Date firstStartTime = cexpStart.getNextValidTimeAfter(current);
      this.heartBeatTimer.schedule(new PauseOrResumeScheduleTask(
        this,
        this.heartBeatTimer,
        PauseOrResumeScheduleTask.TYPE_RESUME,
        tmpStr
      ), firstStartTime);
      this.currentScheduleServer.setNextRunStartTime(ScheduleUtil.transferDataToString(firstStartTime));
      if (this.taskTypeInfo.getPermitRunEndTime() == null || this.taskTypeInfo.getPermitRunEndTime().equals("-1")) {
        this.currentScheduleServer.setNextRunEndTime("当不能获取到数据的时候pause");
      } else {
        try {
          String tmpEndStr = this.taskTypeInfo.getPermitRunEndTime();
          CronExpression cexpEnd = new CronExpression(tmpEndStr);
          Date firstEndTime = cexpEnd.getNextValidTimeAfter(firstStartTime);
          Date nowEndTime = cexpEnd.getNextValidTimeAfter(current);
          if (!nowEndTime.equals(firstEndTime) && current.before(nowEndTime)) {
            isRunNow = true;
            firstEndTime = nowEndTime;
          }
          this.heartBeatTimer.schedule(
            new PauseOrResumeScheduleTask(this, this.heartBeatTimer,
                                          PauseOrResumeScheduleTask.TYPE_PAUSE, tmpEndStr
            ),
            firstEndTime
          );
          this.currentScheduleServer.setNextRunEndTime(ScheduleUtil.transferDataToString(firstEndTime));
        } catch (Exception e) {
          log.error("计算第一次执行时间出现异常:" + currentScheduleServer.getUuid(), e);
          throw new Exception("计算第一次执行时间出现异常:" + currentScheduleServer.getUuid(), e);
        }
      }
    }
    if (isRunNow) {
      this.resume("开启服务立即启动");
    }
    this.rewriteScheduleInfo();
  }

  /**
   * 当Process没有获取到数据的时候调用，决定是否暂时停止服务器
   */
  boolean isContinueWhenData() throws Exception {
    if (isPauseWhenNoData()) {
      this.pause("没有数据,暂停调度");
      return false;
    } else {
      return true;
    }
  }

  private boolean isPauseWhenNoData() {
    //如果还没有分配到任务队列则不能退出
    return this.currentTaskItemList.size() > 0
           && this.taskTypeInfo.getPermitRunStartTime() != null
           && (this.taskTypeInfo.getPermitRunEndTime() == null || this.taskTypeInfo.getPermitRunEndTime().equals("-1"));

  }

  /**
   * 超过运行的运行时间，暂时停止调度
   */
  void pause(String message) throws Exception {
    if (!this.isPauseSchedule) {
      this.isPauseSchedule = true;
      this.pauseMessage = message;
      if (log.isDebugEnabled()) {
        log.debug("暂停调度 ：" + this.currentScheduleServer.getUuid() + ":" + this.statisticsInfo.getDealDescription());
      }
      if (this.processor != null) {
        this.processor.stopSchedule();
      }
      rewriteScheduleInfo();
    }
  }

  /**
   * 处在了可执行的时间区间，恢复运行
   */
  void resume(String message) throws Exception {
    if (this.isPauseSchedule) {
      if (log.isDebugEnabled()) {
        log.debug("恢复调度:" + this.currentScheduleServer.getUuid());
      }
      this.isPauseSchedule = false;
      this.pauseMessage = message;
      if (this.taskDealBean != null) {
        if (this.taskTypeInfo.getProcessorType() != null && this.taskTypeInfo.getProcessorType()
                                                                             .equalsIgnoreCase("NOTSLEEP")) {
          this.taskTypeInfo.setProcessorType("NOTSLEEP");
          this.processor = new AnyScheduleProcessorNotSleep(this, taskDealBean, this.statisticsInfo);
        } else {
          this.processor = new AnyScheduleProcessorSleep(this, taskDealBean, this.statisticsInfo);
          this.taskTypeInfo.setProcessorType("SLEEP");
        }
      }
      rewriteScheduleInfo();
    }
  }

  /**
   * 当服务器停止的时候，调用此方法清除所有未处理任务，清除服务器的注册信息。
   * 也可能是控制中心发起的终止指令。
   * 需要注意的是，这个方法必须在当前任务处理完毕后才能执行
   */
  public void stop(String strategyName) throws Exception {
    if (log.isInfoEnabled()) {
      log.info("停止服务器 ：" + this.currentScheduleServer.getUuid());
    }
    this.isPauseSchedule = false;
    if (this.processor != null) {
      this.processor.stopSchedule();
    } else {
      this.unRegisterScheduleServer();
    }
  }

  /**
   * 只应该在Processor中调用
   */
  void unRegisterScheduleServer() throws Exception {
    registerLock.lock();
    try {
      if (this.processor != null) {
        this.processor = null;
      }
      if (this.isPauseSchedule) {
        // 是暂停调度，不注销Manager自己
        return;
      }
      if (log.isDebugEnabled()) {
        log.debug("注销服务器 ：" + this.currentScheduleServer.getUuid());
      }
      this.isStopSchedule = true;
      // 取消心跳TIMER
      this.heartBeatTimer.cancel();
      // 从配置中心注销自己
      this.scheduleCenter.unRegisterScheduleServer(
        this.currentScheduleServer.getTaskType(),
        this.currentScheduleServer.getUuid()
      );
    } finally {
      registerLock.unlock();
    }
  }

  ScheduleTaskType getTaskTypeInfo() {
    return taskTypeInfo;
  }

  ScheduleServer getScheduleServer() {
    return this.currentScheduleServer;
  }

}

@Log4j
class HeartBeatTimerTask extends java.util.TimerTask {

  private AnyScheduleManager manager;

  HeartBeatTimerTask(AnyScheduleManager aManager) {
    manager = aManager;
  }

  public void run() {
    try {
      Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
      manager.refreshScheduleServerInfo();
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    }
  }
}

@Log4j
class PauseOrResumeScheduleTask extends java.util.TimerTask {
  static int TYPE_PAUSE = 1;
  static int TYPE_RESUME = 2;
  private AnyScheduleManager manager;
  private Timer timer;
  private int type;
  private String cronTabExpress;

  PauseOrResumeScheduleTask(AnyScheduleManager aManager, Timer aTimer, int aType, String aCronTabExpress) {
    this.manager = aManager;
    this.timer = aTimer;
    this.type = aType;
    this.cronTabExpress = aCronTabExpress;
  }

  public void run() {
    try {
      Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
      this.cancel();//取消调度任务
      Date current = new Date(System.currentTimeMillis());
      CronExpression cexp = new CronExpression(this.cronTabExpress);
      Date nextTime = cexp.getNextValidTimeAfter(current);
      if (this.type == TYPE_PAUSE) {
        manager.pause("暂停调度");
        this.manager.getScheduleServer().setNextRunEndTime(ScheduleUtil.transferDataToString(nextTime));
      } else {
        manager.resume("正在调度");
        this.manager.getScheduleServer().setNextRunStartTime(ScheduleUtil.transferDataToString(nextTime));
      }
      this.timer.schedule(
        new PauseOrResumeScheduleTask(this.manager, this.timer, this.type, this.cronTabExpress),
        nextTime
      );
    } catch (Throwable ex) {
      log.error(ex.getMessage(), ex);
    }
  }
}

class StatisticsInfo {
  private AtomicLong fetchDataNum = new AtomicLong(0);      //读取次数
  private AtomicLong fetchDataCount = new AtomicLong(0);    //读取的数据量
  private AtomicLong dealDataSuccess = new AtomicLong(0);   //处理成功的数据量
  private AtomicLong dealDataFail = new AtomicLong(0);      //处理失败的数据量
  private AtomicLong dealSpendTime = new AtomicLong(0);     //处理总耗时,没有做同步，可能存在一定的误差
  private AtomicLong otherCompareCount = new AtomicLong(0); //特殊比较的次数

  void addFetchDataNum(long value) {
    this.fetchDataNum.addAndGet(value);
  }

  void addFetchDataCount(long value) {
    this.fetchDataCount.addAndGet(value);
  }

  void addDealDataSuccess(long value) {
    this.dealDataSuccess.addAndGet(value);
  }

  void addDealDataFail(long value) {
    this.dealDataFail.addAndGet(value);
  }

  void addDealSpendTime(long value) {
    this.dealSpendTime.addAndGet(value);
  }

  void addOtherCompareCount(long value) {
    this.otherCompareCount.addAndGet(value);
  }

  String getDealDescription() {
    return this.fetchDataCount
           + "/" + this.fetchDataNum
           + "/" + this.dealDataSuccess
           + "/" + this.dealDataFail
           + "/" + this.dealSpendTime
           + "/" + this.otherCompareCount;
  }

}
