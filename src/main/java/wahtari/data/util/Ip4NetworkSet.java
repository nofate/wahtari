package wahtari.data.util;

import java.util.BitSet;


public class Ip4NetworkSet {

    private static final int DEFAULT_CAPACITY = 32;
    private static final int ROOT = 0;
    private static final int NOT_SET = -1;
    private static final long HIGH_BIT = 1L << 31;

    private int[] ones;
    private int[] zeroes;
    private BitSet notches;
    private int size;
    private int capacity;

    public Ip4NetworkSet() {
        this(DEFAULT_CAPACITY);
    }

    public Ip4NetworkSet(int capacity) {
        this.capacity = capacity;
        size = 1;

        ones = new int[capacity];
        zeroes = new int[capacity];
        notches = new BitSet(capacity);
        ones[0] = NOT_SET;
        zeroes[0] = NOT_SET;
    }

    public void put(long addr, long netmask) {
        long bit = HIGH_BIT;
        int curPtr = ROOT;
        int nextPtr = ROOT;

        while ((netmask & bit) != 0) {
            if ((addr & bit) == 0) {
                nextPtr = zeroes[curPtr];
            } else {
                nextPtr = ones[curPtr];
            }
            if (nextPtr == NOT_SET) {
                break;
            }
            bit >>= 1;
            curPtr = nextPtr;
        }

        if (nextPtr != NOT_SET) {
            notches.set(curPtr);
            return;
        }

        while ((netmask & bit) != 0) {
            ensureCapacity();

            nextPtr = size;
            ones[nextPtr] = NOT_SET;
            zeroes[nextPtr] = NOT_SET;

            if ((addr & bit) != 0) {
                ones[curPtr] = nextPtr;
            } else {
                zeroes[curPtr] = nextPtr;
            }

            bit >>= 1;
            curPtr = nextPtr;
            size++;
        }

        notches.set(curPtr);
    }

    public void put(Cidr cidr) {
        int address = cidr.address();
        long netmask = ((1L << (32 - cidr.netmaskBits())) - 1L) ^ 0xffffffffL;
        put(address, netmask);
    }

    public boolean contains(long addr) {
        boolean result = false;

        int curPtr = ROOT;
        long bit = HIGH_BIT;

        while (curPtr != NOT_SET) {
            result = notches.get(curPtr);
            if ((addr & bit) == 0) {
                curPtr = zeroes[curPtr];
            } else {
                curPtr = ones[curPtr];
            }
            bit >>= 1;
        }

        return result;
    }

    private void ensureCapacity() {
        if (size < capacity) return;

        capacity = capacity * 2;
        zeroes = extend(zeroes, capacity);
        ones = extend(ones, capacity);
    }

    private static int[] extend(int[] array, int newSize) {
        int[] newArray = new int[newSize];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }
}

