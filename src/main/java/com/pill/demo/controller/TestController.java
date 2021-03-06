package com.pill.demo.controller;

import com.pill.demo.bean.User;
import com.pill.demo.redis.JedisUtil;
import com.pill.demo.redis.RedisDistributeLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author guowei
 * @version 1.0
 * @description 测试redis集群
 * @date 2019/4/2 14:09
 */
@RestController
public class TestController {
    @Autowired
    private JedisUtil jedisUtil;

    @Autowired
    private RedisDistributeLock distributeLock;

    /**
     * http://127.0.0.1:8081/setUser
     * @return
     */
    @RequestMapping(value = "/setString")
    String setString(){
        jedisUtil.set("user","a");
        return "success";
    }

    /**
     * http://127.0.0.1:8081/getString
     * @return
     */
    @RequestMapping(value = "/getString")
    String getString(){
        return jedisUtil.get("user");
    }

    /**
     * http://127.0.0.1:8081/setUser
     * @return
     */
    @RequestMapping(value = "/setUser")
    String setUser(){
        jedisUtil.setObject("userObject",new User("test",12),100);
        return "success";
    }

    /**
     * http://127.0.0.1:8081/getUser
     * @return
     */
    @RequestMapping(value = "/getUser")
    User getUser(){
       User user= jedisUtil.getObject("userObject",User.class);
       return user;
    }

    /**
     * 集群分布式锁
     * http://127.0.0.1:8081/testLock
     * @return
     */
    @RequestMapping(value = "/testLock")
    String testLock(){
        ExecutorService service = Executors.newFixedThreadPool(20);
        jedisUtil.set("test_inc","0");
        for (int i = 0; i < 100; i++) {
            int index = i;
            service.execute(new Runnable() {
                @Override
                public void run() {
                    try {
//                        if(distributeLock.tryLock("TEST_LOCK_KEY","TEST_LOCK_VAL_"+ index,1000*10,1000*10)){
                        if(distributeLock.tryLock("TEST_LOCK_KEY","TEST_LOCK_VAL_"+ index,1000*5,10,10)){
//                            System.out.println("get lock success:---" + "TEST_LOCK_VAL_"+ index);

                            int test_inc = Integer.valueOf(jedisUtil.get("test_inc"));
                            String uid = UUID.randomUUID().toString();
                            if(test_inc<10){
                                jedisUtil.inc("test_inc");
                                System.out.println("用户：" + uid + "抢购成功,人数:" + (test_inc + 1));
                            }else{
                                System.out.println("用户：" + uid + "抢购失败");
                                return;
                            }
                            if (!distributeLock.tryUnLock("TEST_LOCK_KEY", "TEST_LOCK_VAL_"+ index)){
                                throw new RuntimeException("release lock fail");
                            }
//                            System.out.println("release lock success:---" + "TEST_LOCK_VAL_"+ index);
                        } else {
//                            System.out.println("get lock fail :---" + "TEST_LOCK_VAL_"+ index);
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }
        service.shutdown();
        return "success";
    }

    /**
     * 集群分布式锁
     * http://127.0.0.1:8081/init
     * @return
     */
    @RequestMapping(value = "/init")
    void init(){
        jedisUtil.set("test_inc","0");
    }

    /**
     * 集群分布式锁 使用jmeter测试
     * http://127.0.0.1:8081/testLockByTool
     * @return
     */
    @RequestMapping(value = "/testLockByTool")
    void testLockByTool(){
        String uid = UUID.randomUUID().toString();
        System.out.println(uid+":"+System.currentTimeMillis());
        //两种方式进行获取锁
//        if(distributeLock.tryLock("TEST_LOCK_KEY1","TEST_LOCK_VAL_"+ uid,1000*5,1000*10)){
        if(distributeLock.tryLock("TEST_LOCK_KEY1","TEST_LOCK_VAL_"+ uid,1000*5,10,10)){
            System.out.println("get lock success:---" + "TEST_LOCK_VAL_"+ uid+":"+System.currentTimeMillis());
            int test_inc = Integer.valueOf(jedisUtil.get("test_inc"));
            if(test_inc<10){
                jedisUtil.inc("test_inc");
                System.out.println("用户：" + uid + "抢购成功,人数:" + (test_inc + 1)+":"+System.currentTimeMillis());
            }else{
                System.out.println("用户：" + uid + "抢购失败"+":"+System.currentTimeMillis());
            }
            if (!distributeLock.tryUnLock("TEST_LOCK_KEY1", "TEST_LOCK_VAL_"+ uid)){
                throw new RuntimeException("release lock fail");
            }
            System.out.println("release lock success:---" + "TEST_LOCK_VAL_"+ uid+":"+System.currentTimeMillis());
        } else {
            System.out.println("get lock fail :---" + "TEST_LOCK_VAL_"+ uid);
        }
    }
}
