package nala.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Map;
import java.util.regex.Pattern;
import java.lang.Math;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.Pair;
import javatools.parsers.NumberFormatter;
import nala.Config;
import nala.JoinRelation;
import nala.Result;
import nala.Setting;
import nala.nal.AlignmentSentence;
import nala.nal.TruthValue;
import nala.shingling.QueryResult;
import nala.shingling.ShinglingTable;
import nala.storage.FactStore.PredicateAndObject;
import nala.nal.TruthFunctions;
import javatools.filehandlers.FileLines;
import bak.pcj.IntIterator;
//import bak.pcj.benchmark.Result;
import bak.pcj.map.ObjectKeyIntMap;
import bak.pcj.map.IntKeyOpenHashMap;
import bak.pcj.map.ObjectKeyIntOpenHashMap;
import bak.pcj.set.IntOpenHashSet;
import bak.pcj.set.IntSet;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;

/** Represents a set of facts */
public class FactStore {

    /** Approximate string index */
    ShinglingTable literalIndex;

    /** Maps entity names and literals to their ids */
    // caution that pcj.map returns default value(0)
    public ObjectKeyIntMap entityNames = new ObjectKeyIntOpenHashMap();

    /**
     * Maps relation names to their ids, everyone followed by its inverse relation
     */
    public ObjectKeyIntMap relationNames = new ObjectKeyIntOpenHashMap();

    /** Maps ids to entity names */
    public ArrayList<String> entities = new ArrayList<>();

    /** Maps ids to relation names */
    public ArrayList<String> relations = new ArrayList<>();

    //public ArrayList<Boolean> relation_is_attributive = new ArrayList<>();

    /** stores embeddings of entity name/description. keys in entity_emb must be proper_entity.   */
    // entity_id(big) --> embedding
    public IntKeyOpenHashMap entity_emb = new IntKeyOpenHashMap();
    public IntKeyOpenHashMap trans_entity_emb = new IntKeyOpenHashMap();

    //xch2.0   proper_entity指不是Literal或者class的实体，即单纯的实体 的编号
    public int[] entity_id_to_proper_entity_id;
    public int[] proper_entity_id_to_entity_id;
    public int[] proper_entity_id_to_1v1_id;
    public int[] _1v1_id_to_proper_entity_id;

    /** Maps to the functionalities */
    public double[] functionalities;

    /** Holds the prefix of the fact store */
    public final String prefix;

    /** Holds the prefix expansion of the fact store */
    public final String uri;

    /** Formatters of this fact store */
    public final LiteralFormatter[] formatters;

    /** Store if something is a class */
    public boolean isClass[];
    public boolean isLiteral[];
    //xch1
    public boolean is_proper_entity[];
    public boolean belong_to_1v1_assumption[];
    public int _1v1_assumption_size;
    //public int belong_to_1v1_assumption[];
    public boolean is_attribute_relation[];

    public boolean has_1v1_assumption = false;


    public int joinLengthLimit;

    /**
     * True when the factStore has been loaded and should not be modified any more
     */
    public boolean finalized = false;

    int cached_num_proper_entities;

    int cachedNumLiterals;

    int cachedNumClasses;

    public Setting setting;

    public boolean has_unused = true;
    /** Constant for rdf:type*/
    public int TYPE;

    /** Constant for rdfs:subclassOf*/
    public int SUBCLASSOF;

    public int fs_id;
    public float relation_triple_initial_frequency;
    public double relation_triple_initial_confidence;
    public float attribute_triple_initial_frequency;
    public double attribute_triple_initial_confidence;

    public ArrayList<Boolean> value_in_factStore;
    //public ArrayList<LinkedList<Pair<Integer, Float>>> value_emb_sim;
    //value_emb_sim : value_id --> <entity id of other fs, sim>
    public HashMap<Integer,LinkedList<Pair<Integer, Float>>> value_emb_sim;

    /** Constructor */
    public FactStore(int fs_id, Setting setting, String prefix, String uri, int joinLengthLimit, LiteralFormatter... formis) {
        this.prefix = prefix;
        this.uri = uri;
        this.joinLengthLimit = joinLengthLimit;
        this.setting = setting;
        formatters = formis;
        this.fs_id = fs_id;
        if (this.has_unused){
            addRelation("<xxx-unused>");
            addEntity("<xxx-unused>");
        }
        if(fs_id==1){
            relation_triple_initial_frequency = setting.relation_triple_initial_frequency_fs1;
            relation_triple_initial_confidence = setting.relation_triple_initial_confidence_fs1;
            attribute_triple_initial_frequency = setting.attribute_triple_initial_frequency_fs1;
            attribute_triple_initial_confidence = setting.attribute_triple_initial_confidence_fs1;
        }
        else {
            relation_triple_initial_frequency = setting.relation_triple_initial_frequency_fs2;
            relation_triple_initial_confidence = setting.relation_triple_initial_confidence_fs2;
            attribute_triple_initial_frequency = setting.attribute_triple_initial_frequency_fs2;
            attribute_triple_initial_confidence = setting.attribute_triple_initial_confidence_fs2;
        }
    }

    public static LiteralFormatter[] getArgs(boolean normalizeStrings, boolean normalizeDatesToYears) {
        Collection<LiteralFormatter> formis = new LinkedList<LiteralFormatter>();
        formis.add(LiteralFormatter.CUT_DATATYPE);
        if (normalizeStrings) formis.add(LiteralFormatter.NORMALIZE);
        if (normalizeDatesToYears) formis.add(LiteralFormatter.TRIM_TO_YEAR);
        return (LiteralFormatter[]) formis.toArray(new LiteralFormatter[formis.size()]);
    }

    /** Constructor */
    public FactStore(int fs_id, Setting setting, String prefix, String uri, int joinLengthLimit, boolean normalizeStrings, boolean normalizeDatesToYears) {
        this(fs_id, setting, prefix, uri, joinLengthLimit, getArgs(normalizeStrings, normalizeDatesToYears));
    }

    public FactStore deep_copy(){
        FactStore fs = new FactStore(fs_id, setting, prefix, uri, joinLengthLimit, formatters);
        //todo:迁移facts
        fs.entityNames = new ObjectKeyIntOpenHashMap(entityNames);
        fs.relationNames = new ObjectKeyIntOpenHashMap(relationNames);
        fs.entities = new ArrayList<>(entities);
        fs.relations = new ArrayList<>(relations);
        fs.is_attribute_relation = is_attribute_relation.clone();
        fs.entity_id_to_proper_entity_id = entity_id_to_proper_entity_id.clone();
        fs.proper_entity_id_to_entity_id = proper_entity_id_to_entity_id.clone();
        fs.proper_entity_id_to_1v1_id = proper_entity_id_to_1v1_id.clone();
        fs._1v1_id_to_proper_entity_id = _1v1_id_to_proper_entity_id.clone();
        //functionalities
        fs.isClass = isClass.clone();
        fs.isLiteral = isLiteral.clone();
        fs.is_proper_entity = is_proper_entity.clone();
        fs.belong_to_1v1_assumption = belong_to_1v1_assumption.clone();
        fs.has_1v1_assumption = has_1v1_assumption;
        fs.is_attribute_relation = is_attribute_relation.clone();
        fs.finalized = false;
        fs.cached_num_proper_entities = -1;
        fs.cachedNumLiterals = -1;
        fs.cachedNumClasses = -1;
        fs.TYPE = TYPE;
        fs.SUBCLASSOF = SUBCLASSOF;
        return fs;
    }

    //take the facts from FactStore other and put them in FactStore this 
    public void merge(FactStore other, Result computed){
        assert(fs_id + other.fs_id == 3);
        HashMap<Integer, LinkedList<AlignmentSentence>> this_to_other, other_to_this;
        //FactStore fs_this,fs_other;
        if (fs_id == 1){
            this_to_other = computed.equalityStore.alignment_sentences_fs1_To_fs2;
            other_to_this = computed.equalityStore.alignment_sentences_fs2_To_fs1;
        }
        else if (fs_id == 2){
            this_to_other = computed.equalityStore.alignment_sentences_fs2_To_fs1;
            other_to_this = computed.equalityStore.alignment_sentences_fs1_To_fs2;
        }
        else return;
        int y1,y2;
        LinkedList<AlignmentSentence> list;
        ListIterator<AlignmentSentence> it;
        JoinRelation r1,r2_reverse;
        for (int i : this_to_other.keySet()) {
            list = this_to_other.get(i);
            it = list.listIterator();
            //only gets the first Alignment
            if (!it.hasNext()) 
                continue;
            AlignmentSentence a = it.next();
            y1 = a.similarityStatement.subjTermIndex;
            y2 = a.similarityStatement.predTermIndex;
            List<PredicateAndObject> facts = other.factsAbout(y2); 
			for (int j = 0; j < facts.size(); j++) {
				int y2_predicate = facts.get(j).predicate;
				int y2_obj = facts.get(j).object;
                //attr predicate only has inheritance statement of its reversed form. such as
                //attr_dbp_en_prop:source-	attr_dbp_zh_prop:source-	%0.22;0.99%
                r2_reverse = other.joinRelationByCode(other.inverse(y2_predicate));
                //待解决：双语的属性值可能会造成函数性失真（难以判断属性值是否为同一个）
            }
        }
        for (int i = 0; i < this.maxJoinRelationCode(); i++) {
            r1 = this.joinRelationByCode(i);
        }
        //computed.superRelationsOf1.getTruthValue();
        
    }


    /** holds a predicate and an object */
    public class PredicateAndObject {

        public int predicate;

        public int object;

        public PredicateAndObject(int predicate2, int object2) {
            predicate = predicate2;
            object = object2;
        }

        @Override
        public String toString() {
            return relation(predicate) + " " + entity(object);
        }
    }

    public class SubjectAndObject {

        public int subject;

        public int object;

        public SubjectAndObject(int subject2, int object2) {
            subject = subject2;
            object = object2;
        }
    }

    /** Maps a subject to a list of predicates and objects */
    protected ArrayList<ArrayList<PredicateAndObject>> facts = new ArrayList<ArrayList<PredicateAndObject>>();
    protected ArrayList<ArrayList<SubjectAndObject>> facts_relation_view = new ArrayList<ArrayList<SubjectAndObject>>();
    public ArrayList<ArrayList<TruthValue>> facts_truthvalue = new ArrayList<ArrayList<TruthValue>>();

    /** Adds a fact */
    public void add(int subject, int predicate, int object) {
        synchronized (facts) {
            facts.ensureCapacity(subject);
            while (facts.size() <= subject)
                facts.add(null);
            PredicateAndObject predAndObj = new PredicateAndObject(predicate, object);
            ArrayList<PredicateAndObject> factsAboutSubject = facts.get(subject);
            if (factsAboutSubject == null) facts.set(subject, factsAboutSubject = new ArrayList<PredicateAndObject>());
            factsAboutSubject.add(predAndObj);
        }
        if (!isInverse(predicate)) add(object, inverse(predicate), subject);
        if (!isInverse(predicate)&&setting.entity_clustering){
            synchronized (facts_relation_view) {
                facts_relation_view.ensureCapacity(predicate);
                while (facts_relation_view.size() <= predicate)
                    facts_relation_view.add(null);
                SubjectAndObject subAndObj = new SubjectAndObject(subject, object);
                ArrayList<SubjectAndObject> factsAboutPredicate = facts_relation_view.get(predicate);
                if (factsAboutPredicate == null) facts_relation_view.set(predicate, factsAboutPredicate = new ArrayList<SubjectAndObject>());
                factsAboutPredicate.add(subAndObj);
            }   
        }
        
    }

    //从大id（所有实体和字面量）转为小id（所有实体）
    public int id_big_to_small(int big){
        return entity_id_to_proper_entity_id[big];
    }

    public int id_small_to_big(int small){
        return proper_entity_id_to_entity_id[small];
    }

    public int id_small_to_1v1(int small){
        return proper_entity_id_to_1v1_id[small];
    }

    public int id_1v1_to_small(int _1v1){
        return _1v1_id_to_proper_entity_id[_1v1];
    }

    /** Returns number of entities */
    public int numEntities() {
        return (entities.size());
    }

    public int num_proper_entities() {
        if (cached_num_proper_entities > 0) 
            return cached_num_proper_entities;
        int n = 0;
        for (int i = 0; i < numEntities(); i++)
            if (is_proper_entity(i)) 
                n++;
        if (finalized) 
            cached_num_proper_entities = n;
        return n;
    }

    /** Returns number of classes */
    public int numClasses() {
        if (cachedNumClasses > 0) return cachedNumClasses;
        int n = 0;
        for (int i = 0; i < numEntities(); i++)
            if (isClass(i)) n++;
        if (finalized) cachedNumClasses = n;
        return n;
    }

    /** Returns number of literals */
    public int numLiterals() {
        if (cachedNumLiterals > 0) return cachedNumLiterals;
        int n = 0;
        for (int i = 0; i < numEntities(); i++)
            if (isLiteral(i)) n++;
        if (finalized) cachedNumLiterals = n;
        return n;
    }

    /** Returns number of relations */
    public int numRelations() {
        return (relations.size());
    }

//  /** returns the number of facts */
//  public int size() {
//    return (facts.size());
//  }

    /** returns the functionality of a relation */
    public double functionality(int relation) {
        return (functionalities[relation]);
    }

    /** returns the inverse functionality of a relation */
    public double inverseFunctionality(int relation) {
        return (functionality(inverse(relation)));
    }

    public double functionality(JoinRelation r) {
        double res = 2;
        for (int i = 0; i < r.length(); i++)
            res = Math.min(res, functionality(r.get(i)));
        return res;
    }

    public double inverseFunctionality(JoinRelation r) {
        return functionality(r.reversed());
    }

    /** Check if an entity is a literal */
    public boolean isLiteral(String e) {
        return e.startsWith("\"");
    }

    public boolean isLiteral(int e) {
        if (this.finalized) {
            // cache is ready
            return isLiteral[e];
        }
        return isLiteral(entities.get(e));
    }

    /** Check if entity is a class */
    public boolean isClass(int e) {
        if (this.finalized) {
            // cache is ready
            return isClass[e];
        }
        if (isLiteral(e)) return false;
        ArrayList<PredicateAndObject> myFacts = facts.get(e);
        if (myFacts == null) return (false);
        int type = relation("rdf:type");
        int subClass = relation("rdfs:subclassOf");
        for (int i = 0; i < myFacts.size(); i++) {
            PredicateAndObject po = myFacts.get(i);
            if (po.predicate == inverse(type)) return true;
            if (po.predicate == subClass || po.predicate == inverse(subClass))
                    return true;
        }
        return false;
    }

    public boolean is_proper_entity(int e) {
        if (this.finalized) {
            // cache is ready
            return is_proper_entity[e];
        }
        //if (this.has_unused){
        //    if ((e!=0)&&(!isLiteral(e))&&(!isClass(e))) 
        //        return true;
        //    else
        //        return false;
        //}
        //else{
        if ((!isLiteral(e))&&(!isClass(e))) 
            return true;
        else
            return false;
        //}
    }

    public boolean is_attribute_relation(int e) {
        return is_attribute_relation[e];
    }

    //xch1
    //e is proper entity id (small)
    public boolean belong_to_1v1_assumption(int e) {
        if (this.finalized) {
            // cache is ready
            return belong_to_1v1_assumption[e];
        }
        Announce.message("not finalized");
        return false;
    }

    

    /** Populate caches */
    protected void populateCaches() {
        isClass = new boolean[numEntities()];
        isLiteral = new boolean[numEntities()];
        is_proper_entity = new boolean[numEntities()];
        for (int i = 0; i < numEntities(); i++) {
            isClass[i] = isClass(i);
        }
        for (int i = 0; i < numEntities(); i++) {
            isLiteral[i] = isLiteral(i);
        }
        for (int i = 0; i < numEntities(); i++) {
            is_proper_entity[i] = is_proper_entity(i);
        }
        // System.out.println(numEntities());
    }

    /** Computes the functionalities */
    protected void computeFunctionalities() {
        int[] numOccurrences = new int[numRelations()];
        functionalities = new double[numRelations()];
        int[] numSubjectsPerRelation = new int[numRelations()];
        Announce.progressStart("Computing functionalities in " + uri, numEntities());
        int[] lastSubject = new int[numRelations()];
        for (int subject = 0; subject < facts.size(); subject++) {
            Announce.progressStep();
            ArrayList<PredicateAndObject> myFacts = facts.get(subject);
            if (myFacts == null) continue;
            for (int fact = 0; fact < myFacts.size(); fact++) {
                PredicateAndObject po = myFacts.get(fact);
                numOccurrences[po.predicate]++;
                if (lastSubject[po.predicate] != subject) {
                        lastSubject[po.predicate] = subject;
                        numSubjectsPerRelation[po.predicate]++;
                }
            }
        }
        Announce.progressDone();
        Announce.doing("Functionalities");
        for (int relation = 0; relation < numRelations(); relation++) {
            functionalities[relation] = ((double) numSubjectsPerRelation[relation]) / numOccurrences[relation];
            Announce.message("functionality:", relations.get(relation), functionalities[relation]);
        }
        Announce.message("Number of literals:", numLiterals());
        Announce.done();
    }

    /** Returns the instances of a class */
    public IntSet instancesOf(int clss) {
        IntSet result = new IntOpenHashSet();
        ArrayList<PredicateAndObject> myFacts = facts.get(clss);
        if (myFacts == null) return (result);
        int type = inverse(TYPE);
        for (int i = 0; i < myFacts.size(); i++) {
            PredicateAndObject po = myFacts.get(i);
            if (po.predicate == type) result.add(po.object);
        }
        return (result);
    }

    /** Returns the classes of an instance */
    public IntSet classesOf(int instance) {
        IntSet result = new IntOpenHashSet();
        ArrayList<PredicateAndObject> myFacts = facts.get(instance);
        if (myFacts == null) return (result);
        for (int i = 0; i < myFacts.size(); i++) {
            PredicateAndObject po = myFacts.get(i);
            if (po.predicate == TYPE) result.add(po.object);
        }
        return (result);
    }

    /** returns the classes of an entity */
    public IntSet classesOf(String string) {
        return classesOf(entity(string));
    }

    /** returns the classes and superclasses of an entity */
    public IntSet classesAndSuperClassesOf(String string) {
        return classesAndSuperClassesOf(entity(string));
    }

    /** returns the classes and superclasses of an entity */
    public IntSet classesAndSuperClassesOf(int instance) {
        IntSet result = new IntOpenHashSet();
        IntIterator it=classesOf(instance).iterator();
        while(it.hasNext()) {
            addSuperClassesOf(it.next(), result);
        }
        return (result);
    }

    /** Adds the superclasses of a class*/
    protected void addSuperClassesOf(int c, IntSet result) {
        if(result.contains(c)) return;
        result.add(c);
        ArrayList<PredicateAndObject> myFacts = facts.get(c);
        if (myFacts == null) return;
        for (int i = 0; i < myFacts.size(); i++) {
            PredicateAndObject po = myFacts.get(i);
            if (po.predicate == SUBCLASSOF && !result.contains(po.object)) {
                addSuperClassesOf(po.object, result);
            }
        }
    }

    /** returns the instances of an entity */
    public IntSet instancesOf(String string) {
        return instancesOf(entity(string));
    }

    /** Returns the relation id of a relation name */
    public int relation(String relation) {
        return relationNames.get(relation);
    }

    /** Returns the entity id of an entity name */
    public int entity(String entity) {
        return entityNames.get(entity);
    }

    /** Returns the relation of a relation id */
    public String relation(int relation) {
        return relations.get(relation);
    }

    /** Returns the entity an entity id */
    public String entity(int entity) {
        return entities.get(entity);
    }

    public String proper_entity_s(int entity) {
        return entities.get(id_small_to_big(entity));
    }

    /** Trims everything to size, computes functionalities */
    public void prepare(File _1v1_assumption, ArrayList<ArrayList<Float>> all_entity_emb, ArrayList<ArrayList<Float>> all_trans_entity_emb, ArrayList<String> emb_entity_names, HashMap<Integer,String> id_to_value) {
        Announce.doing("Preparing", uri);
        TYPE = relation("rdf:type");
        SUBCLASSOF = relation("rdfs:subClassOf");
        Announce.doing("Trimming to size");
        this.entities.trimToSize();
        entity_id_to_proper_entity_id = new int[entities.size()];
        this.relations.trimToSize();
        this.entityNames.trimToSize();
        this.relationNames.trimToSize();
        assert (numEntities() == facts.size());
        Announce.message("done loading", entities.size(), "entities");
        populateCaches();
        for (int i = 0; i < facts.size(); i++) {
            if (facts.get(i) != null) facts.get(i).trimToSize();
        }
        is_attribute_relation = new boolean[numRelations()];
        for(int i = 0; i < relations.size(); i++){
            if(relations.get(i).startsWith("attr_")){
                is_attribute_relation[i] = true;
            }
            else{
                is_attribute_relation[i] = false;
            }  
        }

        HashMap<Integer, Integer> facts_same_subject_and_relation_count = new HashMap<Integer, Integer>();
        ArrayList<PredicateAndObject> po;
        ArrayList<TruthValue> facts_truthvalue_i;
        int predicate,predicate2;
        Integer count;
        double c, discount_factor = 1;
        if(setting.modify_initial_confidence == true){
            facts_truthvalue.ensureCapacity(facts.size());
            for (int i = 0; i < facts.size(); i++) {
                po = facts.get(i);
                if (po == null) {
                    continue;
                }
                while (facts_truthvalue.size() <= i)
                    facts_truthvalue.add(null);
                if (facts_truthvalue.get(i) == null) 
                    facts_truthvalue.set(i, new ArrayList<TruthValue>());
                facts_truthvalue_i = facts_truthvalue.get(i);
                facts_truthvalue_i.ensureCapacity(po.size());
                while (facts_truthvalue_i.size() <= po.size())
                    facts_truthvalue_i.add(null);
                for (int j = 0; j < po.size(); j++) {
                    predicate = po.get(j).predicate;
                    if(isInverse(predicate))
                        continue;
                    if(!is_attribute_relation(predicate)){
                        if(fs_id==1)
                            facts_truthvalue_i.set(j,new TruthValue(setting.relation_triple_initial_frequency_fs1,setting.relation_triple_initial_confidence_fs1));
                        else
                            facts_truthvalue_i.set(j,new TruthValue(setting.relation_triple_initial_frequency_fs2,setting.relation_triple_initial_confidence_fs2));
                        continue;
                    }
                    count = facts_same_subject_and_relation_count.get(predicate);
                    if(count == null){
                        count = 0;
                        for (int k = 0; k < po.size(); k++) {
                            predicate2 = po.get(k).predicate;
                            if(predicate2 == predicate)
                                count++;
                        }
                        facts_same_subject_and_relation_count.put(predicate, count);
                    }
                    if(count<20)
                        discount_factor = 1.0 - Math.round(count/5)*0.2;
                    else
                        discount_factor = 1.0 - 4*0.2;
                    if(setting.modify_initial_confidence == false)
                        discount_factor = 1.0;
                    if(fs_id==1){
                        c = setting.attribute_triple_initial_confidence_fs1 * discount_factor;
                        facts_truthvalue_i.set(j,new TruthValue(setting.attribute_triple_initial_frequency_fs1,c));
                    }
                    if(fs_id==2){
                        c = setting.attribute_triple_initial_confidence_fs2 * discount_factor;
                        facts_truthvalue_i.set(j,new TruthValue(setting.attribute_triple_initial_frequency_fs2,c));
                    }
                }
                for (int j = 0; j < po.size(); j++) {
                    //process inverse triples
                    predicate = po.get(j).predicate;
                    if(!isInverse(predicate))
                        continue;
                    if(!is_attribute_relation(predicate)){
                        if(fs_id==1)
                            facts_truthvalue_i.set(j,new TruthValue(setting.relation_triple_initial_frequency_fs1,setting.relation_triple_initial_confidence_fs1));
                        else
                            facts_truthvalue_i.set(j,new TruthValue(setting.relation_triple_initial_frequency_fs2,setting.relation_triple_initial_confidence_fs2));
                        continue;
                    }
                    predicate = inverse(predicate);
                    count = facts_same_subject_and_relation_count.get(predicate);
                    if(count<20)
                        discount_factor = 1.0 - Math.round(count/5)*0.05;
                    else
                        discount_factor = 1.0 - 4*0.05;
                    if(setting.modify_initial_confidence == false)
                        discount_factor = 1.0;
                    if(fs_id==1){
                        c = setting.attribute_triple_initial_confidence_fs1 * discount_factor;
                        facts_truthvalue_i.set(j,new TruthValue(setting.attribute_triple_initial_frequency_fs1,c));
                    }
                    if(fs_id==2){
                        c = setting.attribute_triple_initial_confidence_fs2 * discount_factor;
                        facts_truthvalue_i.set(j,new TruthValue(setting.attribute_triple_initial_frequency_fs2,c));
                    }
                }
            }
        }

        Announce.done();
        computeFunctionalities();
        Announce.done();
        if (setting.literalDistance == Setting.LiteralDistance.SHINGLING || setting.literalDistance == Setting.LiteralDistance.SHINGLINGLEVENSHTEIN) {
            Announce.doing("indexing literals...");
            this.literalIndex = new ShinglingTable(setting.shinglingSize, setting.shinglingFunctions, setting.shinglingTableSize);
            Set<String> indexed = new HashSet<String>();
            for (int i = 0; i < numEntities(); i++) {
                if (!isLiteral(i)) continue;
                if (indexed.contains(entity(i))) continue;
                indexed.add(entity(i));
                this.literalIndex.index(entity(i));
            }
            Announce.done();
        }
        if (setting.debugEntity != null) {
                for (int i = 0 ; i < numEntities(); i++) {
                        if (entity(i).contains(setting.debugEntity)) {
                                Announce.message("DEBUGENTITY");
                                Announce.message(factsAbout(i));
                        }
                }
        }

        this.finalized = true;

        int n = -1;
        for (int i = 0; i < numEntities(); i++){
            if (is_proper_entity(i)) {
                n++;
                entity_id_to_proper_entity_id[i] = n;
            }
        }
        cached_num_proper_entities = n + 1;
        proper_entity_id_to_entity_id = new int[n + 1];
        n = -1;
        for (int i = 0; i < numEntities(); i++){
            if (is_proper_entity(i)) {
                n++;
                proper_entity_id_to_entity_id[n] = i;
            }
        }
        //xch1
        if(_1v1_assumption != null) {
            has_1v1_assumption = true;
            belong_to_1v1_assumption = new boolean[num_proper_entities()];
            FileLines lines;
            try {
                lines = new FileLines(_1v1_assumption, "UTF-8", null);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                lines = null;
                e.printStackTrace();
            }
            //Announce.message("_1v1_ass:");
            for(String e : lines) {
                try {
                        //<xxx-unused> = false
                        belong_to_1v1_assumption[id_big_to_small(entity(e.trim()))] = true;
                        //Announce.message(e);
                } catch (ArrayIndexOutOfBoundsException e1) {}
            }
            int _1v1_ass = 0;
            for(int i = 0; i < num_proper_entities(); i++) {
                if (belong_to_1v1_assumption[i] == true)
                    _1v1_ass ++;
            }
            Announce.message("_1v1_ass:", _1v1_ass);
            proper_entity_id_to_1v1_id = new int[num_proper_entities()];
            _1v1_id_to_proper_entity_id = new int[_1v1_ass];
            n = -1;
            for(int i = 0; i < num_proper_entities(); i++) {
                if (belong_to_1v1_assumption[i] == true){
                    n++;
                    proper_entity_id_to_1v1_id[i] = n;
                    _1v1_id_to_proper_entity_id[n] = i;
                }
            }
            _1v1_assumption_size = _1v1_ass;
        }
        else{
            has_1v1_assumption = false;
        }
        
        if(setting.use_entity_emb_sim){
            //populate entity_emb
            int i=0;
            for(String name : emb_entity_names) { 
                if(entityNames.containsKey(name)){
                    if(!is_proper_entity(entityNames.get(name))) continue;
                    entity_emb.put(entityNames.get(name), all_entity_emb.get(i));
                    if(i<5){
                        D.p(i);
                        D.p(entityNames.get(name));
                        D.p(all_entity_emb.get(i));
                    }
                }
                i++;
            }
        }
        if(setting.use_translate_emb){
            //populate entity_emb
            int i=0;
            for(String name : emb_entity_names) { 
                if(entityNames.containsKey(name)){
                    if(!is_proper_entity(entityNames.get(name))) continue;
                    trans_entity_emb.put(entityNames.get(name), all_trans_entity_emb.get(i));
                    if(i<5){
                        D.p(i);
                        D.p(entityNames.get(name));
                        D.p(all_trans_entity_emb.get(i));
                    }
                }
                i++;
            }
        }
        if(setting.use_attribute_value_emb_sim){
            value_in_factStore = new ArrayList<>(id_to_value.size());
            for(Map.Entry<Integer, String> entry : id_to_value.entrySet()){
                value_in_factStore.add(false);
            }
            for(Map.Entry<Integer, String> entry : id_to_value.entrySet()){
                if(entityNames.containsKey(entry.getValue())){
                    value_in_factStore.set(entry.getKey(), true);
                }
            }
        }
        
    }

    public static double cosine_similarity(List<Float> vectorA, List<Float> vectorB) {
        //assume that vectorA and B has already normalized
        double dotProduct = 0.0;
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
        }
        return dotProduct;

        /*
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
         */
    }

    /** returns the inverse of a relation */
    public static String inverse(String relation) {
        if (relation.endsWith("-")) return (relation.substring(0, relation.length() - 1));
        return (relation + "-");
    }

    /** returns the inverse of a relation */
    public static int inverse(int relation) {
        if (isInverse(relation)) return (relation - 1);
        return (relation + 1);
    }

    /** True for inverse relations */
    public static boolean isInverse(int relation) {
        return (relation & 1) == 1;
    }

    /** add a relation */
    public synchronized int addRelation(String relation) {
        int id = relationNames.size();
        relationNames.put(relation, id);
        relations.add(relation);
        relationNames.put(inverse(relation), inverse(id));
        relations.add(inverse(relation));
        return (id);
    }

    /** returns a relation id (or adds it) */
    public synchronized int getOrAddRelation(String relation) {
        // Don't use lget() here, it's not thread-safe
        if (finalized || relationNames.containsKey(relation)) return (relationNames.get(relation));
        // synchronized (relationNames) {
        return addRelation(relation);
        // }
    }

    /** add an entity */
    public synchronized int addEntity(String entity) {
        int id = entityNames.size();
        entityNames.put(entity, id);
        entities.add(entity);
        return (id);
    }

    /** returns an entity id (or adds it) */
    public synchronized int getOrAddEntity(String entity) {
        // Don't use lget() here, it's not thread-safe
        if (entityNames.containsKey(entity))
                return (entityNames.get(entity));
        // synchronized (entityNames) {
        assert(!finalized);
        return addEntity(entity);
        // }
    }

    /** returns facts about the subject */
    public List<PredicateAndObject> factsAbout(int subject) {
        return (facts.get(subject));
    }

    /** returns facts about the subject */
    public List<PredicateAndObject> factsAbout(String subject) {
        return (facts.get(getOrAddEntity(subject)));
    }

    public List<SubjectAndObject> facts_about_relation(int predicate) {
        return (facts_relation_view.get(predicate));
    }

    /** Pattern for prefix*/
    protected static final Pattern prefixPattern = Pattern.compile("[a-z0-9]{1,5}:.*");

    /** Adds the standard prefix if necessary*/
    public String addPrefix(String uri) {
        if (isLiteral(uri)) return (uri);
        if (uri.startsWith(this.uri)) return (prefix + uri.substring(this.uri.length()));
        if (prefixPattern.matcher(uri).matches()) return (uri);
        return (prefix + uri);
    }

    /** Adds a fact. removes data types. Adds quotes for numbers */
    public void add(String subject, String predicate, String object) {
        object = addPrefix(LiteralFormatter.format(object, formatters));
        // For old YAGO files that can have a literal as the subject
        subject = addPrefix(LiteralFormatter.format(subject, formatters));
        predicate = addPrefix(predicate);
        int predicateId = getOrAddRelation(predicate);
        assert (!isInverse(predicateId));
        int objectId = getOrAddEntity(object);
        int subjectId = getOrAddEntity(subject);
        add(subjectId, predicateId, objectId);
        assert (facts.size() >= subjectId);
        assert (facts.size() >= objectId);
        assert (subjectId <= numEntities());
        assert (objectId <= numEntities());
    }

    /**
     * Loads a file
     * 
     * @throws IOException
     */
    public void load(File f) throws IOException {
        if (f.isDirectory()) {
            load(f.listFiles());
        } else {
            if (functionalities != null) {
                Announce.warning("First load files, then call prepare()!");
                System.exit(2);
            }
            for (String[] fact : Parser.forFile(f)) {
                    try {
//      		Announce.message(fact);
                            add(fact[0], fact[1], fact[2]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                            Announce.message("bad fact:");
                            for (int i = 0; i < fact.length; i++)
                                    Announce.message(fact[i]);
                            System.exit(2);
                    }
            }
        }
    }

    /** Loads a files in the folder that match the regex pattern */
    public void load(File folder, Pattern namePattern) throws IOException {
        List<File> files = new ArrayList<File>();
        for (File file : folder.listFiles())
            if (namePattern.matcher(file.getName()).matches()) files.add(file);
        load(files);
    }

    /** Loads files in parallel */
    public void load(File... files) throws IOException {
        load(Arrays.asList(files));
    }

    /** Loads the files */
    public void load(List<File> files) throws IOException {
        int size = numEntities();
        long time = System.currentTimeMillis();
        long memory = Runtime.getRuntime().freeMemory();
        Announce.doing("Loading files");
        final int[] running = new int[1];
        if (setting.parallelFileLoad) {
                for (final File file : files) {
                    running[0]++;
                    new Thread() {
        
                        public void run() {
                            try {
                                synchronized (Announce.blanks) {
                                    Announce.message("Starting " + file.getName());
                                }
                                load(file);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            synchronized (Announce.blanks) {
                                Announce.message("Finished " + file.getName() + ", still running: " + (running[0] - 1));
                                synchronized (running) {
                                    if (--running[0] == 0) running.notify();
                                }
                            }
                        }
                    }.start();
                }
                try {
                    synchronized (running) {
                        running.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        } else {
                for (final File file : files) {
                        load(file);
                }
        }
        Announce.done("Loaded facts about " + (numEntities() - size) + " entities in "
                        + NumberFormatter.formatMS(System.currentTimeMillis() - time) + " using "
                + ((Runtime.getRuntime().freeMemory() - memory) / 1000000) + " MB");
    }

    /** Return a number larger than the largest join relation code allocated */
    public int maxJoinRelationCode() {
        return (int) Math.pow(maxRelationId() * 2, joinLengthLimit);
    }

    public int maxRelationId() {
        return relations.size();
    }

    public int getJoinLengthLimit() {
        return joinLengthLimit;
    }

    public void reduceJoinLengthLimit(int newLimit) {
        joinLengthLimit = Math.min(joinLengthLimit, newLimit);
    }

    /** Return a join relation from a join relation code */
    public JoinRelation joinRelationByCode(int code) {
        JoinRelation r = new JoinRelation(this);
        int max = maxRelationId();
        while (code != 0) {
            r.push(code % max);
            code /= max;
        }
        r.reverseDirection();
        return r;
    }

    public Collection<QueryResult> similarLiterals(String query, double threshold) {
        return this.literalIndex.query(Config.stripQuotes(query), threshold);
    }

    public List<Integer> properEntities() {
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 1; i < numEntities(); i++)   //unused entity 0
            if (is_proper_entity(i)) result.add(i);
        return result;
    }

    public static double median(double[] in) {
		if (in == null) {
			throw new java.lang.NumberFormatException();
		}
		Arrays.sort(in);
 
		// for (int i = 0; i < in.length; i++) {
		// log.debug("sort: "+i+":::"+in[i]);
		// }
		if (in.length % 2 == 1) {
			return in[(int) Math.floor(in.length / 2)];
		} else {
			double[] avg = new double[2];
			avg[0] = in[(int) Math.floor(in.length / 2) - 1];
			avg[1] = in[(int) Math.floor(in.length / 2)];
			return (avg[0] + avg[1])/2;
 
		}
	}



    public static void main(String[] args) throws Exception {
        int number = 2;
        if (number > 5)
            System.out.println("Number is greater than 5");
        else
            System.out.println("Number is not greater than 5");
        System.setProperty("java.vm.name","Java HotSpot(TM) ");
        Setting setting = new Setting("", "", "", "", "", "", null);
        TruthFunctions.set_setting(setting);
        TruthValue t[], temp; //t[0]表示从多条path选取的最终结果
        TruthValue p[]; //代表由某个path推出的最优y1 <-> y2的推理结果
        double c = 0.6003087066251795;
        c = TruthFunctions.divide_confidence(c,2);
        D.p("c",c);
        c = TruthFunctions.divide_confidence(c,2);
        D.p("c",c);
        t = new TruthValue[25];
        TruthValue tr[]; 
        tr = new TruthValue[25];
        temp = null;
        p = new TruthValue[6];

        double w1 = 0.0;
        double f1 = 0.0;
        D.p((double)(float)(w1 * f1));

        TruthValue negative = new TruthValue(0,0.0); 
        p[1] = new TruthValue(1,0.41); 
        negative = TruthFunctions.revision(negative, p[1]);
        D.p("positive p[1]:",p[1]);
        D.p("negative",negative);
        negative = TruthFunctions.de_revision(negative, p[1]);
        D.p("positive p[1]:",p[1]);
        D.p("de_negative",negative);
        D.p("de_negative.getFrequency()",negative.getFrequency());
        D.p("de_negative.getConfidence()",negative.getConfidence());
        negative = TruthFunctions.revision(negative, p[1]);
        D.p("positive p[1]:",p[1]);
        D.p("negative",negative);
        D.p("negative.getFrequency()",negative.getFrequency());
        negative = TruthFunctions.revision(negative, p[1]);
        D.p("negative",negative);

        D.p("type 3 path negatiive");
        tr[1] = new TruthValue(1,1);    //(∗, x1, y1) → r1 (12)
        tr[2] = new TruthValue(1,0.9);    //x1 ↔ x2 (13)
        tr[3] = TruthFunctions.analogy(tr[1], tr[2]);    //(∗, x2, y1) → r1 (14)
        tr[4] = new TruthValue(1,0.9);    //y1 ↔ y2 (15)    0.8,0.8
        tr[5] = TruthFunctions.analogy(tr[3], tr[4]);    //(∗, x2, y2) → r1 (16)
        D.p("tr[5]",tr[5]);
        tr[6] = new TruthValue(0,setting.missing_triple_initial_confidence);    //(∗, x2, y2) → r2 (17)
        p[1] = TruthFunctions.induction(tr[6], tr[5]); //r1 → r2 (18)  //caution (tr[6], tr[5])
        D.p("p[1]",p[1]);  //%0.00;0.12% - %0.00;0.29%

        D.p("type 3 path positive");
        tr[1] = new TruthValue(1,1);    //(∗, x1, y1) → r1 (12)
        tr[2] = new TruthValue(0.8,0.3);    //x1 ↔ x2 (13)
        tr[3] = TruthFunctions.analogy(tr[1], tr[2]);    //(∗, x2, y1) → r1 (14)
        tr[4] = new TruthValue(0.8,0.3);    //y1 ↔ y2 (15)    0.8,0.8
        tr[5] = TruthFunctions.analogy(tr[3], tr[4]);    //(∗, x2, y2) → r1 (16)
        D.p("tr[5]",tr[5]);
        tr[6] = new TruthValue(1,1);    //(∗, x2, y2) → r2 (17)
        p[1] = TruthFunctions.induction(tr[6], tr[5]); //r1 → r2 (18)  //caution (tr[6], tr[5])
        D.p("p[1]",p[1]);  //%0.00;0.12% - %0.00;0.29%

        D.p("type 5 path");
        t[1] = new TruthValue(1,1);    //(∗, 𝑥1, 𝑦1) → 𝑟11
        //System.out.println(ObjectSizeCalculator.getObjectSize(t[1]));
        t[2] = new TruthValue(1,0.5);    //𝑟11 ↔ 𝑟12          (1,0.7) t[6]%1.00;0.41%
        t[3] = TruthFunctions.analogy(t[1], t[2]);    //(∗, 𝑥1, 𝑦1) → 𝑟12 ⟨𝑓3, 𝑐3⟩
        t[4] = new TruthValue(1,1);    //(∗, 𝑥2, 𝑦2) → 𝑟12
        t[5] = TruthFunctions.comparison(t[3], t[4]);    //(∗, 𝑥1, 𝑦1) ↔ (∗, 𝑥2, 𝑦2) ⟨𝑓5, 𝑐5⟩
        t[6] = new TruthValue(t[5]);    //𝑦1 ↔ 𝑦2 ⟨𝑓6, 𝑐6⟩    //(questionable)
        D.p("t[6]",t[6]);  //%0.70;0.33%  %1.00;0.33%(0.492)
        System.out.println(ObjectSizeCalculator.getObjectSize(t[2]));
        System.out.println(ObjectSizeCalculator.getObjectSize(t[6]));
        System.out.println(ObjectSizeCalculator.getObjectSize(t));
        D.p("type 6 path");
        t[1] = new TruthValue(1,1);    //(∗, 𝑥, 𝑦1) → 𝑟11 ⟨𝑓1, 𝑐1⟩
        t[2] = new TruthValue(1,1);    //(∗, 𝑥, 𝑦2) → 𝑟12 ⟨𝑓2, 𝑐2⟩
        t[3] = TruthFunctions.intersection(t[1], t[2]);    //(∗, 𝑥, 𝑦1) → 𝑟11 ∧ (∗, 𝑥, 𝑦2) → 𝑟12 ⟨𝑓3, 𝑐3⟩
        t[4] = new TruthValue(1,0.7); //((∗, #𝑥, #𝑦1) → 𝑟11 ∧ (∗, #𝑥, #𝑦2) → 𝑟12) ⇒ 𝑟11 ↔ 𝑟12 ⟨𝑓4, 𝑐4⟩
        t[5] = TruthFunctions.deduction(t[3], t[4]);   //𝑟11 ↔ 𝑟12 ⟨𝑓5, 𝑐5⟩
        D.p("t[5]",t[5]);
        t[6] = new TruthValue(1,0.5);    //𝑟11 ↔ 𝑟12⟨𝑓6, 𝑐6⟩
        t[7] = TruthFunctions.revision(t[5], t[6]);   //𝑟11 ↔ 𝑟12 ⟨𝑓7, 𝑐7⟩
        D.p("t[7]",t[7]);
        t[8] = TruthFunctions.analogy(t[1], t[7]);    //(∗, 𝑥, 𝑦1) → 𝑟12 ⟨𝑓8, 𝑐8⟩
        t[9] = new TruthValue(t[8]);   //𝑦1 → (/, 𝑟12, 𝑥, _) ⟨𝑓9, 𝑐9⟩
        t[10] = new TruthValue(t[2]);    //𝑦2 → (/, 𝑟12, 𝑥, _) ⟨𝑓10, 𝑐10⟩
        t[11] = TruthFunctions.comparison(t[9], t[10]);    //1 ↔ 𝑦2 ⟨𝑓11, 𝑐11⟩
        D.p("t[11]",t[11]);   //t[11] %0.88;0.44%  %1.00;0.43%(0.754)

        //Setting setting = new Setting("", "", "", "", "", "", null);
        //new File("/home/a3nm/documents/stage/paris/dummy_conf"));
        FactStore f = new FactStore(1, setting, "imdb:", "http://imdb/", 1, LiteralFormatter.CUT_DATATYPE, LiteralFormatter.NORMALIZE);
        f.load(new File("/home/a3nm/documents/stage/paris/ontologies/imdb_small"), Pattern.compile(".*"));
        // Pattern.compile("happenedIn.tsv"));
        // f.load(new
        // File("/home/a3nm/DOCUMENTS/stage/paris/ontologies/dbpedia_small_uniq.nt"));
        //f.load(new File("/home/a3nm/documents/stage/paris/ontologies/yagodebug"));
        f.prepare(null, null, null, null, null);
        D.p(f.isClass(1));
        D.p(f.entity(1));
        D.p(f.factsAbout(1));
        D.p(f.addPrefix("rdfs:label"));
        String[] examples = new String[] {
                        "p1550813", "p826266", "p868780", "p2210300", "p2407572", "p207566", "p2967074", "p37159", "p2524953",
                        "tt1350852", "p1779811", "p1340570", "tt0659432", "p2608974", "p1601915", "l11224", "p1725119", "tt0796418", "p2174336", "tt1181151", "p2449951", "p465644",
                        "p1357789"};
        for (String example : examples)
                D.p(f.factsAbout("imdb:" + example));
//    D.p(f.classesOf("y2:Ulm"));
//    IntIterator it=f.classesAndSuperClassesOf("y2:Ulm").iterator();
//    while(it.hasNext()) {
//      D.p(f.entity(it.next()));
//    }
    }
}
