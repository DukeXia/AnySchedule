package cn.ictgu.strategy;

import lombok.Data;

/**
 * 调度策略
 * Created by Silence on 2016/12/19.
 */
@Data
public class ScheduleStrategy {

  public enum Kind {Schedule, Java, Bean}

  //策略名称
  private String strategyName;

  //任务分配的机器IP
  private String[] IPList;

  //单JVM最大线程组数量
  private int numOfSingleServer;

  //分配的机器的数量
  private int assignNum;

  //方式
  private Kind kind;

  //任务名称
  private String taskName;

  //任务参数
  private String taskParameter;

  /**
   * 服务状态: pause,resume
   */
  private String sts = STS_RESUME;

  public static String STS_PAUSE = "pause";
  public static String STS_RESUME = "resume";

}
