package cn.ictgu.commen;

/**
 * 单个任务处理接口
 * Created by Silence on 2016/12/20.
 */
public interface IScheduleTaskDealSingle<T>  extends IScheduleTaskDeal<T> {

  /**
   * 执行单个任务
   * @param task Object
   * @param ownSign 当前环境名称
   */
  boolean execute(T task, String ownSign) throws Exception;

}
