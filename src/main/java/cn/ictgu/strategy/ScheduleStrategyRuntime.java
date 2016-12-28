package cn.ictgu.strategy;

import lombok.Data;

/**
 * 运行中的调度策略
 * Created by Silence on 2016/12/19.
 */
@Data
public class ScheduleStrategyRuntime {

  // 策略名称
  String strategyName;

  // 任务管理器UUID
  String uuid;

  // IP
  String ip;

  // 方式
  private ScheduleStrategy.Kind kind;

  // 任务名称
  private String taskName;

  // 任务参数
  private String taskParameter;

  // 需要的任务数量
  int	requestNum;

  // 当前的任务数量
  int currentNum;

  // 信息
  String message;
}
