package cn.ictgu.service;

import cn.ictgu.mapper.AccountMapper;
import cn.ictgu.model.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Silence on 2016/12/27.
 */
@Service
public class AccountService {
  @Autowired
  private AccountMapper mapper;

  public List<Account> getAccounts(int size){
    return mapper.selectAll(size);
  }

  public boolean updateBalance(Account account){
    return 1 == mapper.updateBalance(account);
  }
}
