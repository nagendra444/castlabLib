package com.mediamelon.qubit;
//import com.google.android.exoplayer.chunk.FormatEvaluator;

import android.util.Log;

import com.mediamelon.qubit.ep.RegisterResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
/**
 * Created by Rupesh on 19-03-2015.
 */
public class MMQFQubitModel implements MMQFQubitMetadataFileParser.OnQubitMetadataFileParsedListener{
    class CBRMetadata{
        MMQFQubitMetadataFileParser.VideoTrackMediaAttributes mediaAttributes;
        int getiMOSWeightedAverage()
        {
            double cummulativeWeights = 0;
            double KWeightConst = 0.35;
            double cummulativeWtQualityScore = 0;
            for(int i = 0; i< commonMetadata.noOfSegments; i++)
            {
                Double boxedInt = (Double)mediaAttributes.imosValues.get(i);
                int segmentQuality = (int)boxedInt.intValue();
                double weight = Math.pow(2, -((segmentQuality/10)/KWeightConst));
                cummulativeWeights += weight;
                cummulativeWtQualityScore += ((segmentQuality/10)*weight);
            }
            double retval = ((cummulativeWtQualityScore * 10)/cummulativeWeights);
            return (int)retval;
        }

        double getiMOSAverage()
        {
            double retval = 0;
            for(int i = 0; i< commonMetadata.noOfSegments; i++)
            {
                Double boxedInt = (Double)mediaAttributes.imosValues.get(i);
                retval += boxedInt.doubleValue();
            }
            retval = retval/commonMetadata.noOfSegments;
            return retval;
        }
    }

    class QubitRunningStatistics{
        QubitRunningStatistics()
        {
            imosImprovements = new ArrayList<iMOSImprovementPoint>();
            imosSquared = new ArrayList<Double>();
            imosSquaredCBR = new ArrayList<Double>();
            targetiMOSVect = new ArrayList<Double>();
            totalSegments = 0;
        }

        double getiMOSVarianceCQ()
        {
            int imosSqSum = 0;
            for(int i =0; i<imosSquared.size(); i++)
            {
                imosSqSum += imosSquared.get(i).doubleValue();
                MMLogger.e("QubitModelVarianceFinal", "Elem CQ - " + imosSquared.get(i).doubleValue());
            }
            double retval = (double)imosSqSum/(100 *imosSquared.size());
            MMLogger.e("QubitModelVarianceFinal", "CQ - " + retval);
            return retval;
        }

        double getiMOSStdDeviationCQ()
        {
            double variance = getiMOSVarianceCQ();
            return Math.sqrt(variance);
        }

        public double getiMOSVarianceCBR()
        {
            int imosSqSum = 0;
            for(int i =0; i<imosSquaredCBR.size(); i++)
            {
                imosSqSum += imosSquaredCBR.get(i).doubleValue();
                MMLogger.e("QubitModelVarianceFinal", "Elem CQ - " + imosSquaredCBR.get(i).doubleValue());
            }
            double retval = (double)imosSqSum/(100 * imosSquaredCBR.size());
            MMLogger.e("QubitModelVarianceFinal", "CBR - " + retval);
            return retval;
        }

        public double getiMOSStdDeviationCBR()
        {
            double variance = getiMOSVarianceCBR();
            return Math.sqrt(variance);
        }

        int percentageMaxiMOSImprovement = 0;
        int percentageMiniMOSImprovement = 0;
        iMOSImprovementPoint maxiMOSImpPoint;
        long totalBitsCBR = 0;
        long totalBitsCQ = 0;
        long durationOfPlayback = 0;
        long totaliMOSImprovements = 0;
        double minImos = 0;
        double minImosImprovement = 0;
        ArrayList<iMOSImprovementPoint> imosImprovements;
        ArrayList<Double> imosSquared;
        ArrayList<Double> imosSquaredCBR;
        ArrayList<Double> targetiMOSVect;
        int totalSegments = 0;
    }

    class QubitStatistics{
        QubitStatistics(){
            bitsSaved = new ArrayList<Integer>(commonMetadata.noOfSegments);
            bitsSavedInPerc = new ArrayList<Integer>(commonMetadata.noOfSegments);
            iMOSImpovements = new ArrayList<Double>(commonMetadata.noOfSegments);
            iMOSDiffSq = new ArrayList<Double>(commonMetadata.noOfSegments);
            iMOSDiffSqCBR = new ArrayList<Double>(commonMetadata.noOfSegments);
        }

        void updateStatisticsForSegment(MMQFQubitVideoTrack qubitTrackToUpdateStats, MMQFQubitVideoTrack qubitTrackSelectedForTranslation, int segmentIndex)
        {
            MMQFPresentationVideoTrackSegmentInfo cbrSegInfo = qubitTrackToUpdateStats.videoTrackInfo.getSegmentInfo(segmentIndex);
            MMQFPresentationVideoTrackSegmentInfo cqSegInfo = qubitTrackSelectedForTranslation.videoTrackInfo.getSegmentInfo(segmentIndex);

            if(cbrSegInfo == null || cqSegInfo == null){
                return;
            }

            int totalbitsSaved = (cbrSegInfo.segmentSz - cqSegInfo.segmentSz);
            bitsSaved.add(totalbitsSaved);
            //MMLogger.i("QBRStats", "total Bits Saved - " + totalbitsSaved);

            int bitsSavedInPerc = (totalbitsSaved * 100)/(cbrSegInfo.segmentSz);
            int bitrate = (int)(cqSegInfo.segmentSz/((double)cqSegInfo.duration/qubitTrackSelectedForTranslation.videoTrackInfo.timeScale));
            processAggregatedStatistics_bitrate(bitrate);

            double cqImos = ((Double)(qubitTrackSelectedForTranslation.cbrMetadata.mediaAttributes.imosValues.get(segmentIndex))).doubleValue();
            double actualImos = ((Double)(qubitTrackToUpdateStats.cbrMetadata.mediaAttributes.imosValues.get(segmentIndex))).doubleValue();
            double imosImprovementVal = (double)((cqImos - actualImos) * 100)/actualImos;
            //MMLogger.e("QubitModelVarianceCheck", " For track " + qubitTrackToUpdateStats.videoTrackInfo.bitrate + " SegmentId - " + segmentIndex + "imosVariation" + actualImos + " -> " + cqImos + "  imosImp Val " + imosImprovementVal);

            if(actualImos < cqImos)
            {
                //We improved the quality of segment and took it to near mean.
                iMOSImpovements.add(imosImprovementVal);
            }
            else
            {
                iMOSImpovements.add(0.0);
            }
            //MMLogger.e("QubitModelVariance","cbrSegInfo videotrack bitrate " + qubitTrackToUpdateStats.videoTrackInfo.bitrate + " imos - " + cqImos + " targetimos " + qubitTrackToUpdateStats.cqMetadata.targetiMOS);
            processAggregatedStatistics_imos(actualImos, cqImos, qubitTrackToUpdateStats.cqMetadata.targetiMOS);
        }

        void processAggregatedStatistics_bitrate(int bitrate)
        {
            if(bitrate > peakBitrate)
            {
                peakBitrate = bitrate;
            }

            if(bitrate < minBitrate)
            {
                minBitrate = bitrate;
            }
        }

        void processAggregatedStatistics_imos(double imosCBR, double imos, double targetiMOS)
        {
            if(imos > maxiMOS)
            {
                maxiMOS = imos;
            }

            if(imos < miniMOS)
            {
                miniMOS = imos;
            }

            double imosDiff = (imos - targetiMOS);
            iMOSDiffSq.add(imosDiff * imosDiff);
            double imosDiffCBR = imosCBR - targetiMOS;
            iMOSDiffSqCBR.add(imosDiffCBR * imosDiffCBR);
        }

        void concludeAggregatedStatistics()
        {
            int totaliMOSDiff = 0;
            for(int i=0; i<iMOSDiffSq.size(); i++)
            {
                double imosDiffSq = iMOSDiffSq.get(i).doubleValue();
                if(imosDiffSq != 0) {
                    totaliMOSImprovements++;
                    totaliMOSDiff += imosDiffSq;
                }
            }

            iMOSVariance = totaliMOSDiff/100 * (iMOSDiffSq.size() - 1);
            iMOSStdDeviation = Math.sqrt(iMOSVariance);
        }

        //aggregated statistics
        double maxiMOS = 0;
        double miniMOS = 0;

        double peakBitrate = 0;
        double minBitrate = 0;

        double iMOSVariance = 0;
        double iMOSStdDeviation = 0;
        double totaliMOSImprovements = 0;

        //Cumulative statistics
        ArrayList<Integer> bitsSaved = null;
        ArrayList<Integer> bitsSavedInPerc = null;
        ArrayList<Double> iMOSImpovements = null;
        ArrayList<Double> iMOSDiffSq = null;
        ArrayList<Double> iMOSDiffSqCBR = null;
    }

    class CQMetadata{
        class acpEntry{
            public int seqNum = 0;
            public double slope = 0;
        }

        class BoundedACPEntry{
            public long seqNum = 0;
            public double acpValue = 0;
        }

        class BoundedCBCEntry{
            public long seqNum = 0;
            public long cumulativeBits = 0;
        }

        CQMetadata()
        {
            cbcValues = new ArrayList<Long>(commonMetadata.noOfSegments);
            imosValues = new ArrayList<Double>(commonMetadata.noOfSegments);
            acpValues = new ArrayList<acpEntry>(commonMetadata.noOfSegments);
            constitutingVideoSegmentSrcTrack = new ArrayList<Integer>();
            requiredRateBound = new ArrayList<Double>(commonMetadata.noOfSegments);
            boundedACPEntries = new ArrayList<BoundedACPEntry>(commonMetadata.noOfSegments);
            boundedCBCEntries = new ArrayList<BoundedCBCEntry>(commonMetadata.noOfSegments);
            qubitStatistics = new QubitStatistics();
        }

        void composeACPEntries(MMQFQubitMetadataFileParser.CommonMetadata commonMetadata, long[] cbcValuesLong)
        {
            double segmentDurationInSec = segmentLengthAdjusted; 
            //commonMetadata.framesPerSegment/commonMetadata.frameRate;
            /*
            for(int i = 0; i< commonMetadata.noOfSegments; i++)
            {
                acpEntry entry = GetSlopeOfRepresentation(i, commonMetadata.noOfSegments, segmentDurationInSec, cbcValuesLong);
                double reqRateBound = commonMetadata.rateErrorFactor * entry.slope;
                requiredRateBound.add(reqRateBound);
                AddEntryIntoSortedACPEntries(entry);
            }
            */

            //If no of segments is quite large then, the routine with take quite a lot of time
            //In that case, it is better to approximate it over alternate segments

            for(int i = 0; i< cbcValuesLong.length; i++)
            {
                acpEntry entry = GetSlopeOfRepresentation(i, cbcValuesLong.length, segmentDurationInSec, cbcValuesLong);
                double reqRateBound = commonMetadata.rateErrorFactor * entry.slope;
                requiredRateBound.add(reqRateBound);
                AddEntryIntoSortedACPEntries(entry);
                //RupeshTODO: Should not we set i to entry.seqNum
            }
        }

        acpEntry GetSlopeOfRepresentation(int startingIndex, int segmentCnt, double segmentLen, long[] cbcValuesLong)
        {
            acpEntry entry = new acpEntry();
            long prevBits = 0;
            double maximumRate = 0;
            final int KStartUpDelayForControlPoints = 5;
            if(startingIndex > 0)
            {
                prevBits = cbcValuesLong[startingIndex - 1];
            }

            for(int i = startingIndex; i< segmentCnt; i++)
            {
                double totalBits = cbcValuesLong[i] - prevBits;
                double slope = (totalBits)/(KStartUpDelayForControlPoints + (segmentLen * (i - startingIndex + 1)));
                if (slope >= maximumRate)
                {
                    maximumRate = slope;
                    entry.seqNum = i;
                    entry.slope = maximumRate;
                }
            }
            return entry;
        }

        void AddEntryIntoSortedACPEntries(acpEntry entry)
        {
            //Todo: Use modified insertion sort
            //Use binary search for lookup, and insertionsort for inplace insertion with info from binary search for limited serch region (only if element is not found)
            boolean isPresent = false;
            int i = 0;
            for(i = 0; i< acpValues.size(); i++)
            {
                if(entry.seqNum == ((acpEntry)(acpValues.get(i))).seqNum)
                {
                    isPresent = true;
                    break;
                }
            }
            if(!isPresent)
            {
                //Insertion sort
                for(i = acpValues.size() - 1; i>=0; i--)
                {
                    if (((acpEntry)(acpValues.get(i))).seqNum < entry.seqNum)
                    {
                        break;
                    }
                }
                if(i>=0)
                {
                    //point of insertion
                    acpValues.add(i + 1, entry);
                }
                else
                {
                    acpValues.add(0, entry);
                }
            }
        }

        long getCBCValueAtIndex(int i)
        {
            if(i<cbcValuesSz)
            {
                return cbcValues.get(i).longValue();
            }
            return -1;
        }

        void ComputeBoundedCBCAndACP(MMQFQubitMetadataFileParser.CommonMetadata ccMetadata, long [] cbcValuesLong)
        {


            // Calculate the bound on the Cumulative Bit Curve (CBC) so that the estimated required rate is within the error factor times the actual required rate
            // See MN working records of 27/11/2009, on page 17 of book 019
            // For each GoP, for the given start up delay, calculate the rate to each subsequent critical point and find the maximum.
            // Allow the required rate to be more than this by a given factor, rate_error_factor_for_CBC.
            // These scaled rates have already been found in the calculation of ACP points and are stored in _requiredRateBound[]
            //
            // If current time is t, and estimate CBC is Be, then the estimated rate to ACP point i (Bi, ti), Rei is given by:
            //    Rei = (Bi - Be) / (ti - t + SUD)
            //  And Rei is constrained such that Rei <= required_rate_bound
            // rearranging to get a constraint on Be:
            //    Be >= Bi - (ti - t + SUD) * required_rate_bound

            // Hence for each subsequent critical point find this CBC estimate Be
            // And the largest of these CBC estimates gives the required CBC bound
            // The loop runs from index 1 to N-1 (inclusive), because, as shown on p19, the estimated rate required for index i
            // is given by the CBC bound before it is added to the cumulative total, i.e. CBC_bound[i-1]
            // So the first iteration calculates a CBC_bound[0], and the last iteration a CBC_bound[N-2]
            // The CBC_bound for the last sample is not needed, as it represents the CBC after the last sample, at which
            // time their is nothing left to deliver. But just in case something thinks about looking at it,
            // set it arbitrarily to the same as the previous value.

            long [] cumulative_bit_count_bound = new long[cbcValuesLong.length];
            long number_of_critical_points = acpValues.size();
            double gop_period = segmentLengthAdjusted;//ccMetadata.framesPerSegment/ccMetadata.frameRate;
            int sample_count = 0;
            for (sample_count = 1; sample_count<cbcValuesLong.length; sample_count++)
            {
                cumulative_bit_count_bound[sample_count-1] = -2147483648;//INT_MIN;
                for (int array_index = 0; array_index < number_of_critical_points; array_index++)
                {
                    if (((acpEntry)(acpValues.get(array_index))).seqNum >= sample_count)
                    {
                        // This critical point is not in the past relative to this sample_count, and hence needs to be considered
                        // time_delta is equal to (ti - t) in the above equations
                        double time_delta = (((acpEntry)acpValues.get(array_index)).seqNum - sample_count + 1) * gop_period;

                        // this_cbc_estimate is equal to Be in the above equations
                        double this_cbc_estimate = (cbcValuesLong[((acpEntry)acpValues.get(array_index)).seqNum] - ((time_delta + ccMetadata.acpStartUpDelay) * ((Double) (requiredRateBound.get(sample_count))).doubleValue()));
                        if (this_cbc_estimate > cumulative_bit_count_bound[sample_count-1])
                        {
                            cumulative_bit_count_bound[sample_count-1] = (long)this_cbc_estimate;
                        }
                    }
                }
            }
            cumulative_bit_count_bound[cbcValuesLong.length-1] = cumulative_bit_count_bound[cbcValuesLong.length-2];
            // Now try to find a piecewise straight line description of the CBC, such that this description stays within
            // the bounds of the actual CBC (cumulative_bit_count) and the above calculated CBC bound (cumulative_bit_count_bound)

            // These arrays are used for testing the effectiveness of the algorithms used to generate the CBC points
            // One one set will actually be written to the output meta-data

            int number_of_cbc_points;//[NUMBER_OF_CBC_ALGORITHMS];

            // The GOP indices of the points describing the piecewise straight line approximation to the cumulative bit curve
            long [] cbc_point_sample_index = new long[cbcValuesLong.length];//[NUMBER_OF_CBC_ALGORITHMS][MAX_NUMBER_OF_INTERVALS];
            long [] cbc_point_bit_count = new long[cbcValuesLong.length];//[NUMBER_OF_CBC_ALGORITHMS][MAX_NUMBER_OF_INTERVALS];	// The total bit count at these CBC points
            //#ifdef CBC_MID_POINT_2_ALGORITHM_DEFINE
            // Algorithm 3: Like algorithm 2, but instead of aiming for halfway, aim for three quarters of the way from the bound to the curve.
            // From the previous defined point on the piecewise straight line, (0, (3*cumulative_bit_count[0] + cumulative_bit_count_bound[0]) / 4 )
            // at the start, calculate the slope of a line to the point three quarters of the way between the two bounds at each GoP, and then
            // check whether this is a valid line segment, i.e. that it stays within the bounds.
            // Continue until this is not the case, and then back track to the previous point.
            {
                // Initialise the first point on the piecewise straight line (pwsl_x, pwsl_y)
                int pwsl_x = 0;
                long pwsl_y = (3*cbcValuesLong[0] + cumulative_bit_count_bound[0]) / 4;

                number_of_cbc_points = 0;
                cbc_point_sample_index[number_of_cbc_points] = pwsl_x;
                cbc_point_bit_count[number_of_cbc_points] = pwsl_y;


                number_of_cbc_points++;

                double rate, previous_rate;

                // Note that these rates are calculated in bits/gop_period as there is no benefit in converting to bits/second
                rate = 0.0;
                previous_rate = 0.0;

                // Note that the following loop starts at 1 rather than the usual 0
                // TODO: Is it possible to improve this algorithm? It stops as soon as an invalid line segment is found
                // but there could be a longer one that is valid?
                for (sample_count=1; sample_count<cbcValuesLong.length; sample_count++)
                {
                    double this_end_y = (3*cbcValuesLong[sample_count] + cumulative_bit_count_bound[sample_count]) / 4;

                    // Now check that this is valid for all intermediate samples
                    boolean valid_line_segment = true;
                    int sample_index;
                    for (sample_index = pwsl_x+1; sample_index < sample_count; sample_index++)
                    {
                        double this_y = pwsl_y + ((this_end_y - pwsl_y) * (sample_index - pwsl_x)) / (sample_count - pwsl_x);

                        if ((this_y > cbcValuesLong[sample_index] || (this_y < cumulative_bit_count_bound[sample_index])))
                        {
                            valid_line_segment = false;
                            break;
                        }
                    }

                    if (!valid_line_segment)
                    {
                        // Need to back track to the previous values
                        pwsl_x = sample_count - 1;
                        pwsl_y = (3*cbcValuesLong[pwsl_x] + cumulative_bit_count_bound[pwsl_x]) / 4;

                        cbc_point_sample_index[number_of_cbc_points] = pwsl_x;
                        cbc_point_bit_count[number_of_cbc_points] = pwsl_y;
                        number_of_cbc_points++;
                    }

                    if (sample_count + 1 == cbcValuesLong.length)
                    {
                        // Last time, so add one more point at the end
                        pwsl_x = sample_count;
                        pwsl_y = (3*cbcValuesLong[pwsl_x] + cumulative_bit_count_bound[pwsl_x]) / 4;

                        cbc_point_sample_index[number_of_cbc_points] = pwsl_x;
                        cbc_point_bit_count[number_of_cbc_points] = pwsl_y;
                        number_of_cbc_points++;
                    }
                }
            }

            int j = 0;
            for(j = 0 ; j < number_of_cbc_points; j++)
            {
                BoundedCBCEntry entry = new BoundedCBCEntry();
                entry.seqNum = cbc_point_sample_index[j];
                entry.cumulativeBits = cbc_point_bit_count[j];
                boundedCBCEntries.add(entry);
            }
            for(j = 0; j < number_of_critical_points; j++)
            {
                acpEntry entr = (acpEntry)(acpValues.get(j));
                BoundedACPEntry entry = new BoundedACPEntry();
                entry.seqNum = entr.seqNum;
                entry.acpValue = cbcValuesLong[entr.seqNum];
                boundedACPEntries.add(entry);
            }
        }

        void printDescription()
        {
            MMLogger.i(TAG, "Printing the CQ metadata");
            MMLogger.i(TAG, "Target iMOS - " + targetiMOS);
            MMLogger.i(TAG, "CBC Values - " + cbcValues.toString());
            MMLogger.i(TAG, "iMOS Values - " + imosValues.toString());
            MMLogger.i(TAG, "Constituting Video Tracks - " + constitutingVideoSegmentSrcTrack.toString());
            MMLogger.i(TAG, "Bounded ACP Values - ");
            for(int i = 0; i< boundedACPEntries.size(); i++)
            {
                MMLogger.i(TAG, "ACP Index" + (boundedACPEntries.get(i).seqNum + " Value - " + boundedACPEntries.get(i).acpValue));
            }
            MMLogger.i(TAG, "Bounded CBC Values - ");
            for(int i = 0; i< boundedCBCEntries.size(); i++)
            {
                MMLogger.i(TAG, "CBC Index" + (boundedCBCEntries.get(i).seqNum + " Value - " + boundedCBCEntries.get(i).cumulativeBits));
            }
        }
        double targetiMOS = 0;
        ArrayList<Long> cbcValues = null;
        long [] cbcValuesAdjusted = null;
        int cbcValuesSz = -1;
        ArrayList<Double> imosValues = null;
        ArrayList<acpEntry> acpValues = null;
        ArrayList<Integer> constitutingVideoSegmentSrcTrack = null;
        ArrayList<Double> requiredRateBound = null;
        ArrayList<BoundedACPEntry> boundedACPEntries = null;
        ArrayList<BoundedCBCEntry> boundedCBCEntries = null;
        public QubitStatistics qubitStatistics; //static statistics, compiled while creating CQ track
        private final static String TAG = "QubitModel.CQMetadata";
    }

    class MMQFQubitVideoTrack{
        MMQFQubitVideoTrack()
        {
            cbrMetadata = new CBRMetadata();
            cqMetadata = new CQMetadata();
        }
        CBRMetadata cbrMetadata = null;
        CQMetadata cqMetadata = null;
        MMQFPresentationVideoTrackInfo videoTrackInfo;
    }

    class iMOSImprovementPoint{
        int segmentIndex = 0;
        double cbriMOS = 0;
        double targetiMOS = 0;
        double cqiMOS = 0;
    }

    public void updateRunningQubitStatistics(MMQFQubitVideoTrack qubitVideoTrack, MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segmentInfo)
    {
        int bitsSaved = qubitVideoTrack.cqMetadata.qubitStatistics.bitsSaved.get(segmentInfo.segmentIndex).intValue();
        runningStatistics.totalBitsCBR += (qubitVideoTrack.videoTrackInfo.getSegmentInfo(segmentInfo.segmentIndex).segmentSz);
        runningStatistics.totalBitsCQ += ((qubitVideoTrack.videoTrackInfo.getSegmentInfo(segmentInfo.segmentIndex).segmentSz) - bitsSaved);

        runningStatistics.durationOfPlayback += (qubitVideoTrack.videoTrackInfo.getSegmentInfo(segmentInfo.segmentIndex).duration/qubitVideoTrack.videoTrackInfo.timeScale);
        double imosImprovement = qubitVideoTrack.cqMetadata.qubitStatistics.iMOSImpovements.get(segmentInfo.segmentIndex).doubleValue();
        runningStatistics.totaliMOSImprovements += (imosImprovement > 0.0) ? 1 : 0;
        runningStatistics.totalSegments++;
        MMLogger.i("QubitSessionStats.RS", "Track - " + qubitVideoTrack.videoTrackInfo.bitrate + " SeqNum " + segmentInfo.segmentIndex + " bits Saved " + bitsSaved + " CBRTotal " + runningStatistics.totalBitsCBR + "  CQTotal" + runningStatistics.totalBitsCQ + " imosImpvoment " + imosImprovement);
        if(bitsSaved < 0)
        {
            assert (imosImprovement > 0.0);
        }

        if((runningStatistics.durationOfPlayback * 1000) > minDurationForVarianceComputation) {
            runningStatistics.imosSquared.add(qubitVideoTrack.cqMetadata.qubitStatistics.iMOSDiffSq.get(segmentInfo.segmentIndex));
            runningStatistics.imosSquaredCBR.add(qubitVideoTrack.cqMetadata.qubitStatistics.iMOSDiffSqCBR.get(segmentInfo.segmentIndex));
        }

        runningStatistics.targetiMOSVect.add(qubitVideoTrack.cqMetadata.targetiMOS);
        MMLogger.i("QubitModelVarianceCheck", "Bitrate[ " + segmentInfo.segmentIndex + " ] : " + qubitVideoTrack.videoTrackInfo.bitrate + " Target : " + qubitVideoTrack.cqMetadata.targetiMOS  + " CBR imos : " + qubitVideoTrack.cbrMetadata.mediaAttributes.imosValues.get(segmentInfo.segmentIndex) +  " CBR Squared " + qubitVideoTrack.cqMetadata.qubitStatistics.iMOSDiffSqCBR.get(segmentInfo.segmentIndex) + " CQ iMOS : " + qubitVideoTrack.cqMetadata.imosValues.get(segmentInfo.segmentIndex) + " CQ Squared" + qubitVideoTrack.cqMetadata.qubitStatistics.iMOSDiffSq.get(segmentInfo.segmentIndex));

        iMOSImprovementPoint impPoint = new iMOSImprovementPoint();
        impPoint.segmentIndex = segmentInfo.segmentIndex;
        impPoint.cbriMOS = ((Double)(qubitVideoTrack.cbrMetadata.mediaAttributes.imosValues.get(segmentInfo.segmentIndex))).doubleValue();
        impPoint.targetiMOS = qubitVideoTrack.cqMetadata.targetiMOS;
        impPoint.cqiMOS = ((Double)(qubitVideoTrack.cqMetadata.imosValues.get(segmentInfo.segmentIndex))).doubleValue();
        if(impPoint.cbriMOS < runningStatistics.minImos)
        {
            runningStatistics.minImos = impPoint.cbriMOS;
            runningStatistics.percentageMiniMOSImprovement = (int)imosImprovement;
        }
        runningStatistics.imosImprovements.add(impPoint);

        if(imosImprovement > runningStatistics.percentageMaxiMOSImprovement)
        {
            runningStatistics.percentageMaxiMOSImprovement = (int)imosImprovement;
            runningStatistics.maxiMOSImpPoint = impPoint;
        }
    }

    public String getQubitUrl(String inURL, MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segmentInfo, MMQFQubitEngineInterface.MMQFQubitResource res) {
        if(segmentInfo!=null && segmentInfo.videoTrackInfo != null && segmentInfo.segmentIndex >=0) {
            MMQFQubitVideoTrack qubitVideoTrack = null;
            for (int i = 0; i < qubitModel.size(); i++) {
                qubitVideoTrack = (MMQFQubitVideoTrack) qubitModel.get(i);
                if (qubitVideoTrack != null && ((qubitVideoTrack.videoTrackInfo.bitrate == segmentInfo.videoTrackInfo.bitrate) && (qubitVideoTrack.videoTrackInfo.height == segmentInfo.videoTrackInfo.height)&& (qubitVideoTrack.videoTrackInfo.width == segmentInfo.videoTrackInfo.width))) {
                    break;
                }
                if (i == (qubitModel.size() - 1)) {
                    //could not found matching entry
                    assert (false);
                    qubitVideoTrack = null;
                }
            }
            boolean proceedWithSwitch = true;
            if (qubitVideoTrack != null) {
                segmentInfo.qbrTrackIndex = segmentInfo.cbrTrackIndex = qubitVideoTrack.videoTrackInfo.trackIndex;
                if((qubitVideoTrack.cqMetadata.constitutingVideoSegmentSrcTrack.size()-1) < segmentInfo.segmentIndex)
                {
                    return inURL;
                }
                int qubitTrackToUse = ((Integer) (qubitVideoTrack.cqMetadata.constitutingVideoSegmentSrcTrack.get(segmentInfo.segmentIndex))).intValue();
                
                if(segmentInfo.cbrTrackIndex < qubitTrackToUse){
                    // Check for Buffer in case of Up-switch
                    MMQFPresentationVideoTrackSegmentInfo trackSegInfo = (MMQFPresentationVideoTrackSegmentInfo)qubitVideoTrack.videoTrackInfo.trackSegmentInfoVect.get(segmentInfo.segmentIndex);
                    long segStartTime = trackSegInfo.segmentStartTime;
                    long segduration = trackSegInfo.duration;
                    long bufferedMediaDuration = 0L;
		                // cavityDuration is in millisec.
                    long cavityDuration = Math.abs((segStartTime/qubitVideoTrack.videoTrackInfo.timeScale)*1000 - (bufferedPlaybackTimeInSec*1000));
                    if (cavityDuration < segduration){
                        bufferedMediaDuration = (bufferedPlaybackTimeInSec - playbackPosAtBufferLookUp) * 1000;
                    }

                    long minBufferThresholdForUplift = 4;
                    if (minBufferThresholdForUplift < segduration) {
                        minBufferThresholdForUplift = segduration;
                    }

                    minBufferThresholdForUplift = minBufferThresholdForUplift + ((Integer)(qubitModel.get(qubitTrackToUse).videoTrackInfo.bitrate)/(Integer)(segmentInfo.videoTrackInfo.bitrate));

                    if (bufferedMediaDuration < minBufferThresholdForUplift){
                        proceedWithSwitch = false;
                        MMLogger.i("getQBRChunk Computation :"," Overridden - StartTime: " + segStartTime + " proceedWithSwitch: " + proceedWithSwitch + " minBufferThresholdForUplift " + minBufferThresholdForUplift + " bufferedPlaybackTimeInSec " + bufferedPlaybackTimeInSec);
                    }else{
                        MMLogger.i("getQBRChunk Computation :"," Uplifted - StartTime: " + segStartTime + " proceedWithSwitch: " + proceedWithSwitch + " minBufferThresholdForUplift " + minBufferThresholdForUplift + " bufferedPlaybackTimeInSec " + bufferedPlaybackTimeInSec);
                    }
                                
                    if(proceedWithSwitch == false){
                        qubitTrackToUse = segmentInfo.cbrTrackIndex;
                    }
                }
                
                res.trackIndex = qubitTrackToUse;
                segmentInfo.qbrTrackIndex = qubitTrackToUse;
                MMQFPresentationVideoTrackInfo trackToUse = ((MMQFQubitVideoTrack) (qubitModel.get(qubitTrackToUse))).videoTrackInfo;
                segmentInfo.qbrVideoTrackInfo = trackToUse;
                String segmentRelURL = trackToUse.getSegmentURL(segmentInfo.segmentIndex);
                if (segmentRelURL != null) {
                    String baseURL = trackToUse.getBaseURL(inURL);
                    String KProtocol = "http://";
                    String KProtocolHTTPS = "https://";
                    String retval = null;
                    if(segmentRelURL.length() > KProtocol.length() && (segmentRelURL.substring(0, KProtocol.length()).compareTo(KProtocol) == 0 || segmentRelURL.substring(0, KProtocolHTTPS.length()).compareTo(KProtocolHTTPS) == 0)){
                        retval = segmentRelURL;
                    }
                    else {
                        retval = baseURL + segmentRelURL;
                    }
                    MMLogger.i("getQBRChunk Computation : ", "Sequence - " + segmentInfo.segmentIndex + "Src Bitrate - " + qubitVideoTrack.videoTrackInfo.bitrate + " QubitizedURL " + segmentRelURL + " QBR Bitrate - " + trackToUse.bitrate);
                    updateRunningQubitStatistics(qubitVideoTrack, segmentInfo);
                    return retval;
                }
            }
            return null;
        }
        else {
            return inURL;
        }
    }

    double getQubitBandwidthRequirementsForProfileTestMethod(MMQFPresentationVideoTrackInfo videoTrackInfo){
        int [] playbackPosArr = {4,8,20,30,50,120,520,890, 5000};
        int [] buffLenArr = {2,11,13,20,29,110,190, 250};

        for (int index = 0; index< playbackPosArr.length; index++){
            for(int blenIndex = 0; blenIndex<buffLenArr.length; blenIndex++){
                getQubitBandwidthRequirementsForProfile(videoTrackInfo, playbackPosArr[index], buffLenArr[blenIndex]);
            }
        }
        return 0;
    }

    int getQubitBandwidthRequirementsForProfile(MMQFPresentationVideoTrackInfo videoTrackInfo, int playbackPos, int bufferLength){
        int maximumBitRate = 0;
        int playbackPosInSec = playbackPos;
        int bufferedPos = playbackPos + bufferLength;
        
        bufferedPlaybackTimeInSec = bufferedPos;        // In seconds
        playbackPosAtBufferLookUp = playbackPosInSec;   // In seconds

        //double segmentLength = (int)(commonMetadata.framesPerSegment / commonMetadata.frameRate);
        for(int i = 0; i<qubitModel.size(); i++)
        {
            MMQFQubitVideoTrack qubitTrack = (MMQFQubitVideoTrack)qubitModel.get(i);
            if(qubitTrack.videoTrackInfo == videoTrackInfo)
            {
                long cbcBitsAtBufferedPoint = 0;
                for(int j = 1; j< qubitTrack.cqMetadata.boundedCBCEntries.size(); j++ )
                {
                    CQMetadata.BoundedCBCEntry boundedCBCEntry = qubitTrack.cqMetadata.boundedCBCEntries.get(j);
                    if((boundedCBCEntry.seqNum * segmentLengthAdjusted > bufferedPos) || (j==(qubitTrack.cqMetadata.boundedCBCEntries.size()-1)))
                    {
                        CQMetadata.BoundedCBCEntry prevCBCEntry = qubitTrack.cqMetadata.boundedCBCEntries.get(j-1);
                        double rate = (boundedCBCEntry.cumulativeBits - prevCBCEntry.cumulativeBits)/((boundedCBCEntry.seqNum - prevCBCEntry.seqNum) * segmentLengthAdjusted);
                        cbcBitsAtBufferedPoint = prevCBCEntry.cumulativeBits + (long)((rate * (bufferedPos - (prevCBCEntry.seqNum * segmentLengthAdjusted))));
                        break;
                    }
                }

                //Get the maximum rate
                for(i =0;i<qubitTrack.cqMetadata.boundedACPEntries.size();i++)
                {
                    CQMetadata.BoundedACPEntry entry = qubitTrack.cqMetadata.boundedACPEntries.get(i);
                    if(((entry.seqNum*segmentLengthAdjusted - commonMetadata.acpStartUpDelay) > playbackPos) && (cbcBitsAtBufferedPoint<entry.acpValue))
                    {
                        int rateRequired = (int)((entry.acpValue - cbcBitsAtBufferedPoint) / (entry.seqNum*segmentLengthAdjusted - commonMetadata.acpStartUpDelay - playbackPos));
                        if(rateRequired<0) rateRequired=99999999;	// if -ve then not enough buffer for this qual so translates to a bigger than infinate rate !!
                        // this should never happen with the condition check
                        if(rateRequired > maximumBitRate) {
                            maximumBitRate = rateRequired;
                        }
                    }
                }
                MMLogger.p("ABRSwitchDecision", "getQBRBandwidth - PlaybackPos "+ playbackPos + "BufferLength " + bufferLength + " Deduced MaxBitrate " + maximumBitRate + " Tracks's bitrate" +  videoTrackInfo.bitrate + " Ratio - " + (double)maximumBitRate/videoTrackInfo.bitrate);
                double hh = (double)maximumBitRate/videoTrackInfo.bitrate;
                if(videoTrackInfo.bitrate > maximumBitRate)
                {
                    maximumBitRate = videoTrackInfo.bitrate;
                }

                break;
            }
        }


        return maximumBitRate;
    }

    public MMQFQubitModel(MMQFPresentationInfo aPresentationInfo, URL aUrlOfMetadataFile, int mode, RegisterResponse regResponse)
    {
        registrationResponse = regResponse;
        presentationInfo = aPresentationInfo;
        urlOfMetadataFile = aUrlOfMetadataFile;
        qubitModel = new ArrayList();
        tmpDiffArray = new ArrayList<iMOSDifferenceElem>();
        runningStatistics = new QubitRunningStatistics();
        qubitMode = mode;
        _imosWeightsArray = new ArrayList<Double>();
        _imosWeightsArray.clear();

        if(qubitMode == MMQFQubitConfigurationInterface.QubitMode_Quality){
            _imosWeightsArray.add(3.0);
            _imosWeightsArray.add(2.5);
            _imosWeightsArray.add(2.0);
            _imosWeightsArray.add(1.5);
            _imosWeightsArray.add(1.0);
        }else{
            _imosWeightsArray.add(2.0);
            _imosWeightsArray.add(1.75);
            _imosWeightsArray.add(1.5);
            _imosWeightsArray.add(1.25);
            _imosWeightsArray.add(1.0);
        }

        MMQFPresentationVideoTrackInfo videoTrack = presentationInfo.getVideoTrack(0);
        if (videoTrack != null){
            MMQFPresentationVideoTrackSegmentInfo seginfo = videoTrack.getSegmentInfo(0);
            if(seginfo!= null){
                segmentLength = (double)presentationInfo.getVideoTrack(0).getSegmentInfo(0).duration/presentationInfo.getVideoTrack(0).timeScale;
            }
        }
    }

    public interface OnQubitModelCreatedListener {
        public abstract void onOnQubitModelCreated(MMQFQubitStatusCode status);
    }

    public void setOnQubitModelCreatedListener(OnQubitModelCreatedListener lstnr)
    {
        listener = lstnr;
    }

    public void onQubitMetadataFileParsed(MMQFQubitStatusCode status)
    {
        if(status.status() == MMQFQubitStatusCode.MMQFSuccess)
        {
            MMLogger.p(profilingTag, "onQubitMetadataFileParsed - " + System.currentTimeMillis());
            Boolean modelCreationStatus = CompleteQubitModelCreation();
            if(listener != null)
            {
                if(modelCreationStatus == true) {
                    /*MMLogger.p("ABRSwitchDecision", "ABR Test Starts");
                    for(int i = 0; i< presentationInfo.getVideoTracksCount(); i++) {
                        getQubitBandwidthRequirementsForProfileTestMethod(presentationInfo.getVideoTrack(i));
                    }
                    MMLogger.p("ABRSwitchDecision", "ABR Test Ends");*/
                    listener.onOnQubitModelCreated(status);
                }else{
                    listener.onOnQubitModelCreated(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFFailure));
                }
            }
        }
        else if (status.status() == MMQFQubitStatusCode.MMQFCancelled) {
            listener.onOnQubitModelCreated(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFCancelled));
        }else{
            listener.onOnQubitModelCreated(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFFailure));
        }
    }

    public void CancelPendingRequests(){
        if(metadataFileParser != null){
            metadataFileParser.CancelPendingRequests();
        }
    }

    private static String profilingTag = "MMSmartStreaming.Profile";
    private static String analysisTag = "MMSmartStreaming.Analyze";

    public MMQFQubitStatusCode CreateQubitModel() {
        MMLogger.p(profilingTag, "Qubit Model Creation Started" + System.currentTimeMillis());
        MMQFQubitStatusCode retval = new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFPending);
        metadataFileParser = new MMQFQubitMetadataFileParser();
        metadataFileParser.setOnQubitMetadataFileParsedListener(this);
        metadataFileParser.retrieveAndParseMetadataFile(urlOfMetadataFile);
        return retval;
    }

    private Boolean CompleteQubitModelCreation() {
        MMLogger.p(profilingTag, "Start processing Metadata for Qubit Creation " + System.currentTimeMillis());
        Boolean retval = true;
        commonMetadata = metadataFileParser.getCommonMetadata();
        assert(presentationInfo.getVideoTracksCount() == commonMetadata.noOfVideoTracks);

        Integer maxResolution = 0;
        Integer minResolution = 0x7FFFFFFF;
        Integer maxResolutionWidth = 0;

        Integer maxResolutionHeight = 0;
        Integer minResolutionHeight = 0;


        boolean xResSwitchPossible = true;

        if (commonMetadata.noOfVideoTracks != presentationInfo.getVideoTracksCount()){
            MMLogger.e("MMMapping", "Metadata Avl for " + commonMetadata.noOfVideoTracks + " But presentation has total tracks " + presentationInfo.getVideoTracksCount());
            return false;
        }

        int noOfSegmentsInTrackForConsistency = -1;
        for (int i = 0; i< presentationInfo.getVideoTracksCount(); i++){

            MMQFPresentationVideoTrackInfo videoTrack = presentationInfo.getVideoTrack(i);
            if (videoTrack == null){
                MMLogger.e("MMMapping", "Presentation video track missing for Idx " + i);
                return false;
            }
            if(i>0 && noOfSegmentsInTrackForConsistency != videoTrack.getSegmentCount()){
                MMLogger.e("MMMapping", "Segs in Track " + i + " are " + videoTrack.getSegmentCount() + ". Whereas, no of segs in track 0 are " + noOfSegmentsInTrackForConsistency);
                return false;
            }else {
                noOfSegmentsInTrackForConsistency = videoTrack.getSegmentCount();
            }
        }

        if (Math.abs(noOfSegmentsInTrackForConsistency - commonMetadata.noOfSegments) > 5){
            MMLogger.e("MMMapping", "Segs in hint file " + commonMetadata.noOfSegments + " Segs in presentation " + noOfSegmentsInTrackForConsistency);
        }

        for(int i=0; i< commonMetadata.noOfVideoTracks; i++) {
            MMQFPresentationVideoTrackInfo videoTrack = presentationInfo.getVideoTrack(i);
            if(videoTrack!= null) {
                MMQFQubitVideoTrack qubitVideoTrack = new MMQFQubitVideoTrack();
                qubitVideoTrack.videoTrackInfo = videoTrack;

                //Assumption is that profiles in meta file are laid in same order as in manifests (after sorting manifest profiles in increasing order of bitrate)
                //getVideoTrackMediaAttributesForTrack will just get the track based on index
                //because the unique key params (res, codec) of key (res, codec, bitrate) may be missing in manifests, and also bitrate will not be same in manifest and metafile
                //populate entries in the video track from manifest files (if available)

                qubitVideoTrack.cbrMetadata.mediaAttributes = metadataFileParser.getVideoTrackMediaAttributesForTrack(videoTrack.width, videoTrack.height, videoTrack.bitrate, i);
                MMLogger.i("MMMapping", "Mapping Bitrate " + videoTrack.bitrate + " to " + qubitVideoTrack.cbrMetadata.mediaAttributes.averageBitRate);
                if (qubitVideoTrack.cbrMetadata.mediaAttributes.displayHeight != -1 && qubitVideoTrack.videoTrackInfo.height == -1){
                    qubitVideoTrack.videoTrackInfo.height = qubitVideoTrack.cbrMetadata.mediaAttributes.displayHeight;
                }

                if (qubitVideoTrack.cbrMetadata.mediaAttributes.displayWidth != -1 && qubitVideoTrack.videoTrackInfo.width == -1){
                    qubitVideoTrack.videoTrackInfo.width = qubitVideoTrack.cbrMetadata.mediaAttributes.displayWidth;
                }

                if (qubitVideoTrack.cbrMetadata.mediaAttributes.displayHeight  != -1 && qubitVideoTrack.cbrMetadata.mediaAttributes.displayWidth != -1){
                    Integer res = qubitVideoTrack.cbrMetadata.mediaAttributes.displayWidth * qubitVideoTrack.cbrMetadata.mediaAttributes.displayHeight;
                    if(res > maxResolution){
                        maxResolution = res;
                        maxResolutionWidth = qubitVideoTrack.cbrMetadata.mediaAttributes.displayWidth;
                        maxResolutionHeight = qubitVideoTrack.cbrMetadata.mediaAttributes.displayHeight;
                    }

                    if(res < minResolution){
                        minResolution = res;
                        minResolutionHeight = qubitVideoTrack.cbrMetadata.mediaAttributes.displayHeight;
                    }

                }else{
                    xResSwitchPossible = false;
                }

                qubitModel.add(qubitVideoTrack);
                tmpDiffArray.add(new iMOSDifferenceElem());
            }
        }
        qubitModelSz = qubitModel.size();
        MMLogger.p(profilingTag, "Creating CQ Matrix");
        //Normalize the mediaattributes IMOS values to let them work across resolutions

        if(xResSwitchPossible){
            //Normalize the imos
            ArrayList<Double> normalizedValuesInitial = new ArrayList<Double>();

            Double maxNormalisedValue = -100.0;
            Double minNormalisedValue = 100.0;
            for(int i=0; i< qubitModel.size(); i++) {
                MMQFQubitVideoTrack videoTrack = qubitModel.get(i);
                Double normalizingFactor = 1.0;
                if(commonMetadata.version <= 1){
                    MMLogger.p("ProfileQubit", "Qubit CF-Model");
                    normalizingFactor = 1.0 - (registrationResponse.cfVal * ((float) (maxResolutionHeight - videoTrack.cbrMetadata.mediaAttributes.displayHeight) / videoTrack.cbrMetadata.mediaAttributes.displayHeight));
                }else{
                    MMLogger.p("ProfileQubit", "Qubit ML-Model");
                }
                normalizedValuesInitial.add(normalizingFactor);
                if (normalizingFactor > maxNormalisedValue ){
                    maxNormalisedValue = normalizingFactor;
                }

                if(normalizingFactor < minNormalisedValue){
                    minNormalisedValue = normalizingFactor;
                }
            }

            if(maxNormalisedValue - minNormalisedValue > 0) {
                for (int i = 0; i < qubitModel.size(); i++) {
                    Double normalisedValue = (normalizedValuesInitial.get(i).doubleValue() - minNormalisedValue) / (maxNormalisedValue - minNormalisedValue);
                    if (normalisedValue < 0.1) {
                        normalisedValue = 0.1;
                    }
                    normalizedValuesInitial.set(i, normalisedValue);
                }
            }


            for(int i=0; i< qubitModel.size(); i++) {
                MMQFQubitVideoTrack videoTrack = qubitModel.get(i);
                Double normalizingFactor = normalizedValuesInitial.get(i);

                if(normalizingFactor != 1.0) {
                    for (int j = 0; j < videoTrack.cbrMetadata.mediaAttributes.imosValues.size(); j++) {
                        Double imos = videoTrack.cbrMetadata.mediaAttributes.imosValues.get(j);
                        Double normalizedImos = imos * normalizingFactor;
                        Log.i("MOSANALYSIS", "Track ...[" + i + "] IMOS " + imos + " => " + normalizedImos.intValue());
                        videoTrack.cbrMetadata.mediaAttributes.imosValues.set(j, normalizedImos.doubleValue());
                    }
                }
            }
        }

        updateQubitVideoTracksForCQMetadata();
        MMLogger.p(profilingTag, "Metadata Processed - Qubit Model Creation Complete - " + System.currentTimeMillis());
        return retval;
    }

    void updateQubitVideoTracksForCQMetadata()
    {
        for(int i=0; i<qubitModelSz; i++)
        {
            MMLogger.p(profilingTag, "Creating Model for track "+ i);
            MMQFQubitVideoTrack videoTrack = (MMQFQubitVideoTrack) qubitModel.get(i);
            if(qubitMode == MMQFQubitConfigurationInterface.QubitMode_Bits || qubitMode == MMQFQubitConfigurationInterface.QubitMode_CostSave) {
                videoTrack.cqMetadata.targetiMOS = (int)videoTrack.cbrMetadata.getiMOSWeightedAverage();
                MMLogger.a(analysisTag, "TARGET MOS(Quality) for track " + i + videoTrack.cqMetadata.targetiMOS);
            }
            else{
                videoTrack.cqMetadata.targetiMOS = (int)videoTrack.cbrMetadata.getiMOSAverage();
                MMLogger.a(analysisTag, "TARGET MOS(Bitsave/Costsave) for track " + i + videoTrack.cqMetadata.targetiMOS);
            }

            if(videoTrack.videoTrackInfo.trackSegmentInfoVect.size() < commonMetadata.noOfSegments){
                commonMetadata.noOfSegments = videoTrack.videoTrackInfo.trackSegmentInfoVect.size();
                MMLogger.a(analysisTag, "Packager Seg Vs MOS Seg " + videoTrack.videoTrackInfo.trackSegmentInfoVect.size() + " - " + commonMetadata.noOfSegments);
            }

            for(int j =0; j< commonMetadata.noOfSegments; j++)
            {
                //update the following params for the cq track
                // 1. iMOS values
                // 2. CBR values

                //For imos we need to compare imos score of this track with all available video tracks.
                int sourceTrackIdx = UpdateSegmentiMOSEntryForTargetQuality(i, videoTrack, j);
                videoTrack.cqMetadata.constitutingVideoSegmentSrcTrack.add(sourceTrackIdx);

                MMQFQubitVideoTrack selectedQubitTrack = qubitModel.get(sourceTrackIdx);
                MMQFPresentationVideoTrackSegmentInfo segInfo = selectedQubitTrack.videoTrackInfo.getSegmentInfo(j);

                ArrayList<Long> cbcValues = videoTrack.cqMetadata.cbcValues;
                long segmentSz = segInfo.segmentSz;
                if(j == 0){
                    cbcValues.add(segmentSz);
                }else{

                    cbcValues.add(cbcValues.get(j-1).longValue() + segmentSz);
                }

                // Update the statistics
                // 1. Bits Saved (CBR segment sz - CQ segment sz) in bits and %
                // 2. Peak Bitrate, Min Bitrate
                // 3. Peak iMOS, Min iMOS
                // 4. iMOS improvements
                // 5. iMOS variance
                videoTrack.cqMetadata.qubitStatistics.updateStatisticsForSegment(videoTrack, selectedQubitTrack, j);
            }
            videoTrack.cqMetadata.cbcValuesSz = videoTrack.cqMetadata.cbcValues.size();
            MMLogger.p(profilingTag, "Updating imos entries end for track" + i + " " + System.currentTimeMillis());
            videoTrack.cqMetadata.qubitStatistics.concludeAggregatedStatistics();

            MMLogger.p("ProfileQubitModel", "Compose ACP -- ");

            Object cBCValuesRaw[] = videoTrack.cqMetadata.cbcValues.toArray();
            int skipSize = 1;
            int cbcArrayAdjustedSz = cBCValuesRaw.length;
            if((double)(cBCValuesRaw.length) > maxSegmentsToLookUpForAdaptation){
                skipSize = (int)((double)(cBCValuesRaw.length/(maxSegmentsToLookUpForAdaptation)));
                if(cBCValuesRaw.length%maxSegmentsToLookUpForAdaptation != 0){
                    skipSize++;
                }
                segmentLengthAdjusted = skipSize * segmentLength;
            }
            else{
                segmentLengthAdjusted = segmentLength;
            }

            cbcArrayAdjustedSz = cBCValuesRaw.length/skipSize;
            if((cBCValuesRaw.length % skipSize) != 0){
                cbcArrayAdjustedSz++;
            }

            videoTrack.cqMetadata.cbcValuesAdjusted = new long[cbcArrayAdjustedSz];
            int modifiedCbcValCnt = -1;
            for(int j = 0; j<cBCValuesRaw.length; j+=skipSize){
                videoTrack.cqMetadata.cbcValuesAdjusted[++modifiedCbcValCnt] = ((Long)cBCValuesRaw[j]).longValue();
                long prevValue = videoTrack.cqMetadata.cbcValuesAdjusted[modifiedCbcValCnt];
                for(int iCtr = 1; iCtr< skipSize; iCtr++){
                    if( (j + iCtr) < cBCValuesRaw.length) {
                        videoTrack.cqMetadata.cbcValuesAdjusted[modifiedCbcValCnt] += (((Long)cBCValuesRaw[j + iCtr]).longValue() - prevValue);
                        prevValue = ((Long)cBCValuesRaw[j + iCtr]).longValue();
                    }
                }
            }

            videoTrack.cqMetadata.composeACPEntries(commonMetadata, videoTrack.cqMetadata.cbcValuesAdjusted);
            MMLogger.p("ProfileQubitModel", "ComputeBoundedCBCAndACP -- ");
            videoTrack.cqMetadata.ComputeBoundedCBCAndACP(commonMetadata, videoTrack.cqMetadata.cbcValuesAdjusted);
            MMLogger.p("ProfileQubitModel", "Printing CQ metadata for track" + i);
            //videoTrack.cqMetadata.printDescription();
        }
    }

    private boolean trackIsQubitSwitchable(MMQFQubitVideoTrack videoTrack1, MMQFQubitVideoTrack videoTrack2, int gopIdx)
    {
        return true;
//        //Same resolution
//        MMQFQubitVideoTrack trk = null;
//        int index = -1;
//        if(videoTrack1.cbrMetadata.mediaAttributes.xResSwitches != null && (videoTrack1.cbrMetadata.mediaAttributes.xResSwitches.size()>0) ){
//            index = videoTrack1.cbrMetadata.mediaAttributes.xResSwitches.get(gopIdx).intValue();
//        }
//
//        if(index == -1){
//            trk = videoTrack1;
//        }else{
//            trk = qubitModel.get(index);
//        }
//
//        if( (trk.cbrMetadata.mediaAttributes.displayWidth == videoTrack2.cbrMetadata.mediaAttributes.displayWidth) &&
//                (trk.cbrMetadata.mediaAttributes.displayHeight == videoTrack2.cbrMetadata.mediaAttributes.displayHeight))
//        {
//            return true;
//        }
//        return false;
    }

    class iMOSDifferenceElem{
        iMOSDifferenceElem()
        {
            reset();
        }
        public void reset()
        {
            imosDifference = new Double(0x7FFFFFFF);
            index = 0;
        }
        Double imosDifference;
        Integer index;
    }

    class iMOSComparator implements Comparator<iMOSDifferenceElem> {
        @Override
        public int compare(iMOSDifferenceElem a, iMOSDifferenceElem b) {
            return a.imosDifference < b.imosDifference ? -1 : a.imosDifference == b.imosDifference ? 0 : 1;
        }
    }

    private boolean isCompatibleFrameRate(double srcFrameRate, double targetFrameRate){
        return srcFrameRate == targetFrameRate;
    }

    private boolean isResolutionSwitchSupported(int srcRes_x, int srcRes_y, int targetRes_x, int targetRes_y, int srcBitrate, int targetBitrate){
        int srcRes = srcRes_x * srcRes_y;
        int targetRes = targetRes_x * targetRes_y;
        double xRatio = (srcRes_x * 1.0)/targetRes_x;
        double yRatio = (srcRes_y * 1.0)/targetRes_y;
        
        return true; // Always True, Untill MM finialize on Resolution aspect ratio 
        
/*
        //Need to have same aspect ratio
        if(xRatio != yRatio){
            return false;
        }

        if(targetBitrate> 1000000  || srcBitrate < 2000000){
            return true;
        }else{
            return false;
        }

//        if(xRatio > 1.0){ //downswitch
//            //416 x 234
//            //480 x 270
//            //640 x 360
//            //768 x 432
//            //960 x 540
//            //720p = 1280 x 720
//            //1080p = 1920 x 1080
//            //1440p = 2560 x 1440
//            //2160p = 3840 x 2160
//            //4320p = 7680 x 4320
//            if(xRatio > 3.0 && xRatio < 4){
//                if(srcRes_x >= 3840){
//                    return true;
//                }
//            }else if(xRatio > 2.0 && xRatio <= 3.0){
//                if(srcRes_x >= 1920){
//                    return true;
//                }
//            }else if(xRatio > 1.5 && xRatio <= 2.0){
//                if(srcRes_x >= 1280){
//                    return true;
//                }
//            }else if(xRatio > 1.0 && xRatio <= 1.5){
//                return true;
//            }
//            return false;
//        }else{
//            return true; // upswitch or same res
//        }
*/
    }

    double normalizedDistanceFromTargetQuality(double targetQuality, double distance){
        ArrayList<Double> weightsArr = this._imosWeightsArray;

        double tmp = Math.ceil(targetQuality);
        int weightArrayAnchor = (int)tmp;

        if ((distance <= 0.0) || (weightArrayAnchor > this._imosWeightsArray.size())) {
            if (distance == 0) {
                // log("Input distance is Zero")
            }
            return 0;
        }

        double normalizedDistance = 0.0;
        double offset = distance;
        if (distance > 1) {
            offset = (double)(weightArrayAnchor) - targetQuality;
        }

        normalizedDistance = weightsArr.get(weightArrayAnchor-1) * offset;
        distance -= offset;

        for (;distance > 0 && weightArrayAnchor <= 4;) {
            if (distance > 1) {
                distance--;
                normalizedDistance += weightsArr.get(weightArrayAnchor);
                weightArrayAnchor = weightArrayAnchor + 1;
            } else {
                normalizedDistance += distance * weightsArr.get(weightArrayAnchor);
                weightArrayAnchor = weightArrayAnchor + 1;
                distance = 0;
            }
        }
        return normalizedDistance;
    }


    private class TrackDistanceAndIndexInArray implements Comparable{
        public TrackDistanceAndIndexInArray(){
            index = -1;
            qualDistance = 0;
        }

        @Override
        public boolean equals(Object obj) {
            return (((TrackDistanceAndIndexInArray) obj).qualDistance == qualDistance);
        }

        @Override
        public int compareTo(Object o) {
            TrackDistanceAndIndexInArray e = (TrackDistanceAndIndexInArray) o;
            return (e.qualDistance > qualDistance)?-1:1;
        }

        int index;
        double qualDistance;
    }

    private boolean IsValidUpSwitchDownSwitch(int trackIdx, int targetTrackIdx, int maxStepsUp, int maxStepsDown){
        boolean retval = false;        
        if (trackIdx > targetTrackIdx){//downswitch
            if ((trackIdx - targetTrackIdx) <= maxStepsDown){
                retval = true;
            }
        }else{
            if ((targetTrackIdx - trackIdx) <= maxStepsUp){//upswitch
                retval = true;
            }
        }
        return retval;
    }
    
    final int MaxAllowedMOSDrop = 8; // Also Consider Max Diff in imos quality
    
    private boolean IsDownSwitchPermitted(double downSwitchDistance, double validDownSwitchLimit){
        int downSwitchDistanceInt = (int)(downSwitchDistance * 100); //Compare max till 2 decimals, 1 would have been better as well
        int validDownSwitchLimitInt = (int)(validDownSwitchLimit * 100);
        return (downSwitchDistanceInt < validDownSwitchLimitInt);
    }

    private int findIndexBasedOnQuality(int trackIdx, int maxStepsUp, int maxStepsDown, double [] frame_rates, double [] imos_arr, int [] bandwidth_arr, int[] res_arr_x, int[] res_arr_y, double targetQuality, int resolutionRangeStart, int resolutionRangeEnd, boolean xResolutionSwitchIndex) {
        boolean xresSwitchEnabled = xResolutionSwitchIndex;
        int quality = trackIdx;

        if(trackIdx >= imos_arr.length){
            return trackIdx;
        }

        if(maxStepsUp < 0){
            maxStepsUp = 0; // do not step up
        }
        
        if(maxStepsDown < 0){
            maxStepsDown = 0; // do not step down
        }

        if(maxStepsUp == 0 && maxStepsDown == 0){ //no switching
            return trackIdx;
        }

        int startingIndex = trackIdx;
        if ((startingIndex - maxStepsDown) > 0) {
            startingIndex = startingIndex - maxStepsDown;
        } else {
            startingIndex = 0;
        }

        int endingIndex = trackIdx;
        if ((endingIndex + maxStepsUp) < imos_arr.length) {
            endingIndex = endingIndex + maxStepsUp;
        } else {
            endingIndex = imos_arr.length - 1;
        }

        int higherTrackStartIndex = trackIdx + 1;
        ArrayList<TrackDistanceAndIndexInArray> mosDistEntries = new ArrayList<TrackDistanceAndIndexInArray>();

        for (int iter = startingIndex; iter <= trackIdx; iter++) {
            //If we have a lower bitrate than the active bitrate, whose qual score is more than the current track's target bitrate, pick the loweset bitrate with qual more than target qual of the track
            if (imos_arr[iter] >= targetQuality) {
                //MMLogger.log("VERBOSE", "Lower bitrate" + iter + "with better or equal quality" + imos_arr[iter] + " than target" + targetQuality);
                //We go a candidate, but... We need to see even lower bitrate candidate, if this bitrate is not exactly same as target bitrate, and some lower bitrate is more closer to target
            }

            if (imos_arr[iter] == targetQuality) {
                //MMLogger.log("VERBOSE", "Lower bitrate" + iter + " with equal quality " + imos_arr[iter] + " as target " + targetQuality);
                return iter;
            } else if (imos_arr[iter] > targetQuality) {
                //MMLogger.log("VERBOSE", "Lower bitrate" + iter + " with equal quality " + imos_arr[iter] + " as target " + targetQuality);
                if (0 == mosDistEntries.size()) {
                    //MMLogger.log("VERBOSE", "Lower bitrate" + iter + " with higher quality " + imos_arr[iter] + " than target " + targetQuality + " But no lower bitrate to compare against. Let's choose it right away");
                    return iter;
                }

                Collections.sort(mosDistEntries);

                // We could not find lower bitrate absolute solution, let us pick the lower bitrate candidate (bitrate with mos closest to target mos)
                TrackDistanceAndIndexInArray lowerBitrateCandidate = mosDistEntries.get(0);

                TrackDistanceAndIndexInArray higherBitrateMOSCandidate = new TrackDistanceAndIndexInArray();
                higherBitrateMOSCandidate.index = iter;
                higherBitrateMOSCandidate.qualDistance = (double) (imos_arr[iter] - targetQuality);

                //MMLogger.log("VERBOSE", "Distances Before Normalisation" + lowerBitrateCandidate.mosDistance + ":" + higherBitrateMOSCandidate.mosDistance);

                double lowerQualityClosestNeighbourDist = this.normalizedDistanceFromTargetQuality((double) (targetQuality)/10, lowerBitrateCandidate.qualDistance/10);
                double betterQualityClosestNeighbourDist = this.normalizedDistanceFromTargetQuality((double)(targetQuality)/10, higherBitrateMOSCandidate.qualDistance/10);

                if (lowerQualityClosestNeighbourDist <= betterQualityClosestNeighbourDist && this.IsDownSwitchPermitted(lowerQualityClosestNeighbourDist, MaxAllowedMOSDrop)) {
                    return lowerBitrateCandidate.index;
                }
                return higherBitrateMOSCandidate.index;
            } else {
                TrackDistanceAndIndexInArray obj = new TrackDistanceAndIndexInArray();
                obj.index = iter;
                obj.qualDistance =  (double) (targetQuality - imos_arr[iter]);
                mosDistEntries.add(obj);
            }
        }

        Collections.sort(mosDistEntries);

        // We could not find lower bitrate absolute solution, let us pick the lower bitrate candidate (bitrate with mos closest to target mos)
        TrackDistanceAndIndexInArray lowerBitrateCandidate = mosDistEntries.get(0);

        //Process the mos array to have increasing mos scores always
        ArrayList<Double> moscore_upswitches = new ArrayList<Double>();

        for (int iter = higherTrackStartIndex; iter <= endingIndex; iter++) {
            moscore_upswitches.add(imos_arr[iter]);
        }

        for (int outeriter = higherTrackStartIndex; outeriter <= endingIndex; outeriter++) {
            for (int inneriter = outeriter + 1; inneriter <= endingIndex; inneriter++) {
                if (moscore_upswitches.get(inneriter - higherTrackStartIndex) < moscore_upswitches.get(outeriter - higherTrackStartIndex)) {
                    moscore_upswitches.set((inneriter - higherTrackStartIndex),moscore_upswitches.get(outeriter - higherTrackStartIndex));
                }
            }
        }

        ArrayList<TrackDistanceAndIndexInArray> mosDistEntriesGt = new ArrayList<TrackDistanceAndIndexInArray>();
        for (int iter = higherTrackStartIndex; iter <= endingIndex; iter++) {
            TrackDistanceAndIndexInArray obj = new TrackDistanceAndIndexInArray();
            obj.index = iter;
            obj.qualDistance =  Math.abs((double) (imos_arr[iter] - targetQuality));
            mosDistEntriesGt.add(obj);
        }

        Collections.sort(mosDistEntriesGt);

        if (mosDistEntriesGt.size() > 0) {
            TrackDistanceAndIndexInArray higherBitrateMOSCandidate = mosDistEntriesGt.get(0);
            //Tug of War
            //MMLogger.log("VERBOSE","Tug of war between "+ lowerBitrateCandidate.index+ " vs " + higherBitrateMOSCandidate.index);

            double lowerQualityClosestNeighbourDist = this.normalizedDistanceFromTargetQuality((double)(targetQuality)/10, lowerBitrateCandidate.qualDistance/10);
            double betterQualityClosestNeighbourDist = this.normalizedDistanceFromTargetQuality((double)(targetQuality)/10, higherBitrateMOSCandidate.qualDistance/10);

            if (lowerQualityClosestNeighbourDist <= betterQualityClosestNeighbourDist && (lowerBitrateCandidate.index == trackIdx || this.IsDownSwitchPermitted(lowerQualityClosestNeighbourDist, MaxAllowedMOSDrop))) {
                return lowerBitrateCandidate.index;
            }
            return higherBitrateMOSCandidate.index;
        }
        return lowerBitrateCandidate.index;
        // End of findIndexBasedOnQuality
    }

    private int UpdateSegmentiMOSEntryForTargetQuality(int trackIdx, MMQFQubitVideoTrack videoTrack, int index)
    {
        double [] framerates = new double[qubitModelSz];
        int [] bitrates = new int[qubitModelSz];
        int [] res_arr_x = new int[qubitModelSz];
        int [] res_arr_y = new int[qubitModelSz];
        double [] imos_arr = new double[qubitModelSz];
        boolean enableXRes = true;
        for(int i = 0; i < qubitModelSz; i++)
        {
            imos_arr[i] = qubitModel.get(i).cbrMetadata.mediaAttributes.imosValues.get(index).doubleValue();
            res_arr_x[i] = qubitModel.get(i).cbrMetadata.mediaAttributes.displayWidth;
            res_arr_y[i] = qubitModel.get(i).cbrMetadata.mediaAttributes.displayHeight;
            bitrates[i] = qubitModel.get(i).videoTrackInfo.bitrate; //qubitModel.get(i).cbrMetadata.mediaAttributes.averageBitRate;
            framerates[i] = qubitModel.get(i).cbrMetadata.mediaAttributes.frameRate;
        }
        int chosenSegmentCBRId = findIndexBasedOnQuality(trackIdx, registrationResponse.maxStepsUp, registrationResponse.maxStepsDown, framerates, imos_arr, bitrates, res_arr_x, res_arr_y, videoTrack.cqMetadata.targetiMOS, -1, -1, enableXRes);
        if(qubitMode == MMQFQubitConfigurationInterface.QubitMode_CostSave && chosenSegmentCBRId > trackIdx){
            chosenSegmentCBRId = trackIdx;
        }
        videoTrack.cqMetadata.imosValues.add(qubitModel.get(chosenSegmentCBRId).cbrMetadata.mediaAttributes.imosValues.get(index));
        return chosenSegmentCBRId;
    }

    public MMQFQubitStatisticsInterface.MMQFSegmentQualityInfo getSegmentQualityInfo(MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segmentInfo)
    {
        MMQFQubitStatisticsInterface.MMQFSegmentQualityInfo info = null;
        if(segmentInfo!=null && segmentInfo.videoTrackInfo != null && segmentInfo.segmentIndex >=0) {
            MMQFQubitVideoTrack qubitVideoTrack = null;
            for (int i = 0; i < qubitModelSz; i++) {
                qubitVideoTrack = (MMQFQubitVideoTrack) qubitModel.get(i);
                if (qubitVideoTrack != null && ((qubitVideoTrack.videoTrackInfo.bitrate == segmentInfo.videoTrackInfo.bitrate) && qubitVideoTrack.videoTrackInfo.height == segmentInfo.videoTrackInfo.height)) {
                    break;
                }
                if (i == (qubitModelSz - 1)) {
                    //could not found matching entry
                    assert (false);
                    qubitVideoTrack = null;
                }
            }
            if (qubitVideoTrack != null) {
                if( (qubitVideoTrack.cqMetadata.constitutingVideoSegmentSrcTrack.size()-1) < segmentInfo.segmentIndex)
                {
                    info = new MMQFQubitStatisticsInterface.MMQFSegmentQualityInfo();
                    info.qubitizedSegmentQuality =  qubitVideoTrack.cqMetadata.targetiMOS;
                    info.requestedSegmentQuality = qubitVideoTrack.cqMetadata.targetiMOS;
                    return info;
                }

                int qubitTrackToUse = segmentInfo.qbrTrackIndex; // @rupesh pls Review Here ((Integer) (qubitVideoTrack.cqMetadata.constitutingVideoSegmentSrcTrack.get(segmentInfo.segmentIndex))).intValue();
                MMQFPresentationVideoTrackInfo qubitTrack = ((MMQFQubitVideoTrack) (qubitModel.get(qubitTrackToUse))).videoTrackInfo;
                info = new MMQFQubitStatisticsInterface.MMQFSegmentQualityInfo();
                info.qubitizedSegmentQuality = ((Double)(qubitVideoTrack.cqMetadata.imosValues.get(segmentInfo.segmentIndex))).doubleValue();
                info.requestedSegmentQuality = ((Double)(qubitVideoTrack.cbrMetadata.mediaAttributes.imosValues.get(segmentInfo.segmentIndex))).doubleValue();
                info.profileId = qubitTrack.trackIndex;
            }
        }
        return info;
    }

    public MMQFQubitStatisticsInterface.MMQFSegmentSizeInfo getAverageSegmentSizeInfo(MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segmentInfo) {
        MMQFQubitStatisticsInterface.MMQFSegmentSizeInfo info = null;
        if(segmentInfo!=null && segmentInfo.videoTrackInfo != null && segmentInfo.segmentIndex >=0) {
            MMQFQubitVideoTrack qubitVideoTrack = null;
            for (int i = 0; i < qubitModelSz; i++) {
                qubitVideoTrack = (MMQFQubitVideoTrack) qubitModel.get(i);
                if (qubitVideoTrack != null && (qubitVideoTrack.videoTrackInfo.bitrate == segmentInfo.videoTrackInfo.bitrate)) {
                    break;
                }
                if (i == (qubitModelSz - 1)) {
                    //could not found matching entry
                    assert (false);
                    qubitVideoTrack = null;
                }
            }
            if (qubitVideoTrack != null) {
                if( (qubitVideoTrack.cqMetadata.constitutingVideoSegmentSrcTrack.size()-1) < segmentInfo.segmentIndex)
                {
                    info = new MMQFQubitStatisticsInterface.MMQFSegmentSizeInfo();
                    if(info != null) {
                        //We can return the segment size same for both the CQ and CBR
                        info.requestedSegmentSz = segmentInfo.videoTrackInfo.getSegmentInfo(segmentInfo.segmentIndex).segmentSz;
                        info.qubitizedSegmentSz = info.requestedSegmentSz;
                        info.segmentStartTime = segmentInfo.videoTrackInfo.getSegmentInfo(segmentInfo.segmentIndex).segmentStartTime;
                        info.segmentDuration = segmentInfo.videoTrackInfo.getSegmentInfo(segmentInfo.segmentIndex).duration;
                        info.timescale = segmentInfo.videoTrackInfo.timeScale;
                    }
                    return info;
                }

                int qubitTrackToUse = segmentInfo.qbrTrackIndex; // @rupesh pls Review here ((Integer) (qubitVideoTrack.cqMetadata.constitutingVideoSegmentSrcTrack.get(segmentInfo.segmentIndex))).intValue();
                MMQFPresentationVideoTrackInfo qubitTrack = ((MMQFQubitVideoTrack) (qubitModel.get(qubitTrackToUse))).videoTrackInfo;
                info = new MMQFQubitStatisticsInterface.MMQFSegmentSizeInfo();
                MMQFPresentationVideoTrackSegmentInfo cbcSegmentInfo = qubitVideoTrack.videoTrackInfo.getSegmentInfo(segmentInfo.segmentIndex);
                if(cbcSegmentInfo != null) {
                    MMLogger.e("DebugNow", "Bitrate is "  + qubitVideoTrack.videoTrackInfo.bitrate + " timescale " + qubitVideoTrack.videoTrackInfo.timeScale);
                    info.requestedSegmentSz = (int)((double)(cbcSegmentInfo.duration * qubitVideoTrack.videoTrackInfo.bitrate)/qubitVideoTrack.videoTrackInfo.timeScale);
                    info.segmentStartTime = cbcSegmentInfo.segmentStartTime;
                    info.segmentDuration = cbcSegmentInfo.duration;
                    info.timescale = qubitVideoTrack.videoTrackInfo.timeScale;

                }
                MMQFPresentationVideoTrackSegmentInfo qubitSegmentInfo = qubitTrack.getSegmentInfo(segmentInfo.segmentIndex);
                if(qubitSegmentInfo != null)
                {
                    info.qubitizedSegmentSz = (int)((double)(qubitSegmentInfo.duration * qubitTrack.bitrate)/qubitTrack.timeScale);
                }
            }
        }
        return info;
    }

    //Some inspector methods for statistics info
    public MMQFQubitStatisticsInterface.MMQFSegmentSizeInfo getSegmentSizeInfo(MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segmentInfo)
    {
        MMQFQubitStatisticsInterface.MMQFSegmentSizeInfo info = null;
        if(segmentInfo!=null && segmentInfo.videoTrackInfo != null && segmentInfo.segmentIndex >=0) {
            MMQFQubitVideoTrack qubitVideoTrack = null;
            for (int i = 0; i < qubitModelSz; i++) {
                qubitVideoTrack = (MMQFQubitVideoTrack) qubitModel.get(i);
                if (qubitVideoTrack != null && (qubitVideoTrack.videoTrackInfo.bitrate == segmentInfo.videoTrackInfo.bitrate)) {
                    break;
                }
                if (i == (qubitModelSz - 1)) {
                    //could not found matching entry
                    assert (false);
                    qubitVideoTrack = null;
                }
            }
            if (qubitVideoTrack != null) {
                if( (qubitVideoTrack.cqMetadata.constitutingVideoSegmentSrcTrack.size()-1) < segmentInfo.segmentIndex)
                {
                    info = new MMQFQubitStatisticsInterface.MMQFSegmentSizeInfo();
                    if(info != null) {
                        //We can return the segment size same for both the CQ and CBR
                        info.requestedSegmentSz = segmentInfo.videoTrackInfo.getSegmentInfo(segmentInfo.segmentIndex).segmentSz;
                        info.qubitizedSegmentSz = info.requestedSegmentSz;
                        info.segmentStartTime = segmentInfo.videoTrackInfo.getSegmentInfo(segmentInfo.segmentIndex).segmentStartTime;
                        info.segmentDuration = segmentInfo.videoTrackInfo.getSegmentInfo(segmentInfo.segmentIndex).duration;
                        info.timescale = segmentInfo.videoTrackInfo.timeScale;

                        info.qbrBitrate = info.cbrBitrate = segmentInfo.videoTrackInfo.bitrate;
                    }
                    return info;
                }

                int qubitTrackToUse = segmentInfo.qbrTrackIndex; // @rupesh pls Review required here  ((Integer) (qubitVideoTrack.cqMetadata.constitutingVideoSegmentSrcTrack.get(segmentInfo.segmentIndex))).intValue();
                MMQFPresentationVideoTrackInfo qubitTrack = ((MMQFQubitVideoTrack) (qubitModel.get(qubitTrackToUse))).videoTrackInfo;
                info = new MMQFQubitStatisticsInterface.MMQFSegmentSizeInfo();
                MMQFPresentationVideoTrackSegmentInfo cbcSegmentInfo = qubitVideoTrack.videoTrackInfo.getSegmentInfo(segmentInfo.segmentIndex);
                if(cbcSegmentInfo != null) {
                    info.requestedSegmentSz = cbcSegmentInfo.segmentSz;
                    info.segmentStartTime = cbcSegmentInfo.segmentStartTime;
                    info.segmentDuration = cbcSegmentInfo.duration;
                    info.timescale = qubitVideoTrack.videoTrackInfo.timeScale;
                    info.cbrBitrate = qubitVideoTrack.videoTrackInfo.bitrate;
                }
                MMQFPresentationVideoTrackSegmentInfo qubitSegmentInfo = qubitTrack.getSegmentInfo(segmentInfo.segmentIndex);
                if(qubitSegmentInfo != null)
                {
                    info.qubitizedSegmentSz = qubitSegmentInfo.segmentSz;
                    info.qbrBitrate = qubitTrack.bitrate;
                }
            }
        }
        return info;
    }

    public ArrayList<MMQFQubitMetadataFileParser.VideoTrackMediaAttributes> getMetatDataArrayList() {
        return metadataFileParser.getVideoTracksMetaDataArrayList();
    }
    public String getAudioCodecInfo() {
        return metadataFileParser.getAudioCodec();
    }
    public String getVideoCodecInfo() {
        return metadataFileParser.getVideoCodec();
    }

    void LogQubitModel(){
        //Target Quality Levels

        MMLogger.p("LogQubitModel", "Target Quality Levels");
        for(int i=0; i<qubitModelSz; i++){
            MMQFQubitVideoTrack track = qubitModel.get(i);
            MMLogger.p("LogQubitModel", "Bitrate: " + track.videoTrackInfo.bitrate + " target imos: " + track.cqMetadata.targetiMOS);
        }
        //Transformation Matrix
        MMLogger.p("LogQubitModel", "Transformation Matrix");
        int noOfSegments = qubitModel.get(0).videoTrackInfo.trackSegmentInfoVect.size();
        if(noOfSegments>commonMetadata.noOfSegments){
            noOfSegments = commonMetadata.noOfSegments;
        }
        String line = new String();
        for(int j = 0; j< noOfSegments; j++){
            line = "";
            line += "Segment No :[" + j  + "]" + '\t' + ":, ";
            for(int i=0; i<qubitModelSz; i++){
                MMQFQubitVideoTrack track = qubitModel.get(i);
                line += track.cqMetadata.constitutingVideoSegmentSrcTrack.get(j);
                line += "," + '\t';
            }
            MMLogger.d("LogQubitModel-QBR", line);
        }
        MMLogger.p("LogQubitModel-QBR", "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        //Unbounded ACP
        /*
        MMLogger.p("LogQubitModel", "Unbounded ACP");
        for(int i=0; i<qubitModelSz; i++){
            MMQFQubitVideoTrack track = qubitModel.get(i);
            MMLogger.p("LogQubitModel", "Track Bitrate - " + track.videoTrackInfo.bitrate);
            for(int j=0; j<track.cqMetadata.acpValues.size(); j++){
                CQMetadata.acpEntry entry = track.cqMetadata.acpValues.get(j);
                MMLogger.p("LogQubitModel", "SeqNum: " + entry.seqNum + " Slope - " + entry.slope);
            }
        }

        //CBC Values
        MMLogger.p("LogQubitModel", "Bounded CBC");
        for(int i=0; i<qubitModelSz; i++){
            MMQFQubitVideoTrack track = qubitModel.get(i);
            MMLogger.p("LogQubitModel", "Track Bitrate - " + track.videoTrackInfo.bitrate);
            for(int j=0; j<track.cqMetadata.boundedCBCEntries.size(); j++){
                CQMetadata.BoundedCBCEntry entry = track.cqMetadata.boundedCBCEntries.get(j);
                MMLogger.p("LogQubitModel", "SeqNum: " + entry.seqNum + " Cumulative Bits " + entry.cumulativeBits);
            }
        }

        //ACP Values
        MMLogger.p("LogQubitModel", "Bounded ACP");
        for(int i=0; i<qubitModelSz; i++){
            MMQFQubitVideoTrack track = qubitModel.get(i);
            MMLogger.p("LogQubitModel", "Track " + track.videoTrackInfo.bitrate);
            for(int j=0; j<track.cqMetadata.boundedACPEntries.size(); j++){
                CQMetadata.BoundedACPEntry entry = track.cqMetadata.boundedACPEntries.get(j);
                MMLogger.p("LogQubitModel", "SeqNum: " + entry.seqNum + " ACP Value " + entry.acpValue);
            }
        }*/
    }

    public Integer getTrackIndex(int bitrate){
        Integer index = null;
        for (int trackIndex=0 ; trackIndex < qubitModel.size(); trackIndex++) {
            MMQFQubitVideoTrack videotrack = (MMQFQubitVideoTrack) qubitModel.get(trackIndex);
            if (videotrack.videoTrackInfo.bitrate == bitrate){
                index = new Integer(trackIndex);
                break;
            }
        }
        return index;
    }

    public int getQBRTrackIndex(int trackIndex, int sequenceIndex){
        if (trackIndex!= -1 && trackIndex < qubitModel.size()) {
            MMQFQubitVideoTrack qubitVideoTrack = null;
            qubitVideoTrack = (MMQFQubitVideoTrack) qubitModel.get(trackIndex);
            if( (qubitVideoTrack.cqMetadata.constitutingVideoSegmentSrcTrack.size()-1) >= sequenceIndex)
            {
                int qubitTrackToUse = ((Integer) (qubitVideoTrack.cqMetadata.constitutingVideoSegmentSrcTrack.get(sequenceIndex))).intValue();
                return qubitTrackToUse;
            }
        }
        return -1;
    }

    public MMQFQubitStatisticsInterface.MMQFSegmentInfo getSegmentInfo(int trackIndex, int sequenceNumber){
        MMQFQubitStatisticsInterface.MMQFSegmentInfo segmentInfo = null;
        if (qubitModel!= null && trackIndex != -1 && trackIndex < qubitModel.size()) {
            MMQFQubitVideoTrack qubitTrack = ((MMQFQubitVideoTrack) (qubitModel.get(trackIndex)));
            segmentInfo = new MMQFQubitStatisticsInterface.MMQFSegmentInfo();
            segmentInfo.mosScore = ((Double)(qubitTrack.cbrMetadata.mediaAttributes.imosValues.get(sequenceNumber))).doubleValue();
            segmentInfo.profileIdx = trackIndex;
            segmentInfo.segSize = qubitTrack.videoTrackInfo.getSegmentInfo(sequenceNumber).segmentSz;
            segmentInfo.segmentIndex = qubitTrack.videoTrackInfo.getSegmentInfo(sequenceNumber).segmentIndex;
            segmentInfo.segmentStartTime = (qubitTrack.videoTrackInfo.getSegmentInfo(sequenceNumber).segmentStartTime * 1000)/qubitTrack.videoTrackInfo.timeScale;
            segmentInfo.duration = (qubitTrack.videoTrackInfo.getSegmentInfo(sequenceNumber).duration * 1000) / qubitTrack.videoTrackInfo.timeScale;
            segmentInfo.trackBitrate = qubitTrack.videoTrackInfo.bitrate;
            segmentInfo.codecInformation = qubitTrack.videoTrackInfo.codecInfo;
            segmentInfo.representationWidth = qubitTrack.videoTrackInfo.width;
            segmentInfo.representationHeight = qubitTrack.videoTrackInfo.height;
        }
        return segmentInfo;
    }

    void resetRunningStatistics(){
        runningStatistics = new QubitRunningStatistics();
    }

    private  ArrayList<Double> _imosWeightsArray;

    private ArrayList<Integer> xResQBRTrackIdx;
    private int minDurationForVarianceComputation = 10000 + 2000;
    private OnQubitModelCreatedListener listener = null;
    private MMQFQubitMetadataFileParser metadataFileParser = null;
    private ArrayList<MMQFQubitVideoTrack> qubitModel;
    private ArrayList<iMOSDifferenceElem> tmpDiffArray;
    public QubitRunningStatistics runningStatistics;//Running stats of the ongoing session <Dynamic>
    private int qubitMode;
    private URL urlOfMetadataFile = null;
    private MMQFPresentationInfo presentationInfo = null;
    private RegisterResponse registrationResponse;
    private double segmentLength = 0;
    private double segmentLengthAdjusted = 0;
    private int maxSegmentsToLookUpForAdaptation = 1800; //60*60/2  = 1 hour of 2 sec segments
    private int qubitModelSz = 0;
    private int bufferedPlaybackTimeInSec = 0;
    private int playbackPosAtBufferLookUp = 0;
    MMQFQubitMetadataFileParser.CommonMetadata commonMetadata = null;
}
