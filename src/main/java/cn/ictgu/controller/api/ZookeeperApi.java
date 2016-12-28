package cn.ictgu.controller.api;

import cn.ictgu.strategy.AnyScheduleManagerFactory;
import cn.ictgu.zk.ZKTools;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Zookeeper API
 * Created by Silence on 2016/12/24.
 */
@RestController
@RequestMapping("/zookeeper")
public class ZookeeperApi {

  @Autowired
  private AnyScheduleManagerFactory factory;

  @RequestMapping("/query")
  public List<String> query(HttpServletRequest request) throws Exception {
    String path = request.getParameter("path");
    ZooKeeper zk = factory.getZkManager().getZooKeeper();
    String[] list = ZKTools.getTree(zk, path);
    List<String> result = new ArrayList<>();
    Stat stat = new Stat();
    for (String name : list) {
      byte[] value = zk.getData(name, false, stat);
      if (value == null) {
        result.add("<p>" + name + "</p>");
      } else {
        result.add("<p>" + name + "[v." + stat.getVersion() + "][" + new String(value) + "]</p>");
      }
    }
    return result;
  }

  @RequestMapping("/delete")
  public String delete(HttpServletRequest request) {
    String path = request.getParameter("path");
    String message = "success";
    try {
      ZooKeeper zk = factory.getZkManager().getZooKeeper();
      ZKTools.deleteTree(zk, path);
    } catch (Throwable e) {
      message = "ERROR: " + e.getMessage();
    }
    return message;
  }

}
