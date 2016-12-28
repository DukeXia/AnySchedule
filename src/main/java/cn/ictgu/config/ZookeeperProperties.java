package cn.ictgu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zookeeper Config Properties
 * Created by Silence on 2016/12/19.
 */
@ConfigurationProperties(prefix = "zookeeper")
@Data
public class ZookeeperProperties{
  private String host;
  private String port;
  private Integer timeout;
  private String rootPath;
  private String username;
  private String password;
}
