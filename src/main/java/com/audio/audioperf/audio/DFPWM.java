package com.audio.audioperf.audio;

/**
 * Standard DFPWM1a codec, matching music.madefor.cc and OpenComputers.
 * This is the public DFPWM1a variant defined by Ben "GreaseMonkey" Russell:
 * PREC = 10 charge precision, +-1 response steps with a minimum response
 * strength of 8, and LSB-first bit packing.
 */
public class DFPWM {
    private static final int PREC = 10;
    private static final int ROUND = 1 << (PREC - 1);
    private static final int RESP_MAX = 1023;
    private static final int RESP_MIN = 2 << (PREC - 8);

    private int charge;
    private int strength;
    private boolean lastbit;

    public DFPWM() {
        this.charge = 0;
        this.strength = 0;
        this.lastbit = false;
    }

    private void ctxUpdate(boolean bit) {
        int target = bit ? 127 : -128;
        int ncharge = charge + ((strength * (target - charge) + ROUND) >> PREC);
        if (ncharge == charge && ncharge != target) {
            ncharge += bit ? 1 : -1;
        }

        int nstrength = strength;
        if (bit == lastbit) {
            if (nstrength != RESP_MAX) nstrength++;
        } else {
            if (nstrength != 0) nstrength--;
        }
        if (nstrength < RESP_MIN) nstrength = RESP_MIN;

        charge = ncharge;
        strength = nstrength;
        lastbit = bit;
    }

    /**
     * Decompresses DFPWM1a bytes into signed 8-bit PCM samples.
     *
     * @param output the output PCM buffer (signed bytes).
     * @param input  the input DFPWM bytes.
     * @param outOff the offset in the output buffer.
     * @param inOff  the offset in the input buffer.
     * @param count  the number of DFPWM bytes to decode.
     */
    public void decompress(byte[] output, byte[] input, int outOff, int inOff, int count) {
        int outIdx = outOff;
        for (int i = 0; i < count && inOff < input.length; i++) {
            int d = input[inOff++] & 0xFF;
            for (int j = 0; j < 8; j++) {
                boolean bit = ((d >> j) & 1) != 0;
                ctxUpdate(bit);
                output[outIdx++] = (byte) charge;
            }
        }
    }

    /**
     * Compresses signed 8-bit PCM samples into DFPWM1a bytes.
     *
     * @param output the output DFPWM buffer.
     * @param input  the input PCM buffer (signed bytes).
     * @param outOff the offset in the output buffer.
     * @param inOff  the offset in the input buffer.
     * @param count  the number of DFPWM bytes to produce (count*8 input samples).
     */
    public void compress(byte[] output, byte[] input, int outOff, int inOff, int count) {
        int outIdx = outOff;
        int inIdx = inOff;
        for (int i = 0; i < count; i++) {
            int d = 0;
            for (int j = 0; j < 8; j++) {
                if (inIdx >= input.length) {
                    return;
                }
                int s = input[inIdx++];
                boolean bit;
                if (s > charge) {
                    bit = true;
                } else if (s == charge) {
                    bit = charge == 127;
                } else {
                    bit = false;
                }
                d = (d >> 1) | (bit ? 0x80 : 0);
                ctxUpdate(bit);
            }
            output[outIdx++] = (byte) d;
        }
    }
}
