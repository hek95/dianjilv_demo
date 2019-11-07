package com.qf.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

/**
 * 分布式锁操作工具
 */
@Component
public class LockUtil {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    //添加分布式锁的lua脚本
    private String lockLua="--使用lua脚本添加分布式锁\n"+
            "--需要的变量\n"+
            "local lockName=KEYS[1]\n"+
            "local lockValue=ARGV[1]\n"+
            "local lockTimeOut=ARGV[2]\n"+
            "\n"+
            "--设置分布式锁\n"+
            "local result=redis.call('setnx',lockName,lockValue)\n"+
            "\n"+
            "--判断是否获得分布式锁\n"+
            "if result==1 then\n"+
            "\t--获得分布锁,添加超时时间\n"+
            "\t redis.call('expire',lockName,lockTimeOut)\n"+
            "\t--返回1表示获得分布式锁\t\n"+
            "\treturn '1'\n"+
            "else\n"+
            "\t--表示没有获得分布式锁\n"+
            "\treturn '0'\n"+
            "end\n";
    //删除分布式锁的lua脚本
    private String lockDelLua="--删除分布式锁的lua脚本\n"+
            "--获得变量\n"+
            "local lockName=KEYS[1]\n"+
            "local uuid=ARGV[1]\n"+
             "\n"+
            "--获得锁的value\n"+
            "local lockValue=redis.call('get',lockName)\n"+
            "\n"+
            "--判断锁的value和添加锁时的uuid是否一致\n"+
            "if lockValue==uuid then\n"+
            "\t--说明锁是当前线程添加\n"+
            "\tredis.call('del',lockName);\n"+
            "\t--返回成功\n"+
            "\treturn '1'\n"+
            "end\n"+
            "\n"+
            "--说明不是当前线程添加\n"+
            "return '0'";
    private ThreadLocal<String>threadLocal=new ThreadLocal<>();
    /**
     * 添加分布式锁
     *
     */
    public boolean lock(String lockName,Integer timeoutSecond){
        //锁的value
        String uuid = UUID.randomUUID().toString();
        threadLocal.set(uuid);
        //调用lua脚本添加分布式锁
        String luaResult=(String) stringRedisTemplate.execute(new DefaultRedisScript(lockLua,String.class),
                Collections.singletonList(lockName),uuid,timeoutSecond+"");
        return Integer.parseInt(luaResult)==1;

    }
    /**
     * 解除分布式锁
     */
    public boolean  unlock(String lockName){
        String uuid=threadLocal.get();
        //调用lua脚本删除分布式锁
        String result=(String)stringRedisTemplate.execute(new DefaultRedisScript(lockLua,String.class), Collections.singletonList(lockName),uuid);
        return Integer.parseInt(result)==1;
    }
}
