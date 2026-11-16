package com.naci.guestbook.mapper;

import com.naci.guestbook.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    /**
     * 按用户 ID 查询用户。
     *
     * @param id 用户 ID
     * @return 用户实体；不存在时返回 null
     */
    User findById(@Param("id") Long id);

    /**
     * 按用户名查询用户。
     *
     * @param username 用户名
     * @return 用户实体；不存在时返回 null
     */
    User findByUsername(@Param("username") String username);

    /**
     * 新增用户，并回填数据库生成的主键。
     *
     * @param user 待新增用户
     * @return 受影响行数
     */
    int insert(User user);
}
