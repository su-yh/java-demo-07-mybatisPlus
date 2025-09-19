package com.suyh1101.generator.plus;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 这里使用 6 个字节来存储ID 值
 */
@Component
public class SuyhIdGenerator implements IdentifierGenerator {
    // 最后一次生成的ID
    private long lastId;

    // 相对开始时间时间戳
    private final long startMs;

    // 一个时间单位内允许生成的ID 数量
    // 这里给了18 个二进制位来存储一个时间单位内的ID
    public static final int MAX_SEQUENCE = 1 << 18;

    public SuyhIdGenerator() {
        // 这个值是可以修改的，但是一个工程应该只在首次使用的时候指定，后面就只能固定该值了。
        LocalDate localDate = LocalDate.of(2025, 1, 1);
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.of("UTC"));
        startMs = zonedDateTime.toInstant().toEpochMilli();
        System.out.println("Start timestamp (UTC): 0x" + Long.toHexString(startMs));
        System.out.println("Start timestamp (UTC): " + startMs);

        long relativeMs = System.currentTimeMillis() - startMs;

        // 当前时间戳相对于 20250101(UTC 时区时间)
        // (initMs >> 10 + 1): 将最低10 位清0，并+ 1，使得初始值为当前时间未来的 1024 毫秒时间里面的，而不是已经过去的时间
        // (<< 10): 把最低10 位补0  用来存储基础的ID 增量值
        // (<< 8):  除了一个时间单位的ID 增量值外，再给8 个二进制位来存储额外的ID 增量值
        // 何为增量值：就是在一个单位时间内（这里的单位时间是1024 毫秒），允许生成多少个ID 值，如果超过了自然是不允许的，只有等下一个单位时间才可以。
        // 当前时间以一个固定的时间偏移量，毫秒
        lastId = ((relativeMs >> 10) + 1) << (10 + 8);
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
        if (n <= 0 || n > MAX_SEQUENCE) {
            throw new IllegalArgumentException("Invalid number of IDs requested: " + n);
        }

        while (true) {
            Long id = obtainStartId(n);
            if (id != null) {
                return id;
            }

            // TODO: suyh - 测试时使用这个
            if (true) {
                Thread.yield();
                continue;
            }

            // 等到下一个时间周期
            try {
                for (int i = 0; i < 100; i++) {
                    TimeUnit.MICROSECONDS.sleep(10L);
                }
            } catch (InterruptedException ignored) {
            }
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
     * @param curMs 当前时间戳，单位：毫秒
     */
    private long maxId(long curMs) {
        long relativeMs = curMs - startMs;

        // 清理掉一个时间单位上的二进制数，然后空出ID 容量部分的二进制位
        long msPart = (relativeMs >> 10) << 18;

        // 时间部分再或上容量就是当前时间
        return msPart | (MAX_SEQUENCE - 1);
    }

    @Override
    public String nextUUID(Object entity) {
        return nextUuid();
    }

}
