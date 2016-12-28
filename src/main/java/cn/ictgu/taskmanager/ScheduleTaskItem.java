package cn.ictgu.taskmanager;

import lombok.Data;

/**
 * 任务队列类型
 * Created by Silence on 2016/12/19.
 */
@Data
public class ScheduleTaskItem {

  // 处理任务类型
  private String taskType;

  // 原始任务类型
  private String baseTaskType;

  // 完成状态
  private TaskItemSts sts = TaskItemSts.ACTIVTE;

  // 任务处理需要的参数
  private String dealParameter = "";

  // 任务处理情况,用于任务处理器会写一些信息
  private String dealDesc = "";

  // 队列的环境标识
  private String ownSign;

  // 任务队列ID
  private String taskItem;

  // 持有当前任务队列的任务处理器
  private String currentScheduleServer;

  // 正在申请此任务队列的任务处理器
  private String requestScheduleServer;

  // 数据版本号
  private long version;

  public enum TaskItemSts {
    ACTIVTE, FINISH, HALT
  }

}
