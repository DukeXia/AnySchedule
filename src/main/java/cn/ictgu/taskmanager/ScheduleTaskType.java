package cn.ictgu.taskmanager;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 任务类型
 * Created by Silence on 2016/12/19.
 */
@Data
public class ScheduleTaskType implements Serializable {

  private static final long serialVersionUID = 1L;

  // 任务类型
  private String baseTaskType;

  // 向配置中心更新心跳信息的频率
  private long heartBeatRate = 30 * 1000;

  // 判断一个服务器死亡的周期。 为了安全，至少是心跳周期的两倍以上
  private long judgeDeadInterval = 60 * 1000;

  // 当没有数据的时候，休眠的时间
  private int sleepTimeNoData = 500;

  // 在每次数据处理晚后休眠的时间
  private int sleepTimeInterval = 0;

  // 每次获取数据的数量
  private int fetchDataNumber = 500;

  // 在批处理的时候，每次处理的数据量
  private int executeNumber = 1;

  // 线程数
  private int threadNumber = 5;

  // 调度器类型，默认"SLEEP"
  private String processorType = "SLEEP";

  // 允许执行的开始时间
  private String permitRunStartTime;

  // 允许执行的结束时间
  private String permitRunEndTime;

  // 清除过期环境信息的时间间隔,以天为单位
  private double expireOwnSignInterval = 1;

  // 处理任务的BeanName
  private String dealBeanName;

  // 任务bean的参数，由用户自定义格式的字符串
  private String taskParameter;

  // 任务类型：静态static, 动态dynamic
  private String taskKind = TASK_KIND_STATIC;

  public static String TASK_KIND_STATIC = "static";
  public static String TASK_KIND_DYNAMIC = "dynamic";

  // 任务项数组
  private String[] taskItems;

  // 每个线程组能处理的最大任务项目数目
  private int maxTaskItemsOfOneThreadGroup = 0;

  // 版本号
  private long version;

  // 服务状态: 暂停, 恢复
  private String sts = STS_RESUME;

  public static String STS_PAUSE = "pause";
  public static String STS_RESUME = "resume";

  @Override
  public String toString() {
    return "ScheduleTaskType{" +
           "baseTaskType='" + baseTaskType + '\'' +
           ", heartBeatRate=" + heartBeatRate +
           ", judgeDeadInterval=" + judgeDeadInterval +
           ", sleepTimeNoData=" + sleepTimeNoData +
           ", sleepTimeInterval=" + sleepTimeInterval +
           ", fetchDataNumber=" + fetchDataNumber +
           ", executeNumber=" + executeNumber +
           ", threadNumber=" + threadNumber +
           ", processorType='" + processorType + '\'' +
           ", permitRunStartTime='" + permitRunStartTime + '\'' +
           ", permitRunEndTime='" + permitRunEndTime + '\'' +
           ", expireOwnSignInterval=" + expireOwnSignInterval +
           ", dealBeanName='" + dealBeanName + '\'' +
           ", taskParameter='" + taskParameter + '\'' +
           ", taskKind='" + taskKind + '\'' +
           ", taskItems=" + Arrays.toString(taskItems) +
           ", maxTaskItemsOfOneThreadGroup=" + maxTaskItemsOfOneThreadGroup +
           ", version=" + version +
           ", sts='" + sts + '\'' +
           '}';
  }

  public static String[] splitTaskItem(String str) {
    List<String> list = new ArrayList<>();
    int start = 0;
    int index = 0;
    while (index < str.length()) {
      if (str.charAt(index) == ':') {
        index = str.indexOf('}', index) + 1;
        list.add(str.substring(start, index).trim());
        while (index < str.length()) {
          if (str.charAt(index) == ' ') {
            index = index + 1;
          } else {
            break;
          }
        }
        index = index + 1; //跳过逗号
        start = index;
      } else if (str.charAt(index) == ',') {
        list.add(str.substring(start, index).trim());
        while (index < str.length()) {
          if (str.charAt(index) == ' ') {
            index = index + 1;
          } else {
            break;
          }
        }
        index = index + 1; //跳过逗号
        start = index;
      } else {
        index = index + 1;
      }
    }
    if (start < str.length()) {
      list.add(str.substring(start).trim());
    }
    return list.toArray(new String[0]);
  }

}
