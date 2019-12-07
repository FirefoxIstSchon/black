package group35;

import java.util.*;

import genius.core.Domain;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;



class Utils {



    static ArrayList<Bid> calc_all_possible_bids(Domain domain) {


        ArrayList<List<ValueDiscrete>> issue_values = new ArrayList<>();

        for (Issue issue : domain.getIssues())

            issue_values.add(((IssueDiscrete) issue).getValues());

        ArrayList<Bid> all_possible_bids = new ArrayList<>();

        HashMap<Integer, Value> bid_combination;

        for (List<ValueDiscrete> combination : getCombinations(issue_values)) {

            bid_combination = new HashMap<>();

            for (int i = 0; i < combination.size(); i++)

                bid_combination.put(i, combination.get(i));

            all_possible_bids.add(new Bid(domain, bid_combination));

        }

        return all_possible_bids;


    }



    private static <T> Set<List<T>> getCombinations(List<List<T>> lists) {

        Set<List<T>> combinations = new HashSet<>();
        Set<List<T>> newCombinations;

        int index = 0;

        // extract each of the integers in the first list
        // and add each to ints as a new list
        for(T i: lists.get(0)) {
            List<T> newList = new ArrayList<T>();
            newList.add(i);
            combinations.add(newList);
        }
        index++;
        while(index < lists.size()) {
            List<T> nextList = lists.get(index);
            newCombinations = new HashSet<List<T>>();
            for(List<T> first: combinations) {
                for(T second: nextList) {
                    List<T> newList = new ArrayList<T>(first);
                    newList.add(second);
                    newCombinations.add(newList);
                }
            }
            combinations = newCombinations;

            index++;
        }

        return combinations;
    }



}

