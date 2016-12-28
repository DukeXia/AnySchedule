package cn.ictgu.taskmanager;

import cn.ictgu.commen.IScheduleTaskDeal;
import cn.ictgu.commen.IScheduleTaskDealMulti;
import cn.ictgu.commen.IScheduleTaskDealSingle;
import cn.ictgu.commen.TaskItemDefine;
import lombok.extern.log4j.Log4j;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SLEEP模式
 * Created by Silence on 2016/12/20.
 */
@Log4j
class AnyScheduleProcessorSleep<T> implements IScheduleProcessor, Runnable {
  private final LockObject m_lockObject = new LockObject();
  private List<Thread> threadList = new CopyOnWriteArrayList<>();

  // 任务管理器
  private AnyScheduleManager scheduleManager;

  // 任务类型
  private ScheduleTaskType taskTypeInfo;

  // 任务处理的接口类
  private IScheduleTaskDeal<T> taskDealBean;

  /**
   * 当前任务队列的版本号
   */
  //protected long taskListVersion = 0;
  //final Object lockVersionObject = new Object();
  //final Object lockRunningList = new Object();

  // 任务列表
  private List<T> taskList = new CopyOnWriteArrayList<>();

  /**
   * 是否可以批处理
   */
  private boolean isMultiTask = false;

  /**
   * 是否已经获得终止调度信号
   */
  private boolean isStopSchedule = false;   // 用户停止队列调度
  private boolean isSleeping = false;

  private StatisticsInfo statisticsInfo;

  /**
   * 创建一个任务调度器
   * @param anyScheduleManager
   * @param taskDealBean 任务处理的BEAN
   * @param statisticsInfo 任务处理信息
   */
  AnyScheduleProcessorSleep(AnyScheduleManager anyScheduleManager, IScheduleTaskDeal<T> taskDealBean, StatisticsInfo statisticsInfo) throws Exception {
    this.scheduleManager = anyScheduleManager;
    this.statisticsInfo = statisticsInfo;
    this.taskTypeInfo = this.scheduleManager.getTaskTypeInfo();
    this.taskDealBean = taskDealBean;
    if (this.taskDealBean instanceof IScheduleTaskDealSingle<?>) {
      if (taskTypeInfo.getExecuteNumber() > 1) {
        taskTypeInfo.setExecuteNumber(1);
      }
      isMultiTask = false;
    } else {
      isMultiTask = true;
    }
    if (taskTypeInfo.getFetchDataNumber() < taskTypeInfo.getThreadNumber() * 10) {
      log.error("参数设置不合理，系统性能不佳。【每次从数据库获取的数量fetchnum】 >= 【线程数量threadnum】*【最少循环次数10】");
    }
    for (int i = 0; i < taskTypeInfo.getThreadNumber(); i++) {
      this.startThread(i);
    }
  }

  /**
   * 需要注意的是，调度服务器从配置中心注销的工作，必须在所有线程退出的情况下才能做
   */
  public void stopSchedule() throws Exception {
    // 设置停止调度的标志,调度线程发现这个标志，执行完当前任务后，就退出调度
    this.isStopSchedule = true;
    // 清除所有未处理任务,但已经进入处理队列的，需要处理完毕
    this.taskList.clear();
  }

  private void startThread(int index) {
    Thread thread = new Thread(this);
    threadList.add(thread);
    String threadName = this.scheduleManager.getScheduleServer().getTaskType() + "-" + this.scheduleManager.getCurrentSerialNumber() + "-exe" + index;
    thread.setName(threadName);
    thread.start();
  }

  private synchronized Object getScheduleTaskId() {
    if (this.taskList.size() > 0) {
      return this.taskList.remove(0);   // 按正序处理
    }
    return null;
  }

  private synchronized Object[] getScheduleTaskIdMulti() {
    if (this.taskList.size() == 0) {
      return null;
    }
    int size = taskList.size() > taskTypeInfo.getExecuteNumber() ? taskTypeInfo.getExecuteNumber() : taskList.size();
    Object[] result = null;
    if (size > 0) {
      result = (Object[]) Array.newInstance(this.taskList.get(0).getClass(), size);
      for (int i = 0; i < size; i++) {
        result[i] = this.taskList.remove(0);  // 按正序处理
      }
    }
    return result;
  }

  public void clearAllHasFetchData() {
    this.taskList.clear();
  }

  public boolean isDealFinishAllData() {
    return this.taskList.size() == 0;
  }

  public boolean isSleeping() {
    return this.isSleeping;
  }

  private int loadScheduleData() {
    try {
      //在每次数据处理完毕后休眠固定的时间
      if (this.taskTypeInfo.getSleepTimeInterval() > 0) {
        //处理完一批数据后，开始休眠
        this.isSleeping = true;
        Thread.sleep(taskTypeInfo.getSleepTimeInterval());
        this.isSleeping = false;
        //休眠后恢复
      }
      List<TaskItemDefine> taskItems = this.scheduleManager.getCurrentScheduleTaskItemList();
      // 根据队列信息查询需要调度的数据，然后增加到任务列表中
      if (taskItems.size() > 0) {
        List<TaskItemDefine> tmpTaskList = new ArrayList<>();
        synchronized (taskItems) {
            tmpTaskList.addAll(taskItems);
        }
        List<T> tmpList = this.taskDealBean.selectTasks(taskTypeInfo.getTaskParameter(), scheduleManager.getScheduleServer().getOwnSign(), this.scheduleManager.getTaskItemCount(), tmpTaskList, taskTypeInfo.getFetchDataNumber());
        scheduleManager.getScheduleServer().setLastFetchDataTime(new Timestamp(scheduleManager.scheduleCenter.getSystemTime()));
        if (tmpList != null) {
          this.taskList.addAll(tmpList);
        }
      } else {
        log.info("没有获取到需要处理的数据队列");
      }
      addFetchNum(taskList.size());
      return this.taskList.size();
    } catch (Throwable ex) {
      log.error("获取任务错误：", ex);
    }
    return 0;
  }

  @SuppressWarnings({"rawtypes", "unchecked", "static-access"})
  public void run() {
    try {
      long startTime = 0;
      while (true) {
        this.m_lockObject.addThread();
        Object executeTask;
        while (true) {
          if (this.isStopSchedule) {//停止队列调度
            this.m_lockObject.releaseThread();
            this.m_lockObject.notifyOtherThread();//通知所有的休眠线程
            synchronized (this.threadList) {
              this.threadList.remove(Thread.currentThread());
              if (this.threadList.size() == 0) {
                this.scheduleManager.unRegisterScheduleServer();
              }
            }
            return;
          }
          //加载调度任务
          if (!this.isMultiTask) {
            executeTask = this.getScheduleTaskId();
          } else {
            executeTask = this.getScheduleTaskIdMulti();
          }
          if (executeTask == null) {
            break;
          }
          try {   //运行相关的程序
            startTime = scheduleManager.scheduleCenter.getSystemTime();
            if (!this.isMultiTask) {
              if (((IScheduleTaskDealSingle) this.taskDealBean).execute(executeTask, scheduleManager.getScheduleServer().getOwnSign())) {
                addSuccessNum(1, scheduleManager.scheduleCenter.getSystemTime() - startTime);
              } else {
                addFailNum(1, scheduleManager.scheduleCenter.getSystemTime() - startTime);
              }
            } else {
              if (((IScheduleTaskDealMulti) this.taskDealBean).execute((Object[]) executeTask, scheduleManager.getScheduleServer().getOwnSign())) {
                addSuccessNum(((Object[]) executeTask).length, scheduleManager.scheduleCenter.getSystemTime() - startTime);
              } else {
                addFailNum(((Object[]) executeTask).length, scheduleManager.scheduleCenter.getSystemTime() - startTime);
              }
            }
          } catch (Throwable ex) {
            if (!this.isMultiTask) {
              addFailNum(1, scheduleManager.scheduleCenter.getSystemTime() - startTime);
            } else {
              addFailNum(((Object[]) executeTask).length, scheduleManager.scheduleCenter.getSystemTime() - startTime);
            }
            log.error("Task :" + executeTask + " 处理失败", ex);
          }
        }
        //当前队列中所有的任务都已经完成
        log.debug(Thread.currentThread().getName() + "：当前运行线程数量:" + this.m_lockObject.count());
        if (!this.m_lockObject.releaseThreadButNotLast()) {
          int size;
          Thread.currentThread().sleep(100);
          startTime = scheduleManager.scheduleCenter.getSystemTime();
          // 装载数据
          size = this.loadScheduleData();
          if (size > 0) {
            this.m_lockObject.notifyOtherThread();
          } else {
            //判断当没有数据的是否，是否需要退出调度
            if (!this.isStopSchedule && this.scheduleManager.isContinueWhenData()) {
              // 没有加载到数据，开始休眠
              this.isSleeping = true;
              Thread.currentThread().sleep(this.scheduleManager.getTaskTypeInfo().getSleepTimeNoData());
              this.isSleeping = false;
              //休眠结束
            } else {
              //没有数据，退出调度，唤醒所有沉睡线程
              this.m_lockObject.notifyOtherThread();
            }
          }
          this.m_lockObject.releaseThread();
        } else {      // 将当前线程放置到等待队列中。直到有线程装载到了新的任务数据
          // 不是最后一个线程，休眠
          this.m_lockObject.waitCurrentThread();
        }
      }
    } catch (Throwable e) {
      log.error(e.getMessage(), e);
    }
  }

  private void addFetchNum(long num) {
    this.statisticsInfo.addFetchDataCount(1);
    this.statisticsInfo.addFetchDataNum(num);
  }

  private void addSuccessNum(long num, long spendTime) {
    this.statisticsInfo.addDealDataSuccess(num);
    this.statisticsInfo.addDealSpendTime(spendTime);
  }

  private void addFailNum(long num, long spendTime) {
    this.statisticsInfo.addDealDataFail(num);
    this.statisticsInfo.addDealSpendTime(spendTime);
  }
}
