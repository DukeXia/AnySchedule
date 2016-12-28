package cn.ictgu.anytask;

import cn.ictgu.commen.IScheduleTaskDealSingle;
import cn.ictgu.commen.TaskItemDefine;
import cn.ictgu.model.Account;
import cn.ictgu.service.AccountService;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 数据库处理任务测试
 * Created by Silence on 2016/12/27.
 */
@Log4j
@Component("dbTask")
public class DbTask implements IScheduleTaskDealSingle<Account>{

  @Autowired
  private AccountService service;

  @Override
  public List<Account> selectTasks(String taskParameter, String ownSign, int taskItemNum, List<TaskItemDefine> taskItemList, int eachFetchDataNum) throws Exception {
    log.info("[dbTask]开始收集任务------------>");
    log.info("taskItemDefine:"+taskItemList);
    return service.getAccounts(eachFetchDataNum);
  }

  @Override
  public Comparator<Account> getComparator() {
    return null;
  }

  @Override
  public boolean execute(Account task, String ownSign) throws Exception {
    log.info("[dbTask]开始执行任务----------->"+task.getUsername()+"余额："+task.getBalance()+"，执行+1操作");
    task.setBalance(task.getBalance() + 1);
    return service.updateBalance(task);
  }
}