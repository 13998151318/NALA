package nala;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ListIterator;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.Pair;
import javatools.datatypes.Triple;
import nala.SubThingStore.SubPair;
import nala.lapjv.lapjv;
import nala.nal.AlignmentSentence;
import nala.nal.Stamp;
import nala.nal.TruthFunctions;
import nala.nal.TruthValue;
import nala.nal.Stamp.BaseEntry;
import nala.sparseLAPJV.LAPJV;
import nala.sparseLAPJV.SparseCostMatrix;
import nala.storage.FactStore;
import nala.storage.FactStore.PredicateAndObject;

/** This class is part of the PARIS ontology matching project at INRIA Saclay/France.
 * 
 * It is licensed under a Creative Commons Attribution Non-Commercial License
 * by the author Fabian M. Suchanek (http://suchanek.name). For all further information,
 * see http://webdam.inria.fr/paris
 *
 * This class stores pairs of entities with a score. It is used for the entity alignment.
 * The first entity will live in one fact store, and the second entity will live in another fact store.
 * If a TSV file is set, every assignment will be printed into that file (in addition to storing it in the data base).
 * The pairs are represented by associating one element to each other element (so we assume takeMaxMax).
 * This class is thread-safe and one instance is shared between all threads in findEqualsOf */


public class EqualityStore extends SubThingStore<Integer> implements Closeable {
    /** Maps an entity from the first factstore to its equality pair*/
    //only gets the left to best right alignment from alignment_sentences_fs1_To_fs2
    protected int[] subIndexMatch;
    protected TruthValue[] subIndexScore;

    /** Maps an entity from the second factstore to its equality pair*/
    protected int[] superIndexMatch;
    protected TruthValue[] superIndexScore;
    
    //xch2.0
    //AlignmentSentence: <entity_id(big)    entity_id(big)>
    //alignment_sentences_fs1_To_fs2[proper_entity_id(small)]
    public HashMap<Integer, LinkedList<AlignmentSentence>> alignment_sentences_fs1_To_fs2;
    public HashMap<Integer, LinkedList<AlignmentSentence>> alignment_sentences_fs2_To_fs1;

    public HashMap<Integer, LinkedList<AlignmentSentence>> alignment_sentences_fs1_To_fs2_old;
    public HashMap<Integer, LinkedList<AlignmentSentence>> alignment_sentences_fs2_To_fs1_old;
    //proper_entity_id  proper_entity_id
    public HashMap<Integer, Integer> temp_alignment_proper_id;
    public HashMap<Integer, AlignmentSentence> temp_alignment_proper_id_sentences;
    //AlignmentSentence alignment_sentences_fs1_To_fs2[][];
    //AlignmentSentence alignment_sentences_fs2_To_fs1[][];
    Setting setting;
    int run;

	public EqualityStore(FactStore fs11, FactStore fs21, Setting setting1, int run1) throws IOException {
        super(fs11, fs21);
        subIndexMatch = new int[fs1.numEntities() + fs1.numClasses() + 1];
        subIndexScore = new TruthValue[fs1.numEntities() + fs1.numClasses() + 1];
        superIndexMatch = new int[fs2.numEntities() + fs2.numClasses() + 1];
        superIndexScore = new TruthValue[fs2.numEntities() + fs2.numClasses() + 1];
        setting = setting1;
        run = run1;
        alignment_sentences_fs1_To_fs2 = new HashMap<Integer, LinkedList<AlignmentSentence>>((int)(fs1.num_proper_entities()/0.75 + 1));
        alignment_sentences_fs2_To_fs1 = new HashMap<Integer, LinkedList<AlignmentSentence>>((int)(fs2.num_proper_entities()/0.75 + 1));
        for (int i = 0; i < fs1.num_proper_entities(); i++) {
            alignment_sentences_fs1_To_fs2.put(i, new LinkedList<AlignmentSentence>());
        }
        for (int i = 0; i < fs2.num_proper_entities(); i++) {
            alignment_sentences_fs2_To_fs1.put(i, new LinkedList<AlignmentSentence>());
        }
        //alignment_sentences_fs1_To_fs2 = new AlignmentSentence[fs1.num_proper_entities()][setting.max_alignment_sentences];
        //alignment_sentences_fs2_To_fs1 = new AlignmentSentence[fs2.num_proper_entities()][setting.max_alignment_sentences];
    }
    public EqualityStore(int num_proper_entities, Setting setting1) throws IOException {
        super(null, null);
        setting = setting1;
        alignment_sentences_fs1_To_fs2 = new HashMap<Integer, LinkedList<AlignmentSentence>>((int)(num_proper_entities/0.75 + 1));
        alignment_sentences_fs2_To_fs1 = new HashMap<Integer, LinkedList<AlignmentSentence>>((int)(num_proper_entities/0.75 + 1));
        for (int i = 0; i < num_proper_entities; i++) {
            alignment_sentences_fs1_To_fs2.put(i, new LinkedList<AlignmentSentence>());
        }
        for (int i = 0; i < num_proper_entities; i++) {
            alignment_sentences_fs2_To_fs1.put(i, new LinkedList<AlignmentSentence>());
        }
    }

    @Override
	public Collection<SubPair<Integer>> all() {
        ArrayList<SubPair<Integer>> result = new ArrayList<SubPair<Integer>>();

        for (int i = 0; i < subIndexScore.length; i++) {
            if (subIndexScore[i] == null)
                continue; 
            if (subIndexScore[i].getConfidence() > 0) {
                result.add(new SubPair<Integer>(i, subIndexMatch[i], subIndexScore[i]));
            }
        }
        return result;
    }
    public TruthValue getTruthValue(Integer sub, Integer supr){
        return null;
    }
    protected void setTruthValue(Integer sub, Integer supr, TruthValue val){
    }

    public String toTsv(SubPair<Integer> p) {
		return fs1.entity(p.sub)+"\t"+fs2.entity(p.supr)+"\t"+p.truthValue.toString()+"\n";
	}
	
    public void double_delete(boolean fs1_To_fs2, int i, AlignmentSentence a){
        LinkedList<AlignmentSentence> list;
        Iterator<AlignmentSentence> it2;
        int j2, i2;
        AlignmentSentence a2;
        if (fs1_To_fs2){
            //double delete
            
            j2 = fs2.id_big_to_small(a.similarityStatement.predTermIndex);
            //search for and delete j2 <-> i
            list = alignment_sentences_fs2_To_fs1.get(j2);
            it2 = list.listIterator();
            while (it2.hasNext()) {
                a2 = it2.next();
                i2 = fs1.id_big_to_small(a2.similarityStatement.predTermIndex);
                if (i2 == i){
                    it2.remove();
                    break;
                }
            }
        }
        else{
            //double delete
            
            j2 = fs1.id_big_to_small(a.similarityStatement.predTermIndex);
            //search for and delete j2 <-> i
            list = alignment_sentences_fs1_To_fs2.get(j2);
            it2 = list.listIterator();
            while (it2.hasNext()) {
                a2 = it2.next();
                i2 = fs2.id_big_to_small(a2.similarityStatement.predTermIndex);
                if (i2 == i){
                    it2.remove();
                    break;
                }
            }
        }
    }

    public static void getTraceInfo(String msg,Object var2) {
        StringBuffer sb = new StringBuffer();
        StackTraceElement[] stacks = new Throwable().getStackTrace();
        sb.append(Thread.currentThread().getName() + stacks[1].getClassName() + stacks[1].getLineNumber());
        D.p(sb.append(": " + msg + var2.toString()).toString());
    }


    public int recursively_delete_alignment_sentence(boolean fs1_To_fs2, int i, int previous){
        /* 
        level++;
        if(level>40000 && level<40020){
            D.p("fs1_To_fs2:"+fs1_To_fs2+"  level:"+level);
            if(fs1_To_fs2)
                D.p("i:"+i+" "+fs1.proper_entity_s(i)+"  previous:"+previous+"  "+fs2.proper_entity_s(previous));
            if(!fs1_To_fs2)
                D.p("i:"+i+" "+fs2.proper_entity_s(i)+"  previous:"+previous+"  "+fs1.proper_entity_s(previous));
            
        }
        if(level>=40020)
            return -1;
        */
        //this call focus on entity i
        LinkedList<AlignmentSentence> list;
        Iterator<AlignmentSentence> it;
        AlignmentSentence a, a2;
        int j, j_next, break_iter_pos = 0;
        int returnj = -1;
        if (fs1_To_fs2){
            //getTraceInfo("i ", fs1.proper_entity_s(i));
            list = alignment_sentences_fs1_To_fs2.get(i);
            //it = list.listIterator();
            while (!list.isEmpty()) {
                a = list.getFirst();
                j = fs2.id_big_to_small(a.similarityStatement.predTermIndex);
                if (j != previous){
                    j_next = recursively_delete_alignment_sentence(!fs1_To_fs2, j, i);
                    if (j_next == i){
                        returnj = j;
                        //getTraceInfo("returnj ", fs2.proper_entity_s(returnj));
                        break;
                    }
                    else{
                        if (!list.isEmpty()){
                            if (list.getFirst().similarityStatement.predTermIndex == j){
                                list.removeFirst();
                            }
                        }  
                    }
                    //no need to double delete i <-> j
                }
                else{
                    returnj = previous;
                    //getTraceInfo("returnj ", fs2.proper_entity_s(returnj));
                    break;
                }
            }
            if (list.size() >= 2){
                it = list.listIterator(1);
                while (it.hasNext()) {
                    a2 = it.next();
                    it.remove();
                    double_delete(fs1_To_fs2, i, a2);
                }
            }
            return returnj;
        }
        else if (!fs1_To_fs2){
            //getTraceInfo("i ", fs2.proper_entity_s(i));
            list = alignment_sentences_fs2_To_fs1.get(i);
            //it = list.listIterator();
            while (!list.isEmpty()) {
                a = list.getFirst();
                j = fs1.id_big_to_small(a.similarityStatement.predTermIndex);
                if (j != previous){
                    j_next = recursively_delete_alignment_sentence(!fs1_To_fs2, j, i);
                    if (j_next == i){
                        returnj = j;
                        //getTraceInfo("returnj ", fs1.proper_entity_s(returnj));
                        break;
                    }
                    else{
                        if (!list.isEmpty()){
                            if (list.getFirst().similarityStatement.predTermIndex == j){
                                list.removeFirst();
                            }
                        }
                    }
                    //no need to double delete i <-> j
                }
                else{
                    returnj = previous;
                    //getTraceInfo("returnj ", fs1.proper_entity_s(returnj));
                    break;
                }
            }
            if (list.size() >= 2){
                it = list.listIterator(1);
                while (it.hasNext()) {
                    a2 = it.next();
                    it.remove();
                    double_delete(fs1_To_fs2, i, a2);
                }
            }
            return returnj;
        }
        return -1;
    }

    public static LinkedList<AlignmentSentence> deep_clone_list(LinkedList<AlignmentSentence> list){
        LinkedList<AlignmentSentence> list_new = new LinkedList<>();
        Iterator<AlignmentSentence> it;
        AlignmentSentence a, a2;
        it = list.listIterator();
        while (it.hasNext()) {
            a2 = it.next();
            list_new.add(a2);
        }
        return list_new;
    }

    public void deep_clone_alignment_sentences() {
        LinkedList<AlignmentSentence> list, list_new;
        alignment_sentences_fs1_To_fs2_old = new HashMap<Integer, LinkedList<AlignmentSentence>>((int)(fs1.num_proper_entities()/0.75 + 1));
        alignment_sentences_fs2_To_fs1_old = new HashMap<Integer, LinkedList<AlignmentSentence>>((int)(fs2.num_proper_entities()/0.75 + 1));
        for (int i = 0; i < fs1.num_proper_entities(); i++) {
            list_new = deep_clone_list(alignment_sentences_fs1_To_fs2.get(i));
            alignment_sentences_fs1_To_fs2_old.put(i, list_new);
        }
        for (int i = 0; i < fs2.num_proper_entities(); i++) {
            list_new = deep_clone_list(alignment_sentences_fs2_To_fs1.get(i));
            alignment_sentences_fs2_To_fs1_old.put(i, list_new);
        }

    }

    public int get_position_in_fs1_To_fs2_old(int i, int j){
        LinkedList<AlignmentSentence> list;
        Iterator<AlignmentSentence> it;
        AlignmentSentence a2;
        int pos = -1;
        list = alignment_sentences_fs1_To_fs2_old.get(i);
        it = list.listIterator();
        while (it.hasNext()) {
            a2 = it.next();
            pos++;
            if (a2.similarityStatement.predTermIndex == j)
                return pos;
        }
        return -1;
    }

    public boolean modify_matches(){
        LinkedList<AlignmentSentence> list1, list2, list_r;
        Iterator<AlignmentSentence> it;
        AlignmentSentence a1, a1_new, a2r, a2, a2_new, highest_a1_new = null, highest_a2_new = null;
        int pos = -1;
        int pos2 = -1;
        int j;
        int returnj = -1;
        double e1,e2,e1new,e2new,highest;
        int a1_pred, a1_new_pred, a2_sub_small, highest_a2_sub_small = -1, highest_a1_new_pred = -1;
        boolean modified = false;
        for (int i = 0; i < fs1.num_proper_entities(); i++) {
            if (alignment_sentences_fs1_To_fs2.get(i).isEmpty())
                continue;
            a1 = alignment_sentences_fs1_To_fs2.get(i).getFirst();
            pos = get_position_in_fs1_To_fs2_old(i, a1.similarityStatement.predTermIndex);
            if (pos <= 0)
                continue;
            e1 = a1.truthValue.getExpectation();
            a1_pred = a1.similarityStatement.predTermIndex;
            highest = 0;
            highest_a2_sub_small = -1;
            for (j = 0; j < pos; j++) {
                list1 = alignment_sentences_fs1_To_fs2_old.get(i);
                a1_new = list1.get(j);
                a1_new_pred = a1_new.similarityStatement.predTermIndex;
                e1new = a1_new.truthValue.getExpectation();
                list_r = alignment_sentences_fs2_To_fs1.get(fs2.id_big_to_small(a1_new_pred));
                if (list_r == null || list_r.isEmpty())
                    continue;
                a2r = list_r.getFirst();
                a2_sub_small = fs1.id_big_to_small(a2r.similarityStatement.predTermIndex);
                pos2 = get_position_in_fs1_To_fs2_old(a2_sub_small, a1_pred);
                if (pos2 < 0)
                    continue;
                e2 = a2r.truthValue.getExpectation();
                list2 = alignment_sentences_fs1_To_fs2_old.get(a2_sub_small);
                a2_new = list2.get(pos2);
                e2new = a2_new.truthValue.getExpectation();
                if (e1 + e2 >= e1new + e2new) //modify not successsful
                    continue;
                if (highest >= e1new + e2new) //modify not successsful
                    continue;
                highest = e1new + e2new;
                highest_a2_sub_small = a2_sub_small;
                highest_a1_new = a1_new;
                highest_a2_new = a2_new;
                highest_a1_new_pred = a1_new_pred;
            }
            if (highest_a2_sub_small == -1)
                continue;
            modified = true;
            list1 = alignment_sentences_fs1_To_fs2.get(i);
            list2 = alignment_sentences_fs1_To_fs2.get(highest_a2_sub_small);
            list1.pollFirst();
            list1.addFirst(highest_a1_new);
            list2.pollFirst();
            list2.addFirst(highest_a2_new);
            list1 = alignment_sentences_fs2_To_fs1.get(fs2.id_big_to_small(a1_pred));
            list2 = alignment_sentences_fs2_To_fs1.get(fs2.id_big_to_small(highest_a1_new_pred));
            list1.pollFirst();
            list1.addFirst(highest_a2_new.swap_clone());
            list2.pollFirst();
            list2.addFirst(highest_a1_new.swap_clone());
        }
        return modified;
    }

    private int level = 0;

	public void takeMaxMaxBothWays() {

        deep_clone_alignment_sentences();

        int print = 0;
        for (int i : alignment_sentences_fs1_To_fs2.keySet()) {
            try {
                level = 0;
                recursively_delete_alignment_sentence(true, i, -1);
            }
            catch (StackOverflowError e) {
                System.err.println("true recursion level was " + level);
                System.err.println("reported recursion level was " +
                                   e.getStackTrace().length);
                System.err.println(e.getStackTrace());
                print++;
                if(print < 5)
                    e.printStackTrace();
            }
        }
        populate_right_to_left_1();
        if (setting.modify_matches && run >= 5){
            boolean modified = true;
            while(modified){
                modified = modify_matches();
            }
        }
            
        //boolean keep_only_one_alignment = false;
	}


    public void new_takeMaxMaxBothWays() {
        int print = 0;
        boolean stop = false, found;
        int alignments_done_in_this_turn;
        LinkedList<AlignmentSentence> list, list2;
        Iterator<AlignmentSentence> it, it2;
        AlignmentSentence a = null, a2; 
        int i2 = -1, j = -1, j_next, break_iter_pos = 0;
        int returnj = -1;
        do{
            stop = true;
            //alignments_done_in_this_turn = 0;
            for (int i : alignment_sentences_fs1_To_fs2.keySet()) {
                if (temp_alignment_proper_id.keySet().contains(i)){
                    continue;
                }
                //getTraceInfo("i ", fs1.proper_entity_s(i));
                list = alignment_sentences_fs1_To_fs2.get(i);
                it = list.listIterator();
                found = false;
                while (it.hasNext()) {
                    a = it.next();
                    j = fs2.id_big_to_small(a.similarityStatement.predTermIndex); 
                    if (!temp_alignment_proper_id.values().contains(j)){
                        found = true;
                        break;
                    }
                }
                if(!found){
                    continue;
                }
                found = false;
                list2 = alignment_sentences_fs2_To_fs1.get(j);
                it2 = list2.listIterator();
                while (it2.hasNext()) {
                    a2 = it2.next();
                    i2 = fs1.id_big_to_small(a2.similarityStatement.predTermIndex); 
                    if (!temp_alignment_proper_id.keySet().contains(i2)){
                        found = true;
                        break;
                    }
                }
                if(!found){
                    continue;
                }
                if(i == i2){
                    temp_alignment_proper_id.put(i, j);
                    temp_alignment_proper_id_sentences.put(i, a);
                    stop = false;
                    break;
                }
            }
        } while(stop == false);
        
        //boolean keep_only_one_alignment = false;
	}


    public void sparseLAPJV() {
        LinkedList<AlignmentSentence> list;
        Iterator<AlignmentSentence> it;
        AlignmentSentence a;
        int j, i1, j1;
        int sparse_alignment;
        SparseCostMatrix cm;
        double[] cc = new double[ (fs1._1v1_assumption_size + 1) * setting.max_sparse_alignment + fs2._1v1_assumption_size]; 
        int[] kk = new int[ (fs1._1v1_assumption_size + 1) * setting.max_sparse_alignment + fs2._1v1_assumption_size];
        int[] number = new int[ fs1._1v1_assumption_size + 1];
        int nRows = fs1._1v1_assumption_size;
        int nCols = fs2._1v1_assumption_size;
        int current_cc_size = 0;
        int current_row = -1;
        ArrayList<Pair<Integer,Double>> temp_alignments;
        //Pair<Integer,Double> temp_alignment;
        assert(fs1._1v1_assumption_size == fs2._1v1_assumption_size);
        for (int i : alignment_sentences_fs1_To_fs2.keySet()) {
            //getTraceInfo("i ", fs1.proper_entity_s(i));
            if (!fs1.belong_to_1v1_assumption(i)){
                continue;
            }
            current_row++;
            list = alignment_sentences_fs1_To_fs2.get(i);
            sparse_alignment = 0;
            temp_alignments = new ArrayList<>(setting.max_sparse_alignment);
            i1 = fs1.id_small_to_1v1(i);
            it = list.listIterator();
            //IllegalArgumentException( "All the rows must have at least one cost. Row "
            if (!it.hasNext()){
                temp_alignments.add(new Pair<Integer,Double>(i1, 1.)); //i1 is a random alignment
                sparse_alignment++;
                number[current_row] = sparse_alignment;
                D.p("!it.hasNext(): i1=" + i1);
            }
            while (it.hasNext()) {
                if (sparse_alignment >= setting.max_sparse_alignment){
                    break;
                }
                a = it.next();
                j = fs2.id_big_to_small(a.similarityStatement.predTermIndex);
                j1 = fs2.id_small_to_1v1(j);
                temp_alignments.add(new Pair<Integer,Double>(j1, 1 - a.truthValue.getExpectation()));
                sparse_alignment++;
                number[current_row] = sparse_alignment;
            }
            Collections.sort(temp_alignments, new Comparator<Pair<Integer,Double>>() {
                @Override
                public int compare(Pair<Integer,Double> p1, Pair<Integer,Double> p2) {
                    return p1.first - p2.first;
                }
                });
            for (Pair<Integer,Double> t : temp_alignments) {
                cc[current_cc_size] = t.second;
                kk[current_cc_size] = t.first;
                current_cc_size++;
            }
        }
        cm = new SparseCostMatrix(cc, kk, number, nRows, nCols, current_cc_size);
        LAPJV lapjv = new LAPJV(cm);
        if(!lapjv.checkInput()){
            D.p(lapjv.getErrorMessage());
            return;
        }
        lapjv.process();
        int[] result = lapjv.getResult();
        D.p("lapjv.getProcessingTime()");
        D.p(lapjv.getProcessingTime());
        a = null;//new AlignmentSentence(-1, -1, null, 1);
        for (int i : alignment_sentences_fs1_To_fs2.keySet()) {
            if (!fs1.belong_to_1v1_assumption(i)){
                continue;
            }
            i1 = fs1.id_small_to_1v1(i);
            list = alignment_sentences_fs1_To_fs2.get(i);
            it = list.listIterator();
            j1 = -1;
            while (it.hasNext()) {
                a = it.next();
                j = fs2.id_big_to_small(a.similarityStatement.predTermIndex);
                j1 = fs2.id_small_to_1v1(j);
                if (result[i1] == j1){
                    break;
                }
            }
            list = new LinkedList<>();
            if (result[i1] == j1){
                list.add(a);
            }
            alignment_sentences_fs1_To_fs2.put(i, list);
        }
        takeMaxMaxBothWays();
    }


    public void lapjv() {
        LinkedList<AlignmentSentence> list;
        Iterator<AlignmentSentence> it;
        AlignmentSentence a;
        int j, i1, j1;
        double[][] assigncost = new double[fs1._1v1_assumption_size][fs2._1v1_assumption_size];
        int current_row = -1;
        for (int i = 0; i < assigncost.length; i++) {
            for (j = 0; j < assigncost[0].length; j++) {
                assigncost[i][j] = 1;
            }
        }
        for (int i : alignment_sentences_fs1_To_fs2.keySet()) {
            //getTraceInfo("i ", fs1.proper_entity_s(i));
            if (!fs1.belong_to_1v1_assumption(i)){
                continue;
            }
            current_row++;
            list = alignment_sentences_fs1_To_fs2.get(i);
            i1 = fs1.id_small_to_1v1(i);
            it = list.listIterator();
            while (it.hasNext()) {
                a = it.next();
                j = fs2.id_big_to_small(a.similarityStatement.predTermIndex);
                j1 = fs2.id_small_to_1v1(j);
                assigncost[i1][j1] = 1 - a.truthValue.getExpectation();
            }
        }
        final long start = System.currentTimeMillis();
        int[] result = lapjv.execute(assigncost);
        final long end = System.currentTimeMillis();
        final long processingTime = end - start;
        D.p("lapjv processingTime:", processingTime);
        assert(fs1._1v1_assumption_size <= fs2._1v1_assumption_size);
        j = 0;
        for (int i : alignment_sentences_fs1_To_fs2.keySet()){
            if (!fs1.belong_to_1v1_assumption(i)){
                continue;
            }
            j++;
            D.p(fs1.entity(fs1.id_small_to_big(i)), "\t", fs2.entity(fs2.id_small_to_big(fs2.id_1v1_to_small(result[fs1.id_small_to_1v1(i)]))));
            if (j>10)
                break;
        }
        D.p("\n\n");
        a = null;//new AlignmentSentence(-1, -1, null, 1);
        for (int i : alignment_sentences_fs1_To_fs2.keySet()) {
            if (!fs1.belong_to_1v1_assumption(i)){
                continue;
            }
            i1 = fs1.id_small_to_1v1(i);
            list = alignment_sentences_fs1_To_fs2.get(i);
            it = list.listIterator();
            j1 = -1;
            while (it.hasNext()) {
                a = it.next();
                j = fs2.id_big_to_small(a.similarityStatement.predTermIndex);
                j1 = fs2.id_small_to_1v1(j);
                if (result[i1] == j1){
                    break;
                }
            }
            list = new LinkedList<>();
            if (result[i1] == j1){
                list.add(a);
            }
            alignment_sentences_fs1_To_fs2.put(i, list);
        }
        //populate_right_to_left();
        //takeMaxMaxBothWays();
    }

    public TruthValue getValueInt_left(int left_big, int right_big) {
        Integer left_small = fs1.id_big_to_small(left_big);
        ListIterator<AlignmentSentence> it = alignment_sentences_fs1_To_fs2.get(left_small).listIterator();
        while (it.hasNext()) {	
            AlignmentSentence a = it.next();
            if(a.similarityStatement.predTermIndex == right_big){
                return a.truthValue;
            }
        }
        //return null;
        if (setting.use_c_as_probability_value)
            return new TruthValue(1, 0);
        else
            return new TruthValue(0, setting.unaligned_entity_equal_initial_confidence);
    }

    public TruthValue getValueInt_right(int right_big, int left_big) {
        Integer right_small = fs2.id_big_to_small(right_big);
        ListIterator<AlignmentSentence> it = alignment_sentences_fs2_To_fs1.get(right_small).listIterator();
        while (it.hasNext()) {	
            AlignmentSentence a = it.next();
            if(a.similarityStatement.predTermIndex == left_big){
                return a.truthValue;
            }
        }
        //return null;
        if (setting.use_c_as_probability_value)
            return new TruthValue(1, 0);
        else
            return new TruthValue(0, setting.unaligned_entity_equal_initial_confidence);
    }

	public Collection<Pair<Object, TruthValue>> left_to_right(Integer left_big) {
        Integer left_small = fs1.id_big_to_small(left_big);
		List<Pair<Object, TruthValue>> result = new ArrayList<Pair<Object, TruthValue>>();
        ListIterator<AlignmentSentence> it = alignment_sentences_fs1_To_fs2.get(left_small).listIterator();
        while (it.hasNext()) {	
            AlignmentSentence a = it.next();
            if(a.truthValue.getConfidence() < Config.THETA){
                continue;
            }
            if(a.truthValue.getFrequency() < Config.THETA){
                continue;
            }
            result.add(new Pair<Object, TruthValue>(a.similarityStatement.predTermIndex, new TruthValue(a.truthValue)));
        }
		return (result);
    }

    public Collection<Pair<Object, TruthValue>> right_to_left(Integer right_big) {
        Integer right_small = fs2.id_big_to_small(right_big);
		List<Pair<Object, TruthValue>> result = new ArrayList<Pair<Object, TruthValue>>();
        ListIterator<AlignmentSentence> it = alignment_sentences_fs2_To_fs1.get(right_small).listIterator();
        while (it.hasNext()) {	
            AlignmentSentence a = it.next();
            if(a.truthValue.getConfidence() < Config.THETA){
                continue;
            }
            if(a.truthValue.getFrequency() < Config.THETA){
                continue;
            }
            result.add(new Pair<Object, TruthValue>(a.similarityStatement.predTermIndex, new TruthValue(a.truthValue)));
        }
		return (result);
	}


    public void insert_alignment_sentence_left(AlignmentSentence inserting_alignment_sentence){
        insert_alignment_sentence_left(inserting_alignment_sentence, alignment_sentences_fs1_To_fs2, fs1, setting.max_alignment_sentences);
    }


    public static void insert_alignment_sentence_left(AlignmentSentence inserting_alignment_sentence, HashMap<Integer, LinkedList<AlignmentSentence>> alignment_sentences_fs1_To_fs2, FactStore fs1, int max_alignment_sentences){
        int subj_big = inserting_alignment_sentence.similarityStatement.subjTermIndex;
        int subj_small = fs1.id_big_to_small(subj_big);
        LinkedList<AlignmentSentence> list = alignment_sentences_fs1_To_fs2.get(subj_small);
        ListIterator<AlignmentSentence> it = list.listIterator();
        boolean inserted = false;
        while (it.hasNext()) {
            AlignmentSentence a = it.next();
            if(inserting_alignment_sentence.truthValue.getExpectation() > a.truthValue.getExpectation()){
                it.previous();
                it.add(inserting_alignment_sentence);
                inserted = true;
                break;
            }
            else if (inserting_alignment_sentence.truthValue.getExpectation() == a.truthValue.getExpectation()) {
                if(inserting_alignment_sentence.similarityStatement.predTermIndex > a.similarityStatement.predTermIndex){
                    it.previous();
                    it.add(inserting_alignment_sentence);
                    inserted = true;
                    break;
                }
                //inserting_alignment_sentence.truthValue.setConfidence(inserting_alignment_sentence.truthValue.getConfidence() - 0.01);
                //inserting_alignment_sentence.truthValue.setFrequency(inserting_alignment_sentence.truthValue.getFrequency() - 0.01);
                //70000+ count
                //getTraceInfo("i ", fs1.proper_entity_s(subj_small));
                //if (inserting_alignment_sentence.truthValue.getConfidence() != 0 && inserting_alignment_sentence.truthValue.getFrequency() != 0 ){
                //    it.previous();
                //    continue;
                //}
                //else
                //    return;
            }
        }
        if (!inserted && list.size() < max_alignment_sentences){
            list.addLast(inserting_alignment_sentence);
            return;
        }
        if(list.size() > max_alignment_sentences){
            list.removeLast();
        }
    }

    public void insert_alignment_sentence_right(AlignmentSentence inserting_alignment_sentence){
        int subj_big = inserting_alignment_sentence.similarityStatement.subjTermIndex;
        int subj_small = fs2.id_big_to_small(subj_big);
        LinkedList<AlignmentSentence> list = alignment_sentences_fs2_To_fs1.get(subj_small);
        ListIterator<AlignmentSentence> it = list.listIterator();
        boolean inserted = false;
        while (it.hasNext()) {
            AlignmentSentence a = it.next();
            if(inserting_alignment_sentence.truthValue.getExpectation() > a.truthValue.getExpectation()){
                it.previous();
                it.add(inserting_alignment_sentence);
                inserted = true;
                break;
            }
            else if (inserting_alignment_sentence.truthValue.getExpectation() == a.truthValue.getExpectation()) {
                if(inserting_alignment_sentence.similarityStatement.predTermIndex > a.similarityStatement.predTermIndex){
                    it.previous();
                    it.add(inserting_alignment_sentence);
                    inserted = true;
                    break;
                }
            }
        }
        if (!inserted && list.size() < setting.max_alignment_sentences){
            list.addLast(inserting_alignment_sentence);
            return;
        }
        if(list.size() > setting.max_alignment_sentences){
            list.removeLast();
        }
    }

    public void populate_right_to_left() {
        //alignment_sentences_fs2_To_fs1 = new HashMap<Integer, LinkedList<AlignmentSentence>>((int)(fs2.num_proper_entities()/0.75 + 1));
        for (int i = 0; i < fs2.num_proper_entities(); i++) {
            alignment_sentences_fs2_To_fs1.put(i, new LinkedList<AlignmentSentence>());
        }
        for (int i : alignment_sentences_fs1_To_fs2.keySet()) {
            LinkedList<AlignmentSentence> list = alignment_sentences_fs1_To_fs2.get(i);
            ListIterator<AlignmentSentence> it = list.listIterator();
            while (it.hasNext()) {
                AlignmentSentence a = it.next();
                insert_alignment_sentence_right(a.swap_clone());
            }
        }
    }

    public void populate_right_to_left_1() {
        //alignment_sentences_fs2_To_fs1 = new HashMap<Integer, LinkedList<AlignmentSentence>>((int)(fs2.num_proper_entities()/0.75 + 1));
        for (int i = 0; i < fs2.num_proper_entities(); i++) {
            alignment_sentences_fs2_To_fs1.put(i, new LinkedList<AlignmentSentence>());
        }
        for (int i : alignment_sentences_fs1_To_fs2.keySet()) {
            LinkedList<AlignmentSentence> list = alignment_sentences_fs1_To_fs2.get(i);
            ListIterator<AlignmentSentence> it = list.listIterator();
            if (it.hasNext()) {
                AlignmentSentence a = it.next();
                insert_alignment_sentence_right(a.swap_clone());
            }
        }
    }

    // only displays alignment_sentences_fs1_To_fs2
    public void display_evidence(AlignmentSentence alignmentSentence, Result computed, BufferedWriter w, boolean english) throws IOException {
        if (english)
            w.write("Explaining alignment pair:\n");
        else
            w.write("解释对齐实体对：\n");
        int y1 = alignmentSentence.similarityStatement.subjTermIndex;
        int y2 = alignmentSentence.similarityStatement.predTermIndex;
        int x1, x2, r1, r2, i=-1;
        BaseEntry base;
        PredicateAndObject po1, po2;
        TruthValue r2_inherits_r1, r1_inherits_r2, t1, t2;
        TruthValue xeqv;
        w.write(fs1.entity(y1) + " <-> " + fs2.entity(y2) + " . " + alignmentSentence.truthValue.toString() + "\n");
        ListIterator<BaseEntry> it = alignmentSentence.stamp.get_evidential_Iterator();
        while (it.hasNext()) {
            base = it.next();
            i++;
            if (i >= setting.display_evidence_count){
                break;
            }
            switch(base.evidence_type){
                case 1:
                    if (english)
                        w.write("{\nThe " + i + "th evidence is from path type 1:\n");
                    else
                        w.write("{\n 第" + i + "条证据：\n由以下两条fact、关系/属性谓词的对齐、实体对齐推理得到:\n");
                    //PredicateAndObject po1 = 
                    po1 = fs1.factsAbout(y1).get(base.fact_id_1);
                    x1 = po1.object;
                    x2 = base.x2;
                    po2 = fs2.factsAbout(x2).get(base.fact_id_2);
                    assert(y2 == po2.object);
                    r1 = FactStore.inverse(po1.predicate);
                    r2 = po2.predicate;
                    if (setting.modify_initial_confidence){
                        t1 = new TruthValue(fs1.facts_truthvalue.get(y1).get(base.fact_id_1));
                        t2 = new TruthValue(fs2.facts_truthvalue.get(x2).get(base.fact_id_2));
                    }
                    else{
                        if (fs1.isLiteral(x1)){
                            t1 = new TruthValue(setting.attribute_triple_initial_frequency_fs1, setting.attribute_triple_initial_confidence_fs1);
                            t2 = new TruthValue(setting.attribute_triple_initial_frequency_fs2, setting.attribute_triple_initial_confidence_fs2);
                        }
                        else{
                            t1 = new TruthValue(setting.relation_triple_initial_frequency_fs1, setting.relation_triple_initial_confidence_fs1);
                            t2 = new TruthValue(setting.relation_triple_initial_frequency_fs2, setting.relation_triple_initial_confidence_fs2);
                        }
                    }
                    w.write("fact1:  " + fs1.entity(y1) + " " + po1.toString() + " . " + t1.toString() + "\n"); 
                    w.write("fact2:  " + fs2.entity(y2) + " " + fs2.relation(FactStore.inverse(po2.predicate)) + " " + fs2.entity(x2)+ " . " + t2.toString() + "\n"); 
                    r2_inherits_r1 = computed.subRelation(fs2, r2, r1);
                    r1_inherits_r2 = computed.subRelation(fs1, r1, r2);
                    w.write(fs1.relation(r1) + " --> " + fs2.relation(r2) + " " + r1_inherits_r2 + "\n");
                    w.write("functionality: " + fs2.relation(r2) + " " + fs2.functionality(r2) + "\n");
                    w.write(fs2.relation(r2) + " --> " + fs1.relation(r1) + " " + r2_inherits_r1 + "\n"); 
                    w.write("functionality: " + fs1.relation(r1) + " " + fs1.functionality(r1) + "\n");
                    xeqv = base.x_truth; //xeqv = computed.equality(fs1, x1, x2);
                    w.write(fs1.entity(x1) + " <-> " + fs2.entity(x2) + "  " + xeqv.toString() + "\n");
                    if (english)
                        w.write("The conclusion of the path (also a piece of evidence) is" + fs1.entity(y1) + " <-> " + fs2.entity(y2) + " . " + base.truthValue.toString() + "\n");
                    else
                        w.write("所得语句及（属于该条证据的）真值为" + fs1.entity(y1) + " <-> " + fs2.entity(y2) + " . " + base.truthValue.toString() + "\n");
                    w.write("}\n");
                    break;
                case 2:
                    if (english)
                        w.write("{\nThe " + i + "th evidence is from path type 2:\n");
                    else
                        w.write("{\n 第" + i + "条证据：\n由name/description嵌入相似度得到\n");
                    if (english)
                        w.write("The conclusion of the path (also a piece of evidence) is" + fs1.entity(y1) + " <-> " + fs2.entity(y2) + " . " + base.truthValue.toString() + "\n");
                    else
                        w.write("所得语句及（属于该条证据的）真值为" + fs1.entity(y1) + " <-> " + fs2.entity(y2) + " . " + base.truthValue.toString() + "\n");
                    w.write("}\n");
            }
            
        }      
    }

    public void display_all_evidences(Result computed, boolean english) throws IOException {
        BufferedWriter w = new BufferedWriter(tsvWriter);
        String str;
        if (english)
            str = "Here shows all/partial entity alignment evidences\n";
        else
            str = "展示部分/全部对齐的证据\n";
        w.write(str);
        for (int i : alignment_sentences_fs1_To_fs2.keySet()) {
            if (i>1000)
                break;
            if (english)
                str = "For the" + i + "th entity   " + fs1.entity(i) + ":\n";
            else
                str = "对于第" + i + "个实体   " + fs1.entity(i) + ":\n";
            w.write(str);
            LinkedList<AlignmentSentence> list = alignment_sentences_fs1_To_fs2.get(i);
            ListIterator<AlignmentSentence> it = list.listIterator();
            while (it.hasNext()) {
                AlignmentSentence a = it.next();
                display_evidence(a, computed, w, english);
            }
        }
        w.close();
    }

    public void dump_eqv_full(File file, int display_num, boolean use_temp_alignment) throws IOException {
        dump_eqv_full(file, display_num, use_temp_alignment, true, -1);
    }

    public void dump_eqv_full(File file, int display_num, boolean use_temp_alignment, int limit) throws IOException {
        dump_eqv_full(file, display_num, use_temp_alignment, true, limit);
    }

    public void dump_eqv_full(File file, int display_num, boolean use_temp_alignment, boolean left_to_right, int limit) throws IOException {
        if (tsvWriter != null) try {
            tsvWriter.close();
        } catch (IOException e) {
        }
        if(use_temp_alignment){
            Announce.doing("Saving TSV");
            BufferedWriter w = new BufferedWriter(new FileWriter(file));
            String str = "";
            int y1, y2;
            int k=0;
            for (int i : temp_alignment_proper_id_sentences.keySet()) {
                k=0;
                AlignmentSentence a = temp_alignment_proper_id_sentences.get(i);
                y1 = a.similarityStatement.subjTermIndex;
                y2 = a.similarityStatement.predTermIndex;
                str = fs1.entity(y1) + "\t" + fs2.entity(y2) + "\t" + a.truthValue.toString() + "\n";
                w.write(str);
            }
            w.close();
            Announce.done();
            return;
        }
        HashMap<Integer, LinkedList<AlignmentSentence>> this_to_other, other_to_this;
        FactStore fs_this,fs_other;
        if (left_to_right){
            this_to_other = alignment_sentences_fs1_To_fs2;
            other_to_this = alignment_sentences_fs2_To_fs1;
            fs_this = fs1;
            fs_other = fs2;
        }
        else{
            this_to_other = alignment_sentences_fs2_To_fs1;
            other_to_this = alignment_sentences_fs1_To_fs2;
            fs_this = fs2;
            fs_other = fs1;
        }
        Announce.doing("Saving TSV");
        BufferedWriter w = new BufferedWriter(new FileWriter(file));
        String str = "";
        int y1, y2;
        int k=0;
        for (int i : this_to_other.keySet()) {
            k=0;
            LinkedList<AlignmentSentence> list = this_to_other.get(i);
            ListIterator<AlignmentSentence> it = list.listIterator();
            while (it.hasNext()) {
                k++;
                AlignmentSentence a = it.next();
                y1 = a.similarityStatement.subjTermIndex;
                y2 = a.similarityStatement.predTermIndex;
                str = fs_this.entity(y1) + "\t" + fs_other.entity(y2) + "\t" + a.truthValue.toString() + "\n";
                w.write(str);
                if(k >= display_num)
                    break;
            }
            if(limit > 0 && i > limit)
                break;
        }
        w.close();
        Announce.done();
    }
    

	
//	/** only keep the n pairs in the store with highest score (and pairs tied with them) */
//	public synchronized void threshold(int n) {
//	  DoubleList scores=new DoubleArrayStack();
//	  for(int i = 0; i < subIndexScore.length; i++) {
//	  	if(subIndexScore[i] == 0.) continue;
//	    DoubleCollection values=((IntKeyDoubleOpenHashMap)(it.getValue())).values();
//	    scores.addAll(values);
//	  }
//	  if(scores.size()<=n) return;
//	  double[] scoresdb=scores.toArray();
//	  Arrays.sort(scoresdb);
//	  double pivot=scoresdb[scoresdb.length-n];
//	  it = index.entries();
//    while(it.hasNext()) {
//      it.next();
//      IntKeyDoubleOpenHashMap values=((IntKeyDoubleOpenHashMap)(it.getValue()));
//      IntKeyDoubleMapIterator it2 = values.entries();
//      while(it2.hasNext()) {
//        it2.next();
//        if(it2.getValue()<pivot) it2.remove();
//      }
//    }
//	}

    public static void main(String[] args) throws Exception {
        /*
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j <  5; j++) {	
                //xch2.0 notice that the alignment_sentences_fs2_To_fs1's sentence's subjTermIndex is a term index in fs2
                //while alignment_sentences_fs1_To_fs2's sentence's subjTermIndex is a term index in fs1
                if (j == 3){
                    break;
                }
                D.p(i);
                D.p(j);
                D.p("");
            }
        }  */
            /** premises:  (∗, x1, y1) -> r1          ...1   f1=1
             *             (∗, x2, y2) -> r2          ...2   f2=1
             *                      r1 -> r2          ...3
             *                      r2 -> r1          ...4
             *                     x1 <-> x2          ...5
             *                      r1 -> [fun]       ...6
             *                      r2 -> [fun]       ...7
             *           ((∗, #a, $b1) -> #r ∧ (∗, #a, $b2) -> #r ∧ #r -> [fun]) ⇒ $b1 <-> $b2          ...8
             *                      
             *       inference path 1:
             *       from 1 and 3: (∗, x1, y1) -> r2          ...9     f=f3  c=f3*c1*c3              P54 Deduction 
             *       from 2 and 5: (∗, x1, y2) -> r2          ...10    f=f5  c=f5*c2*c5              P80 Analogy  
             *       from 8, 9, 10 and 7: y1 <-> y2           ...11      P118 Conditional deduction*3
             *   
             */
        Setting setting = new Setting(null, "D:/", "nu", "nu", null, "nu",null);
        EqualityStore equalityStore = new EqualityStore(10, setting);
        TruthValue truthValue1 = new TruthValue(1,1);
        TruthValue truthValue2 = new TruthValue(1f, 1);
        TruthValue truthValue3 = new TruthValue(1f, 1);
        TruthValue truthValue4 = new TruthValue(0.2f, 1);
        TruthValue truthValue5 = new TruthValue(1,1);
        TruthValue truthValue6 = new TruthValue(1,1);
        TruthValue truthValue7 = new TruthValue(1,1);
        TruthValue truthValue8 = new TruthValue(1,1);
        TruthValue truthValue9  = TruthFunctions.deduction(truthValue1, truthValue3);
        TruthValue truthValue10  = TruthFunctions.analogy(truthValue2, truthValue5);

        TruthValue temp;
        temp = TruthFunctions.conditional_deduction_3(truthValue8, truthValue9, truthValue10, truthValue7);

        
        D.p(temp.getFrequency());
        D.p(temp.getConfidence());
        temp = TruthFunctions.revision(temp, temp);
        D.p(temp.getFrequency());
        D.p(temp.getConfidence());
        /*
        temp = TruthFunctions.deduction(truthValue1, truthValue2);
        temp = TruthFunctions.deduction(temp, truthValue3);
        temp = TruthFunctions.deduction(temp, truthValue4);
        D.p(temp.getFrequency());
        D.p(temp.getConfidence());

        temp = TruthFunctions.conditional_deduction_3(truthValue1, truthValue2, truthValue3, truthValue4);
        D.p(temp.getFrequency());
        D.p(temp.getConfidence());
        
        0.9 0.7  0.94 0.8
        0.846 0.47376  0.8 0.85
        0.6768 0.251742528  0.6 0.7 
        0.40608 0.071559324039168

        */
        AlignmentSentence alignmentSentence1 = new AlignmentSentence(2, 1, truthValue1, null);
        AlignmentSentence alignmentSentence2 = new AlignmentSentence(2, 0, truthValue2, null);
        AlignmentSentence alignmentSentence3 = new AlignmentSentence(2, 3, truthValue3, null);
        AlignmentSentence alignmentSentence4 = new AlignmentSentence(2, 4, truthValue4, null);
        AlignmentSentence alignmentSentence5 = new AlignmentSentence(2, 2, truthValue5, null);
        AlignmentSentence alignmentSentence6 = new AlignmentSentence(2, 5, truthValue6, null);
        AlignmentSentence alignmentSentence7 = new AlignmentSentence(7, 4, truthValue1, null);
        equalityStore.insert_alignment_sentence_left(alignmentSentence1);
        equalityStore.insert_alignment_sentence_left(alignmentSentence4);
        equalityStore.insert_alignment_sentence_left(alignmentSentence3);
        equalityStore.insert_alignment_sentence_left(alignmentSentence2);
        equalityStore.insert_alignment_sentence_left(alignmentSentence6);
        equalityStore.insert_alignment_sentence_left(alignmentSentence5);
        equalityStore.insert_alignment_sentence_left(alignmentSentence7);
        for (int i : equalityStore.alignment_sentences_fs1_To_fs2.keySet()) {
            LinkedList<AlignmentSentence> list = equalityStore.alignment_sentences_fs1_To_fs2.get(i);
            ListIterator<AlignmentSentence> it = list.listIterator();
            while (it.hasNext()) {
                AlignmentSentence a = it.next();
                D.p(a.toString());
            }
        }
        D.p("");
        equalityStore.populate_right_to_left();
        for (int i : equalityStore.alignment_sentences_fs2_To_fs1.keySet()) {
            LinkedList<AlignmentSentence> list = equalityStore.alignment_sentences_fs2_To_fs1.get(i);
            ListIterator<AlignmentSentence> it = list.listIterator();
            while (it.hasNext()) {
                AlignmentSentence a = it.next();
                D.p(a.toString());
            }
        }
    }
}
