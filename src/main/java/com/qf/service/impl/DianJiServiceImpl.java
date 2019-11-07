package com.qf.service.impl;

import com.qf.dao.DianJiLvMapper;
import com.qf.entity.DianJiLv;
import com.qf.service.IDianJiService;
import com.qf.util.LockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
@Service
public class DianJiServiceImpl implements IDianJiService {
    @Autowired //注入分布式锁工具类对象
    private LockUtil lockUtil;
    @Autowired //注入mapper
    private DianJiLvMapper dianJiLvMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override  //统计点击率的方法
    public int addDianJiLv() {
    //-->线程1
    //-->线程2
//   redisTemplate.opsForValue().setIfAbsent("","",5, TimeUnit.SECONDS);
     //获得分布式锁
     boolean islock=lockUtil.lock("mylock",5);
      //判断是否获得锁
        if(islock){
            //获得分布式锁
            DianJiLv dianJiLv=dianJiLvMapper.selectById(1);
            dianJiLv.setDianji(dianJiLv.getDianji()+1);

            //更新点击率
            int result=dianJiLvMapper.updateById(dianJiLv);
            //释放分布式锁
            lockUtil.unlock("mylock");
            return result;
        }else{
            //设置锁的自旋
            try{
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return this.addDianJiLv();
        }

    }
}
