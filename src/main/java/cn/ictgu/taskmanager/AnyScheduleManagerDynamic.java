package cn.ictgu.taskmanager;

import cn.ictgu.commen.TaskItemDefine;
import cn.ictgu.strategy.AnyScheduleManagerFactory;

import java.util.List;

/**
 * Created by Silence on 2016/12/20.
 */
public class AnyScheduleManagerDynamic extends AnyScheduleManager {

  AnyScheduleManagerDynamic(
    AnyScheduleManagerFactory aFactory,
    String baseTaskType, String ownSign, int managerPort,
    String jxmUrl, IScheduleDataManager aScheduleCenter
  ) throws Exception {
    super(aFactory, baseTaskType, ownSign, aScheduleCenter);
  }

  public void initial() throws Exception {
    if (scheduleCenter.isLeader(
      this.currentScheduleServer.getUuid(),
      scheduleCenter.loadScheduleServerNames(this.currentScheduleServer.getTaskType())
    )) {
      // 是第一次启动，检查对应的zk目录是否存在
      this.scheduleCenter.initialRunningInfo4Dynamic(
        this.currentScheduleServer.getBaseTaskType(),
        this.currentScheduleServer.getOwnSign()
      );
    }
    computerStart();
  }

  public void refreshScheduleServerInfo() throws Exception {
    throw new Exception("没有实现");
  }

  public boolean isNeedReLoadTaskItemList() throws Exception {
    throw new Exception("没有实现");
  }

  public void assignScheduleTask() throws Exception {
    throw new Exception("没有实现");

  }

  public List<TaskItemDefine> getCurrentScheduleTaskItemList() {
    throw new RuntimeException("没有实现");
  }

  public int getTaskItemCount() {
    throw new RuntimeException("没有实现");
  }

}
