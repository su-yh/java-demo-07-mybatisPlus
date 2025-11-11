package com.suyh1101.generator.plusv5;

import org.apache.commons.codec.binary.Base32;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;


/**
 * 这里使用7 个字节来存储ID 值
 * 其中18 位用来存储增量值，剩下的38 位用来存储时间，在这些位里面，每一位表示1024 ms ，所以最大可使用约：8,925 年
 */
public class SuyhIdGenerator {
    public static final Random RANDOM = new Random();
    public static final Base32 B32 = new Base32(0, null, false, (byte) 0);
    // 一个时间单位内允许生成的ID数量（18位二进制）
    public static final int MAX_SEQUENCE = 1 << 18;

    // 最后一次生成的ID
    protected long lastId;

    // 起始时间戳（UTC 2025-01-01）
    public final long startMs;
    public final boolean shuffle = false;

    // id 的有效字节为7 个字节
    private static final int ID_BYTES = 7;
    // 复用的位存储数组（仅在同步方法内使用）
    // uuid 的二进制位数据存储
    private static final boolean[] BITS_56 = new boolean[ID_BYTES * Byte.SIZE];

    // 固定乱序映射规则（0~55表示原始位索引，值表示新位置）
    // 注意：需确保包含0~56每个数字恰好一次
    // 数组下标是新数据的下标位置
    // 数组元素是原始数据的下标位置
    private static final int[] SHUFFLE_RULE_56BIT = {
            35, 26, 45, 12, 55, 18, 29, 40,
            24, 39, 15, 50, 36, 10, 49, 2,
            31, 44, 21, 5, 14, 47, 34, 23,
            41, 19, 20, 3, 51, 33, 7, 25,
            42, 1, 53, 37, 0, 11, 54, 30,
            48, 6, 27, 4, 16, 32, 13, 28,
            43, 9, 38, 8, 52, 22, 17, 46
    };

    public SuyhIdGenerator() {
        // 这个值是可以修改的，但是一个工程应该只在首次使用的时候指定，后面就只能固定该值了。
        this(LocalDate.of(2025, 1, 1));
    }

    public SuyhIdGenerator(LocalDate localDate) {
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.of("UTC"));
        this.startMs = zonedDateTime.toInstant().toEpochMilli();

        long relativeMs = System.currentTimeMillis() - startMs;

        // 当前时间戳相对于 20250101(UTC 时区时间)
        // (initMs >> 10 + 1): 将最低10 位清0，并+ 1，使得初始值为当前时间未来的 1024 毫秒时间里面的，而不是已经过去的时间
        // (<< 10): 把最低10 位补0  用来存储基础的ID 增量值
        // (<< 8):  除了一个时间单位的ID 增量值外，再给8 个二进制位来存储额外的ID 增量值
        // 何为增量值：就是在一个单位时间内（这里的单位时间是1024 毫秒），允许生成多少个ID 值，如果超过了自然是不允许的，只有等下一个单位时间才可以。
        // 当前时间以一个固定的时间偏移量，毫秒
        // 初始化lastId到当前时间的下一个时间单位（1024ms）
        this.lastId = ((relativeMs >> 10) + 1) << (10 + 8);

        // 验证乱序规则的有效性
        validateShuffleRule();
    }

    // 验证乱序规则是否包含0~55所有数字，每个数字必须出现并且只出现一次
    private void validateShuffleRule() {
        validateShuffleRule(SHUFFLE_RULE_56BIT, SHUFFLE_RULE_56BIT.length);
    }

    private void validateShuffleRule(int[] shuffleRule, int size) {
        boolean[] seen = new boolean[size];
        for (int index : shuffleRule) {
            if (index < 0 || index >= size || seen[index]) {
                throw new IllegalArgumentException("Invalid SHUFFLE_RULE: 包含重复或越界的索引, size: " + size);
            }
            seen[index] = true;
        }
    }

    /**
     * 返回12 个字符长度的字符串
     */
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
            byte[] ubs = shuffle ? shuffle56Bits(id) : mapping56Bits(id);
            uuids[i] = B32.encodeToString(ubs);
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

    private byte[] mapping56Bits(long id) {
        byte[] bytes = new byte[ID_BYTES];

        // 第一步：将 id 的低7字节映射到 bytes[0] ~ bytes[6]
        // 每个字节8位，循环7个字节（0~6索引）
        for (int byteIndex = 0; byteIndex < ID_BYTES; byteIndex++) {
            // 计算当前字节对应的位移（每个字节占8位，低字节在前）
            int shift = byteIndex * Byte.SIZE;
            // 提取 id 中当前字节的8位数据（& 0xFF 确保只取低8位）
            byte currentByte = (byte) ((id >> shift) & 0xFF);
            // 存入字节数组对应位置
            bytes[byteIndex] = currentByte;
        }

        return bytes;
    }

    /**
     * 这个本身没有问题，但是大小写敏感，如果使用mysql 数据库来写入唯一索引的数据时会发生冲突的情况
     * 随机一个 int
     * 取id 的低56 位乱序生成一个新的 7 字节数据
     *
     * @param id 原始id 值
     * @return 乱序后的字节数组
     */
    protected byte[] shuffle56Bits(long id) {
        int indOffset = 0;
        // 每一位都存储为boolean 值
        // id 的有效二进制位数据放在前面的数组位置
        for (int i = 0; i < ID_BYTES * Byte.SIZE; i++) {
            BITS_56[indOffset + i] = (id & (1L << i)) != 0;
        }

        // 按规则重排
        byte[] shuffled = new byte[ID_BYTES];
        for (int i = 0; i < ID_BYTES; i++) {
            shuffled[i] = 0;
            for (int j = 0; j < Byte.SIZE; j++) {
                int ind = i * Byte.SIZE + j;    // 数组下标位置
                int originalIndex = SHUFFLE_RULE_56BIT[ind];
                if (BITS_56[originalIndex]) {
                    shuffled[i] |= (byte) (1 << j);
                }
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
     * 计算当前时间单位的最大可用ID
     */
    protected long maxId(long curMs) {
        long relativeMs = curMs - startMs;

        // 时间单位：1024ms（2^10），左移18位给序列号留出空间
        long msPart = (relativeMs >> 10) << 18;
        return msPart | (MAX_SEQUENCE - 1);
    }

    /**
     * 这是提供给定时调度任务调用的，每11 秒（包含10 个时间单位）调用一次
     * <p>
     * 每过一段时间（10 个 时间单位），对id 的时间部分做重置在10 个时间单位前。
     * 让id 不要总是只+1
     * 当前方法不做加锁处理，加锁在方法{@link #doResetRelativeMs(long)} 中处理
     * <p>
     * 为什么要定时调整，这是因为一个服务器运行时间会很长，在这段时间里面如果ID 总是自增，会不太好。
     * 尽量让其乱序一点，而保留自增是简化逻辑，使得一次批量获取id 时变得简单。
     * 同时使得连续的ID 不会太多。
     * 一般在10 个时间单位里面连续并没有太大问题，在实际的项目中ID 的生成并不会那么频繁。
     * 这样也比较符合实际情况。
     */
    public void resetRelativeMs() {
        long curMs = System.currentTimeMillis();
        long relativeMs = curMs - startMs;
        if (relativeMs <= 0) {
            return;
        }

        // 时间单位：1024ms（2^10），左移18位给序列号留出空间
        // 这里的 -10 达到往前推 10 个时间单位的效果
        long msPart = ((relativeMs >> 10) - 10) << 18;
        if (msPart <= 0) {
            return;
        }
        // 当10个时间单位前的时间点晚于lastId时，才执行重置
        if (msPart <= lastId) {
            return;
        }

        doResetRelativeMs(msPart);
    }

    protected synchronized void doResetRelativeMs(long msPart) {
        // 取时间部分，以及保留lastId 的容量部分
        lastId = msPart | (lastId & (MAX_SEQUENCE - 1));
    }


    //
    //
    //
    // /**
    //  * 从 Base64 编码的 UUID 反推原始 ID 和随机值
    //  * @param base64Uuid 由 IdGenerator 生成的 Base64 字符串（12 字符）
    //  * @return 结果对象，包含原始 ID 和随机值
    //  * @throws IllegalArgumentException 非法输入（长度/格式错误）
    //  */
    // @NonNull
    // public Result unShuffle72Bits(String base64Uuid) {
    //     // 1. 验证输入合法性
    //     if (base64Uuid == null || base64Uuid.length() != 12) {
    //         throw new IllegalArgumentException("非法的 Base64 UUID，必须是 12 个字符");
    //     }
    //
    //     // 2. Base64 解码 → 9 字节数组（核心：必须用和生成时一致的 Base64 编码器）
    //     byte[] shuffledBytes;
    //     try {
    //         shuffledBytes = Base64.getDecoder().decode(base64Uuid);
    //     } catch (IllegalArgumentException e) {
    //         throw new IllegalArgumentException("Base64 解码失败，非法格式", e);
    //     }
    //     if (shuffledBytes.length != UUID_BYTES) {
    //         throw new IllegalArgumentException("解码后必须是 9 字节");
    //     }
    //
    //     // 3. 9 字节数组 → 重排后的 72 位布尔数组（bits72[新位索引] = 对应位的值）
    //     boolean[] bits72 = new boolean[UUID_BYTES * Byte.SIZE];
    //     for (int byteIdx = 0; byteIdx < UUID_BYTES; byteIdx++) {
    //         byte b = shuffledBytes[byteIdx];
    //         for (int bitIdxInByte = 0; bitIdxInByte < Byte.SIZE; bitIdxInByte++) {
    //             int newBitIndex = byteIdx * Byte.SIZE + bitIdxInByte; // 新位的全局索引（0~71）
    //             // 提取该字节的第 bitIdxInByte 位（低位在前，和生成时一致）
    //             bits72[newBitIndex] = (b & (1 << bitIdxInByte)) != 0;
    //         }
    //     }
    //
    //     // 4. 按逆规则还原 → 原始 72 位布尔数组（originalBits[原始位索引] = 对应位的值）
    //     boolean[] originalBits = new boolean[UUID_BYTES * Byte.SIZE];
    //     for (int i = 0; i < UUID_BYTES; i++) {
    //         for (int j = 0; j < Byte.SIZE; j++) {
    //             int ind = i * Byte.SIZE + j;    // 数组下标位置
    //             int originalIndex = SHUFFLE_RULE_56BIT[ind];
    //             originalBits[originalIndex] = bits72[ind];
    //         }
    //     }
    //
    //     // 5. 拆分原始 72 位：前 56 位 → ID（7 字节），后 16 位 → 随机值（2 字节）
    //     long originalId = 0L;
    //     int randomValue = 0;
    //
    //     // 5.1 提取前 56 位 → 原始 ID（低位在前，和生成时的 (id & (1L << i)) 逻辑一致）
    //     int idBitCount = ID_BYTES * Byte.SIZE; // 56 位
    //     for (int i = 0; i < idBitCount; i++) {
    //         if (originalBits[i]) {
    //             originalId |= (1L << i); // 第 i 位为 true 则置位
    //         }
    //     }
    //
    //     // 5.2 提取后 16 位 → 随机值（低位在前，和生成时的 (randValue & (1 << i)) 逻辑一致）
    //     int randomBitCount = (UUID_BYTES - ID_BYTES) * Byte.SIZE; // 16 位
    //     int randomBitStartIndex = idBitCount; // 随机值从第 56 位开始
    //     for (int i = 0; i < randomBitCount; i++) {
    //         int originalBitIndex = randomBitStartIndex + i;
    //         if (originalBits[originalBitIndex]) {
    //             randomValue |= (1 << i); // 第 i 位为 true 则置位
    //         }
    //     }
    //
    //     return new Result(originalId, randomValue);
    // }
    //
    // /**
    //  * 反推结果封装
    //  */
    // @Data
    // public static class Result {
    //     private final long originalId; // 原始 ID
    //     private final int randomValue; // 随机值（仅低 16 位有效）
    //
    //     public Result(long originalId, int randomValue) {
    //         this.originalId = originalId;
    //         this.randomValue = randomValue & 0xFFFF; // 确保只保留低 16 位
    //     }
    // }
    //
    // public byte[] shuffle72Bits(long id, int randomValue) {
    //     int indOffset = 0;
    //     // 填充 ID 部分（56 位）
    //     for (int i = 0; i < ID_BYTES * Byte.SIZE; i++) {
    //         BITS_56[indOffset + i] = (id & (1L << i)) != 0;
    //     }
    //     // 填充随机值部分（16 位）
    //     indOffset = ID_BYTES * Byte.SIZE;
    //     for (int i = 0; i < (UUID_BYTES - ID_BYTES) * Byte.SIZE; i++) {
    //         BITS_56[indOffset + i] = (randomValue & (1 << i)) != 0;
    //     }
    //     // 按规则重排（同原方法）
    //     byte[] shuffled = new byte[UUID_BYTES];
    //     for (int i = 0; i < UUID_BYTES; i++) {
    //         for (int j = 0; j < Byte.SIZE; j++) {
    //             int ind = i * Byte.SIZE + j;
    //             int originalIndex = SHUFFLE_RULE_56BIT[ind];
    //             if (BITS_56[originalIndex]) {
    //                 shuffled[i] |= (byte) (1 << j);
    //             }
    //         }
    //     }
    //     return shuffled;
    // }
}

