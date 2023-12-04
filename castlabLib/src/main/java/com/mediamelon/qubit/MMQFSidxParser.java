package com.mediamelon.qubit;

/**
 * Created by Rupesh on 23-07-2015.
 */
//Taken from exoplayer

import com.google.android.exoplayer.mminclusion.util.ParsableByteArray;

/**
 * Defines segments within a media stream.
 */
public final class MMQFSidxParser {
    class MMQFSegmentIndex{
        public final int sizeBytes;
        public final int length;
        public final int[] sizes;
        public final long[] offsets;
        public final long[] durationsUs;
        public final long[] timesUs;
        public MMQFSegmentIndex(int sizeBytes, int[] sizes, long[] offsets, long[] durationsUs,
                              long[] timesUs) {
            this.sizeBytes = sizeBytes;
            this.length = sizes.length;
            this.sizes = sizes;
            this.offsets = offsets;
            this.durationsUs = durationsUs;
            this.timesUs = timesUs;
        }
    }
    private static int getAtomTypeInteger(String typeName) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result <<= 8;
            result |= typeName.charAt(i);
        }
        return result;
    }

    MMQFSegmentIndex parse(byte[] bytestream){
        MMQFSegmentIndex retval = null;
        //get the first 8 bytes
        final int KAtomSzToParse = 8;
        if(bytestream.length>=KAtomSzToParse){
            ParsableByteArray byteArr = new ParsableByteArray(bytestream, bytestream.length);
            int atomSz = byteArr.readInt();
            int atomType = byteArr.readInt();
            if(atomType == getAtomTypeInteger("sidx")){
                int fullAtom = byteArr.readInt();
                int version = 0x000000FF & (fullAtom >> 24);
                //skip 4 bytes
                byteArr.skipBytes(4);

                long timescale = byteArr.readUnsignedInt();
                long earliestPresentationTime;
                long firstOffset = startOffset;
                if (version == 0) {
                    earliestPresentationTime = byteArr.readUnsignedInt();
                    firstOffset += byteArr.readUnsignedInt();
                } else {
                    earliestPresentationTime = byteArr.readUnsignedLongToLong();
                    firstOffset += byteArr.readUnsignedLongToLong();
                }

                byteArr.skipBytes(2);

                int referenceCount = byteArr.readUnsignedShort();
                int[] sizes = new int[referenceCount];
                long[] offsets = new long[referenceCount];
                long[] durationsUs = new long[referenceCount];
                long[] timesUs = new long[referenceCount];

                long offset = firstOffset;
                long time = earliestPresentationTime;
                long timeUs = scaleLargeTimestamp(time, 1000000L, timescale);
                for (int i = 0; i < referenceCount; i++) {
                    int firstInt = byteArr.readInt();

                    int type = 0x80000000 & firstInt;
                    if (type != 0) {
                        assert(false);
                    }
                    long referenceDuration = byteArr.readUnsignedInt();

                    sizes[i] = 0x7fffffff & firstInt;
                    offsets[i] = offset;

                    // Calculate time and duration values such that any rounding errors are consistent. i.e. That
                    // timesUs[i] + durationsUs[i] == timesUs[i + 1].
                    timesUs[i] = timeUs;
                    time += referenceDuration;
                    timeUs = scaleLargeTimestamp(time, 1000000, timescale);
                    durationsUs[i] = timeUs - timesUs[i];

                    byteArr.skipBytes(4);
                    offset += sizes[i];
                }

                return new MMQFSegmentIndex(-1, sizes, offsets, durationsUs, timesUs);
            }
        }
        return retval;
    }

    public static long scaleLargeTimestamp(long timestamp, long multiplier, long divisor) {
        if (divisor >= multiplier && (divisor % multiplier) == 0) {
            long divisionFactor = divisor / multiplier;
            return timestamp / divisionFactor;
        } else if (divisor < multiplier && (multiplier % divisor) == 0) {
            long multiplicationFactor = multiplier / divisor;
            return timestamp * multiplicationFactor;
        } else {
            double multiplicationFactor = (double) multiplier / divisor;
            return (long) (timestamp * multiplicationFactor);
        }
    }
    public long startOffset = 0;
}
