package com.suyh1101.generator.plusv2;


import com.suyh1101.generator.UuidModeEnums;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * @author suyh
 * @since 2025-09-20
 */
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
        SuyhIdGenerator idGenerator = new SuyhIdGenerator(UuidModeEnums.UNORDERED_PLUS);
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 100; i++) {
            stopWatch.start("i-" + i);
            String uuid = idGenerator.nextUuid();
            System.out.println("i: " + i + ", uuid: " + uuid);
            stopWatch.stop();
        }
        System.out.println(stopWatch.prettyPrint());
    }

    @Test
    public void testUniqueUuids() {
        SuyhIdGenerator idGenerator = new SuyhIdGenerator(UuidModeEnums.UNORDERED_PLUS);
        int n = SuyhIdGenerator.MAX_SEQUENCE;

        Set<String> idSet = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            System.out.println("i: " + i);
            String[] ids = idGenerator.nextUuids(n);
            for (String uuid : ids) {
                // System.out.println("uuid: " + uuid);
                boolean res = idSet.add(uuid);
                Assertions.assertTrue(res);
            }
        }
    }
}