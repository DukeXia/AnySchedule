package cn.ictgu.zk;

import lombok.extern.log4j.Log4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Zookeeper Tools
 * Created by Silence on 2016/12/19.
 */
@Log4j
public class ZKTools {

  static void createPath(ZooKeeper zk, String path, CreateMode createMode, List<ACL> acl) throws Exception {
    String[] list = path.split("/");
    String zkPath = "";
    for (String str : list) {
      if (!str.equals("")) {
        zkPath = zkPath + "/" + str;
        if (zk.exists(zkPath, false) == null) {
          zk.create(zkPath, null, acl, createMode);
        }
      }
    }
  }

  public static void deleteTree(ZooKeeper zk, String path) throws Exception {
    String[] list = getTree(zk, path);
    for (int i = list.length - 1; i >= 0; i--) {
      zk.delete(list[i], -1);
    }
  }

  public static String[] getTree(ZooKeeper zk, String path) throws Exception {
    if (zk.exists(path, false) == null) {
      return new String[0];
    }
    List<String> dealList = new ArrayList<>();
    dealList.add(path);
    int index = 0;
    while (index < dealList.size()) {
      String tempPath = dealList.get(index);
      List<String> children = zk.getChildren(tempPath, false);
      if (!tempPath.equalsIgnoreCase("/")) {
        tempPath = tempPath + "/";
      }
      Collections.sort(children);
      for (int i = children.size() - 1; i >= 0; i--) {
        dealList.add(index + 1, tempPath + children.get(i));
      }
      index++;
    }
    return dealList.toArray(new String[0]);
  }
}
