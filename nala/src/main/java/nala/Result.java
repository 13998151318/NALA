package nala;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.Pair;
import javatools.datatypes.Triple;
import javatools.parsers.DateParser;
import javatools.parsers.NumberParser;
import nala.Setting.LiteralDistance;
import nala.nal.AlignmentSentence;
import nala.nal.TruthValue;
import nala.shingling.QueryResult;
import nala.shingling.ShinglingTable;
import nala.storage.FactStore;



/** This class is part of the PARIS ontology matching project at INRIA Saclay/France.
 * 
 * It is licensed under a Creative Commons Attribution Non-Commercial License
 * by the author Fabian M. Suchanek (http://suchanek.name). For all further information,
 * see http://webdam.inria.fr/paris
 *
 * This class holds all information that is computed in of PARIS: 
 * subclass, equality, subproperty*/

public class Result implements Closeable {

    // -----------------------------------------------------------------
    //             Stores
    // -----------------------------------------------------------------

    /** First fact store*/
    protected FactStore factStore1;

    /** Second fact store*/
    protected FactStore factStore2;

    /** Stores equalities. */
    public EqualityStore equalityStore;

    /** Maps classes of 1 to super classes of 2*/
    public HashSubThingStore<Integer> superClassesOf1;

    /** Maps classes of 2 to super classes of 1*/
    public HashSubThingStore<Integer> superClassesOf2;
    
    Setting setting;
    
    /** Raw outputs of the computation */
    public MapperOutput mapperOutput1;
    public MapperOutput mapperOutput2;
    
    /** Maps relations of 1 to super relations of 2. Redundant with neighborhoods and relationNormalizers, but more efficient to query. */
    public SubRelationStore superRelationsOf1;

    /** Maps relations of 2 to super relations of 1*/
    public SubRelationStore superRelationsOf2;

    /** TSV File folder*/
    public final File tsvFolder;
    
    boolean allowWrite;

    public HashMap<String,Integer> value_to_id;

    public HashMap<Integer,String> id_to_value;

    public double avg_sim_confidence;
        
    // -----------------------------------------------------------------
    //             Constructor
    // -----------------------------------------------------------------

    /** Constructor
     * @throws IOException 
     * @throws InterruptedException */
    public Result(Setting setting, FactStore fs1, FactStore fs2, File tsvfolder, HashMap<String,Integer> value_to_id_, HashMap<Integer,String> id_to_value_) throws IOException, InterruptedException {
        Announce.doing("Creating computing environment");
        this.tsvFolder=tsvfolder;
        
        equalityStore = new EqualityStore(fs1, fs2, setting, -1);
        superClassesOf1 = new SubClassStore(fs1, fs2);
        superClassesOf2 = new SubClassStore(fs2, fs1);
        superRelationsOf1 = new HashSubRelationStore(fs1, fs2);
        superRelationsOf2 = new HashSubRelationStore(fs2, fs1);    	
        factStore1 = fs1;
        factStore2 = fs2;
        mapperOutput1 = new MapperOutput(fs1);
        mapperOutput2 = new MapperOutput(fs2);
        this.setting = setting;
        value_to_id = value_to_id_;
        id_to_value = id_to_value_;
        Announce.done();
        print();
        avg_sim_confidence = 0;
    }

    public double compute_avg_sim_confidence(){
        int y1, y2;
        int k=0;
        avg_sim_confidence = 0;
        for (int i : equalityStore.alignment_sentences_fs1_To_fs2.keySet()) {
            LinkedList<AlignmentSentence> list =  equalityStore.alignment_sentences_fs1_To_fs2.get(i);
            ListIterator<AlignmentSentence> it = list.listIterator();
            while (it.hasNext()) {
                k++;
                AlignmentSentence a = it.next();
                avg_sim_confidence += a.truthValue.getConfidence();
                break;
            }
        }
        avg_sim_confidence /= k;
        return avg_sim_confidence;
    }
    
    public SubRelationStore superRelationsForFactStore(FactStore fs) {
        if (fs == factStore1) return superRelationsOf1;
        if (fs == factStore2) return superRelationsOf2;
        return null;
    }
    
    public HashSubThingStore<Integer> superClassesForFactStore(FactStore fs) {
        if (fs == factStore1) return superClassesOf1;
        if (fs == factStore2) return superClassesOf2;
        return null;
    }
    
    public MapperOutput mapperOutputForFactStore(FactStore fs) {
        if (fs == factStore1) return mapperOutput1;
        if (fs == factStore2) return mapperOutput2;
        assert(false);
        return null;
    }
    
    /** Return the other factStore */
    public FactStore other(FactStore fs) {
        if (fs == factStore1) return factStore2;
        if (fs == factStore2) return factStore1;
        assert(false);
        return null;
    }
    
    @Override
    public void close() throws IOException {
        Announce.doing("Closing stores");
        equalityStore.close();
        superClassesOf1.close();
        superClassesOf2.close();
        superRelationsOf1.close();
        superRelationsOf2.close();
        Announce.done();
    }
    

    public void resetAndPrune(FactStore fs) {
        Announce.doing("Resetting and pruning neighborhoods...");
    
        HashArrayNeighborhood[] n = mapperOutputForFactStore(fs).neighborhoods; 
        for (int i = 0; i < fs.maxJoinRelationCode(); i++) {
            if (n[i] != null)
                n[i].reset();
        }
        for (int i = 0; i < fs.maxJoinRelationCode(); i++) {
//			if (n[i] != null)
//				n[i].prune(HashNeighborhood.occurrenceThreshold);
        }
    }

    /** Sets the TSV writers to new files*/
    public void startIteration(int iteration) throws IOException {
        Announce.doing("Creating tsv files",iteration,"in",tsvFolder);
        equalityStore.setTSVfile(new File(tsvFolder,iteration+"_eqv.tsv"));
        superClassesOf1.setTSVfile(new File(tsvFolder,iteration+"_superclasses1.tsv"));
        superClassesOf2.setTSVfile(new File(tsvFolder,iteration+"_superclasses2.tsv"));
        Announce.done();
    }


    // -----------------------------------------------------------------
    //             Subrelations and classes
    // -----------------------------------------------------------------

    /** Returns the superclasses of a class*/
    public Collection<Integer> superClassesOf(FactStore fs, Integer x) {
        return superClassesForFactStore(fs).superOf(x);
    }

    public int reversed(int x) { return -x; }
    
    public JoinRelation reversed(JoinRelation r) {
        JoinRelation r2 = new JoinRelation(r);
        r2.reverse();
        return r2;
    }

    /** Returns the subclasses of a class*/
    public Collection<Integer> subClassesOf(FactStore fs, Integer x) {
        return superClassesForFactStore(other(fs)).subOf(x);
    }

    /** Return how much the first is a subrelation of the second*/
    public TruthValue subRelation(FactStore fssub, JoinRelation sub, JoinRelation supr) {
        return superRelationsForFactStore(fssub).getTruthValue(sub, supr);
    }

    /** Return how much the first is a subrelation of the second*/
    public TruthValue subRelation(FactStore fssub, int sub, int supr) {
        return superRelationsForFactStore(fssub).getTruthValueCode(sub, supr);
    }
    
    /** Return how much the first is a subclass of the second*/
    public TruthValue subClass(FactStore fssub, Integer sub, Integer supr) {
        return superClassesForFactStore(fssub).getTruthValue(sub, supr);
    }

    /** Makes the first is a subclass of the second*/
    public void setSubclass(FactStore fssub, Integer sub, Integer supr, TruthValue val) {
        superClassesForFactStore(fssub).setTruthValue(sub, supr, val);
    }
    
    // -----------------------------------------------------------------
    //             Equality
    // -----------------------------------------------------------------

public TruthValue stringEquality_old(String s1, String s2) {
    if (s1.equals(s2)) {
        if (setting.use_c_as_probability_value)
            return new TruthValue(1.f, 1);
        else 
            return new TruthValue(1.f, setting.literal_equal_initial_confidence);
    }
    if (setting.use_c_as_probability_value){
        switch (Config.entityType(s1)) {
        case NUMBER:
            if (Config.entityType(s2) == Config.EntityType.STRING)
                return ( new TruthValue(1, dateCompare(s1, s2)));
            return (new TruthValue(1, numCompare(s1, s2)));
        case STRING:
            return (new TruthValue(1, stringCompare(setting, s1, s2)));
        case DATE:
            return ( new TruthValue(1, (float)dateCompare(s1, s2)));
        case RESOURCE:
            assert(false);
            return new TruthValue(1, 0);
        default:
            assert(false);
            return new TruthValue(1, 0);
        }
    }
    else{
        switch (Config.entityType(s1)) {
        case NUMBER:
            if (Config.entityType(s2) == Config.EntityType.STRING)
                return ( new TruthValue(dateCompare(s1, s2), setting.literal_equal_initial_confidence));
            return (new TruthValue(numCompare(s1, s2), setting.literal_equal_initial_confidence));
        case STRING:
            return (new TruthValue(stringCompare(setting, s1, s2), setting.literal_equal_initial_confidence));
        case DATE:
            return ( new TruthValue(dateCompare(s1, s2), setting.literal_equal_initial_confidence));
        case RESOURCE:
            assert(false);
            return new TruthValue(0, setting.literal_equal_initial_confidence);
        default:
            assert(false);
            return new TruthValue(0, setting.literal_equal_initial_confidence);
        }
    }
}

public TruthValue stringEquality(FactStore fs1, int s1, int s2) {
    FactStore fs2 = other(fs1);
    String s11,s22;
    Integer x1_id, x2_id;
    int count;
    LinkedList<Pair<Integer, Float>> list;
    ListIterator<Pair<Integer, Float>> it;
    s11 = fs1.entity(s1);
    s22 = fs2.entity(s2);
    if (s11.equals(s22)) {
        if (setting.use_c_as_probability_value)
            return new TruthValue(1.f, 1);
        else 
            return new TruthValue(1.f, setting.literal_equal_initial_confidence);
    }
    if (!setting.use_attribute_value_emb_sim){
        return new TruthValue(0.f, 0.);
    }
    x1_id = value_to_id.get(s11);
    x2_id = value_to_id.get(s22);
    if (x1_id == null || x2_id == null){
        return new TruthValue(0.f, 0.);
    }
    if (!setting.use_c_as_probability_value){
        list = fs1.value_emb_sim.get(x1_id);
        it = list.listIterator();
        count = 0;
        while (it.hasNext()) {
            Pair<Integer, Float> a = it.next();
            if(a.first() == s2){
                return TruthValue.truthValue_of_attribute_literal(a.second() , setting.attribute_value_emb_sim_confidence);
            }
            count++;
            if (count >=setting.literal_approximate_equal_count){
                break;
            }
        }
        list = fs2.value_emb_sim.get(x2_id);
        it = list.listIterator();
        count = 0;
        while (it.hasNext()) {
            Pair<Integer, Float> a = it.next();
            if(a.first() == s1){
                return TruthValue.truthValue_of_attribute_literal(a.second() , setting.attribute_value_emb_sim_confidence);
            }
            count++;
            if (count >=setting.literal_approximate_equal_count){
                break;
            }
        }
        return new TruthValue(0.f, 0.);  
    }
    else{
        list = fs1.value_emb_sim.get(x1_id);
        it = list.listIterator();
        count = 0;
        while (it.hasNext()) {
            Pair<Integer, Float> a = it.next();
            if(a.first() == s2){
                return new TruthValue(1 , a.second());
            }
            count++;
            if (count >=setting.literal_approximate_equal_count){
                break;
            }
        }
        list = fs2.value_emb_sim.get(x2_id);
        it = list.listIterator();
        count = 0;
        while (it.hasNext()) {
            Pair<Integer, Float> a = it.next();
            if(a.first() == s1){
                return new TruthValue(1 , a.second());
            }
            count++;
            if (count >=setting.literal_approximate_equal_count){
                break;
            }
        }
        return new TruthValue(0.f, 0.);
    }
}
    /** Returns the equality. */
    public TruthValue equality(FactStore fs1, int s1, int s2) {
        FactStore fs2 = other(fs1);
        if (fs1.isLiteral(s1)) {
            if (!fs2.isLiteral(s2)) 
                return new TruthValue(0.f, 0.);
            return stringEquality(fs1, s1, s2);
        } else {
            if (fs2.isLiteral(s2))
                return new TruthValue(0.f, 0.);
            if (fs1 == factStore1)
                        return equalityStore.getValueInt_left(s1, s2);
                return equalityStore.getValueInt_right(s1, s2);
        }
    }
    
    /** Says to whom you are equal with scores*/
    public Collection<Pair<Object,TruthValue>> equalToScoredId(FactStore fs, int x1) {
        if (!fs.isLiteral(x1))
            return equalToScored(fs, (Integer) x1);
        // The implementation of EqualityStore does assert(this.setting.takeMaxMax), so no point in doing this check
//    if(this.setting.takeMax)
//    	return(equalToScoredMax(fs,x1));
        return literalEqualToScored(fs, fs.entity(x1));
    }

    
    /** Says to whom you are equal with score*/
    public Collection<Pair<Object,TruthValue>> equalToScored(FactStore fs, Integer x1) {
        // The implementation of EqualityStore does assert(this.setting.takeMaxMax), so no point in doing this check
//    if (this.setting.takeMax)
//    	return(equalToScoredMax(fs,x1));
        return trueEqualToScored(fs, x1);
    }

//  /** Says to whom you are equal with scores*/
//  public Collection<Pair<Object,Double>> equalToScoredMax(FactStore fs, Object x1) {
//    if (x1 instanceof Integer)
//    	return (equalToScoredMax(fs, (Integer) x1));
//    return (literalEqualToScored(fs, x1));
//  }

    /** Says to whom you are equal with score*/
    protected Collection<Pair<Object,TruthValue>> trueEqualToScored(FactStore fs, Integer x1) {
        //factStore1 represents "the left knowledge graph"
        if (fs == factStore1)
            return (equalityStore.left_to_right((Integer) x1));
        else
            return (equalityStore.right_to_left((Integer) x1));
    }
    
    /** Says to whom you are equal as a literal */
    public Collection<Pair<Object, TruthValue>> literalEqualToScored(FactStore fs, String x1) {
        List<Pair<Object, TruthValue>> equal_list = new ArrayList<>();
        int x1_id;
        int count;
        if (setting.use_attribute_value_emb_sim){
            if (other(fs).entity(x1) != 0){
                assert(other(fs).isLiteral(other(fs).entity(x1)));
                equal_list.add(new Pair<Object,TruthValue>(other(fs).entity(x1),new TruthValue(1.0f , setting.literal_equal_initial_confidence)));
            }
            if (value_to_id.get(x1) == null){
                return equal_list;
            }
            x1_id = value_to_id.get(x1);
            LinkedList<Pair<Integer, Float>> list = fs.value_emb_sim.get(x1_id);
            ListIterator<Pair<Integer, Float>> it = list.listIterator();
            count = 0;
            while (it.hasNext()) {
                Pair<Integer, Float> a = it.next();
                equal_list.add(new Pair<Object,TruthValue>(a.first(), TruthValue.truthValue_of_attribute_literal(a.second() , setting.attribute_value_emb_sim_confidence)));
                count++;
                if (count >=setting.literal_approximate_equal_count){
                    break;
                }
            }
            return equal_list;
        }
        else{
            if (other(fs).entity(x1) == 0) return (Collections.emptyList());
            assert(other(fs).isLiteral(other(fs).entity(x1)));
            return (Arrays.asList(new Pair<Object,TruthValue>(other(fs).entity(x1),new TruthValue(1.0f , setting.literal_equal_initial_confidence))));
        }
    }

    public double computeQueryResultScore(QueryResult qr, String x1, LiteralDistance ld) {
        double score;
        if (ld == LiteralDistance.SHINGLING) {
            score = qr.trueScore;
        } else {
            score = LevenshteinDistance.similarity(qr.result, (String) x1);
        }
        assert (score >= 0 && score <= 1);
        // we're not supposed to find an exact match now
        assert(!setting.noApproxIfExact || !qr.result.equals((String) x1));
        if (score < setting.postLiteralDistanceThreshold)
            return 0;
        score /= setting.penalizeApproxMatches; // we must do it here because we don't call equality()
        // squaring
        if (setting.shinglingSquare)
            score = score*score;
        return score;
    }

    /** Check if the literal exists exactly in the other ontology */

    /** Returns equality*/
    public static double numCompare(String s1, String s2) {
        String[] n1 = NumberParser.getNumberAndUnit(s1, new int[2]);
        if (n1 == null) return (-1);
        String[] n2 = NumberParser.getNumberAndUnit(s2, new int[2]);
        if (n2 == null) return (-1);
        if (D.equal(n1[1], n2[1])) return (0);
        try {
            double d1 = Double.parseDouble(s1);
            double d2 = Double.parseDouble(s2);
            double val = 1 / (1 + 100 * Math.abs((d1 - d2) / d1));
            return (val);
        } catch (Exception e) {
            return (-1);
        }
    }

    /** Compares two strings*/
    public static double stringCompare(Setting setting, String s1, String s2) {
        double score;
        s1=Config.stripQuotes(s1);
        s2=Config.stripQuotes(s2);    
        String splitBy="";
        switch(setting.literalDistance) {
            case IDENTITY:
                score = (s1.equals(s2)?1:0);
                break;
            case NORMALIZE:
                score = (Config.normalizeString(s1).equals(Config.normalizeString(s2))?1:0);
                break;
            case BAGOFWORDS:
                splitBy="\\W";
            case BAGOFCHARS:
                List<String> s1split=Arrays.asList(s1.split(splitBy));
                List<String> s2split=Arrays.asList(s2.split(splitBy));        
                Set<String> intersection = new TreeSet<String>(s1split);
                intersection.retainAll(s2split);
                if (intersection.size() == 0) return (0);
                Set<String> union = new TreeSet<String>(s1split);
                union.addAll(s2split);
                double val = intersection.size() / (double) union.size();
                score = (val);
                break;
            case LEVENSHTEIN:
            case SHINGLINGLEVENSHTEIN:
                score = LevenshteinDistance.similarity(s1, s2);
                break;
//      case JARO_WINKLER:
//        JaroWinklerDistance jwd = new JaroWinklerDistance();
//      	double d = jwd.proximity(s1, s2);
//      	//Announce.debug("proximity of ", s1, " and ", s2, " is ", d);
//      	score = d;
//      	break;
            case SHINGLING:
                score = ShinglingTable.goldStandard(s1, s2, setting.shinglingSize);
                break;
            default:
                assert(false);
                score = -1;
                break;
        }
        if (s1.equals(s2))
            assert(score > 0.999);
        else
            score /= setting.penalizeApproxMatches;
        return score;
    }
    
    /** Compares two dates*/
    public static double dateCompare(String s1, String s2) {
        s1 = Config.stripQuotes(s1);
        s2 = Config.stripQuotes(s2);
        if (!DateParser.isDate(s2) && !NumberParser.isInt(s2)) return (0);
        return (DateParser.includes(s1, s2) || DateParser.includes(s2, s1) ? 1 : 0);
    }


    /** Prints a human-readable summary*/
    public void print() {
        Announce.message("Equalities examples:");
        for (Triple<Integer, Integer, TruthValue> pair2 : equalityStore.sample()) {
            Announce.message("    " + factStore1.entity(pair2.first()) + " = " + factStore2.entity(pair2.second()) + " " + pair2.third());
        }
//    Announce.message("Subclasses: " + (superClassesOf1.size() + superClassesOf2.size()) + ", for example");
//    for (Pair<Integer, Integer> pair : superClassesOf1.sample()) {
//      Announce.message("    " + factStore1.toString(pair.first()) + " < " + factStore2.toString(pair.second()));
//    }
//    for (Pair<Integer, Integer> pair : superClassesOf2.sample()) {
//      Announce.message("    " + factStore2.toString(pair.first()) + " < " + factStore1.toString(pair.second()));
//    }
        //Announce.message("Subrelations: " + (superRelationsOf1.size() + superRelationsOf2.size()) + ", for example");
        Announce.message("Subrelation examples:");
        for (Triple<JoinRelation, JoinRelation,TruthValue> pair : superRelationsOf1.sample()) {
            Announce.message("    " + pair.first().toString() + " < " + pair.second().toString()+"  "+pair.third().toString());
        }
        for (Triple<JoinRelation, JoinRelation,TruthValue> pair : superRelationsOf2.sample()) {
            Announce.message("    " + pair.first().toString() + " < " + pair.second().toString()+"  "+pair.third().toString());
        }
        Announce.message("Memory (Mb):");
        Announce.message("   Java Free: " + Runtime.getRuntime().freeMemory() / 1000 / 1000);
        Announce.message("   Java Max: " + Runtime.getRuntime().maxMemory() / 1000 / 1000);
        Announce.message("   Java Total: " + Runtime.getRuntime().totalMemory() / 1000 / 1000);
    }

    
    public void printNeighborhoodsForFactStore(FactStore fs) {
        for (int i = 0; i < fs.maxJoinRelationCode(); i++) {
            if (mapperOutputForFactStore(fs).neighborhoods[i] == null) continue;
            JoinRelation r = fs.joinRelationByCode(i);
            Announce.message("== neighborhood for", r, "==");
            mapperOutputForFactStore(fs).neighborhoods[i].print(new JoinRelation(other(fs)));
        }
    }
    public void printNeighborhoods() {
        printNeighborhoodsForFactStore(factStore1);
        Announce.message("== // // ==");
        printNeighborhoodsForFactStore(factStore2);
    }
    
}
