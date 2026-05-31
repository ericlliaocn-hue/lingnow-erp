package cc.lingnow.common.util;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 *
 * @author LingNow Team
 */
@Slf4j
@Component
public class RedisUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("Redis set error: key={}, value={}", key, value, e);
            return false;
        }
    }

    /**
     * 设置缓存（带过期时间）
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间(秒)
     * @return true成功 false失败
     */
    public boolean set(String key, Object value, long timeout) {
        try {
            if (timeout > 0) {
                redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("Redis set error: key={}, value={}, timeout={}", key, value, timeout, e);
            return false;
        }
    }

    /**
     * 获取缓存
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     *
     * @param key 键（可以传一个或多个）
     * @return 删除数量
     */
    public Long del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                Boolean result = redisTemplate.delete(key[0]);
                return result != null && result ? 1L : 0L;
            } else {
                Long count = redisTemplate.delete(java.util.Arrays.asList(key));
                return count != null ? count : 0L;
            }
        }
        return 0L;
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true存在 false不存在
     */
    public boolean hasKey(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return result != null && result;
        } catch (Exception e) {
            log.error("Redis hasKey error: key={}", key, e);
            return false;
        }
    }

    /**
     * 删除匹配的key
     *
     * @param pattern 匹配模式
     */
    public void delByPattern(String pattern) {
        try {
            java.util.Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.error("Redis delByPattern error: pattern={}", pattern, e);
        }
    }

    /**
     * 设置过期时间
     *
     * @param key     键
     * @param timeout 过期时间(秒)
     * @return true成功 false失败
     */
    public boolean expire(String key, long timeout) {
        try {
            if (timeout > 0) {
                Boolean result = redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
                return result != null && result;
            }
            return false;
        } catch (Exception e) {
            log.error("Redis expire error: key={}, timeout={}", key, timeout, e);
            return false;
        }
    }

    /**
     * 获取过期时间
     *
     * @param key 键
     * @return 过期时间(秒) -1表示永久有效
     */
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null ? expire : -1;
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 递增因子
     * @return 递增后的值
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("递增因子必须大于0");
        }
        Long result = redisTemplate.opsForValue().increment(key, delta);
        return result != null ? result : 0L;
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 递减因子
     * @return 递减后的值
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("递减因子必须大于0");
        }
        Long result = redisTemplate.opsForValue().increment(key, -delta);
        return result != null ? result : 0L;
    }

}
