package group35;

import agents.anac.y2015.TUDMixedStrategyAgent.BidGenerator;
import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.NegotiationInfo;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;

import java.util.*;

import static java.util.stream.Collectors.toMap;


public class OpponentModel {
    private List<Map<String,Integer>> opponentFrequency;
    private Map<Bid, Double> possibleBidsUtils;
    private ArrayList<Bid> possibleBids;
    private double[] wHat, wNorm;
    private double noBids;
    private List<Map<String,Double>> opWeightings;


    public OpponentModel(NegotiationInfo info) {

        AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
        AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;
        List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();


        opponentFrequency = new ArrayList<>();
        wHat = new double[issues.size()];
        wNorm = new double[issues.size()];
        possibleBidsUtils = new HashMap<Bid, Double>();
        noBids = 0;
        // TODO: THIS NEEDS TO BE CHANGED... uses method from anac 2015
        possibleBids = BidGenerator.BidList((AdditiveUtilitySpace) utilitySpace);

        // Initializes opponentFrequency to 0
        for (Issue issue : issues){
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
            Map<String,Integer> Issues = new HashMap<String, Integer>();

            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()){
                Issues.put(valueDiscrete.getValue(), 0);
            }
            opponentFrequency.add(Issues);
        }
    }

    public void updateOpponentModel(Bid lastBid){
        noBids ++;
        opWeightings = new ArrayList<>();

        for (int i=0; i<lastBid.getIssues().size(); i++) {

            // Increment the frequency of the value for each issue
            Map<String, Integer> freqMap = opponentFrequency.get(i);
            String val = lastBid.getValue(i + 1).toString();
            Integer frequency = freqMap.get(val);
            freqMap.put(val, frequency + 1);
            opponentFrequency.set(i, freqMap);

            // Calculate option weighting
            List<Integer> freqArr = new ArrayList<Integer>();
            Integer currentRank;

            Map<String, Integer> sorted = freqMap
                    .entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(
                            toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                    LinkedHashMap::new));

            Map<String, Double> opWeighting = new HashMap<String, Double>();
            double count = 1;
            double k = sorted.size();
            for (String key : sorted.keySet()) {
                double calc = (k - count + 1) / k;
                opWeighting.put(key, calc);
                count++;
            }
            opWeightings.add(opWeighting);
            wHat[i] = calcWhat(freqMap);
        }
        /// Now that wHat is full, we can loop through again for wNorm
        for (int i=0; i<lastBid.getIssues().size(); i++){
            wNorm[i] = wHat[i]/sum(wHat);
//				System.out.println(wNorm[i]);
        }
        // TODO: normalize opWeighting?????
    }

    private double calcWhat(Map<String,Integer> issue){
        double calc = 0;
        for (Map.Entry<String,Integer> entry : issue.entrySet()) {
            Double value = (double)entry.getValue();
            calc = calc + (value*value/noBids);
        }
        return calc;
    }

    private double sum(double[] What){
        double calc = 0;
        for (int i=0; i<What.length; i++){
            calc = calc + What[i];
        }
        return calc;
    }

    public double predictUtility(Bid aBid){
        double calc = 0;
        Map<Integer, Value> selectedOptions = aBid.getValues();
        for (int i=0; i<wNorm.length; i++){
            Map<String,Double> theseOptions = opWeightings.get(i);
            String opForThisIssue = selectedOptions.get(i+1).toString();
            calc = calc + (wNorm[i] * theseOptions.get(opForThisIssue));
        }
        return calc;
    }

}
