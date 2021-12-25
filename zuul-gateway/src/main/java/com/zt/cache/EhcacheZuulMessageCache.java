package com.zt.cache;

import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.ehcache.EhCacheCache;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description ehcache zuul网关报文缓存
 */
@Slf4j
@Component
public class EhcacheZuulMessageCache implements ZuulMessageCache<String> {

    private static final String CACHE_NAME = "zuulMessage";

    @Autowired
    private CacheManager cacheManager;

    private Cache cache;

    @PostConstruct
    public void init() {
        cache = cacheManager.getCache(CACHE_NAME);
    }

    @Override
    public boolean set(String key, String o) {
        return set(key, o, Long.MAX_VALUE);
    }

    @Override
    public boolean set(String key, String o, long timeout) {
        cache.put(key, o);
        if (log.isDebugEnabled()) {
            log.debug("cache data, key: {}, value: {}", key, o);
        }
        return true;
    }

    @Override
    public String get(String key) {
        Cache.ValueWrapper wrapper = cache.get(key);
        return wrapper != null ? wrapper.get().toString() : null;
    }

    @Override
    public Map<String, String> getByPattern(String regex) {
        if (this.cache instanceof EhCacheCache) {
            try {
                // 获取 ehcache 对象实例
                final Field field = this.cache.getClass().getDeclaredField("cache");
                field.setAccessible(true);
                final Ehcache ehcache = (Ehcache)ReflectionUtils.getField(field, this.cache);
                final List<String> keys = ehcache.getKeys();

                PathMatcher pm = new AntPathMatcher();
                if (!CollectionUtils.isEmpty(keys)) {
                    // 正则匹配出所有符合的key值
                    final List<String> list = keys.stream().filter(key -> pm.match(regex, key)).collect(Collectors.toList());

                    // 获取缓存
                    if (CollectionUtils.isEmpty(list)) {
                        return Collections.EMPTY_MAP;
                    }
                    final Map<Object, Element> all = ehcache.getAll(list);

                    Map<String, String> result = new HashMap<>(all.size());
                    all.entrySet().stream().forEach(entry -> {
                        final Object value = entry.getValue().getObjectValue();
                        result.put(entry.getKey().toString(), value == null ? null : value.toString());
                    });
                    return result;
                }
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Application cache is not use ehcache");
    }

}
