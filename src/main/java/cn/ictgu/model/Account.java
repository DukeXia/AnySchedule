package cn.ictgu.model;

import lombok.Data;

import java.util.Date;

/**
 * 账目
 * Created by Silence on 2016/12/27.
 */
@Data
public class Account {
  private long id;
  private String username;
  private int balance;
  private Date rowUpdateTime;
}
