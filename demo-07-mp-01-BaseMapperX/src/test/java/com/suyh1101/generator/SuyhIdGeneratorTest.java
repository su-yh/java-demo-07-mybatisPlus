package com.suyh1101.generator;


import com.suyh1101.generator.plus.SuyhIdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

public class SuyhIdGeneratorTest {
    @Test
    public void testZoneDateTime() {
        LocalDate localDate = LocalDate.of(2025, 1, 1);
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.of("UTC"));
        long epochMilli = zonedDateTime.toInstant().toEpochMilli();
        System.out.println("epochMilli: " + epochMilli);
        System.out.println("epochMilli: 0x" + Long.toHexString(epochMilli));

        long ts = epochMilli + 0x2a1909000L;
        System.out.println("ts: " + ts);
    }

    @Test
    public void testShift() {
        long val = Long.MIN_VALUE >>> 2;
        val = val >> 1;
        System.out.println("0x" + Long.toHexString(val));
    }

    @Test
    public void testLogicAnd() {
        LocalDate localDate = LocalDate.of(2025, 1, 1);
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.of("UTC"));
        long startMs = zonedDateTime.toInstant().toEpochMilli();

        long initMs = System.currentTimeMillis() - startMs;
        System.out.println("0x" + Long.toHexString(initMs));

        // 将低十位二进制重置为0
        long initId = initMs & ~0b1111111111;
        System.out.println("0x" + Long.toHexString(initId));
    }

    @Test
    public void test001() {
        // 下面几个的结果完全一样，都是10 个二进制位都为1 的数
        long a = ~((1L << 10) - 1);
        long b = -(1L << 10);
        long c = ~(1 << 10) + 1;
        long d = ~0b1111111111;
        System.out.println("0x" + Long.toHexString(a));
        System.out.println("0x" + Long.toHexString(b));
        System.out.println("0x" + Long.toHexString(c));
        System.out.println("0x" + Long.toHexString(d));
    }

    @Test
    public void testIds() {
        int n = SuyhIdGenerator.MAX_SEQUENCE;
        SuyhIdGenerator idGenerator = new SuyhIdGenerator();
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 10; i++) {
            stopWatch.start("i-" + i);
            long startId = idGenerator.nextIds(n);
            System.out.println("i: " + i + ", startId: 0x" + Long.toHexString(startId));
            stopWatch.stop();
        }
        System.out.println(stopWatch.prettyPrint());
    }

    @Test
    public void testUuids() {
        SuyhIdGenerator idGenerator = new SuyhIdGenerator();
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 100; i++) {
            stopWatch.start("i-" + i);
            String uuid = idGenerator.nextUuid();
//            System.out.println("i: " + i + ", uuid: " + uuid);
            stopWatch.stop();
        }
        System.out.println(stopWatch.prettyPrint());
    }

    protected String convertUuidUnordered(long id) {
        // 提取低48位（6个字节），避免高16位干扰
        id = id & 0xFFFF_FFFF_FFFFL;
        byte[] bytes = new byte[6];

        // 第一步：提取6个字节的原始值（正确提取每个字节）
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) (id >> (i * 8)); // 每个字节占8位，正确位移
        }

        // 第二步：提取每个字节的最低位（第0位）
        int[] lowBits = new int[6];
        for (int i = 0; i < 6; i++) {
            lowBits[i] = bytes[i] & 1; // 保留最低位（0或1）
        }

        // 第三步：对最低位进行乱序（示例：固定置换规则，可根据需求调整）
        // 这里的置换规则是 [0,1,2,3,4,5] → [5,3,1,4,2,0]（示例，可自定义）
        int[] shuffleRule = {5, 3, 1, 4, 2, 0}; // 乱序映射规则
        int[] shuffledBits = new int[6];
        for (int i = 0; i < 6; i++) {
            shuffledBits[i] = lowBits[shuffleRule[i]];
        }

        // 第四步：将乱序后的最低位重新赋值给每个字节（其他位保持不变）
        for (int i = 0; i < 6; i++) {
            // 清空原最低位，再设置为乱序后的位
            bytes[i] = (byte) ((bytes[i] & 0xFE) | shuffledBits[i]);
        }

        // 生成Base64编码
        return Base64.getEncoder().encodeToString(bytes);
    }
}