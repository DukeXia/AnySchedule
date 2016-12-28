package cn.ictgu.commen;

import lombok.Data;

/**
 * 任务定义，提供关键信息给使用者
 * Created by Silence on 2016/12/19.
 */
@Data
public class TaskItemDefine {

  //任务项ID
  private String taskItemId;

  //任务项自定义参数
  private String parameter;

  @Override
  public String toString() {
    return "TaskItemDefine{" +
           "taskItemId='" + taskItemId + '\'' +
           ", parameter='" + parameter + '\'' +
           '}';
  }
}
