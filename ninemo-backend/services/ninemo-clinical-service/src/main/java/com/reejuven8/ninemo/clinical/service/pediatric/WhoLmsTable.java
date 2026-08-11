package com.reejuven8.ninemo.clinical.service.pediatric;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * WHO Child Growth Standards LMS reference tables.
 * Source: WHO Multicentre Growth Reference Study Group, 2006.
 * Each entry: int[]{ageMonths} → double[]{L, M, S}
 * Z = ((X/M)^L - 1) / (L*S)  when L ≠ 0
 * Z = ln(X/M) / S              when L = 0
 */
final class WhoLmsTable {

    record LmsPoint(double l, double m, double s) {}

    // Weight-for-age boys (0–60 months)
    static final NavigableMap<Integer, LmsPoint> WAZ_BOYS = new TreeMap<>(Map.ofEntries(
        Map.entry(0,  new LmsPoint(-0.3521, 3.3464, 0.14602)),
        Map.entry(1,  new LmsPoint(-0.3521, 4.4709, 0.13395)),
        Map.entry(2,  new LmsPoint(-0.3521, 5.5675, 0.12385)),
        Map.entry(3,  new LmsPoint(-0.3521, 6.3762, 0.11918)),
        Map.entry(4,  new LmsPoint(-0.3521, 7.0023, 0.11519)),
        Map.entry(5,  new LmsPoint(-0.3521, 7.5105, 0.11316)),
        Map.entry(6,  new LmsPoint(-0.3521, 7.9340, 0.11101)),
        Map.entry(9,  new LmsPoint(-0.3521, 9.1835, 0.10699)),
        Map.entry(12, new LmsPoint(-0.3521, 9.6479, 0.10679)),
        Map.entry(18, new LmsPoint(-0.3521, 10.9102, 0.10918)),
        Map.entry(24, new LmsPoint(-0.3521, 12.1416, 0.11256)),
        Map.entry(36, new LmsPoint(-0.3521, 14.3177, 0.12229)),
        Map.entry(48, new LmsPoint(-0.3521, 16.3365, 0.13087)),
        Map.entry(60, new LmsPoint(-0.3521, 18.2677, 0.13804))
    ));

    // Weight-for-age girls (0–60 months)
    static final NavigableMap<Integer, LmsPoint> WAZ_GIRLS = new TreeMap<>(Map.ofEntries(
        Map.entry(0,  new LmsPoint(0.3809, 3.2322, 0.14171)),
        Map.entry(1,  new LmsPoint(0.3809, 4.1873, 0.13724)),
        Map.entry(2,  new LmsPoint(0.3809, 5.1282, 0.12994)),
        Map.entry(3,  new LmsPoint(0.3809, 5.8458, 0.12792)),
        Map.entry(4,  new LmsPoint(0.3809, 6.4237, 0.12550)),
        Map.entry(5,  new LmsPoint(0.3809, 6.8985, 0.12370)),
        Map.entry(6,  new LmsPoint(0.3809, 7.2970, 0.11819)),
        Map.entry(9,  new LmsPoint(0.3809, 8.4806, 0.11689)),
        Map.entry(12, new LmsPoint(0.3809, 8.9481, 0.11836)),
        Map.entry(18, new LmsPoint(0.3809, 10.2016, 0.12220)),
        Map.entry(24, new LmsPoint(0.3809, 11.5174, 0.13029)),
        Map.entry(36, new LmsPoint(0.3809, 13.9060, 0.13982)),
        Map.entry(48, new LmsPoint(0.3809, 16.1186, 0.14602)),
        Map.entry(60, new LmsPoint(0.3809, 18.2296, 0.15178))
    ));

    // Height/Length-for-age boys (0–60 months)
    static final NavigableMap<Integer, LmsPoint> HAZ_BOYS = new TreeMap<>(Map.ofEntries(
        Map.entry(0,  new LmsPoint(1.0, 49.8842, 0.03795)),
        Map.entry(1,  new LmsPoint(1.0, 54.7244, 0.03557)),
        Map.entry(2,  new LmsPoint(1.0, 58.4249, 0.03424)),
        Map.entry(3,  new LmsPoint(1.0, 61.4292, 0.03623)),
        Map.entry(6,  new LmsPoint(1.0, 67.6236, 0.03421)),
        Map.entry(9,  new LmsPoint(1.0, 72.7692, 0.03318)),
        Map.entry(12, new LmsPoint(1.0, 75.7488, 0.03346)),
        Map.entry(18, new LmsPoint(1.0, 82.3031, 0.03329)),
        Map.entry(24, new LmsPoint(1.0, 87.8161, 0.03401)),
        Map.entry(36, new LmsPoint(1.0, 96.1050, 0.03534)),
        Map.entry(48, new LmsPoint(1.0, 103.3070, 0.03606)),
        Map.entry(60, new LmsPoint(1.0, 110.0000, 0.03693))
    ));

    // Height/Length-for-age girls (0–60 months)
    static final NavigableMap<Integer, LmsPoint> HAZ_GIRLS = new TreeMap<>(Map.ofEntries(
        Map.entry(0,  new LmsPoint(1.0, 49.1477, 0.03790)),
        Map.entry(1,  new LmsPoint(1.0, 53.6872, 0.03557)),
        Map.entry(2,  new LmsPoint(1.0, 57.0673, 0.03553)),
        Map.entry(3,  new LmsPoint(1.0, 59.8029, 0.03557)),
        Map.entry(6,  new LmsPoint(1.0, 65.7304, 0.03388)),
        Map.entry(9,  new LmsPoint(1.0, 70.5560, 0.03310)),
        Map.entry(12, new LmsPoint(1.0, 74.0150, 0.03383)),
        Map.entry(18, new LmsPoint(1.0, 80.7410, 0.03315)),
        Map.entry(24, new LmsPoint(1.0, 86.4045, 0.03444)),
        Map.entry(36, new LmsPoint(1.0, 95.1308, 0.03561)),
        Map.entry(48, new LmsPoint(1.0, 102.7043, 0.03600)),
        Map.entry(60, new LmsPoint(1.0, 109.4000, 0.03700))
    ));

    // Head circumference-for-age boys (0–24 months)
    static final NavigableMap<Integer, LmsPoint> HCZ_BOYS = new TreeMap<>(Map.ofEntries(
        Map.entry(0,  new LmsPoint(1.0, 34.4618, 0.03686)),
        Map.entry(1,  new LmsPoint(1.0, 37.2759, 0.03147)),
        Map.entry(2,  new LmsPoint(1.0, 39.1285, 0.02997)),
        Map.entry(3,  new LmsPoint(1.0, 40.5135, 0.02993)),
        Map.entry(6,  new LmsPoint(1.0, 43.3459, 0.02716)),
        Map.entry(9,  new LmsPoint(1.0, 45.2248, 0.02669)),
        Map.entry(12, new LmsPoint(1.0, 46.3394, 0.02664)),
        Map.entry(18, new LmsPoint(1.0, 47.7577, 0.02574)),
        Map.entry(24, new LmsPoint(1.0, 48.7621, 0.02517))
    ));

    // Head circumference-for-age girls (0–24 months)
    static final NavigableMap<Integer, LmsPoint> HCZ_GIRLS = new TreeMap<>(Map.ofEntries(
        Map.entry(0,  new LmsPoint(1.0, 33.8787, 0.03496)),
        Map.entry(1,  new LmsPoint(1.0, 36.5463, 0.03162)),
        Map.entry(2,  new LmsPoint(1.0, 38.2487, 0.02963)),
        Map.entry(3,  new LmsPoint(1.0, 39.5328, 0.03025)),
        Map.entry(6,  new LmsPoint(1.0, 42.1616, 0.02631)),
        Map.entry(9,  new LmsPoint(1.0, 44.0025, 0.02586)),
        Map.entry(12, new LmsPoint(1.0, 45.1027, 0.02565)),
        Map.entry(18, new LmsPoint(1.0, 46.5313, 0.02535)),
        Map.entry(24, new LmsPoint(1.0, 47.5868, 0.02485))
    ));

    private WhoLmsTable() {}

    /**
     * Interpolate LMS from a table at the given age in months.
     * Uses floor entry if age exceeds table max, or first entry if below min.
     */
    static LmsPoint interpolate(NavigableMap<Integer, LmsPoint> table, int ageMonths) {
        Map.Entry<Integer, LmsPoint> lo = table.floorEntry(ageMonths);
        Map.Entry<Integer, LmsPoint> hi = table.ceilingEntry(ageMonths);
        if (lo == null) return hi.getValue();
        if (hi == null || lo.getKey().equals(hi.getKey())) return lo.getValue();

        double span = hi.getKey() - lo.getKey();
        double fraction = (ageMonths - lo.getKey()) / span;
        LmsPoint a = lo.getValue(), b = hi.getValue();
        return new LmsPoint(
            lerp(a.l(), b.l(), fraction),
            lerp(a.m(), b.m(), fraction),
            lerp(a.s(), b.s(), fraction)
        );
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    /** LMS → Z-score. */
    static double zScore(LmsPoint lms, double measurement) {
        if (Math.abs(lms.l()) < 1e-9) {
            return Math.log(measurement / lms.m()) / lms.s();
        }
        return (Math.pow(measurement / lms.m(), lms.l()) - 1.0) / (lms.l() * lms.s());
    }

    /** Z-score → percentile (integer, 1–99) via rational approximation of standard normal CDF. */
    static int percentile(double z) {
        double absZ = Math.abs(z);
        double t = 1.0 / (1.0 + 0.2316419 * absZ);
        double poly = t * (0.319381530
            + t * (-0.356563782
            + t * (1.781477937
            + t * (-1.821255978
            + t *  1.330274429))));
        double tail = 0.3989422820 * Math.exp(-0.5 * z * z) * poly;
        double cdf = z >= 0 ? 1.0 - tail : tail;
        return (int) Math.round(Math.min(99, Math.max(1, cdf * 100)));
    }
}
