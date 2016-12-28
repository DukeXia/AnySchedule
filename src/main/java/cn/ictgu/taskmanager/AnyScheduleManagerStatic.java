package cn.ictgu.taskmanager;

import cn.ictgu.commen.TaskItemDefine;
import cn.ictgu.strategy.AnyScheduleManagerFactory;
import lombok.extern.log4j.Log4j;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.Map;

/**
 * 静态调度
 * Created by Silence on 2016/12/20.
 */
@Log4j
public class AnyScheduleManagerStatic extends AnyScheduleManager {

  /**
   * 总的任务数量
   */
  private int taskItemCount = 0;

  private long lastFetchVersion = -1;

  private final Object NeedReloadTaskItemLock = new Object();

  public AnyScheduleManagerStatic(
    AnyScheduleManagerFactory aFactory,
    String baseTaskType, String ownSign, IScheduleDataManager aScheduleCenter
  ) throws Exception {
    super(aFactory, baseTaskType, ownSign, aScheduleCenter);
  }

  private void initialRunningInfo() throws Exception {
    scheduleCenter.clearExpireScheduleServer(
      this.currentScheduleServer.getTaskType(),
      this.taskTypeInfo.getJudgeDeadInterval()
    );
    List<String> list = scheduleCenter.loadScheduleServerNames(this.currentScheduleServer.getTaskType());

    if (scheduleCenter.isLeader(this.currentScheduleServer.getUuid(), list)) {
      log.info("第一次启动，清除所有垃圾数据");
      this.scheduleCenter.initialRunningInfo4Static(
        this.currentScheduleServer.getBaseTaskType(),
        this.currentScheduleServer.getOwnSign(),
        this.currentScheduleServer.getUuid()
      );
    }
  }

  public void initial() throws Exception {
    new Thread(this.currentScheduleServer.getTaskType() + "-" + this.currentSerialNumber + "-StartProcess") {
      @SuppressWarnings("static-access")
      public void run() {
        try {
          log.info("开始获取调度任务队列...... of " + currentScheduleServer.getUuid());
          while (!isRuntimeInfoInitial) {
            if (isStopSchedule) {
              log.debug("外部命令终止调度,退出调度队列获取：" + currentScheduleServer.getUuid());
              return;
            }
            //log.error("isRuntimeInfoInitial = " + isRuntimeInfoInitial);
            try {
              initialRunningInfo();
              isRuntimeInfoInitial = scheduleCenter.isInitialRunningInfoSuccess(
                currentScheduleServer.getBaseTaskType(),
                currentScheduleServer.getOwnSign()
              );
            } catch (Throwable e) {
              //忽略初始化的异常
              log.error(e.getMessage(), e);
            }
            if (!isRuntimeInfoInitial) {
              Thread.currentThread().sleep(1000);
            }
          }
          int count = 0;
          lastReloadTaskItemListTime = scheduleCenter.getSystemTime();
          while (getCurrentScheduleTaskItemListNow().size() <= 0) {
            if (isStopSchedule) {
              log.debug("外部命令终止调度,退出调度队列获取：" + currentScheduleServer.getUuid());
              return;
            }
            Thread.currentThread().sleep(1000);
            count = count + 1;
            // log.error("尝试获取调度队列，第" + count + "次 ") ;
          }
          String tmpStr = "TaskItemDefine:";
          for (int i = 0; i < currentTaskItemList.size(); i++) {
            if (i > 0) {
              tmpStr = tmpStr + ",";
            }
            tmpStr = tmpStr + currentTaskItemList.get(i);
          }
          log.info("获取到任务处理队列，开始调度：" + tmpStr + "  of  " + currentScheduleServer.getUuid());

          //任务总量
          taskItemCount = scheduleCenter.loadAllTaskItem(currentScheduleServer.getTaskType()).size();

          //只有在已经获取到任务处理队列后才开始启动任务处理器
          computerStart();
        } catch (Exception e) {
          log.error(e.getMessage(), e);
          String str = e.getMessage();
          if (str.length() > 300) {
            str = str.substring(0, 300);
          }
          startErrorInfo = "启动处理异常：" + str;
        }
      }
    }.start();
  }

  /**
   * 定时向数据配置中心更新当前服务器的心跳信息。
   * 如果发现本次更新的时间如果已经超过了，服务器死亡的心跳周期，则不能在向服务器更新信息。
   * 而应该当作新的服务器，进行重新注册。
   */
  public void refreshScheduleServerInfo() throws Exception {
    try {
      rewriteScheduleInfo();
      //如果任务信息没有初始化成功，不做任务相关的处理
      if (!this.isRuntimeInfoInitial) {
        return;
      }
      //重新分配任务
      this.assignScheduleTask();

      //判断是否需要重新加载任务队列，避免任务处理进程不必要的检查和等待
      boolean tmpBoolean = this.isNeedReLoadTaskItemList();
      if (tmpBoolean != this.isNeedReloadTaskItem) {
        //只要不相同，就设置需要重新装载，因为在心跳异常的时候，做了清理队列的事情，恢复后需要重新装载。
        synchronized (NeedReloadTaskItemLock) {
          this.isNeedReloadTaskItem = true;
        }
        rewriteScheduleInfo();
      }

      if (this.isPauseSchedule || this.processor != null && processor.isSleeping()) {
        //如果服务已经暂停了，则需要重新定时更新 cur_server 和 req_server
        //如果服务没有暂停，一定不能调用的
        this.getCurrentScheduleTaskItemListNow();
      }
    } catch (Throwable e) {
      //清除内存中所有的已经取得的数据和任务队列,避免心跳线程失败时候导致的数据重复
      this.clearMemoInfo();
      if (e instanceof Exception) {
        throw (Exception) e;
      } else {
        throw new Exception(e.getMessage(), e);
      }
    }
  }

  /**
   * 在leader重新分配任务，在每个server释放原来占有的任务项时，都会修改这个版本号
   */
  private boolean isNeedReLoadTaskItemList() throws Exception {
    return this.lastFetchVersion < this.scheduleCenter.getReloadTaskItemFlag(this.currentScheduleServer.getTaskType());
  }

  /**
   * 判断某个任务对应的线程组是否处于僵尸状态。
   * true 表示有线程组处于僵尸状态。需要告警。
   */
  private boolean isExistZombieServ(String type, Map<String, Stat> statMap) throws Exception {
    boolean exist = false;
    for (String key : statMap.keySet()) {
      Stat s = statMap.get(key);
      if (this.scheduleCenter.getSystemTime() - s.getMtime() > this.taskTypeInfo.getHeartBeatRate() * 40) {
        log.error("zombie serverList exists! serv=" + key + " ,type=" + type + "超过40次心跳周期未更新");
        exist = true;
      }
    }
    return exist;

  }

  /**
   * 根据当前调度服务器的信息，重新计算分配所有的调度任务
   * 任务的分配是需要加锁，避免数据分配错误。为了避免数据锁带来的负面作用，通过版本号来达到锁的目的
   * <p>
   * 1、获取任务状态的版本号
   * 2、获取所有的服务器注册信息和任务队列信息
   * 3、清除已经超过心跳周期的服务器注册信息
   * 3、重新计算任务分配
   * 4、更新任务状态的版本号【乐观锁】
   * 5、根系任务队列的分配信息
   */
  public void assignScheduleTask() throws Exception {
    scheduleCenter.clearExpireScheduleServer(this.currentScheduleServer.getTaskType(), this.taskTypeInfo.getJudgeDeadInterval());
    List<String> serverList = scheduleCenter
      .loadScheduleServerNames(this.currentScheduleServer.getTaskType());

    if (!scheduleCenter.isLeader(this.currentScheduleServer.getUuid(), serverList)) {
      if (log.isDebugEnabled()) {
        log.debug(this.currentScheduleServer.getUuid() + ":不是负责任务分配的Leader,直接返回");
      }
      return;
    }
    //设置初始化成功标准，避免在leader转换的时候，新增的线程组初始化失败
    scheduleCenter.setInitialRunningInfoSuccess(
      this.currentScheduleServer.getBaseTaskType(),
      this.currentScheduleServer.getTaskType(),
      this.currentScheduleServer.getUuid()
    );
    scheduleCenter.clearTaskItem(this.currentScheduleServer.getTaskType(), serverList);
    scheduleCenter.assignTaskItem(
      this.currentScheduleServer.getTaskType(),
      this.currentScheduleServer.getUuid(),
      this.taskTypeInfo.getMaxTaskItemsOfOneThreadGroup(),
      serverList
    );
  }

  /**
   * 重新加载当前服务器的任务队列
   * 1、释放当前服务器持有，但有其它服务器进行申请的任务队列
   * 2、重新获取当前服务器的处理队列
   * <p>
   * 为了避免此操作的过度，阻塞真正的数据处理能力。系统设置一个重新装载的频率。例如1分钟
   * <p>
   * 特别注意：
   * 此方法的调用必须是在当前所有任务都处理完毕后才能调用，否则是否任务队列后可能数据被重复处理
   */

  public List<TaskItemDefine> getCurrentScheduleTaskItemList() {
    try {
      if (this.isNeedReloadTaskItem) {
        //特别注意：需要判断数据队列是否已经空了，否则可能在队列切换的时候导致数据重复处理
        //主要是在线程不休眠就加载数据的时候一定需要这个判断
        if (this.processor != null) {
          while (!this.processor.isDealFinishAllData()) {
            Thread.sleep(50);
          }
        }
        //真正开始处理数据
        synchronized (NeedReloadTaskItemLock) {
          this.getCurrentScheduleTaskItemListNow();
          this.isNeedReloadTaskItem = false;
        }
      }
      this.lastReloadTaskItemListTime = this.scheduleCenter.getSystemTime();
      return this.currentTaskItemList;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  //由于上面在数据执行时有使用到synchronized ，但是心跳线程并没有对应加锁。
  //所以在此方法上加一下synchronized。20151015
  private synchronized List<TaskItemDefine> getCurrentScheduleTaskItemListNow() throws Exception {
    try {
      Map<String, Stat> statMap = this.scheduleCenter.getCurrentServerStatList(this.currentScheduleServer.getTaskType());
      //server下面的机器节点的运行时环境是否在刷新，如果
      isExistZombieServ(this.currentScheduleServer.getTaskType(), statMap);
    } catch (Exception e) {
      log.error("zombie serverList exists， Exception:", e);
    }
    //获取最新的版本号
    this.lastFetchVersion = this.scheduleCenter.getReloadTaskItemFlag(this.currentScheduleServer.getTaskType());
    log.debug(" this.currentScheduleServer.getTaskType()=" + this.currentScheduleServer.getTaskType() + ",  need reload=" + isNeedReloadTaskItem);
    try {
      //是否被人申请的队列
      this.scheduleCenter.releaseDealTaskItem(
        this.currentScheduleServer.getTaskType(),
        this.currentScheduleServer.getUuid()
      );
      //重新查询当前服务器能够处理的队列
      //为了避免在休眠切换的过程中出现队列瞬间的不一致，先清除内存中的队列
      this.currentTaskItemList.clear();
      this.currentTaskItemList = this.scheduleCenter.reloadDealTaskItem(
        this.currentScheduleServer.getTaskType(), this.currentScheduleServer.getUuid());

      //如果超过10个心跳周期还没有获取到调度队列，则报警
      if (this.currentTaskItemList.size() == 0 && scheduleCenter.getSystemTime() - this.lastReloadTaskItemListTime > this.taskTypeInfo.getHeartBeatRate() * 20) {
        StringBuilder builder = new StringBuilder();
        builder.append("调度服务器");
        builder.append(this.currentScheduleServer.getUuid());
        builder.append("[TASK_TYPE=");
        builder.append(this.currentScheduleServer.getTaskType());
        builder.append("]自启动以来，超过20个心跳周期，还没有获取到分配的任务队列;");
        builder.append("  currentTaskItemList.size() =" + currentTaskItemList.size());
        builder.append(" ,scheduleCenter.getSystemTime()=" + scheduleCenter.getSystemTime());
        builder.append(" ,lastReloadTaskItemListTime=" + lastReloadTaskItemListTime);
        builder.append(" ,taskTypeInfo.getHeartBeatRate()=" + taskTypeInfo.getHeartBeatRate() * 10);
        log.error(builder.toString());
      }
      if (this.currentTaskItemList.size() > 0) {
        //更新时间戳
        this.lastReloadTaskItemListTime = scheduleCenter.getSystemTime();
      }
      return this.currentTaskItemList;
    } catch (Throwable e) {
      this.lastFetchVersion = -1; //必须把把版本号设置小，避免任务加载失败
      if (e instanceof Exception) {
        throw (Exception) e;
      } else {
        throw new Exception(e);
      }
    }
  }

  public int getTaskItemCount() {
    return this.taskItemCount;
  }

}
