package cn.ictgu.strategy;

/**
 * 策略调度服务
 * Created by Silence on 2016/12/19.
 */
public interface IStrategyTask {

  /**
   * 为策略初始化任务参数
   * @param strategyName 策略名称
   * @param taskParameter 任务参数
   */
  void initialTaskParameter(String strategyName, String taskParameter) throws Exception;

  /**
   * 停止策略
   * @param strategyName 策略名称
   */
  void stop(String strategyName) throws Exception;

}
