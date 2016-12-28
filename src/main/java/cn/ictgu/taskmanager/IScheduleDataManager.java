package cn.ictgu.taskmanager;

import cn.ictgu.commen.TaskItemDefine;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.Map;

/**
 * 调度配置中心客户端接口，可以有基于数据库的实现，可以有基于ConfigServer的实现
 * Created by Silence on 2016/12/19.
 */
public interface IScheduleDataManager {

  long getSystemTime();

  /**
   * 重新装载当前server需要处理的数据队列
   * @param taskType  任务类型
   * @param serverUUID  当前server的UUID
   */
  List<TaskItemDefine> reloadDealTaskItem(String taskType, String serverUUID) throws Exception;

  /**
   * 加载所有的任务队列信息
   * @param taskType 任务类型
   */
  List<ScheduleTaskItem> loadAllTaskItem(String taskType) throws Exception;

  /**
   * 释放自己把持，别人申请的队列
   * @param taskType 任务类型
   * @param serverUUID 当前serverUUID
   */
  void releaseDealTaskItem(String taskType, String serverUUID) throws Exception;

  /**
   * 加载任务类型相关信息
   * @param taskType 任务类型
   */
  ScheduleTaskType loadTaskTypeBaseInfo(String taskType) throws Exception;

  /**
   * 清除已经过期的调度服务器信息
   * @param taskType 任务类型
   * @param expireTime 到期时间
   */
  int clearExpireScheduleServer(String taskType, long expireTime) throws Exception;

  /**
   * 清除任务信息，当调度服务器已经不存在的时候，清除任务信息
   * @param taskType 任务类型
   * @param serverList 不存在的服务器列表
   */
  int clearTaskItem(String taskType, List<String> serverList) throws Exception;

  /**
   * 获取所有的有效服务器信息
   * @param taskType 任务类型
   */
  List<ScheduleServer> selectAllValidScheduleServer(String taskType) throws Exception;

  List<String> loadScheduleServerNames(String taskType) throws Exception;

  /**
   * 重新分配任务队列
   * @param taskType 任务类型
   * @param currentUUID 当前服务器的UUID
   * @param maxNumOfOneServer 单个服务器处理的最大任务项数量
   * @param serverList 有效的调度服务器列表
   */
  void assignTaskItem(String taskType, String currentUUID, int maxNumOfOneServer, List<String> serverList) throws Exception;

  /**
   * 刷新调度服务器，发送心跳信息
   * @param scheduleServer 调度服务器
   */
  boolean refreshScheduleServer(ScheduleServer scheduleServer) throws Exception;

  /**
   * 注册调度服务器
   * @param scheduleServer 调度服务器
   */
  void registerScheduleServer(ScheduleServer scheduleServer) throws Exception;

  /**
   * 注销调度服务器
   * @param taskType 任务类型
   * @param serverUUID 调度服务器UUID
   */
  void unRegisterScheduleServer(String taskType, String serverUUID) throws Exception;

  /**
   * 清除已经过期的OWN_SIGN的自动生成的数据
   * @param baseTaskType       任务类型
   * @param serverUUID         调度服务器UUID
   * @param expireDateInternal 过期时间，以天为单位
   */
  void clearExpireTaskTypeRunningInfo(String baseTaskType, String serverUUID, double expireDateInternal) throws Exception;

  /**
   * 是否是分配任务的Leader
   * @param serverUUID 服务器UUID
   * @param serverList 服务器列表
   */
  boolean isLeader(String serverUUID, List<String> serverList);

  /**
   * 暂停所有的调度服务器
   * @param baseTaskType 任务类型名称
   */
  void pauseAllServer(String baseTaskType) throws Exception;

  /**
   * 唤醒所有的调度服务器
   * @param baseTaskType 任务类型名称
   */
  void resumeAllServer(String baseTaskType) throws Exception;

  /**
   * 获得所有任务类型信息
   */
  List<ScheduleTaskType> getAllTaskTypeBaseInfo() throws Exception;

  /**
   * 清除一个任务类型的运行期信息
   * @param baseTaskType 任务类型名称
   */
  void clearTaskType(String baseTaskType) throws Exception;

  /**
   * 创建一个新的任务类型
   * @param scheduleTaskType 调度任务类型
   */
  void createBaseTaskType(ScheduleTaskType scheduleTaskType) throws Exception;

  /**
   * 修改已经存在的任务类型
   * @param scheduleTaskType 调度任务类型
   */
  void updateBaseTaskType(ScheduleTaskType scheduleTaskType) throws Exception;

  /**
   * 得到一个任务类型的所有运行期信息
   * @param baseTaskType 任务类型名称
   */
  List<ScheduleTaskTypeRunningInfo> getAllTaskTypeRunningInfo(String baseTaskType) throws Exception;

  /**
   * 删除一个任务类型
   * @param baseTaskType 任务类型名称
   */
  void deleteTaskType(String baseTaskType) throws Exception;

  /**
   * 创建任务项。注意已存在的CurrentSever和RequestServer不会起作用
   * @param taskItems 任务项数组
   */
  void createScheduleTaskItem(ScheduleTaskItem[] taskItems) throws Exception;

  /**
   * 初始化任务调度的域信息和静态任务信息
   * @param baseTaskType 任务类型名称
   * @param ownSign 域
   * @param uuid 调度服务器UUID
   */
  void initialRunningInfo4Static(String baseTaskType, String ownSign, String uuid) throws Exception;

  /**
   * 初始化任务调度的域信息和动态任务信息
   * @param baseTaskType 任务类型名称
   * @param ownSign 域
   */
  void initialRunningInfo4Dynamic(String baseTaskType, String ownSign) throws Exception;

  /**
   * 运行期信息是否初始化成功
   * @param baseTaskType 任务类型名称
   * @param ownSign 域
   */
  boolean isInitialRunningInfoSuccess(String baseTaskType, String ownSign) throws Exception;

  /**
   * 设置运行成功标志
   * @param baseTaskType 任务类型名称
   * @param taskType 任务类型
   * @param uuid 调度服务器UUID
   */
  void setInitialRunningInfoSuccess(String baseTaskType, String taskType, String uuid) throws Exception;

  /**
   * 获取服务器列表中的Leader
   * @param serverList 服务器列表
   */
  String getLeader(List<String> serverList);

  /**
   * 更新刷新后的任务队列标志
   * @param taskType 任务类型
   */
  long updateReloadTaskItemFlag(String taskType) throws Exception;

  /**
   * 得到刷新后的任务队列标志
   * @param taskType 任务类型
   */
  long getReloadTaskItemFlag(String taskType) throws Exception;

  /**
   * 通过任务类型获取当前运行的服务器列表信息
   * @param taskType 任务类型
   */
  Map<String, Stat> getCurrentServerStatList(String taskType) throws Exception;


  /**
   * 根据条件查询当前调度服务
   */
  List<ScheduleServer> selectScheduleServer(String baseTaskType, String ownSign, String ip, String orderStr) throws Exception;

  /**
   * 查询调度服务的历史记录
   */
  List<ScheduleServer> selectHistoryScheduleServer(String baseTaskType, String ownSign, String ip, String orderStr) throws Exception;

  List<ScheduleServer> selectScheduleServerByManagerFactoryUUID(String factoryUUID) throws Exception;

  /**
   * 更新任务的状态和处理信息
   */
  void updateScheduleTaskItemStatus(String taskType, String taskItem, ScheduleTaskItem.TaskItemSts sts, String message) throws Exception;

  /**
   * 删除任务项
   */
  void deleteScheduleTaskItem(String taskType, String taskItem) throws Exception;

  /**
   * 获取一种任务类型的处理队列数量
   * @param taskType 任务类型
   */
  int queryTaskItemCount(String taskType) throws Exception;


}
