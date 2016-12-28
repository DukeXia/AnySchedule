package cn.ictgu.taskmanager;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 任务运行信息
 * Created by Silence on 2016/12/19.
 */
@Data
public class ScheduleTaskTypeRunningInfo {

  private long id;

  // 原始任务类型
  private String baseTaskType;

  // 任务类型：baseTaskType + "-" + ownSign
  private String taskType;

  // 环境标识
  private String ownSign;

  // 最后一次任务分配的时间
  private Timestamp lastAssignTime;

  // 最后一次执行任务分配的服务器
  private String lastAssignUUID;

  private Timestamp gmtCreate;

  private Timestamp gmtModified;
}
