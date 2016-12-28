package cn.ictgu.taskmanager;

import cn.ictgu.commen.ScheduleUtil;
import lombok.Data;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 调度服务器
 * Created by Silence on 2016/12/19.
 */
@Data
public class ScheduleServer {

  //全局唯一编号
  private String uuid;

  private long id;

  // 任务类型
  private String taskType;

  // 原始任务类型
  private String baseTaskType;

  private String ownSign;

  // 机器IP地址
  private String ip;

  // 机器名称
  private String hostName;

  // 数据处理线程数量
  private int threadNum;

  // 服务器开始时间
  private Timestamp registerTime;

  // 最后一次心跳通知时间
  private Timestamp heartBeatTime;

  // 最后一次取数据时间
  private Timestamp lastFetchDataTime;

  /**
   * 处理描述信息，例如读取的任务数量，处理成功的任务数量，处理失败的数量，处理耗时
   * FetchDataCount=4430,FetchDataNum=438570,DealDataSuccess=438570,DealDataFail=0,DealSpendTime=651066
   */
  private String dealInfoDesc;

  // 下一次开始运行时间
  private String nextRunStartTime;

  // 下一次运行结束时间
  private String nextRunEndTime;

  // 配置中心的当前时间
  private Timestamp centerServerTime;

  // 数据版本号
  private long version;

  // 是否注册
  private boolean isRegister;

  //任务管理器UUID
  private String managerFactoryUUID;


  static ScheduleServer createScheduleServer(IScheduleDataManager aScheduleCenter, String aBaseTaskType,String aOwnSign, int aThreadNum) throws Exception {
    ScheduleServer result = new ScheduleServer();
    result.baseTaskType = aBaseTaskType;
    result.ownSign = aOwnSign;
    result.taskType = ScheduleUtil.getTaskTypeByBaseAndOwnSign(
      aBaseTaskType, aOwnSign);
    result.ip = ScheduleUtil.getLocalIP();
    result.hostName = ScheduleUtil.getLocalHostName();
    result.registerTime = new Timestamp(aScheduleCenter.getSystemTime());
    result.threadNum = aThreadNum;
    result.heartBeatTime = null;
    result.dealInfoDesc = "调度初始化";
    result.version = 0;
    result.uuid = result.ip + "$" + (UUID.randomUUID().toString().replaceAll("-", "").toUpperCase());
    SimpleDateFormat DATA_FORMAT_yyyyMMdd = new SimpleDateFormat("yyMMdd");
    String s = DATA_FORMAT_yyyyMMdd.format(new Date(aScheduleCenter.getSystemTime()));
    result.id = Long.parseLong(s) * 100000000 + Math.abs(result.uuid.hashCode() % 100000000);
    return result;
  }

}
