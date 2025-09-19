package com.suyh1101.generator.plus;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

/**
 * 这里使用 6 个字节来存储ID 值
 */
@Component
public class SuyhIdGenerator implements IdentifierGenerator {
    // 最后一次生成的ID
    private long lastId;
    // 一个时间单位内允许生成的ID 数量
    // 这里给了18 个二进制位来存储一个时间单位内的ID
    public static final int TIME_UNIT_ID_CAPACITY = 1 << 18 - 1;

    public SuyhIdGenerator() {
        // 这个值是可以修改的，但是一个工程应该只在首次使用的时候指定，后面就只能固定该值了。
        LocalDate localDate = LocalDate.of(2025, 1, 1);
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.of("UTC"));
        long startMs = zonedDateTime.toInstant().toEpochMilli();
        System.out.println("startId: 0x" + Long.toHexString(startMs));
        System.out.println("startId: " + startMs);

        long initMs = System.currentTimeMillis() - startMs;
        System.out.println("initMs: 0x" + Long.toHexString(initMs));
        System.out.println("initMs: " + initMs);
        System.out.println("initMs: " + Duration.ofMillis(initMs).toMinutes());

        // 当前时间戳相对于 20250101(UTC 时区时间)
        // (initMs >> 10 + 1): 将最低10 位清0，并+ 1，使得初始值为当前时间未来的 1024 毫秒时间里面的，而不是已经过去的时间
        // (<< 10): 把最低10 位补0  用来存储基础的ID 增量值
        // (<< 8):  除了一个时间单位的ID 增量值外，再给8 个二进制位来存储额外的ID 增量值
        // 何为增量值：就是在一个单位时间内（这里的单位时间是1024 毫秒），允许生成多少个ID 值，如果超过了自然是不允许的，只有等下一个单位时间才可以。
        // 当前时间以一个固定的时间偏移量，毫秒
        lastId = ((initMs >> 10) + 1) << (10 + 8);
        System.out.println("lastId: 0x" + Long.toHexString(lastId));
    }

    @Override
    public Number nextId(Object entity) {
        return nextId();
    }

    @NonNull
    public String nextUuid() {
        long id = nextId();
        // 一个id 的有效存储范围固定为6 个字节，超过的全部丢弃。
        // 主要就是为base64 做处理。3 的倍数是刚刚好。
        byte[] bytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) (id >> i);
        }

        return Base64.getEncoder().encodeToString(bytes);
    }

    public long nextId() {
        return nextIds(1);
    }

    public long nextIds(int n) {
        while (true) {
            Long id = obtainStartId(n);
            if (id != null) {
                return id;
            }
            Thread.yield();
        }
    }

    /**
     *
     * @param n 希望获得id 的数量
     * @return 返回第一个可用的id，该id + n 则为最后一个可用id
     */
    public synchronized Long obtainStartId(int n) {
        long curMs = System.currentTimeMillis();
        long maxId = maxId(curMs);
        System.out.println("maxId: 0x" + Long.toHexString(maxId));
        long expectMaxId = lastId + n;
        if (expectMaxId > maxId) {
            // 需要等到下一个时间单位才允许再次生成新的ID 值。
            return null;
        }

        long startId = lastId + 1;
        lastId = lastId + n;
        return startId;
    }

    /**
     * 当前时刻允许使用的最大ID 值
     *
     * @param ms 时间戳，单位：毫秒
     */
    private long maxId(long ms) {
        // 清理掉一个时间单位上的二进制数，然后空出ID 容量部分的二进制位
        long msPart = (ms >> 10) << 18;

        // 时间部分再或上容量就是当前时间
        return msPart | TIME_UNIT_ID_CAPACITY;
    }

    @Override
    public String nextUUID(Object entity) {
        return nextUuid();
    }

}
