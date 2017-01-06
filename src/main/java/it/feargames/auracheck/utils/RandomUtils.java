package it.feargames.auracheck.utils;

import org.apache.commons.lang.RandomStringUtils;

import java.util.Random;

/**
 * Random utilities.
 */
public class RandomUtils {
    public final static Random RANDOM = new Random();

    // Utility class
    private RandomUtils(){
    }

    /**
     * Get a random nickname
     *
     * @return a random nickname
     */
    public static String randomNickname() {
        int size = 3 + RANDOM.nextInt(6);
        return RandomStringUtils.randomAlphabetic(size);
    }
}
