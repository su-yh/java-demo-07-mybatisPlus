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
    // 一个时间单位内允许生成的ID 数量
    // 这里给了18 个二进制位来存储一个时间单位内的ID
    public static final int MAX_SEQUENCE = 1 << 18;

    // 最后一次生成的ID
    protected long lastId;

    // 相对开始时间时间戳
    protected final long startMs;

    // 生成uuid 时是否乱序
    protected final boolean uuidOrdered;

    public SuyhIdGenerator() {
        this(false);
    }

    public SuyhIdGenerator(boolean uuidOrdered) {
        // 这个值是可以修改的，但是一个工程应该只在首次使用的时候指定，后面就只能固定该值了。
        LocalDate localDate = LocalDate.of(2025, 1, 1);
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.of("UTC"));
        startMs = zonedDateTime.toInstant().toEpochMilli();
        System.out.println("Start timestamp (UTC): 0x" + Long.toHexString(startMs));
        System.out.println("Start timestamp (UTC): " + startMs);

        long relativeMs = initSystemMs() - startMs;

        // 当前时间戳相对于 20250101(UTC 时区时间)
        // (initMs >> 10 + 1): 将最低10 位清0，并+ 1，使得初始值为当前时间未来的 1024 毫秒时间里面的，而不是已经过去的时间
        // (<< 10): 把最低10 位补0  用来存储基础的ID 增量值
        // (<< 8):  除了一个时间单位的ID 增量值外，再给8 个二进制位来存储额外的ID 增量值
        // 何为增量值：就是在一个单位时间内（这里的单位时间是1024 毫秒），允许生成多少个ID 值，如果超过了自然是不允许的，只有等下一个单位时间才可以。
        // 当前时间以一个固定的时间偏移量，毫秒
        lastId = ((relativeMs >> 10) + 1) << (10 + 8);

        this.uuidOrdered = uuidOrdered;
    }

    public static Long DEBUG_INIT = null;
    public static Long DEBUG_CURR = null;

    public static long initSystemMs() {
        return DEBUG_INIT != null ? DEBUG_INIT : System.currentTimeMillis();
    }

    public static long currentMs() {
        return DEBUG_CURR != null ? DEBUG_CURR : System.currentTimeMillis();
    }

    @Override
    public synchronized Number nextId(Object entity) {
        return nextId();
    }

    @NonNull
    public synchronized String nextUuid() {
        String[] uuids = nextUuids(1);
        return uuids[0];
    }

    @NonNull
    public synchronized String[] nextUuids(int n) {
        long startId = nextIds(n);

        String[] uuids = new String[n];
        for (int i = 0; i < n; i++) {
            long id = startId + i;
            long curId = this.uuidOrdered ? id : shuffleLow48Bits(id);
            uuids[i] = convertUuid(curId);
        }

        return uuids;
    }

    public synchronized long nextId() {
        return nextIds(1);
    }

    public synchronized long nextIds(int n) {
        if (n <= 0 || n > MAX_SEQUENCE) {
            throw new IllegalArgumentException("Invalid number of IDs requested: " + n);
        }

        while (true) {
            Long id = allocateIds(n);
            if (id != null) {
                return id;
            }

            // 等 0.1 秒再去尝试
            try {
                for (int i = 0; i < 10; i++) {
                    TimeUnit.MICROSECONDS.sleep(10L);
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    // 一个id 的有效存储范围固定为6 个字节，超过的全部丢弃。
    // 主要就是为base64 做处理。3 的倍数是刚刚好。
    protected static final byte[] BYTES = new byte[6];
    protected String convertUuid(long id) {
        for (int j = 0; j < 6; j++) {
            BYTES[j] = (byte) (id >> (j * 8));
        }
        return Base64.getEncoder().encodeToString(BYTES);
    }

    // 固定乱序映射规则（0~47表示位索引，值表示新位置）
    protected static final int[] SHUFFLE_RULE = {
            47, 46, 45, 44, 43, 0,
            41, 40, 39, 38, 37, 1,
            35, 34, 33, 32, 31, 2,
            29, 28, 27, 26, 25, 3,
            23, 22, 21, 20, 19, 4,
            17, 16, 15, 14, 13, 5,
            12, 11, 10, 9, 8, 7,
            6, 18, 24, 30, 36, 42
    };

    // 每一个位置的二进制位的实际值
    protected static final boolean[] BITS = new boolean[48];

    // 将低48位的每一位按固定乱序重新组装
    protected long shuffleLow48Bits(long id) {
        // 每一位都存储为boolean 值
        for (int i = 0; i < 48; i++) {
            BITS[i] = (id & (1L << i)) != 0;
        }

        // 按乱序规则重排
        long shuffled = 0;
        for (int i = 0; i < 48; i++) {
            int index = SHUFFLE_RULE[i];
            if (BITS[index]) {
                shuffled |= (1L << i);
            }
        }

        return shuffled;
    }

    /**
     * @param n 希望获得id 的数量
     * @return 返回第一个可用的id，该id + n 则为最后一个可用id
     */
    protected Long allocateIds(int n) {
//        long curMs = currentMs();
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
    protected long maxId(long curMs) {
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
