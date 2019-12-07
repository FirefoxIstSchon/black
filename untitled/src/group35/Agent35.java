package group35;

import java.util.*;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

/**
 * A simple example agent that makes random bids above a minimum target utility.
 *
 * @author Tim Baarslag
 */
public class Agent35 extends AbstractNegotiationParty
{
	private Bid lastOffer;
    private ArrayList<Bid> possibleBids;
	private OpponentModel opponentModel;
    private Map<Bid, Float> possibleBidsUtils;
	private PlayerModel playerModel;

    // TODO: Change Opponent Model to use Utils instead of ANAC method

	/**
	 * Initializes a new instance of the agent.
	 */
	@Override
	public void init(NegotiationInfo info)
	{
		super.init(info);

		AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
		AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;


		List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();
		opponentModel = new OpponentModel(info);
		playerModel = new PlayerModel(getDomain(), info, user);

		possibleBidsUtils = new HashMap<Bid, Float>();
		possibleBids = bidGen.BidList((AdditiveUtilitySpace) utilitySpace);

		// Fill possibleBidsUtils with our predicted utility for every possible bid
		for (int i=0;i<possibleBids.size();i++){
			possibleBidsUtils.put(possibleBids.get(i), playerModel.estimate_utility_of(possibleBids.get(i)));
		}
		possibleBidsUtils.put(playerModel.known_bids_hi_to_lo.get(0), 1f);

	}

	/**
	 * Makes a random offer above the minimum utility target
	 * Accepts anything above
	 */

	@Override
	public Action chooseAction(List<Class<? extends Action>> possibleActions)
	{
		// Check for acceptance if we have received an offer
		if (lastOffer != null)
			if (timeline.getTime() >= 0.98)
				if (playerModel.estimate_utility_of(lastOffer) >= utilitySpace.getReservationValue())  // old accept strategy
//				if (getUtility(lastOffer) >= simpleConcessionTarget())
					return new Accept(getPartyId(), lastOffer);
				else
					return new EndNegotiation(getPartyId());
		// Otherwise, send out a random offer above the target utility

		Bid mybid = maxOpUtilAboveTarget();

		return new Offer(getPartyId(), mybid);
	}



	private Bid maxOpUtilAboveTarget(){
		// Finds the bid above our range which is predicted to have highest utility for opponent
		// Only makes predictions if we already have some opponent frequencies
		if(lastOffer!=null) {
			Map<Bid, Double> bidsAndPredictions = new HashMap<Bid, Double>();
			double target = nashConcessionTarget();
			for (Bid key : possibleBidsUtils.keySet()) {
				if (possibleBidsUtils.get(key) >= target) {
					bidsAndPredictions.put(key, opponentModel.predictUtility(key));
				}
			}
			Bid bestBid = null;
			double val = 0;
			for (Bid key : bidsAndPredictions.keySet()) {
				if (bidsAndPredictions.get(key) > val) {
					bestBid = key;
					val = bidsAndPredictions.get(key);
				}
			}
			return bestBid;
		}
		else {
			return(getMaxUtilityBid());
		}
	}

	private double getNashUtil(){

			Map<Bid, Double> utilProducts = new HashMap<Bid, Double>();
			double resVal = utilitySpace.getReservationValue();
			for (Bid key : possibleBidsUtils.keySet()) {
				// Nash products excluding reservation values?
				utilProducts.put(key, (opponentModel.predictUtility(key)-resVal)*(possibleBidsUtils.get(key)-resVal));
			}

			Bid nashBid = null;
			double val = 0;
			for (Bid key : utilProducts.keySet()) {
				if (utilProducts.get(key) > val) {
					nashBid = key;
					val = utilProducts.get(key);
				}
			}
			return possibleBidsUtils.get(nashBid)-0.1;
	}


	private double nashConcessionTarget()
	{
		Bid maxUtilBid;
		double time = getTimeLine().getTime();	// Gets the time, running from t = 0 (start) to t = 1 (deadline)

		System.out.println("reservation value"+utilitySpace.getReservationValue());

		maxUtilBid = playerModel.known_bids_hi_to_lo.get(0);

		double max, min, target;
		double beta1 = 0.2;
		double beta2 = 0.5;
		max = 1;
		min = getNashUtil();

		/// Splits concession strategy into 2 parts
		// Before T = 0.9 we act hardheaded and concede to nash point. After this we concede rapidly towards reservation
		if (time < 0.9){
			// Normalize 0-1
			time = (time - 0)/(0.9);
			target = max - ((max - min)) * Math.pow(time, (1/beta1));
		}
		else{
			// Normalize 0-1
			time = (time - 0.9)/(1-0.9);
			target = min - ((min - utilitySpace.getReservationValue())) * Math.pow(time, (1/beta2));
		}
		return target;
	}

	private Bid getMaxUtilityBid() {
		return playerModel.known_bids_hi_to_lo.get(0);
	}


	/**
	 * Remembers the offers received by the opponent.
	 */
	@Override
	public void receiveMessage(AgentID sender, Action action)
	{
		if (action instanceof Offer)
		{
			lastOffer = ((Offer) action).getBid();
			opponentModel.updateOpponentModel(lastOffer);
		}
	}


	@Override
	public String getDescription()
	{
		return "I am the greatest negotiation agent";
	}

	/**
	 * This stub can be expanded to deal with preference uncertainty in a more sophisticated way than the default behavior.
	 */
	@Override
	public AbstractUtilitySpace estimateUtilitySpace()
	{
		return super.estimateUtilitySpace();
	}

}
