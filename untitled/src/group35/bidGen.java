package group35;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class bidGen {

    public static ArrayList<Bid> BidList(AdditiveUtilitySpace var0) {
        List var1 = var0.getDomain().getIssues();
        ArrayList var2 = new ArrayList();
        ArrayList var3 = new ArrayList();
        int var4 = 1;
        int var5 = var1.size();
        int[] var6 = new int[var5];

        for(int var7 = 0; var7 < var1.size(); ++var7) {
            var2.add((ArrayList)((IssueDiscrete)var1.get(var7)).getValues());
            var6[var7] = ((ArrayList)var2.get(var7)).size();
            var4 *= var6[var7];
        }

        ValueDiscrete[][] var16 = new ValueDiscrete[var4][var5];

        int var8;
        int var10;
        for(var8 = 0; var8 < var5; ++var8) {
            int var9 = 1;
            var10 = var6[var8];
            int var11 = 1;

            int var12;
            for(var12 = 0; var12 < var8; ++var12) {
                var9 *= var6[var12];
            }

            for(var12 = var5 - 1; var12 > var8; --var12) {
                var11 *= var6[var12];
            }

            for(var12 = 0; var12 < var9; ++var12) {
                for(int var13 = 0; var13 < var10; ++var13) {
                    for(int var14 = 0; var14 < var11; ++var14) {
                        var16[var14 + var10 * var11 * var12 + var13 * var11][var8] = (ValueDiscrete)((ArrayList)var2.get(var8)).get(var13);
                    }
                }
            }
        }

        for(var8 = 0; var8 < var4; ++var8) {
            HashMap var17 = new HashMap();

            for(var10 = 0; var10 < var5; ++var10) {
                var17.put(((Issue)var1.get(var10)).getNumber(), var16[var8][var10]);
            }

            try {
                Bid var18 = new Bid(var0.getDomain(), var17);
                var3.add(var18);
            } catch (Exception var15) {
                var15.printStackTrace();
            }
        }

        return var3;
    }


}
