package nala.nal;

import java.util.ArrayList;

import nala.Setting;

public final class TruthFunctions {
    public static Setting setting;

    public final static void set_setting(Setting setting1) {
        setting = setting1;
    }
    /**
     * A function where the output is conjunctively determined by the inputs
     * @param arr The inputs, each in [0, 1]
     * @return The output that is no larger than each input
     */
    public final static double and(final double... arr) {
        float product = 1;
        for (final double f : arr) {
            product *= f;
        }
        return product;
    }
    
    /**
     * A function where the output is disjunctively determined by the inputs
     * @param arr The inputs, each in [0, 1]
     * @return The output that is no smaller than each input
     */
    public final static float or(final float... arr) {
        float product = 1;
        for (final float f : arr) {
            product *= (1 - f);
        }
        return 1 - product;
    }

    public final static double or(final double... arr) {
        double product = 1;
        for (final double f : arr) {
            product *= (1 - f);
        }
        return 1 - product;
    }

    public final static double de_or(final double arr1, final double arr2) {
        //double product = 1;
        //product *= (1 - arr1) * (1 - arr2); //arr1 = 1 - (1 - narr1)/(1 - arr2)     1 - narr1 = (1 - arr1) * (1 - arr2)
        //return 1 - product;
        // arr1 = 1 - (1 - arr2)*(1 - narr1)  //  (1 - arr2)*(1 - narr1) = 1 - arr1 // (1 - narr1) = (1 - arr1)/(1 - arr2)
        if (arr2 == 1)
            return 0;
        else
            return 1 - (1 - arr1)/(1 - arr2);
    }

    /**
     * A function to convert weight to confidence
     * @param w Weight of evidence, a non-negative real number
     * @param narParameters parameters of the reasoner
     * @return The corresponding confidence, in [0, 1)
     */
    public final static double w2c(final double w) {
        return w / (w + TruthValue.HORIZON);
    }
    
    /**
     * A function to convert confidence to weight
     * @param c confidence, in [0, 1)
     * @param narParameters parameters of the reasoner
     * @return The corresponding weight of evidence, a non-negative real number
     */
    public final static double c2w(final double c) {
        if(c >= 1){
            return TruthValue.HORIZON * 10000;
        }
        return TruthValue.HORIZON * c / (1 - c);
    }

    /* ----- double argument functions, called in MatchingRules ----- */
    /**
     * {&lt;S ==&gt; P&gt;, &lt;S ==&gt; P&gt;} |- &lt;S ==&gt; P&gt;
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static final TruthValue probabilistic_revision_new(final TruthValue v1, final TruthValue v2) {
        return probabilistic_revision_new(v1, v2, new TruthValue());
    }

    public static final TruthValue probabilistic_revision(final TruthValue v1, final TruthValue v2) {
        if(setting.all_revision)
            return revision(v1, v2, new TruthValue());
        else
            return probabilistic_revision(v1, v2, new TruthValue());
    }

    public static final TruthValue paris_revision(final TruthValue v1, final TruthValue v2) {
        return paris_revision(v1, v2, new TruthValue());
    }

    public static final TruthValue revision(TruthValue v1, TruthValue v2) {
        if(setting.all_prob_revision)
            return probabilistic_revision(v1, v2, new TruthValue());
        else
            return revision(v1, v2, new TruthValue());
    }

    public static final TruthValue probabilistic_de_revision(final TruthValue v1, final TruthValue v2) {
        if(setting.all_revision)
            return de_revision(v1, v2, new TruthValue());
        else
            return probabilistic_de_revision(v1, v2, new TruthValue());
    }
    
    /**
    private static final TruthValue revision(final TruthValue v1, final TruthValue v2, final TruthValue result) {

        result.setFrequency(1);
        result.setConfidence(or(v1.getConfidence(), v2.getConfidence()));
        return result;
    }
    */
    
    //xch2.1设想：两阶段式真值函数 动态函数性
    private static final TruthValue probabilistic_revision_new(final TruthValue v1, final TruthValue v2, final TruthValue result) {
        final float f1 = v1.getFrequency();
        final float f2 = v2.getFrequency();
        //if(v1.getConfidence() >= 1 || v2.getConfidence() >= 1){
        //    result.setFrequency( or(f1,f2));
        //    result.setConfidence(1);
        //    return result;
        //}
        final double c1 = v1.getConfidence();
        final double c2 = v2.getConfidence();
        final double w1 = c2w(v1.getConfidence());
        final double w2 = c2w(v2.getConfidence());
        final double w = w1 + w2;
        result.setFrequency( or(f1*c1,f2*c2));
        result.setConfidence( w2c(w) );
        return result;
    }

    private static final TruthValue probabilistic_revision(final TruthValue v1, final TruthValue v2, final TruthValue result) {
        final float f1 = v1.getFrequency();
        final float f2 = v2.getFrequency();
        final double w1 = c2w(v1.getConfidence());
        final double w2 = c2w(v2.getConfidence());
        final double w = w1 + w2;
        result.setFrequency( or(f1,f2));
        result.setConfidence( w2c(w) );
        return result;
    }

    private static final TruthValue paris_revision(final TruthValue v1, final TruthValue v2, final TruthValue result) {
        final double c1 = v1.getConfidence();
        final double c2 = v2.getConfidence();
        result.setFrequency( 1 );
        result.setConfidence( or(c1,c2) );
        return result;
    }

    private static final TruthValue revision(TruthValue v1, TruthValue v2, TruthValue result) {
        float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        //if(v1.getConfidence() >= 1 || v2.getConfidence() >= 1){
        //    result.setFrequency( or(f1,f2));
        //    result.setConfidence(1);
        //    return result;
        //}
        double w1 = c2w(v1.getConfidence());
        double w2 = c2w(v2.getConfidence());
        double w = w1 + w2;
        if (w == 0)
            result.setFrequency(0);
        else
            result.setFrequency( (float)((w1 * f1 + w2 * f2) / w) );
        result.setConfidence( w2c(w) );
        return result;
    }
    
    // assume that 
    // v3 =  probabilistic_revision(v1, v2);

    // v1 =  probabilistic_de_revision(v3, v2);
    private static final TruthValue probabilistic_de_revision_new(final TruthValue v3, final TruthValue v2, final TruthValue result) {
        final float f3 = v3.getFrequency();
        final float f2 = v2.getFrequency();
        final double c3 = v3.getConfidence();
        final double c2 = v2.getConfidence();
        final double w3 = c2w(c3);
        final double w2 = c2w(c2);
        final double w = w3 - w2;
        final double c1 = w2c(w);
        //除零
        result.setFrequency( de_or(f3,f2*c2)/c1);
        result.setConfidence( c1 );
        return result;
    }

    // assume that 
    // v3 =  probabilistic_revision(v1, v2);

    // v1 =  probabilistic_de_revision(v3, v2);
    private static final TruthValue probabilistic_de_revision(final TruthValue v3, final TruthValue v2, final TruthValue result) {
        final float f3 = v3.getFrequency();
        final float f2 = v2.getFrequency();
        final double c3 = v3.getConfidence();
        final double c2 = v2.getConfidence();
        final double w3 = c2w(c3);
        final double w2 = c2w(c2);
        final double w1 = w3 - w2;
        final double c1 = w2c(w1);
        if (w1 == 0)
            result.setFrequency(0);
        else
            result.setFrequency( de_or(f3,f2));
        
        result.setConfidence( c1 );
        return result;
    }

    // assume that 
    // v3 =  revision(v1, v2);    v3.setFrequency( (float)((w1 * f1 + w2 * f2) / w3) );

    // v1 =  de_revision(v3, v2);
    private static final TruthValue de_revision(TruthValue v3, TruthValue v2, TruthValue result) {
        final float f3 = v3.getFrequency();
        final float f2 = v2.getFrequency();
        final double c3 = v3.getConfidence();
        final double c2 = v2.getConfidence();
        final double w3 = c2w(c3);
        final double w2 = c2w(c2);
        final double w1 = w3 - w2;
        final double c1 = w2c(w1);
        if (w1 == 0)
            result.setFrequency(0);
        else
            result.setFrequency( (f3 * w3 - f2 * w2) / w1);
        result.setConfidence( c1 );
        return result;
    }

    
    public static final TruthValue de_revision(TruthValue v1, TruthValue v2) {
        if(setting.all_prob_revision)
            return probabilistic_de_revision(v1, v2, new TruthValue());
        else
            return de_revision(v1, v2, new TruthValue());
    }


    /* ----- double argument functions, called in SyllogisticRules ----- */
    /**
     * {&lt;S ==&gt; M&gt;, &lt;M ==&gt; P&gt;} |- &lt;S ==&gt; P&gt;
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static final TruthValue deduction(final TruthValue v1, final TruthValue v2) {
        final float f1 = v1.getFrequency();
        float f2 = v2.getFrequency();
        final double c1 = v1.getConfidence();
        final double c2 = v2.getConfidence();
        final float f = (float)and(f1, f2);
        final double c = and(c1, c2, f);
        return new TruthValue(f, c);
    }

    /**
     * {&lt;S ==&gt; M&gt;, &lt;M &lt;=&gt; P&gt;} |- &lt;S ==&gt; P&gt;
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static final TruthValue analogy(final TruthValue v1, final TruthValue v2) {
        final float f1 = v1.getFrequency();
        final float f2 = v2.getFrequency();
        final double c1 = v1.getConfidence();
        final double c2 = v2.getConfidence();
        final float f = (float)and(f1, f2);
        final double c = and(c1, c2, f2);
        return new TruthValue(f, c);
    }

    public static final int compareTruthValuef(final TruthValue v1, final TruthValue v2){
        if(v1.getFrequency() > v2.getFrequency())
            return 1;
        if(v1.getFrequency() == v2.getFrequency())
            return 0;
        if(v1.getFrequency() < v2.getFrequency())
            return -1;
        return 0;
    }

    //xch2.1 在EqualityStore测试通过
    public static final TruthValue conditional_deduction_3(final TruthValue v1, final TruthValue v2, final TruthValue v3, final TruthValue v4){
        ArrayList<TruthValue> t = new ArrayList<>();
        TruthValue temp = new TruthValue(v1);
        t.add(0, v2);
        t.add(1, v3);
        t.add(2, v4);
        t.sort((a1,a2)->{return compareTruthValuef(a2,a1);});  //(a2,a1)说明降序
        for(int i = 0; i < t.size(); i++){
            temp =  deduction(temp, t.get(i));
        }
        return temp;
    }

    /**
     * {&lt;M --&gt; S&gt;, &lt;M &lt;-&gt; P&gt;} |- &lt;M --&gt; (S&amp;P)&gt;
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static final TruthValue intersection(final TruthValue v1, final TruthValue v2) {
        final float f1 = v1.getFrequency();
        final float f2 = v2.getFrequency();
        final double c1 = v1.getConfidence();
        final double c2 = v2.getConfidence();
        final float f = (float)and(f1, f2);
        final double c = and(c1, c2);
        return new TruthValue(f, c);
    }

    /**
     * {&lt;M ==&gt; S&gt;, &lt;M ==&gt; P&gt;} |- &lt;S &lt;=&gt; P&gt;
     * @param v1 Truth value of the first premise
     * @param v2 Truth value of the second premise
     * @return Truth value of the conclusion
     */
    public static final TruthValue comparison(final TruthValue v1, final TruthValue v2) {
        final float f1 = v1.getFrequency();
        final float f2 = v2.getFrequency();
        final double c1 = v1.getConfidence();
        final double c2 = v2.getConfidence();
        final float f0 = or(f1, f2);
        final float f = (f0 == 0) ? 0 : ((float)and(f1, f2) / f0);
        final double w = and(f0, c1, c2);
        final double c = w2c(w);
        return new TruthValue(f, c);
    }

    public static final TruthValue abduction(final TruthValue v1, final TruthValue v2) {
        return induction(v2, v1);
    }

    public static final TruthValue induction(final TruthValue v1, final TruthValue v2) {
        final float f1 = v1.getFrequency();
        final float f2 = v2.getFrequency();
        final double c1 = v1.getConfidence();
        final double c2 = v2.getConfidence();
        final double w = and(f2, c1, c2);
        final double c = w2c(w);
        return new TruthValue(f1, c);
    }

    public static final double divide_confidence(double c, int divide_confidence_num) {
        final double w = c2w(c);
        return w2c(w/divide_confidence_num);
    }
}

