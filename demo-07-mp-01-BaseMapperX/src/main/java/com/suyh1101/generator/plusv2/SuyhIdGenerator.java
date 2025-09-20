package com.suyh1101.generator.plusv2;

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
 * 其中18 位用来存储增量值，30 位用来存储时间，在这30 位里面，每一位表示1024 ms ，所以最大可使用约：34.86 年
 * 从 2025-01-01 开始计算，这套 ID 系统可以使用到大约 2059 年（具体是 2025 + 34.86 ≈ 2059 年底）
 */
@Component
public class SuyhIdGenerator implements IdentifierGenerator {
    // 一个时间单位内允许生成的ID数量（18位二进制）
    public static final int MAX_SEQUENCE = 1 << 18;

    // 最后一次生成的ID
    protected long lastId;

    // 起始时间戳（UTC 2025-01-01）
    protected final long startMs;

    // 是否生成有序UUID
    protected final boolean uuidOrdered;

    // 复用的字节数组（仅在同步方法内使用）
    private static final byte[] BYTES = new byte[6];
    // 复用的位存储数组（仅在同步方法内使用）
    private static final boolean[] BITS = new boolean[48];

    // 固定乱序映射规则（0~47表示原始位索引，值表示新位置）
    // 注意：需确保包含0~47每个数字恰好一次
    private static final int[] SHUFFLE_RULE = {
            23, 10, 45, 3, 37, 18,
            7, 41, 14, 29, 0, 32,
            46, 19, 25, 8, 34, 12,
            30, 4, 27, 16, 42, 39,
            11, 22, 38, 5, 20, 44,
            33, 1, 9, 2, 40, 15,
            28, 6, 31, 17, 24, 35,
            47, 13, 36, 21, 43, 26
    };

    public SuyhIdGenerator() {
        this(false);
    }

    public SuyhIdGenerator(boolean uuidOrdered) {
        // 这个值是可以修改的，但是一个工程应该只在首次使用的时候指定，后面就只能固定该值了。
        LocalDate localDate = LocalDate.of(2025, 1, 1);
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.of("UTC"));
        this.startMs = zonedDateTime.toInstant().toEpochMilli();

        long relativeMs = initSystemMs() - startMs;

        // 当前时间戳相对于 20250101(UTC 时区时间)
        // (initMs >> 10 + 1): 将最低10 位清0，并+ 1，使得初始值为当前时间未来的 1024 毫秒时间里面的，而不是已经过去的时间
        // (<< 10): 把最低10 位补0  用来存储基础的ID 增量值
        // (<< 8):  除了一个时间单位的ID 增量值外，再给8 个二进制位来存储额外的ID 增量值
        // 何为增量值：就是在一个单位时间内（这里的单位时间是1024 毫秒），允许生成多少个ID 值，如果超过了自然是不允许的，只有等下一个单位时间才可以。
        // 当前时间以一个固定的时间偏移量，毫秒
        // 初始化lastId到当前时间的下一个时间单位（1024ms）
        this.lastId = ((relativeMs >> 10) + 1) << (10 + 8);

        this.uuidOrdered = uuidOrdered;

        // 验证乱序规则的有效性
        validateShuffleRule();
    }

    // 调试用的时间戳控制
    public static Long DEBUG_INIT = null;
    public static Long DEBUG_CURR = null;

    private static long initSystemMs() {
        return DEBUG_INIT != null ? DEBUG_INIT : System.currentTimeMillis();
    }

    private static long currentMs() {
        return DEBUG_CURR != null ? DEBUG_CURR : System.currentTimeMillis();
    }

    // 验证乱序规则是否包含0~47所有数字
    private void validateShuffleRule() {
        boolean[] seen = new boolean[48];
        for (int index : SHUFFLE_RULE) {
            if (index < 0 || index >= 48 || seen[index]) {
                throw new IllegalArgumentException("Invalid SHUFFLE_RULE: 包含重复或越界的索引");
            }
            seen[index] = true;
        }
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

            // 等待下一个时间单位
            try {
                for (int i = 0; i < 10; i++) {
                    TimeUnit.MICROSECONDS.sleep(1L);
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    // 转换ID为Base64编码（复用BYTES数组）
    protected String convertUuid(long id) {
        for (int j = 0; j < 6; j++) {
            BYTES[j] = (byte) (id >> (j * 8));
        }
        return Base64.getEncoder().encodeToString(BYTES);
    }

    // 对低48位进行乱序重排（复用BITS数组）
    protected long shuffleLow48Bits(long id) {
        // 每一位都存储为boolean 值
        for (int i = 0; i < 48; i++) {
            BITS[i] = (id & (1L << i)) != 0;
        }

        // 按规则重排
        long shuffled = 0;
        for (int i = 0; i < 48; i++) {
            int originalIndex = SHUFFLE_RULE[i];
            if (BITS[originalIndex]) {
                shuffled |= (1L << i);
            }
        }

        return shuffled;
    }

    /**
     * 分配一批ID
     * @param n 所需ID数量
     * @return 起始ID，若当前时间单位不足则返回null
     */
    protected Long allocateIds(int n) {
        long curMs = currentMs();
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
     * 计算当前时间单位的最大可用ID
     */
    protected long maxId(long curMs) {
        long relativeMs = curMs - startMs;

        // 时间单位：1024ms（2^10），左移18位给序列号留出空间
        long msPart = (relativeMs >> 10) << 18;
        return msPart | (MAX_SEQUENCE - 1);
    }

    @Override
    public String nextUUID(Object entity) {
        return nextUuid();
    }
}
