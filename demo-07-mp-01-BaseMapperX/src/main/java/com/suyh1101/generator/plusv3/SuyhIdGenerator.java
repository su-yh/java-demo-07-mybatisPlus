// package com.suyh1101.generator.plusv3;
//
// import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
// import org.springframework.lang.NonNull;
//
// import java.time.LocalDate;
// import java.time.ZoneId;
// import java.time.ZonedDateTime;
// import java.util.Base64;
// import java.util.Random;
// import java.util.concurrent.TimeUnit;
//
// /**
//  * 这里使用7 个字节来存储ID 值
//  * 其中18 位用来存储增量值，剩下的38 位用来存储时间，在这些位里面，每一位表示1024 ms ，所以最大可使用约：8,925 年
//  * 生成uuid 时，会随便生成一个数字，然后取低两字节与这7 个字节的数据组合，进行二进制位重新洗牌，生成一个新的9 字节数组，
//  * 最后使用这个9 字节数据使用base64 编码生成一个12 个字节的uuid
//  */
// public class SuyhIdGenerator implements IdentifierGenerator {
//     public static final Random RANDOM = new Random();
//     // 一个时间单位内允许生成的ID数量（18位二进制）
//     public static final int MAX_SEQUENCE = 1 << 18;
//
//     // 最后一次生成的ID
//     protected long lastId;
//
//     // 起始时间戳（UTC 2025-01-01）
//     protected final long startMs;
//
//     // id 的有效字节为7 个字节
//     private static final int ID_BYTES = 7;
//     // uuid 占用9 个字节
//     private static final int UUID_BYTES = 9;
//     // 复用的位存储数组（仅在同步方法内使用）
//     // uuid 的二进制位数据存储
//     private static final boolean[] BITS_72 = new boolean[UUID_BYTES * Byte.SIZE];
//
//     // 固定乱序映射规则（0~71表示原始位索引，值表示新位置）
//     // 注意：需确保包含0~71每个数字恰好一次
//     private static final int[] SHUFFLE_RULE_72bit = {
//             35, 68, 26, 45, 12, 55,
//             18, 29, 40, 64, 24, 39,
//             15, 50, 62, 36, 57, 10,
//             49, 2, 31, 60, 44, 21,
//             5, 14, 47, 65, 34, 23,
//             70, 41, 19, 69, 59, 20,
//             3, 63, 51, 33, 7, 25,
//             42, 1, 53, 37, 0, 66,
//             11, 54, 30, 48, 6, 27,
//             58, 4, 16, 32, 67, 13,
//             28, 43, 9, 71, 56, 38,
//             8, 52, 22, 61, 17, 46
//     };
//
//     public SuyhIdGenerator() {
//         // 这个值是可以修改的，但是一个工程应该只在首次使用的时候指定，后面就只能固定该值了。
//         LocalDate localDate = LocalDate.of(2025, 1, 1);
//         ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.of("UTC"));
//         this.startMs = zonedDateTime.toInstant().toEpochMilli();
//
//         long relativeMs = System.currentTimeMillis() - startMs;
//
//         // 当前时间戳相对于 20250101(UTC 时区时间)
//         // (initMs >> 10 + 1): 将最低10 位清0，并+ 1，使得初始值为当前时间未来的 1024 毫秒时间里面的，而不是已经过去的时间
//         // (<< 10): 把最低10 位补0  用来存储基础的ID 增量值
//         // (<< 8):  除了一个时间单位的ID 增量值外，再给8 个二进制位来存储额外的ID 增量值
//         // 何为增量值：就是在一个单位时间内（这里的单位时间是1024 毫秒），允许生成多少个ID 值，如果超过了自然是不允许的，只有等下一个单位时间才可以。
//         // 当前时间以一个固定的时间偏移量，毫秒
//         // 初始化lastId到当前时间的下一个时间单位（1024ms）
//         this.lastId = ((relativeMs >> 10) + 1) << (10 + 8);
//
//         // 验证乱序规则的有效性
//         validateShuffleRule();
//     }
//
//     // 验证乱序规则是否包含0~47所有数字，每个数字必须出现并且只出现一次
//     private void validateShuffleRule() {
//         validateShuffleRule(SHUFFLE_RULE_72bit, SHUFFLE_RULE_72bit.length);
//     }
//
//     private void validateShuffleRule(int[] shuffleRule, int size) {
//         boolean[] seen = new boolean[size];
//         for (int index : shuffleRule) {
//             if (index < 0 || index >= size || seen[index]) {
//                 throw new IllegalArgumentException("Invalid SHUFFLE_RULE: 包含重复或越界的索引, size: " + size);
//             }
//             seen[index] = true;
//         }
//     }
//
//     @Override
//     public synchronized Number nextId(Object entity) {
//         return nextId();
//     }
//
//     @NonNull
//     public synchronized String nextUuid() {
//         String[] uuids = nextUuids(1);
//         return uuids[0];
//     }
//
//     @NonNull
//     public synchronized String[] nextUuids(int n) {
//         long startId = nextIds(n);
//
//         String[] uuids = new String[n];
//
//         for (int i = 0; i < n; i++) {
//             long id = startId + i;
//             byte[] ubs = shuffle72Bits(id);
//             uuids[i] = Base64.getEncoder().encodeToString(ubs);
//         }
//
//         return uuids;
//     }
//
//     public synchronized long nextId() {
//         return nextIds(1);
//     }
//
//     public synchronized long nextIds(int n) {
//         if (n <= 0 || n > MAX_SEQUENCE) {
//             throw new IllegalArgumentException("Invalid number of IDs requested: " + n);
//         }
//
//         while (true) {
//             Long id = allocateIds(n);
//             if (id != null) {
//                 return id;
//             }
//
//             // 等待下一个时间单位
//             try {
//                 for (int i = 0; i < 10; i++) {
//                     TimeUnit.MICROSECONDS.sleep(1L);
//                 }
//             } catch (InterruptedException ignored) {
//             }
//         }
//     }
//
//     /**
//      * 随机一个 int
//      * 取id 的低48 位 和 随机值的 低24 位 乱序生成一个新的 9 字节数据
//      * @param id 原始id 值
//      * @return 乱序后的字节数组
//      */
//     protected byte[] shuffle72Bits(long id) {
//         int indOffset = 0;
//         // 每一位都存储为boolean 值
//         // id 的有效二进制位数据放在前面的数组位置
//         for (int i = 0; i < ID_BYTES * Byte.SIZE; i++) {
//             BITS_72[indOffset + i] = (id & (1L << i)) != 0;
//         }
//
//         // 随机值的低位，用来补充剩下的空位以达到 UUID_BYTES 个字节的数据
//         int randValue = RANDOM.nextInt();
//         indOffset = ID_BYTES * Byte.SIZE;
//         for (int i = 0; i < (UUID_BYTES - ID_BYTES) * Byte.SIZE; i++) {
//             BITS_72[indOffset + i] = (randValue & (1 << i)) != 0;
//         }
//
//         // 按规则重排
//         byte[] shuffled = new byte[UUID_BYTES];
//         for (int i = 0; i < UUID_BYTES; i++) {
//             for (int j = 0; j < Byte.SIZE; j++) {
//                 int ind = i * Byte.SIZE + j;    // 数组下标位置
//                 int originalIndex = SHUFFLE_RULE_72bit[ind];
//                 if (BITS_72[originalIndex]) {
//                     shuffled[i] |= (byte) (1 << j);
//                 }
//             }
//         }
//
//         return shuffled;
//     }
//
//     /**
//      * 分配一批ID
//      * @param n 所需ID数量
//      * @return 起始ID，若当前时间单位不足则返回null
//      */
//     protected Long allocateIds(int n) {
//         long curMs = System.currentTimeMillis();
//         long maxId = maxId(curMs);
//         long expectMaxId = lastId + n;
//
//         if (expectMaxId > maxId) {
//             // 需要等到下一个时间单位才允许再次生成新的ID 值。
//             return null;
//         }
//
//         long startId = lastId + 1;
//         lastId = lastId + n;
//         return startId;
//     }
//
//     /**
//      * 计算当前时间单位的最大可用ID
//      */
//     protected long maxId(long curMs) {
//         long relativeMs = curMs - startMs;
//
//         // 时间单位：1024ms（2^10），左移18位给序列号留出空间
//         long msPart = (relativeMs >> 10) << 18;
//         return msPart | (MAX_SEQUENCE - 1);
//     }
//
//     @Override
//     public String nextUUID(Object entity) {
//         return nextUuid();
//     }
// }
