package group35;

import java.io.*;
import java.util.*;

import genius.core.Domain;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.User;
import genius.core.uncertainty.UserModel;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.Value;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;

//import scpsolver.constraints.LinearEqualsConstraint;
//import scpsolver.constraints.LinearBiggerThanEqualsConstraint;
//import scpsolver.constraints.LinearSmallerThanEqualsConstraint;

//import scpsolver.problems.LinearProgram;
//import scpsolver.lpsolver.SolverFactory;
//import scpsolver.lpsolver.LinearProgramSolver;

import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;


class PlayerModel {



    List<Issue> issues;

    UserModel user_model;

    User user;

    List<Bid> known_bids_hi_to_lo;

    HashMap<ValueDiscrete, Double> OptionWeightings;


    private String epsilon = "0.00001";



    PlayerModel(Domain domain, NegotiationInfo info, User user) {


        this.issues = domain.getIssues();

        this.user_model = info.getUserModel();

        this.user = user;

        this.known_bids_hi_to_lo = user_model.getBidRanking().getBidOrder();

        Collections.reverse(this.known_bids_hi_to_lo);

        calc_phis();


    }



    float estimate_utility_of(Bid bid) {


        float estimated_utility = 0;

        for (Value value : bid.getValues().values())

            estimated_utility += OptionWeightings.get((ValueDiscrete) value);

        return estimated_utility;


    }



    boolean try_elicit_from_user(Bid bid) {


        if (!known_bids_hi_to_lo.contains(bid)) {

            user_model = user.elicitRank(bid, user_model);

            known_bids_hi_to_lo = user_model.getBidRanking().getBidOrder();

            Collections.reverse(known_bids_hi_to_lo);

            calc_phis();

            return true;

        }

        return false;


    }



    void calc_phis() {


        HashMap<ValueDiscrete, String> phis = new HashMap<>();
        HashMap<String, ValueDiscrete> phis_reversed = new HashMap<>();
        ArrayList<String>              just_phis = new ArrayList<>();

        String phi;
        int phi_ctr = 0;

        for (Issue issue : issues)

            for (ValueDiscrete value : ((IssueDiscrete) issue).getValues()) {

                phi = "phi"+phi_ctr;

                phis.put(value, phi);
                phis_reversed.put(phi, value);
                just_phis.add(phi);

                phi_ctr++;

            }

        Bid    bid_hi, bid_lo;
        String phi_hi, phi_lo;

        String equation, slack;

        String[] slacks = new String[known_bids_hi_to_lo.size()-1];
        String[] equations = new String[known_bids_hi_to_lo.size()-1 + slacks.length + 1 + just_phis.size()];

        for (int bid_ctr = 0; bid_ctr < known_bids_hi_to_lo.size()-1; bid_ctr++) {

            bid_hi = known_bids_hi_to_lo.get(bid_ctr);
            bid_lo = known_bids_hi_to_lo.get(bid_ctr+1);

            equation = "";

            for (Issue issue : issues) {

                phi_hi = phis.get((ValueDiscrete) bid_hi.getValue(issue));
                phi_lo = phis.get((ValueDiscrete) bid_lo.getValue(issue));

                equation += "+ "+phi_hi+" ";
                equation += "- "+phi_lo+" ";

            }

            slack = "slack"+bid_ctr;

            equation += "+ "+slack+" ";

            slacks[bid_ctr] = slack;

            equation += "> " + epsilon;

            equations[bid_ctr] = equation;

        }

        int slack_ctr = 0;

        for (String s : slacks) {

            equations[known_bids_hi_to_lo.size()-1 +slack_ctr] = "+ "+s+" > 0";

            slack_ctr++;

        }

        Bid highest_bid = known_bids_hi_to_lo.get(0);

        equation = "";

        for (Issue issue : issues) {

            phi = phis.get((ValueDiscrete) highest_bid.getValue(issue));

            equation += "+ "+phi+" ";

        }

        equation += "= 1";

        equations[known_bids_hi_to_lo.size()-1+slacks.length] = equation;

        phi_ctr = 0;

        for (String s : just_phis) {

            equations[known_bids_hi_to_lo.size()-1+slacks.length+1 +phi_ctr] = "+ "+s+" > 0";

            phi_ctr++;

        }

        //

        int hm_phis = just_phis.size();
        int hm_slacks = slacks.length;
        int hm_phis_and_slacks = hm_phis + hm_slacks;

        String[] str_arr;
        String str, str2;
        int str_ctr;

        int variable_index;

        double[] current_eq_converted;

        // create linear program

        //LinearProgram linearProgram;

        LinearObjectiveFunction linearProgram;

        current_eq_converted = new double[hm_phis_and_slacks];

        for (slack_ctr = 0; slack_ctr < hm_slacks; slack_ctr++)

            current_eq_converted[hm_phis+slack_ctr]++;

        //linearProgram = new LinearProgram(current_eq_converted);

        linearProgram = new LinearObjectiveFunction(current_eq_converted, 0);

        //linearProgram.setMinProblem(true);

        // add equations

        Collection<LinearConstraint> constraints_array = new ArrayList<LinearConstraint>();

        for (int equation_ctr = 0; equation_ctr < equations.length; equation_ctr++) {

            equation = equations[equation_ctr];

            str_arr = equation.split(" ");

            current_eq_converted = new double[hm_phis_and_slacks];

            str_ctr = 0;

            while (str_ctr < str_arr.length) {

                str = str_arr[str_ctr];

                str2 = str_arr[str_ctr+1];

                switch (str) {

                    case "+":

                        variable_index = try_get_index_of_phi(str2, just_phis);

                        if (variable_index == -1) {

                            variable_index = try_get_index_of_slack(str2, slacks);

                            current_eq_converted[hm_phis + variable_index]++;

                        } else {

                            current_eq_converted[variable_index]++;

                        }

                        break;

                    case "-":

                        variable_index = try_get_index_of_phi(str2, just_phis);

                        if (variable_index == -1) {

                            variable_index = try_get_index_of_slack(str2, slacks);

                            current_eq_converted[hm_phis + variable_index]--;

                        } else {

                            current_eq_converted[variable_index]--;

                        }

                        break;

                    case ">":

                        //linearProgram.addConstraint(new LinearBiggerThanEqualsConstraint(current_eq_converted, Double.parseDouble(str2), "c" + equation_ctr));
                        constraints_array.add(new LinearConstraint(current_eq_converted, Relationship.GEQ, Double.parseDouble(str2)));
                        break;

                    case "<":

                        //linearProgram.addConstraint(new LinearSmallerThanEqualsConstraint(current_eq_converted, Double.parseDouble(str2), "c" + equation_ctr));
                        constraints_array.add(new LinearConstraint(current_eq_converted, Relationship.LEQ, Double.parseDouble(str2)));
                        break;

                    case "=":

                        //linearProgram.addConstraint(new LinearEqualsConstraint(current_eq_converted, Double.parseDouble(str2), "c" + equation_ctr));
                        constraints_array.add(new LinearConstraint(current_eq_converted, Relationship.EQ, Double.parseDouble(str2)));
                        break;

                    default:

                        System.out.println("Unknown items in equations: " + str + " " + str2);

                }

                str_ctr +=2;

            }

        }

        // solve

        //LinearProgramSolver obj = SolverFactory.newDefault();

        SimplexSolver obj = new SimplexSolver();

        //double[] solutions = obj.solve(linearProgram);

        PointValuePair solutions_obtained = obj.optimize(new MaxIter((int) 1e5), linearProgram, new LinearConstraintSet(constraints_array), GoalType.MINIMIZE, new NonNegativeConstraint(true));

        double[] solutions = new double[hm_phis];

        for (phi_ctr = 0; phi_ctr < hm_phis; phi_ctr++)

            solutions[phi_ctr] = solutions_obtained.getKey()[phi_ctr];

        assert solutions_obtained.getKey().length == hm_phis_and_slacks; // todo; remove when sure.

        // save

        OptionWeightings = new HashMap<>();

        ValueDiscrete phi_value;
        double phi_weight;

        for (phi_ctr = 0; phi_ctr < hm_phis; phi_ctr++) {

            phi = just_phis.get(phi_ctr);

            phi_value = phis_reversed.get(phi);

            phi_weight = solutions[phi_ctr];

            OptionWeightings.put(phi_value, phi_weight);

        }

        // cleanup_phis();

//        // print known offers
//
//        System.out.println("Known bids:");
//
//        for (Bid bid : known_bids_hi_to_lo) {
//
//            System.out.println("Bid: ");
//
//            for (Value value : bid.getValues().values())
//
//                System.out.println("\t" + ((ValueDiscrete) value).getValue());
//
//        }
//
//        // print phis
//
//        System.out.println("Phis:");
//
//        for (Issue issue : issues)
//
//            for (ValueDiscrete value : ((IssueDiscrete) issue).getValues())
//
//                System.out.println("Value: " + value.getValue() + ", Phi: " + OptionImportances.get(value));
//
//        System.out.println("Slacks:");
//
//        for (slack_ctr = 0; slack_ctr < hm_slacks; slack_ctr++)
//
//            System.out.println("Slack: " + slacks[slack_ctr] + ", Slack: " + solutions[hm_phis+slack_ctr]);
//
//        for (Bid bid : known_bids_hi_to_lo)
//
//            System.out.println(bid.getValues() + " " + estimate_utility_of(bid));


    }



    int try_get_index_of_phi(String phi, ArrayList<String> phis) {


        return phis.indexOf(phi);


    }



    int try_get_index_of_slack(String slack, String[] slacks) {


        for (int slack_ctr = 0; slack_ctr < slacks.length; slack_ctr++)

            if (slacks[slack_ctr].equals(slack))

                return slack_ctr;

        return -1;


    }



    void cleanup_phis() {


        double[] avgs = new double[issues.size()];

        Double phi;

        float total, div;

        int issue_ctr = 0;

        for (Issue issue : issues) {

            total = 0;

            div = 0;

            for (ValueDiscrete value : ((IssueDiscrete) issue).getValues()) {

                phi = OptionWeightings.get(value);

                if (phi != null) {

                    total += phi;

                    div++;

                }

            }

            avgs[issue_ctr] = total/div;

            issue_ctr++;

        }

        issue_ctr = 0;

        for (Issue issue : issues) {

            for (ValueDiscrete value : ((IssueDiscrete) issue).getValues())

                if (OptionWeightings.get(value) != null)

                    OptionWeightings.put(value, avgs[issue_ctr]);

            issue_ctr++;

        }


    }



}

