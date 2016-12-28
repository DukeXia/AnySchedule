package cn.ictgu.mapper;

import cn.ictgu.model.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 数据库 account 表映射
 * Created by Silence on 2016/12/27.
 */
@Mapper
public interface AccountMapper {

  @Select("SELECT * FROM account LIMIT #{size}")
  List<Account> selectAll(@Param("size") int size);

  @Update("UPDATE account SET balance = #{balance} WHERE id = #{id}")
  int updateBalance(Account account);

}
