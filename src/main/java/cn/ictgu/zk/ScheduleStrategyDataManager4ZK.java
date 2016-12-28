package cn.ictgu.zk;

import cn.ictgu.strategy.AnyScheduleManagerFactory;
import cn.ictgu.strategy.ManagerFactoryInfo;
import cn.ictgu.strategy.ScheduleStrategy;
import cn.ictgu.strategy.ScheduleStrategyRuntime;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 调度策略数据管理
 * 策略信息和任务管理器相关信息处理
 * Created by Silence on 2016/12/19.
 */
public class ScheduleStrategyDataManager4ZK {

  private ZKManager zkManager;
  private String PATH_Strategy;
  private String PATH_ManagerFactory;
  private Gson gson;

  /**
   * 构造函数，初始化 Zookeeper 信息节点： rootPath/strategy 和 rootPath/factory
   * @param aZkManager ZK管理器
   */
  public ScheduleStrategyDataManager4ZK(ZKManager aZkManager) throws Exception {
    this.zkManager = aZkManager;
    gson = new GsonBuilder().registerTypeAdapter(Timestamp.class, new TimestampTypeAdapter()).setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    this.PATH_Strategy = this.zkManager.getRootPath() + "/strategy";
    this.PATH_ManagerFactory = this.zkManager.getRootPath() + "/factory";
    if (this.getZooKeeper().exists(this.PATH_Strategy, false) == null) {
      ZKTools.createPath(getZooKeeper(), this.PATH_Strategy, CreateMode.PERSISTENT, this.zkManager.getAcl());
    }
    if (this.getZooKeeper().exists(this.PATH_ManagerFactory, false) == null) {
      ZKTools.createPath(getZooKeeper(), this.PATH_ManagerFactory, CreateMode.PERSISTENT, this.zkManager.getAcl());
    }
  }

  /**
   * 根据策略名称查询策略对象
   * @param strategyName 策略名称
   * @return 调度策略对象
   */
  public ScheduleStrategy loadStrategy(String strategyName) throws Exception {
    String zkPath = this.PATH_Strategy + "/" + strategyName;
    if (this.getZooKeeper().exists(zkPath, false) == null) {
      return null;
    }
    String valueString = new String(this.getZooKeeper().getData(zkPath, false, null));
    return this.gson.fromJson(valueString, ScheduleStrategy.class);
  }

  /**
   * 创建调度策略
   * @param scheduleStrategy 调度策略
   */
  public void createScheduleStrategy(ScheduleStrategy scheduleStrategy) throws Exception {
    String zkPath = this.PATH_Strategy + "/" + scheduleStrategy.getStrategyName();
    String valueString = this.gson.toJson(scheduleStrategy);
    if (this.getZooKeeper().exists(zkPath, false) == null) {
      this.getZooKeeper().create(zkPath, valueString.getBytes(), this.zkManager.getAcl(), CreateMode.PERSISTENT);
    } else {
      throw new Exception("调度策略"+ scheduleStrategy.getStrategyName() + "已经存在,如果确认需要重建，请先调用deleteMachineStrategy(String taskType)删除");
    }
  }

  /**
   * 更新调度策略信息
   * @param scheduleStrategy 调度策略
   */
  public void updateScheduleStrategy(ScheduleStrategy scheduleStrategy)
    throws Exception {
    String zkPath = this.PATH_Strategy + "/" + scheduleStrategy.getStrategyName();
    String valueString = this.gson.toJson(scheduleStrategy);
    if (this.getZooKeeper().exists(zkPath, false) == null) {
      this.getZooKeeper().create(zkPath, valueString.getBytes(), this.zkManager.getAcl(), CreateMode.PERSISTENT);
    } else {
      this.getZooKeeper().setData(zkPath, valueString.getBytes(), -1);
    }

  }

  /**
   * 删除策略
   * @param strategyName 策略名称
   */
  public void deleteMachineStrategy(String strategyName) throws Exception {
    String zkPath = this.PATH_Strategy + "/" + strategyName;
    if (this.getZooKeeper().getChildren(zkPath, null).size() > 0) {
      throw new Exception("不能删除" + strategyName + "的运行策略，会导致必须重启整个应用才能停止失去控制的调度进程。" + "可以先清空IP地址，等所有的调度器都停止后再删除调度策略");
    }
    ZKTools.deleteTree(this.getZooKeeper(), zkPath);
  }

  /**
   * 暂停调度策略
   * @param strategyName 策略名称
   */
  public void pause(String strategyName) throws Exception {
    ScheduleStrategy strategy = this.loadStrategy(strategyName);
    strategy.setSts(ScheduleStrategy.STS_PAUSE);
    this.updateScheduleStrategy(strategy);
  }

  /**
   * 恢复调度策略
   * @param strategyName 策略名称
   */
  public void resume(String strategyName) throws Exception {
    ScheduleStrategy strategy = this.loadStrategy(strategyName);
    strategy.setSts(ScheduleStrategy.STS_RESUME);
    this.updateScheduleStrategy(strategy);
  }

  /**
   * 加载所有的调度策略，Zookeeper信息节点：rootPath/strategy
   * @return 调度策略列表
   */
  public List<ScheduleStrategy> loadAllScheduleStrategy() throws Exception {
    String zkPath = this.PATH_Strategy;
    List<ScheduleStrategy> result = new ArrayList<>();
    List<String> names = this.getZooKeeper().getChildren(zkPath, false);
    Collections.sort(names);
    for (String name : names) {
      result.add(this.loadStrategy(name));
    }
    return result;
  }

  /**
   * 注册任务管理器
   * @return 需要全部注销的调度，例如当IP不在列表中
   */
  public List<String> registerManagerFactory(AnyScheduleManagerFactory managerFactory) throws Exception {
    if (managerFactory.getUuid() == null) {
      String uuid = managerFactory.getIp() + "$" + managerFactory.getHostName() + "$" + UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
      String zkPath = this.PATH_ManagerFactory + "/" + uuid + "$";
      zkPath = this.getZooKeeper().create(zkPath, null, this.zkManager.getAcl(), CreateMode.EPHEMERAL_SEQUENTIAL);
      managerFactory.setUuid(zkPath.substring(zkPath.lastIndexOf("/") + 1));
    } else {
      String zkPath = this.PATH_ManagerFactory + "/" + managerFactory.getUuid();
      if (this.getZooKeeper().exists(zkPath, false) == null) {
        this.getZooKeeper().create(zkPath, null, this.zkManager.getAcl(), CreateMode.EPHEMERAL);
      }
    }
    List<String> result = new ArrayList<>();
    for (ScheduleStrategy scheduleStrategy : loadAllScheduleStrategy()) {
      boolean isFind = false;
      // TODO 这段代码创建了zk节点，却在下面直接删除掉了，可能要改
      if (ScheduleStrategy.STS_RESUME.equalsIgnoreCase(scheduleStrategy.getSts()) && scheduleStrategy.getIPList() != null) {
        for (String ip : scheduleStrategy.getIPList()) {
          if (ip.equals("127.0.0.1") || ip.equalsIgnoreCase("localhost") || ip.equals(managerFactory.getIp()) || ip.equalsIgnoreCase(managerFactory.getHostName())) {
            //添加可管理调度器
            String zkPath = this.PATH_Strategy + "/" + scheduleStrategy.getStrategyName() + "/" + managerFactory.getUuid();
            if (this.getZooKeeper().exists(zkPath, false) == null) {
              this.getZooKeeper().create(zkPath, null, this.zkManager.getAcl(), CreateMode.EPHEMERAL);
            }
            isFind = true;
            break;
          }
        }
      }
      if (!isFind) {
        //清除原来注册的任务管理器
        String zkPath = this.PATH_Strategy + "/" + scheduleStrategy.getStrategyName() + "/" + managerFactory.getUuid();
        if (this.getZooKeeper().exists(zkPath, false) != null) {
          ZKTools.deleteTree(this.getZooKeeper(), zkPath);
          result.add(scheduleStrategy.getStrategyName());
        }
      }
    }
    return result;
  }

  /**
   * 注销任务管理器，停止调度
   * @param managerFactory 任务管理器
   */
  public void unRegisterManagerFactory(AnyScheduleManagerFactory managerFactory) throws Exception {
    for (String strategyName : this.getZooKeeper().getChildren(this.PATH_Strategy, false)) {
      String zkPath = this.PATH_Strategy + "/" + strategyName + "/" + managerFactory.getUuid();
      if (this.getZooKeeper().exists(zkPath, false) != null) {
        ZKTools.deleteTree(this.getZooKeeper(), zkPath);
      }
    }
  }

  /**
   * 加载调度策略的运行状态， Zookeeper信息节点：rootPath/strategy/strategyName/uuid
   * @param strategyName 策略名称
   * @param managerFactoryUUID 任务管理器UUID
   * @return 调度策略的运行状态
   */
  private ScheduleStrategyRuntime loadScheduleStrategyRuntime(String strategyName, String managerFactoryUUID) throws Exception {
    String zkPath = this.PATH_Strategy + "/" + strategyName + "/" + managerFactoryUUID;
    ScheduleStrategyRuntime result = null;
    if (this.getZooKeeper().exists(zkPath, false) != null) {
      byte[] value = this.getZooKeeper().getData(zkPath, false, null);
      if (value != null) {
        String valueString = new String(value);
        result = this.gson.fromJson(valueString, ScheduleStrategyRuntime.class);
        if (null == result) {
          throw new Exception("gson 反序列化异常,对象为null");
        }
        if (null == result.getStrategyName()) {
          throw new Exception("gson 反序列化异常,策略名字为null");
        }
        if (null == result.getUuid()) {
          throw new Exception("gson 反序列化异常,uuid为null");
        }
      } else {
        result = new ScheduleStrategyRuntime();
        result.setStrategyName(strategyName);
        result.setUuid(managerFactoryUUID);
        result.setRequestNum(0);
        result.setMessage("");
      }
    }
    return result;
  }

  /**
   * 加载所有调度策略的运行状态
   * @return 调度策略的运行状态列表
   */
  public List<ScheduleStrategyRuntime> loadAllScheduleStrategyRuntime() throws Exception {
    List<ScheduleStrategyRuntime> result = new ArrayList<>();
    String zkPath = this.PATH_Strategy;
    for (String taskType : this.getZooKeeper().getChildren(zkPath, false)) {
      for (String uuid : this.getZooKeeper().getChildren(zkPath + "/" + taskType, false)) {
        result.add(loadScheduleStrategyRuntime(taskType, uuid));
      }
    }
    return result;
  }

  /**
   * 加载某个任务管理器的所有调度策略的运行状态
   * @param managerFactoryUUID 任务管理器UUID
   * @return 调度策略的运行状态列表
   */
  public List<ScheduleStrategyRuntime> loadAllScheduleStrategyRuntimeByUUID(String managerFactoryUUID)
    throws Exception {
    List<ScheduleStrategyRuntime> result = new ArrayList<>();
    String zkPath = this.PATH_Strategy;
    List<String> strategyNameList = this.getZooKeeper().getChildren(zkPath, false);
    Collections.sort(strategyNameList);
    for (String strategyName : strategyNameList) {
      if (this.getZooKeeper().exists(zkPath + "/" + strategyName + "/" + managerFactoryUUID, false) != null) {
        result.add(loadScheduleStrategyRuntime(strategyName, managerFactoryUUID));
      }
    }
    return result;
  }

  /**
   * 加载某个策略名称的所有调度策略的运行状态
   * @param strategyName 策略名称
   * @return 调度策略的运行状态列表
   */
  public List<ScheduleStrategyRuntime> loadAllScheduleStrategyRuntimeByStrategyName(String strategyName)
    throws Exception {
    List<ScheduleStrategyRuntime> result = new ArrayList<>();
    String zkPath = this.PATH_Strategy;
    if (this.getZooKeeper().exists(zkPath + "/" + strategyName, false) == null) {
      return result;
    }
    List<String> uuidList = this.getZooKeeper().getChildren(zkPath + "/" + strategyName, false);
    Collections.sort(uuidList, (u1, u2)->u1.substring(u1.lastIndexOf("$") + 1).compareTo(u2.substring(u2.lastIndexOf("$") + 1)));
    for (String uuid : uuidList) {
      result.add(loadScheduleStrategyRuntime(strategyName, uuid));
    }
    return result;
  }

  /**
   * 更新请求数量
   */
  public void updateStrategyRuntimeReqestNum(String strategyName, String managerFactoryUUID, int requestNum)
    throws Exception {
    String zkPath = this.PATH_Strategy + "/" + strategyName + "/" + managerFactoryUUID;
    ScheduleStrategyRuntime result;
    if (this.getZooKeeper().exists(zkPath, false) != null) {
      result = this.loadScheduleStrategyRuntime(strategyName, managerFactoryUUID);
    } else {
      result = new ScheduleStrategyRuntime();
      result.setStrategyName(strategyName);
      result.setUuid(managerFactoryUUID);
      result.setRequestNum(requestNum);
      result.setMessage("");
    }
    result.setRequestNum(requestNum);
    String valueString = this.gson.toJson(result);
    this.getZooKeeper().setData(zkPath, valueString.getBytes(), -1);
  }

  /**
   * 更新策略调度时的错误信息
   * @param strategyName 策略名称
   * @param managerFactoryUUID 任务管理器UUID
   * @param message 信息
   */
  public void updateStrategyRuntimeErrorMessage(String strategyName, String managerFactoryUUID, String message)
    throws Exception {
    String zkPath = this.PATH_Strategy + "/" + strategyName + "/" + managerFactoryUUID;
    ScheduleStrategyRuntime result;
    if (this.getZooKeeper().exists(zkPath, false) != null) {
      result = this.loadScheduleStrategyRuntime(strategyName, managerFactoryUUID);
    } else {
      result = new ScheduleStrategyRuntime();
      result.setStrategyName(strategyName);
      result.setUuid(managerFactoryUUID);
      result.setRequestNum(0);
    }
    result.setMessage(message);
    String valueString = this.gson.toJson(result);
    this.getZooKeeper().setData(zkPath, valueString.getBytes(), -1);
  }

  public void updateManagerFactoryInfo(String uuid, boolean isStart) throws Exception {
    String zkPath = this.PATH_ManagerFactory + "/" + uuid;
    if (this.getZooKeeper().exists(zkPath, false) == null) {
      throw new Exception("任务管理器不存在:" + uuid);
    }
    this.getZooKeeper().setData(zkPath, Boolean.toString(isStart).getBytes(), -1);
  }

  /**
   * 加载任务管理器信息
   * @param managerFactoryUUID 任务管理器UUID
   * @return 任务管理器信息
   */
  public ManagerFactoryInfo loadManagerFactoryInfo(String managerFactoryUUID) throws Exception {
    String zkPath = this.PATH_ManagerFactory + "/" + managerFactoryUUID;
    if (this.getZooKeeper().exists(zkPath, false) == null) {
      throw new Exception("任务管理器不存在:" + managerFactoryUUID);
    }
    byte[] value = this.getZooKeeper().getData(zkPath, false, null);
    ManagerFactoryInfo result = new ManagerFactoryInfo();
    result.setUuid(managerFactoryUUID);
    if (value == null) {
      result.setStart(true);
    } else {
      result.setStart(Boolean.parseBoolean(new String(value)));
    }
    return result;
  }

  /**
   * 加载所有任务管理器信息
   * @return 任务管理器信息列表
   */
  public List<ManagerFactoryInfo> loadAllManagerFactoryInfo() throws Exception {
    String zkPath = this.PATH_ManagerFactory;
    List<ManagerFactoryInfo> result = new ArrayList<>();
    List<String> names = this.getZooKeeper().getChildren(zkPath, false);
    Collections.sort(names, (o1, o2)->o1.substring(o1.lastIndexOf("$") + 1).compareTo(o2.substring(o2.lastIndexOf("$") + 1)));
    for (String name : names) {
      ManagerFactoryInfo info = new ManagerFactoryInfo();
      info.setUuid(name);
      byte[] value = this.getZooKeeper().getData(zkPath + "/" + name, false, null);
      if (value == null) {
        info.setStart(true);
      } else {
        info.setStart(Boolean.parseBoolean(new String(value)));
      }
      result.add(info);
    }
    return result;
  }

  public ZooKeeper getZooKeeper() throws Exception {
    return this.zkManager.getZooKeeper();
  }

}

