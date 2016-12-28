package cn.ictgu.anytask;

import cn.ictgu.commen.IScheduleTaskDealSingle;
import cn.ictgu.commen.TaskItemDefine;
import lombok.extern.log4j.Log4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Http任务测试(爬取熊猫TV “全部” 页面下的主播昵称)
 * Created by Silence on 2016/12/28.
 */
@Log4j
@Component("httpTask")
public class HttpTask implements IScheduleTaskDealSingle<String> {

  private static final String UA = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36";

  @Override
  public boolean execute(String task, String ownSign) throws Exception {
    log.info("熊猫tv主播：" + task);
    return true;
  }

  @Override
  public List<String> selectTasks(String taskParameter, String ownSign, int taskItemNum, List<TaskItemDefine> taskItemList, int eachFetchDataNum) throws Exception {
    List<String> nickNames = new ArrayList<>();
    Document document = Jsoup.connect("http://www.panda.tv/all").userAgent(UA).get();
    Elements elements = document.select("li.video-list-item.video-no-tag");
    int size = elements.size();
    for (TaskItemDefine taskItemDefine : taskItemList){
      int taskItemId = Integer.valueOf(taskItemDefine.getTaskItemId());
      while (taskItemId < size){
        Element element = elements.get(taskItemId);
        nickNames.add(element.select("span.video-nickname").text());
        taskItemId += 10;
      }
    }
    return nickNames;
  }

  @Override
  public Comparator<String> getComparator() {
    return null;
  }
}
