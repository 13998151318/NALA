package pnal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import bak.pcj.IntIterator;
import bak.pcj.map.IntKeyOpenHashMap;
import bak.pcj.set.IntOpenHashSet;
import bak.pcj.set.IntSet;

import pnal.storage.FactStore;
import pnal.storage.FactStore.PredicateAndObject;
import pnal.storage.FactStore.SubjectAndObject;
import pnal.storage.Parser;
import pnal.nal.TruthValue;
import pnal.nal.Stamp;
import pnal.nal.AlignmentSentence;
import pnal.nal.TruthFunctions;


import javatools.administrative.Announce;
import javatools.administrative.Announce.Level;
import javatools.administrative.D;
import javatools.datatypes.Pair;
import javatools.parsers.NumberFormatter;
import javatools.filehandlers.CSVFile;
import javatools.filehandlers.CSVLines;
import javatools.filehandlers.FileLines;

import fr.lri.tao.apro.ap.AproBuilder;
import fr.lri.tao.apro.ap.Apro;
import fr.lri.tao.apro.data.DataProvider;
import fr.lri.tao.apro.data.MatrixProvider;

/**
 * This class is part of the PARIS ontology matching project at INRIA
 * Saclay/France.
 * 
 * It is licensed under a Creative Commons Attribution Non-Commercial License by
 * the author Fabian M. Suchanek (http://suchanek.name). For all further
 * information, see http://webdam.inria.fr/paris
 * 
 * This class implements a very cool probabilistic framework for ontology
 * matching
 */

//xch2.0 xch2.0改动为在xch1的基础上试图将paris+中的对齐公式进行NAL推理化改造，并且提供模型自解释性能力，输出对齐结果的主要证据。
//并且最好做出paris程序中断复原功能
//xch2.1 xch2.1改动为 对关系继承进行真值化改造、修复bug 66->70 并试图对单个左属性重复使用的情形进行修正

public class Pnal {

	/** TRUE for test runs */
	public static boolean test = false;

	/** TRUE for debug on. Make it final to allow conditional compiling. */
	public static boolean debug = false;

	/** First ontology */
	public static FactStore factStore1;

	/** Second ontology */
	public static FactStore factStore2;

	/** Stores the equality */
	public static Result computed;

	/** The setting in which we work */
	public static Setting setting;

    public static boolean display_evidence;

    //public static HashMap<Integer,ArrayList<Float>> attribute_value_emb;

    public static HashMap<String,Integer> value_to_id;

    public static HashMap<Integer,String> id_to_value;

    public static HashMap<Integer, LinkedList<AlignmentSentence>> alignment_sentences_of_entity_embedding;

    // [small id][small id]   in AlignmentSentence:[big id][big id] 
    public static HashMap<Integer, HashMap<Integer, AlignmentSentence>> alignment_sentences_of_classes;

    // [small id][small id]
    public static ArrayList<ArrayList<TruthValue>> intensional_similarity_1;

    // [small id][small id]
    public static HashMap<Integer,ArrayList<Integer>> exemplar_view_1;

    public static ArrayList<ArrayList<TruthValue>> intensional_similarity_2;

    public static HashMap<Integer,ArrayList<Integer>> exemplar_view_2;

    public static int[] exemplars_1;

    public static int[] exemplars_2;

    public static int run;

    //public static ArrayList<LinkedList<Pair<Integer, Float>>> entity_emb_sim_mat = new ArrayList<>(40000);

    //public static ArrayList<LinkedList<Pair<Integer, Float>>> value_emb_sim1;
    //public static ArrayList<LinkedList<Pair<Integer, Float>>> value_emb_sim2;
	/** Finds the super classes of a class */
	public static void findSuperClassesOf(Integer subclass, FactStore subStore,
			FactStore superStore) {
		// We ignore classes that contain practically all entities
		if (subStore.entity(subclass).startsWith("owl:")
				|| subStore.entity(subclass)
						.equals("http://www.opengis.net/gml/_Feature"))
			return;
		if (debug)
			Announce.debug("Computing superclasses of",
					subclass);

		// maps each superclass d to
		// SUM x such that type(x,c): 1 - PROD y such that type(y,d): 1-P(x=y)
		Map<Integer, Double> superclassDegree = new TreeMap<Integer, Double>();
		// contains the value
		// # x such that type(x,c) // no longer: and exists y: y=x and type(y,some
		// class)
		double normalizer = 0;
		int counter = 0;
		// Don't compute that for classes that are too far up in the hierarchy
		IntSet subInstances = subStore.instancesOf(subclass);
		if (subInstances == null)
			return;
		IntIterator it = subInstances.iterator();
		while (it.hasNext()) {
			int subclassInstance = it.next();
			if (debug && counter++ > 100)
				break;
			if (debug)
				Announce.debug("   Looking at instance",
						subclassInstance);
			// For each instance x of c...
			boolean foundeqv = false;
			Map<Integer, Double> membershipProduct = new TreeMap<Integer, Double>();
			// maps each superclass d to
			// PROD y such that type(y,d): 1-P(x=y)
			for (Pair<Object, TruthValue> superclassInstancePair : computed
					.equalToScored(subStore, subclassInstance)) {
				if (!(superclassInstancePair.first() instanceof Integer))
					continue;
				Integer superclassInstance = (Integer) superclassInstancePair.first();
				double equality = superclassInstancePair.second().getExpectation();
				if (debug)
					Announce.debug("     Is equal to",
							superStore.entity(superclassInstance), equality);

				if (equality < Config.THETA)
					continue;
				IntSet classes = superStore.classesOf(superclassInstance);
				IntIterator it2 = classes.iterator();
				while (it2.hasNext()) {
					int superClass = it2.next();
					assert(superClass > 0);
					double prod = membershipProduct.containsKey(superClass) ? membershipProduct
							.get(superClass) : 1;
					if (debug)
						Announce.debug("        Scoring for",
								superStore.entity(superClass), prod, 1 - equality
										* equality, prod * (1 - equality));
					prod *= 1 - equality;
					membershipProduct.put(superClass, prod);
					foundeqv = true;
				}
			}
			if (foundeqv) {
				for (Integer superclass : membershipProduct.keySet()) {
					D.addKeyValueDbl(superclassDegree, superclass,
							1 - membershipProduct.get(superclass));
				}
			}
			normalizer++;
		}
		// We do not do the domain/range deduction
		// Collect all classes about which we know something in superclassDegree
		// Say that if we have no instances, the superclassDegree is 0
		// for (Integer superclass : domainSuperclassDegree.keySet()) {
		// if (!superclassDegree.containsKey(superclass))
		// superclassDegree.put(superclass, 0.0);
		// }

		// If the normalizer is 0, superclassDegree(x)=0 for all x.
		// So instanceScore will be 0 anyway. Hence, set the normalizer to 1
		// to avoid NAN values when we compute superclassDegree(x)/normalizer.
		if (normalizer == 0)
			normalizer = 1;

		// Set the final values
		for (Integer superclass : superclassDegree.keySet()) {
			double instanceScore = superclassDegree.get(superclass) / normalizer;
			double domainScore = 1.0; // domainSuperclassDegree.containsKey(superclass)
																// ? domainSuperclassDegree.get(superclass) :
																// 1.0;
			if (1 - (1 - instanceScore) * domainScore < Config.THETA)
				continue;
			if (debug)
				Announce.debug("Setting final value:", superStore.entity(superclass),
						superclassDegree.get(superclass), normalizer,
						superclassDegree.get(superclass) / normalizer, 1
								- (1 - instanceScore) * domainScore);
			if (!test)
				computed.setSubclass(subStore, subclass, superclass, TruthValue.truthValueFromEvidenceAmount(superclassDegree.get(superclass), normalizer));
		}
	}

	public static void computeClassesOneWay(FactStore fs1, FactStore fs2) {
		int counter = fs2.numClasses();
		Announce.progressStart("Computing subclasses one direction", counter);
		for (int cls = 0 ; cls < fs2.numEntities(); cls++) {
			if (!fs2.isClass(cls))
				continue;
			findSuperClassesOf(cls, fs2, fs1);
			Announce.progressStep();
		}
		Announce.progressDone();
	}

	

	/** The class for a threaded findEqualsOf computation.
	 *  It reads the entities from the inputs queue, and writes its aggregated result on the target queue (and in equalities)
	 */
	private static class Mapper implements Runnable {
		int run;
		
		EqualityStore equalities;
		EqualityStoreMultiple equalitiesMultiple;
		BlockingQueue<MapperOutput> target;
		ConcurrentLinkedQueue<Integer> inputs;
		FactStore fs1;
		FactStore fs2;
		int id;
		Map<Integer, Double> equalityProduct;
		Map<Pair<Integer, Pair<Integer, Integer>>, Pair<Pair<Double, Double>, Double>> fullEqualityProduct;
        AlignmentSentence temp_a_s_for_one_entity[];
		boolean localDebug;
		int localJoinLengthLimit1;
		int localJoinLengthLimit2;
		// guide to explore only the interesting relations and joins in the first ontology
		Neighborhood relationGuide;
		MapperOutput mapperOutput;
		IntSet visited1;
		IntSet visited2;
		int limit;

		public Mapper(int run, int id, FactStore factStore,
				EqualityStore equalities, EqualityStoreMultiple equalitiesMultiple, MapperOutput mapperOutput, Neighborhood relationGuide,
				BlockingQueue<MapperOutput> target, ConcurrentLinkedQueue<Integer> inputs, int limit) {
			this.run = run;
			this.equalities = equalities;
			this.equalitiesMultiple = equalitiesMultiple;
			this.target = target;
			this.fs1 = factStore;
			this.fs2 = computed.other(fs1);
			this.inputs = inputs;
			this.id = id;
			this.localDebug = debug;
			// this is to be able to reduce the join length limit during the process
			this.localJoinLengthLimit1 = fs1.getJoinLengthLimit();
			this.localJoinLengthLimit2 = fs2.getJoinLengthLimit();
			visited1 = new IntOpenHashSet();
			visited2 = new IntOpenHashSet();
			this.limit = limit;
			this.relationGuide = relationGuide;
			
			if (setting.sampleEntities > 0) {
				// don't do any joins during the few first runs
				if (run < 2) {
					localJoinLengthLimit1 = 1;
					localJoinLengthLimit2 = 1;
				}
			}
			this.mapperOutput = mapperOutput;
		}
		
		/** Explore the second ontology.
		 * @param visited -- hashset of visited relation/object pairs for the fact in the first ontology
		 * @param newNeighborhood -- the neighborhood at the current iteration, that we write
		 * @param x1
		 * @param r1
		 * @param y1 such that x1 -r1-> y1
		 * @param x2
		 * @param r2
		 * @param y2 such that x2 -r2-> y2
		 * @param xeqv the score between x1 and x2 at the previous iteration
		 * @param oldNeighborhood -- the neighborhood at the previous iteration, actually unused
		 */
		public void exploreSecondOntology(Neighborhood newNeighborhood, int x1,
				JoinRelation r1, int y1, int x2,
				JoinRelation r2, int y2, double xeqv, Neighborhood oldNeighborhood) {
			if (r2.isTrivial())
				return;
			if (y2 < fs2.numEntities() && Config.ignoreClasses && fs2.isClass(y2))
				return;
			double yeqv = computed.equality(fs1, y1, y2).getExpectation();

			// we assume that there are no duplicate facts
			// hence, for a join relation length of 1, there is no need to check visited
			// so we save time for the specific case where no joins are made
			//Pair<Integer, Integer> p = new Pair<Integer, Integer>(r2.code(), y2);
			int p = r2.code()*fs2.numEntities() + y2;
			if (r2.length() == 1 || !visited2.contains(p)) {
				if (localDebug) {
					// don't compute the toString's unless running in debug mode, to save time
					// Announce.debug("Mark as visited", r2.toString(), fs2.entity(y2));
					if (xeqv * yeqv > 0)
						Announce.debug("Align", r1.toString(), r2.toString(), "occurrence", xeqv, "score", xeqv*yeqv);
//					Announce.debug("Old contents of visited:");
//					for (Pair<JoinRelation, Integer> pp : visited) {
//						Announce.debug(pp.first.toString(), fs2.entity(pp.second));
//					}
				}
				if (r2.length() > 1) {
				  visited2.add(p);
//				  if (localDebug) {
//				  	Announce.debug("New contents of visited:");
//						for (Pair<JoinRelation, Integer> pp : visited) {
//							Announce.debug(pp.first.toString(), fs2.entity(pp.second));
//						}
//				  }
				}
				newNeighborhood.registerOccurrence(xeqv);
				newNeighborhood.registerScore(xeqv * yeqv);
				if (equalities != null)
					registerEquality(x1, r1, y1, xeqv, x2, r2, y2);
			} else {
				if (localDebug) {
					//Announce.debug("ignore duplicate", r2.toString(), fs2.entity(y2));
				}
			}
			
			if (r2.length() >= localJoinLengthLimit2)
				return;
			if (!setting.allowLoops && x2 == y2)
				return;
			if (relationGuide != null && newNeighborhood.isEmpty())
				return;
			
			List<PredicateAndObject> facts = fs2.factsAbout(y2); 
			for (int i = 0; i < facts.size(); i++) {
				int r2bis = facts.get(i).predicate;
				int ny2 = facts.get(i).object;
//				Neighborhood n2 = oldNeighborhood == null ? null : oldNeighborhood.getChildRO(r2bis);
				Neighborhood n2 = oldNeighborhood;
//				if (!extendNeighborhoods && oldNeighborhood == null) {
//					continue;
//				}
//				JoinRelation nr2 = new JoinRelation(r2);
				Neighborhood nn2 = null;
				if (relationGuide == null) {
					nn2 = newNeighborhood.getChild(run, r2bis);
				} else {
					nn2 = newNeighborhood.getChildRO(r2bis);
				}
				if (nn2 == null) {
					continue;
				}
				if (setting.interestingnessThreshold && run > 0) {
					if (!nn2.worthTrying()) {
						continue;
					}
				}
				r2.push(r2bis);
				exploreSecondOntology(nn2, x1, r1, y1, x2, r2, ny2, xeqv, n2);
				r2.pop();
			}
		}

		/** register evidence for the equality of y1 and y2 from x1 -r1-> y1 and x2 -r2-> y2	*/
		public void registerEquality(
				int x1, JoinRelation r1, int y1, double xeqv,
				int x2, JoinRelation r2, int y2) {
			
			// when using the one pass method, we must use the small initial weights for
			// the two first iterations
			// otherwise nothing can align
			boolean isFirstRun = (run <= 1);
			
//			if (!Config.treatIdAsRelation && fs2.getIdRel() != null
//					&& r2.isSimpleRelation(-fs2.getIdRel().id))
//				return;
			assert(!Config.treatIdAsRelation);

			TruthValue subprop = computed.subRelation(fs2, r2, r1);

			TruthValue superprop = computed.subRelation(fs1, r1, r2);

			
			if (subprop.getConfidence() < Config.THETA && superprop.getConfidence() < Config.THETA) {
				if (isFirstRun) {
					double val = Config.IOTA / (1 + Config.iotaDependenceOnLength * ((r1.length() - 1) + (r2.length() - 1)));
					subprop.setConfidence(val);
					superprop.setConfidence(val);
				} else
					return;
			}
			
			double fun1 = fs1.functionality(r1) / Config.epsilon;
			double fun1r = fs1.inverseFunctionality(r1) / Config.epsilon;
			
			double fun2 = fs2.functionality(r2) / Config.epsilon;
			double fun2r = fs2.inverseFunctionality(r2) / Config.epsilon; 

			double factor = 1;
			double factor1 = 1 - xeqv * subprop.getFrequency() * fun1 * (Config.bothWayFunctionalities ? fun1r : 1.0);
			double factor2 = 1 - xeqv * superprop.getFrequency() * fun2 * (Config.bothWayFunctionalities ? fun2r : 1.0);
			if (subprop.getFrequency() >= 0 && fun1 >= 0)
				factor *= factor1;
			if (Config.subAndSuper && superprop.getFrequency() >= 0 && fun2 >= 0)
				factor *= factor2;
			
			// with the new method, don't do this for literals
			// also don't do it for very small things
			if (!fs2.isLiteral(y2) && 1 - factor > 0.01) {
				if (!setting.useNewEqualityProduct) {
					// classical equality propagation formula from the PARIS paper
					double val = equalityProduct.containsKey(y2) ? equalityProduct.get(y2)
							: 1.0;
					double oldval = val;
					val *= factor;
					assert(val >= 0 && val <= 1);
					equalityProduct.put((Integer) y2, val);
					if (localDebug) {
						Announce.debug("  Align", fs1.entity(y1), "with", fs2.entity(y2), "for:");
						Announce.debug("    ", fs1.entity(x1),
								r1.toString(), fs1.entity(y1));
						Announce.debug("    ", fs2.entity(x2),
								r2.toString(), fs2.entity(y2));
						Announce.debug("     xeqv=", xeqv, "fun1=", fun1, "fun1r=", fun1r, "fun2=", fun2, "fun2r", fun2r, "r1<r2=", subprop, "r2<r1=", superprop);
						Announce.debug("val=", 1 - val, "oval=", 1 - oldval);
					}
				} else {
					// revised formula from my report
					Pair<Integer, Pair<Integer, Integer>> k = new Pair<Integer, Pair<Integer, Integer>>((Integer) y2, new Pair<Integer, Integer>(
							x1, x2));
				
					if (!fullEqualityProduct.containsKey(k)) {
						fullEqualityProduct.put(k, new Pair<Pair<Double, Double>, Double>(new Pair<Double, Double>(1.0, 1.0), xeqv));
					}
					Pair<Pair<Double, Double>, Double> pval = fullEqualityProduct.get(k);
					if (subprop.getFrequency() >= 0 && fun1 >= 0)
						pval.first.first *= 1 - subprop.getFrequency() * fun1 * (Config.bothWayFunctionalities ? fun1r : 1.0);
					if (Config.subAndSuper && superprop.getFrequency() >= 0 && fun2 >= 0)
						pval.first.second *= 1 - superprop.getFrequency() * fun2 * (Config.bothWayFunctionalities ? fun2r : 1.0);
				}
			}
		}
		
		/** register evidence for the equality of y1 and y2 from x1 -r1-> y1 and x2 -r2-> y2
		 * optimized for non-joins	*/
		public void register_alignment_evidence(
				int x1, int r1, int y1, TruthValue x_truth,
				int x2, int r2, int y2, int fact_id_1, int fact_id_2, boolean record_stamp_evidence) {
			
			// when using the one pass method, we must use the small initial weights for
			// the two first iterations
			// otherwise nothing can align
			boolean isFirstRun = (run <= 1);
            boolean attributive = false;
			if (fs1.isLiteral(x1)){
                attributive = true;
            }
//			if (!Config.treatIdAsRelation && fs2.getIdRel() != null
//					&& r2.isSimpleRelation(-fs2.getIdRel().id))
//				return;
			assert(!Config.treatIdAsRelation);
            TruthValue r2_is_r1;
            TruthValue r1_is_r2;
            //xch与paris-main不同之处13. subRelation的查询获取 TruthValue并且未除 Config.epsilon
            r2_is_r1 = computed.subRelation(fs2, r2, r1);
			r1_is_r2 = computed.subRelation(fs1, r1, r2);
            if (setting.use_c_as_probability_value){
                if (r2_is_r1 != null)
                    r2_is_r1 = r2_is_r1.swapClone().setFrequency(1).divideConfidence(Config.epsilon);
                if (r1_is_r2 != null)
                    r1_is_r2 = r1_is_r2.swapClone().setFrequency(1).divideConfidence(Config.epsilon);
                if (r2_is_r1 == null) {
                    if (isFirstRun) {
                        double val = Config.IOTA;
                        //xch2.1 新增参数
                        //xch与paris-main不同之处14. subRelation IOTA的初始置信度
                        r2_is_r1 = new TruthValue(1, val);
                    } else{
                        r2_is_r1 = new TruthValue(1, 0);
                    }
                }
                if (r1_is_r2 == null) {
                    if (isFirstRun) {
                        double val = Config.IOTA;
                        //xch2.1 新增参数
                        r1_is_r2 = new TruthValue(1, val);
                    } else{
                        r1_is_r2 = new TruthValue(1, 0);
                    }
                }
                //xch与paris-main不同之处15. subRelation获取后的过滤 getFrequency()
                if (r2_is_r1.getConfidence() < Config.IOTA && r1_is_r2.getConfidence() < Config.IOTA) {
                    if (isFirstRun) {
                        double val = Config.IOTA;
                        r2_is_r1.setConfidence(val);
                        r1_is_r2.setConfidence(val);
                    } else if (r2_is_r1.getConfidence() < Config.THETA && r1_is_r2.getConfidence() < Config.THETA)
                        return;
                }
            }
            else{
                if (r2_is_r1 != null)
                    r2_is_r1 = r2_is_r1.clone().divideFrequency(Config.epsilon);
                if (r1_is_r2 != null)
                    r1_is_r2 = r1_is_r2.clone().divideFrequency(Config.epsilon);
                if (r2_is_r1 == null) {
                    if (isFirstRun) {
                        double val = Config.IOTA;
                        //xch2.1 新增参数
                        //xch与paris-main不同之处14. subRelation IOTA的初始置信度
                        r2_is_r1 = new TruthValue(1, val);
                    } else{
                        r2_is_r1 = new TruthValue(0, 1);
                    }
                }
                if (r1_is_r2 == null) {
                    if (isFirstRun) {
                        double val = Config.IOTA;
                        //xch2.1 新增参数
                        r1_is_r2 = new TruthValue(1, val);
                    } else{
                        r1_is_r2 = new TruthValue(0, 1);
                    }
                }
                //xch与paris-main不同之处15. subRelation获取后的过滤 getFrequency()
                if (r2_is_r1.getFrequency() < Config.IOTA && r1_is_r2.getFrequency() < Config.IOTA) {
                    if (isFirstRun) {
                        double val = Config.IOTA;
                        r2_is_r1.setFrequency(val);
                        r1_is_r2.setFrequency(val);
                    } else if (r2_is_r1.getFrequency() < Config.THETA && r1_is_r2.getFrequency() < Config.THETA)
                        return;
                }
            }
			if (!isFirstRun){
                //r1_is_r2.increse_frequency_linearly(setting.increse_r1_is_r2_frequency * r1_is_r2.getExpectation());
                //r2_is_r1.increse_frequency_linearly(setting.increse_r1_is_r2_frequency * r2_is_r1.getExpectation());
                r1_is_r2.increse_frequency_linearly(setting, attributive);
                r2_is_r1.increse_frequency_linearly(setting, attributive);
            }
			
			double fun1 = fs1.functionality(r1) / Config.epsilon;
			double fun2 = fs2.functionality(r2) / Config.epsilon;
			
			double fun1r = -42;
			double fun2r = -42;
			
			if (Config.bothWayFunctionalities) {
				fun1r = fs1.inverseFunctionality(r1) / Config.epsilon;
				fun2r = fs2.inverseFunctionality(r2) / Config.epsilon;
			}
            //another ((∗, $x1, #y) → #r ∧ (∗, $x2, #y) → #r ∧ #r → fun^−1) ⇒ $x1 <-> $x2          ...6



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
             *                                                                 f = 1*2*3*5*7*8   c = 
             * 
             *       inference path 2:      
             *       from 2 and 4: (∗, x2, y2) -> r1          ...12    f=f4  c=              P80  Deduction
             *       from 1 and 5: (∗, x2, y1) -> r1          ...13    f=f5  c=              P54  Analogy
             *       from 8, 12, 13 and 6: y1 <-> y2          ...11      P118 Conditional deduction*3
             *                                                                 f = 1*2*3*5*7*8   c =
             * 
             * 
             *       inference path 3:
             *       from 9 and 5: (∗, x2, y1) -> r2          ...14    f=f3*f5  c=f3*c1*c3*c5*f5         P80 Analogy    {M -> P <f1, c1>, S <-> M <f2, c2>} ` S -> P    f = and(f1, f2)   c = and(f2, c1, c2)
             *       from 8, 14, 2, 7: y1 <-> y2              ...11        P118 Conditional deduction*3  f = and(f1, f2)   c = and(f1, f2, c1, c2)
             *                                                                 f = 1*2*3*5*6*7    7 9 2 6  c = c7*(f3*c1*c3*c5*f5)*f7*(f3*f5) *c2*(f3*f5*f7) *c6*(f3*f5*f7)*f6
             *                                                                                    7 2 9 6  c = c7*c2*f7 *(f3*c1*c3*c5*f5)*f7*(f3*f5) *c6*f6*(f7*f3*f5)       f3*f5?
             *                                                                                    7 2 6 9  c = c7*c2*f7 *c6*f7*f6 *(f3*c1*c3*c5*f5)*(f7*f6)*f3*f5            f6   ?           
             * 
             *       inference path 4:
             *       from 13 and 5: (∗, x1, y2) -> r1          ...15             P80 Analogy    {M -> P <f1, c1>, S <-> M <f2, c2>} ` S -> P    f = and(f1, f2)   c = and(f2, c1, c2)
             *       from 8, 15, 1, 6: y1 <-> y2               ...11     P118 Conditional deduction*3  f = and(f1, f2)   c = and(f1, f2, c1, c2)
             *                                                                 f = 1*2*3*5*6*7    7 1 13 11
             *                                                                                    7 1 11 13 
             *                                                                                    7         
             * 
             *       
             *                                                                                                                        
				*/
            TruthValue t[], temp; //t[0]表示从多条path选取的最终结果
            TruthValue p[]; //代表由某个path推出的最优y1 <-> y2的推理结果
            t = new TruthValue[25];
            temp = null;
            p = new TruthValue[6];
            if (setting.modify_initial_confidence){
                t[1] = new TruthValue(fs1.facts_truthvalue.get(y1).get(fact_id_1));
                t[2] = new TruthValue(fs2.facts_truthvalue.get(x2).get(fact_id_2));
            }
            else{
                if (fs1.isLiteral(x1)){
                    t[1] = new TruthValue(setting.attribute_triple_initial_frequency_fs1, setting.attribute_triple_initial_confidence_fs1);
                    t[2] = new TruthValue(setting.attribute_triple_initial_frequency_fs2, setting.attribute_triple_initial_confidence_fs2);
                }
                else{
                    t[1] = new TruthValue(setting.relation_triple_initial_frequency_fs1, setting.relation_triple_initial_confidence_fs1);
                    t[2] = new TruthValue(setting.relation_triple_initial_frequency_fs2, setting.relation_triple_initial_confidence_fs2);
                }
            }
            //xch2.0 有待改进
            t[3] = new TruthValue(r1_is_r2);
            t[4] = new TruthValue(r2_is_r1);
            t[5] = new TruthValue(x_truth);
            if (setting.use_c_as_probability_value){
                t[6] = new TruthValue(1, fun1);
                t[7] = new TruthValue(1, fun2);
            }
            else{
                t[6] = new TruthValue(fun1, 1);
                t[7] = new TruthValue(fun2, 1);
            }
            t[8] = new TruthValue(1f, 1);
            //inference path 1:
            t[9] = TruthFunctions.deduction(t[1], t[3]);
            t[10] = TruthFunctions.analogy(t[2], t[5]);
            p[1] = TruthFunctions.conditional_deduction_3(t[8], t[9], t[10], t[7]);
            //inference path 2:
            t[12] = TruthFunctions.deduction(t[2], t[4]);
            t[13] = TruthFunctions.analogy(t[1], t[5]);
            p[2] = TruthFunctions.conditional_deduction_3(t[8], t[12], t[13], t[6]);

            //inference path 3:
            t[14] = TruthFunctions.deduction(t[2], t[4]);
            t[15] = TruthFunctions.analogy(t[12], t[5]);

            //p[1][2] = temp;

            //xch与paris-main不同之处16. 推理路径和公式
            //xch2.1 仿照paris Config.subAndSuper进行组合
            if (setting.use_c_as_probability_value)
                t[0] = TruthFunctions.paris_revision(p[1], p[2]);//TruthFunctions.revision(p[1][1], p[1][2]);
            else
                t[0] = TruthFunctions.probabilistic_revision(p[1], p[2]);//TruthFunctions.revision(p[1][1], p[1][2]);

			// also don't do it for very small things
            //xch2.1:此处对于数值改动影响很小？
            //xch与paris-main不同之处17. 结论过滤
            //xch与paris-main不同之处18. rivision和存放证据
            //if (run < 10){
            if (t[0].getFrequency() > 0.01 && t[0].getConfidence() > 0.01) { // && t[0].getConfidence() > 0.02
                if (temp_a_s_for_one_entity[fs2.id_big_to_small(y2)] == null){
                    temp_a_s_for_one_entity[fs2.id_big_to_small(y2)] = new AlignmentSentence(x_truth, y1, y2, t[0], true, fact_id_1, fact_id_2, x2, record_stamp_evidence);
                }
                else{
                    temp_a_s_for_one_entity[fs2.id_big_to_small(y2)].probabilistic_add_evidence(x_truth, fact_id_1, fact_id_2, t[0], x2, record_stamp_evidence, setting.add_evidence_remove_duplicate, fs1.isLiteral(x1), run<setting.add_evidence_remove_duplicate_run, setting.use_c_as_probability_value);
                }
            }
            //}
            /* 
            else{
                if (t[0].getFrequency() > 0.001 && t[0].getConfidence() > 0.001) { // && t[0].getConfidence() > 0.02
                    if (temp_a_s_for_one_entity[fs2.id_big_to_small(y2)] == null){
                        temp_a_s_for_one_entity[fs2.id_big_to_small(y2)] = new AlignmentSentence(x_truth, y1, y2, t[0], true, fact_id_1, fact_id_2, x2, record_stamp_evidence);
                    }
                    else{
                        temp_a_s_for_one_entity[fs2.id_big_to_small(y2)].probabilistic_add_evidence(x_truth, fact_id_1, fact_id_2, t[0], x2, record_stamp_evidence, setting.add_evidence_remove_duplicate, fs1.isLiteral(x1), run<setting.add_evidence_remove_duplicate_run, setting.use_c_as_probability_value);
                    }
                }
            } */
		}


		/** findEqualsOf for a fixed fact x1 -r1-> y1, other arguments are the normalizer of relations in fs2, the neighborhood for r1, the equality products */
		public void findEqualsOfFact(RelationNormalizer normalizer,
			  Neighborhood neighborhood,
				int x1, JoinRelation r1, int y1) {

			if (localDebug) {
				Announce.debug("run", run, "findEqualsOfFact:", fs1.entity(x1),
					r1.toString(), fs1.entity(y1));
			}
			
			// will only be initialized if there is something to do
			Neighborhood oldNeighborhood = null;

			if (!fs1.isLiteral(x1) && Config.ignoreClasses && fs1.isClass(x1))
			  return;

			// we don't do that anymore because we need to align relations
//			if (fun1 < Config.THETA)
//				return;

			for (Pair<Object, TruthValue> x2pair : computed.equalToScoredId(fs1, x1)) {
				int x2 = (Integer) x2pair.first();
				double xeqv = x2pair.second().getExpectation();
				assert(xeqv >= 0 && xeqv <= 1);

				if (xeqv < Config.THETA)
					continue;

				// for all matching x2, y2's, we need to add weight for the normalizer
				for (Pair<Object, TruthValue> y2pair : computed.equalToScored(fs1, y1)) {
					double yeqv = y2pair.second().getExpectation();
					if (localDebug) {
						Object y2pf = y2pair.first();
						Announce.debug("Increment normalizer of", r1.toString(), "by", xeqv*yeqv, "for", fs1.entity(x1), r1.toString(),
								fs1.entity(y1), fs2.entity(x2), y2pf instanceof String ? fs2.entity((String) y2pf) : fs2.entity((int) y2pf));
					}
					normalizer.incrementSimpleNormalizer(r1, xeqv * yeqv);
					normalizer.incrementCurrentRealNormalizer(xeqv * yeqv);
				}
				normalizer.addNormalizer(r1);

				visited2.clear();
				
				List<PredicateAndObject> facts = fs2.factsAbout(x2); 
				for (int i = 0; i < facts.size(); i++) {
//					if (oldNeighborhood == null)
//						oldNeighborhood = computed.getNeighborhood(fs1, r1);
					int r2bis = facts.get(i).predicate;
					int ny2 = facts.get(i).object;
					Neighborhood n2 = oldNeighborhood == null ? null : oldNeighborhood.getChildRO(r2bis);
					JoinRelation nr2 = new JoinRelation(fs2, r2bis);
					Neighborhood nn2 = neighborhood.getChild(run, r2bis);
					if (setting.interestingnessThreshold && run > 0) {
						if (!nn2.worthTrying()) {
							continue;
						}
					}
//					Announce.message("@exploreSecondOntology", fs1.toString(x1), r1.toString(), fs1.toString(y1),
//							"and", fs2.toString(x2), nr2.toString(), fs2.toString(ny2));
					
					exploreSecondOntology(nn2, x1, r1, y1, x2, nr2, ny2, xeqv, n2);
				}
			}

			neighborhood.propagateScores();
		}

		// beware, we accumulate in r1 in the REVERSE order
		/** find possible r1's and y1's for a given x1 by exploring recursively around x1, and call findEqualsOfFact for x1 -r1-> y1
		 * 
		 * @param output
		 * @param equalityProduct
		 * @param fullEqualityProduct
		 * @param x1
		 * @param r1
		 * @param y1
		 * @param rg -- the current relation guide
		 * 
		 * Caution: r1 is built in the reverse order and reversed at the end when calling findEqualsOfFact
		 */
		public void exploreFirstOntology(int x1, JoinRelation r1, int y1, Neighborhood rg) {
			
			// we assume that there are no duplicate facts
			// hence, for a join relation length of 1, there is no need to check visited
			// Pair<Integer, Integer> pvisited = new Pair<Integer, Integer>(r1.code(), y1);
			int pvisited = r1.code()*fs1.numEntities() + y1;
					
			if (r1.length() == 1 || !visited1.contains(pvisited)) {
				if (r1.length() > 1)
				  visited1.add(pvisited);
				if (localDebug) {
					// don't compute the toString's unless running in debug mode, to save time
					// Announce.debug("Mark as visited", r1.toString(), fs1.entity(y1));
				}
				
				// TODO2 reverse r1 and reverse it back
				JoinRelation nr1 = new JoinRelation(r1);
				nr1.reverseDirection();
				if (mapperOutput.neighborhoods[nr1.code()] == null) {
					mapperOutput.neighborhoods[nr1.code()] = new HashArrayNeighborhood(fs2, run, true, Math.min(fs2.getJoinLengthLimit(), setting.sumJoinLengthLimit - nr1.length()));
				}
				findEqualsOfFact(mapperOutput.relationNormalizer,
						mapperOutput.neighborhoods[nr1.code()], x1, nr1,
						y1);
			} else {
				if (localDebug) {
					//Announce.debug("ignore duplicate", r1.toString(), fs1.entity(y1));
				}
			}
			
			if (r1.length() >= localJoinLengthLimit1)
				return;
			if (!setting.allowLoops && x1 == y1)
				return;
			if (relationGuide != null && (rg == null || rg.isEmpty()))
				return;
			// we don't consider joins on the first ontology before the second run
			if (run == 0)
				return;
			
			List<PredicateAndObject> facts = fs1.factsAbout(x1); 
			for (int i = 0; i < facts.size(); i++) {
				PredicateAndObject f = facts.get(i);
				if (f.predicate == r1.getLast())
					continue; // relation will be trivial
				int r1bis = FactStore.inverse(f.predicate);
				Neighborhood nrg = null;
				if (relationGuide != null) {
					nrg = rg.getChildRO(f.predicate);
					if (nrg == null)
						continue;
				}
				r1.push(r1bis);
				exploreFirstOntology(f.object, r1, y1, nrg);
				r1.pop();
			}
		}

		/** Find equality candidates for an entity y1 */
		public void findEqualsOf(int y1) {
			//Announce.message("@CALL findEqualsOf", y1, fs1.toString(y1), "");
			// equalityProduct -- maps candidate y2's to their alignment score with y1
			// fullEqualityProduct -- maps candidate y2's and (x1, x2) to their first direction and second direction scores, and to the equiv of x1 and x2
			equalityProduct.clear();
			fullEqualityProduct.clear();

			Announce.debug("run", run, "findEqualsOf:", fs1.entity(y1), "");
			//HashSet<Pair<Integer, Integer>> visited = new HashSet<Pair<Integer, Integer>>();
			visited1.clear();
			// call exploreFirstOntology for all fact about y1
			// (the first recursive call is unrolled to make things run faster)
			
			List<PredicateAndObject> facts = fs1.factsAbout(y1); 
			for (int i = 0; i < facts.size(); i++) {
				PredicateAndObject f = facts.get(i);
				int nx1 = f.object;
				int r1bis = FactStore.inverse(f.predicate);
				JoinRelation nr1 = new JoinRelation(fs1, r1bis);
				Neighborhood rg = null;
				if (relationGuide != null) {
					rg = relationGuide.getChildRO(f.predicate);
					if (rg == null && !Config.allLengthOneAfterSample)
						continue;
				}
				exploreFirstOntology(nx1, nr1, y1, rg);
			}

			assert (!equalityProduct.keySet().contains(null));
			
			if (equalities != null)
				setEqualities(y1);
		}

		/** Reduces a map to one value with the minimum */
		//xch1 xch1增加一对一假设选项
		public void reduceToMaxMax(int y1) {

			//TruthValue max = new TruthValue(0,0);
            double max = 0;
			int key = -1;
			//xch1 这个版本的1对一假设处理只在run==9时处理
			if (fs1.has_1v1_assumption && fs2.has_1v1_assumption){ // && run == setting.last_run
                for (int y2 = 0; y2 < fs2.num_proper_entities(); y2++) {
                    if (temp_a_s_for_one_entity[y2] == null)
                        continue;
                    if (fs1.belong_to_1v1_assumption(fs1.id_big_to_small(y1))) {
						if (temp_a_s_for_one_entity[y2].truthValue.getConfidence() > max && fs2.belong_to_1v1_assumption(y2)) {
							key = y2;
							max = temp_a_s_for_one_entity[y2].truthValue.getConfidence();
						}
					}
					if (!fs1.belong_to_1v1_assumption(fs1.id_big_to_small(y1))) {
						if (temp_a_s_for_one_entity[y2].truthValue.getConfidence() > max && !fs2.belong_to_1v1_assumption(y2)) {
							key = y2;
							max = temp_a_s_for_one_entity[y2].truthValue.getConfidence();
						}
					}
                }
            }
			else{
                for (int y2 = 0; y2 < fs2.num_proper_entities(); y2++) {
                    if (temp_a_s_for_one_entity[y2] == null)
                        continue;
					if (temp_a_s_for_one_entity[y2].truthValue.getConfidence() > max) {
                        key = y2;
                        max = temp_a_s_for_one_entity[y2].truthValue.getConfidence();
                    }
				}
            }
            for (int y2 = 0; y2 < fs2.num_proper_entities(); y2++) {
                if (temp_a_s_for_one_entity[y2] == null)
                    continue;
                if (y2 != key) {
                    temp_a_s_for_one_entity[y2] = null;
                }
            }
		}


		void setEqualities(int y1) {
            //xch与paris-main不同之处19. setEqualities是否做reduceToMinMin 没做 做法有待改进
            reduce_to_1v1_range_assumption(y1, temp_a_s_for_one_entity, fs1, fs2);
            //reduceToMaxMax(y1);
            for (int y2 = 0; y2 < fs2.num_proper_entities(); y2++) {
                if (temp_a_s_for_one_entity[y2] == null)
                    continue;
                //可优化，提前排序抽取前几
                equalities.insert_alignment_sentence_left(temp_a_s_for_one_entity[y2]);
            }
		}
		

		/** Find equality candidates for an entity y1 */
		public void findEqualsOf1(int y1) {
            //注意y1、x1、y2、x2是fs1中的大序号
            //temporary  AlignmentSentence for the entity y1
            //xch与paris-main不同之处5. 没有equalityProduct结构，有temp_a_s_for_one_entity   alignment_sentences_of_entity_embedding
            temp_a_s_for_one_entity = new AlignmentSentence[fs2.num_proper_entities()];
            
            TruthValue tr[], temp; 
            TruthValue p[]; 
            TruthValue negative_evidence_of_r1;
            tr = new TruthValue[25];
            p = new TruthValue[6];
            //int y2;
            //boolean first_x2pair;
            boolean record_stamp_evidence = false;
            if (display_evidence || setting.add_evidence_remove_duplicate){
                record_stamp_evidence = true;
            }
            
			List<PredicateAndObject> facts = fs1.factsAbout(y1); 
			for (int i = 0; i < facts.size(); i++) {
                negative_evidence_of_r1 = new TruthValue(); 
				int x1 = facts.get(i).object;
				int r1bis = FactStore.inverse(facts.get(i).predicate);
				
				if (!fs1.isLiteral(x1) && Config.ignoreClasses && fs1.isClass(x1))
				  continue;
				
				if (mapperOutput.neighborhoods[r1bis] == null) {
					mapperOutput.neighborhoods[r1bis] = new HashArrayNeighborhood(fs2, run, true, Math.min(fs2.getJoinLengthLimit(), setting.sumJoinLengthLimit - 1));
				}
				Neighborhood currentNeighborhood = mapperOutput.neighborhoods[r1bis];

                
                //xch与paris-main不同之处6.（在函数内层）返回TruthValue，并且过滤由subIndexScore[sub] < Config.THETA 变成 alignment_sentences_fs1_To_fs2[left][i].truthValue.getExpectation() < Config.THETA，下面又进行过滤
				//first_x2pair = true;
                for (Pair<Object, TruthValue> x2pair : computed.equalToScoredId(fs1, x1)) {
					int x2 = (Integer) x2pair.first();
					TruthValue xeqv = x2pair.second();
					assert(xeqv.getFrequency() >= 0 && xeqv.getFrequency() <= 1);

                    //if (xeqv.getConfidence() <= Config.THETA)
                    //    continue;

					// for all matching x2, y2's, we need to add weight for the normalizer
                    //equalToScored only take proper entity as input
                    //xch与paris-main不同之处7.对y2pair的过滤
                    for (Pair<Object, TruthValue> y2pair : computed.equalToScored(fs1, y1)) {
                        TruthValue yeqv = y2pair.second();
                        if (setting.use_path_3){
                            tr[1] = new TruthValue(1,1);    //(∗, x1, y1) → r1 (12)
                            tr[2] = new TruthValue(xeqv);    //x1 ↔ x2 (13)
                            tr[3] = TruthFunctions.analogy(tr[1], tr[2]);    //(∗, x2, y1) → r1 (14)
                            tr[4] = new TruthValue(yeqv);    //y1 ↔ y2 (15)
                            tr[5] = TruthFunctions.analogy(tr[3], tr[4]);    //(∗, x2, y2) → r1 (16)
                            tr[6] = new TruthValue(0,setting.missing_triple_initial_confidence);    //(∗, x2, y2) → r2 (17)
                            p[1] = TruthFunctions.induction(tr[6], tr[5]); //r1 → r2 (18)  //caution (tr[6], tr[5])
                            negative_evidence_of_r1 = TruthFunctions.revision(negative_evidence_of_r1, p[1]);
                        }
                        else{
                            if (setting.use_c_as_probability_value){ 
                                //xch与paris-main不同之处8.怎么增长relationNormalizer
                                mapperOutput.relationNormalizer.incrementCurrentRealNormalizer(xeqv.getConfidence() * yeqv.getConfidence());
                            }
                            else { 
                                //xch与paris-main不同之处8.怎么增长relationNormalizer
                                mapperOutput.relationNormalizer.incrementCurrentRealNormalizer(xeqv.getFrequency() * xeqv.getConfidence() * yeqv.getFrequency() * yeqv.getConfidence());
                            }
                        }
                        break;
                    }
                    //first_x2pair = false;
					List<PredicateAndObject> facts2 = fs2.factsAbout(x2); 
					for (int j = 0; j < facts2.size(); j++) {             
						int ny2 = facts2.get(j).object;
                        //xch与paris-main不同之处9. 字符串匹配时：TruthValue(1.f, setting.literal_equal_initial_confidence)
                        //xch与paris-main不同之处10. 字符串不匹配时：0. 和 TruthValue(0.f, 0.)
                        //xch与paris-main不同之处11. 实体无该对齐时：0. TruthValue(0, unaligned_entity_equal_initial_confidence)
						TruthValue yeqv = computed.equality(fs1, y1, ny2);
						int r2bis = facts2.get(j).predicate;
						Neighborhood nn2 = currentNeighborhood.getChild(run, r2bis);
                        //xch2.1  加以下代码使得第一轮关系继承数21w缩减为2w   35%->40%  THETA*3 ->45%
                        //xch与paris-main不同之处12. ny2 register关系是否有过滤
                        if (setting.use_path_3 && xeqv.getFrequency() > 0 && xeqv.getConfidence() > 0 && yeqv.getFrequency() > 0 && yeqv.getConfidence() > 0){
                            if (mapperOutput.positive_evidence_of_path3_r1.get(r2bis) == null) {
                                mapperOutput.positive_evidence_of_path3_r1.put(r2bis, new ArrayList<Pair<TruthValue, TruthValue>>());
                            }
                            mapperOutput.positive_evidence_of_path3_r1.get(r2bis).add(new Pair<>(xeqv, yeqv));
                        }
                        else{
                            if (setting.use_c_as_probability_value){  //yeqv.getConfidence() > Config.THETA*3
                                nn2.registerOccurrence(xeqv.getConfidence());
                                nn2.registerScore(xeqv.getConfidence() * yeqv.getConfidence());
                            }
                            else {
                                nn2.registerOccurrence(xeqv.getFrequency());
                                nn2.registerScore(xeqv.getFrequency() * xeqv.getConfidence() * yeqv.getFrequency() * yeqv.getConfidence());
                            } 
                        }
                        //xch2.0补充
                        if (fs2.isLiteral(ny2))
                            continue;
//						if (fs2.relation(r2bis).startsWith("dbp:infl") || fs2.relation(r2bis).toString().startsWith("influences"))
//							Announce.message("@@@score", nr1.toString(), fs2.relation(r2bis), fs1.entity(x1), fs1.entity(y1), fs2.entity(x2), fs2.entity(ny2), xeqv, yeqv);
						if (equalities != null)
                            register_alignment_evidence(x1, r1bis, y1, x2pair.second(), x2, r2bis, ny2, i, j, record_stamp_evidence);
					}
                    //xch2.1 只循环一次
                    if(setting.use_attribute_value_emb_sim){
                        if(!fs1.isLiteral(x1)){
                            break;
                        }
                    }
                    else{
                        break;
                    }
				}
                mapperOutput.relationNormalizer.addNormalizer(r1bis);
				currentNeighborhood.propagateScores();
                mapperOutput.compute_path3(r1bis, negative_evidence_of_r1, setting.missing_triple_initial_confidence);
			}
			if (equalities != null){
                if(setting.precompute_emb_sim){
                    if(setting.use_entity_emb_sim||setting.use_translate_emb){
                        //LinkedList<AlignmentSentence> list =  EqualityStore.deep_clone_list(alignment_sentences_of_entity_embedding.get(fs1.id_big_to_small(y1)));
                        LinkedList<AlignmentSentence> list =  alignment_sentences_of_entity_embedding.get(fs1.id_big_to_small(y1));
                        if (list != null){
                            ListIterator<AlignmentSentence> it = list.listIterator();
                            while (it.hasNext()) {
                                AlignmentSentence a = it.next();
                                if (temp_a_s_for_one_entity[fs2.id_big_to_small(a.similarityStatement.predTermIndex)] == null){
                                    temp_a_s_for_one_entity[fs2.id_big_to_small(a.similarityStatement.predTermIndex)] = a;
                                }
                                else{
                                    temp_a_s_for_one_entity[fs2.id_big_to_small(a.similarityStatement.predTermIndex)].add_evidence(a.truthValue, 2, setting.use_c_as_probability_value);
                                }
                            }
                        }
                    }
                }
                else{
                    if(setting.use_entity_emb_sim){
                        TruthValue t;
                        double best = 0;
                        int best_y2 = -1;
                        if(fs1.entity_emb.get(y1)!=null){
                            /* 
                            for(int y2 : fs2.entity_emb.keySet().toArray()){
                                if(fs2.entity_emb.get(y2)!=null){
                                    double sim = FactStore.cosine_similarity((List<Float>)fs1.entity_emb.get(y1), (List<Float>)fs2.entity_emb.get(y2));
                                    if (sim > best){
                                        best = sim;
                                        best_y2 = y2;
                                    }
                                }
                            }*/
                            for(int y2 : fs2.entity_emb.keySet().toArray()){
                                if(fs2.entity_emb.get(y2)!=null){
                                    double sim = FactStore.cosine_similarity((List<Float>)fs1.entity_emb.get(y1), (List<Float>)fs2.entity_emb.get(y2));
                                    if (setting.adaptive_entity_emb_sim_confidence){
                                        if (setting.use_translate_emb){
                                            if (computed.avg_sim_confidence!=0)                                                                        //y2 == best_y2
                                                t = TruthValue.truthValue_of_entity_embedding(sim, TruthFunctions.divide_confidence(computed.avg_sim_confidence, 2), false, setting.use_c_as_probability_value);
                                            else 
                                                t = TruthValue.truthValue_of_entity_embedding(sim,  TruthFunctions.divide_confidence(0.5, 2), false, setting.use_c_as_probability_value);
                                        }
                                        else{
                                            if (computed.avg_sim_confidence!=0)                                                                        //y2 == best_y2
                                                t = TruthValue.truthValue_of_entity_embedding(sim, computed.avg_sim_confidence, false, setting.use_c_as_probability_value);
                                            else 
                                                t = TruthValue.truthValue_of_entity_embedding(sim, 0.5, false, setting.use_c_as_probability_value);
                                        }
                                    }
                                    else
                                        t = TruthValue.truthValue_of_entity_embedding(sim, setting.entity_emb_sim_confidence, false, setting.use_c_as_probability_value);
                                    if (temp_a_s_for_one_entity[fs2.id_big_to_small(y2)] == null){
                                        temp_a_s_for_one_entity[fs2.id_big_to_small(y2)] = new AlignmentSentence(y1, y2, t, 2);
                                    }
                                    else{
                                        temp_a_s_for_one_entity[fs2.id_big_to_small(y2)].add_evidence(t, 2, setting.use_c_as_probability_value);
                                    }
                                }
                            }
                        }
                    }
                    if(setting.use_translate_emb){
                        TruthValue t;
                        double best = 0;
                        int best_y2 = -1;
                        if(fs1.trans_entity_emb.get(y1)!=null){
                            for(int y2 : fs2.trans_entity_emb.keySet().toArray()){
                                if(fs2.trans_entity_emb.get(y2)!=null){
                                    double sim = FactStore.cosine_similarity((List<Float>)fs1.trans_entity_emb.get(y1), (List<Float>)fs2.trans_entity_emb.get(y2));
                                    if (setting.adaptive_entity_emb_sim_confidence){
                                        if (setting.use_entity_emb_sim){
                                            if (computed.avg_sim_confidence!=0)                                                                        //y2 == best_y2
                                                t = TruthValue.truthValue_of_entity_embedding(sim, TruthFunctions.divide_confidence(computed.avg_sim_confidence, 2), false, setting.use_c_as_probability_value);
                                            else 
                                                t = TruthValue.truthValue_of_entity_embedding(sim,  TruthFunctions.divide_confidence(0.5, 2), false, setting.use_c_as_probability_value);
                                        }
                                        else{
                                            if (computed.avg_sim_confidence!=0)                                                                        //y2 == best_y2
                                                t = TruthValue.truthValue_of_entity_embedding(sim, computed.avg_sim_confidence, false, setting.use_c_as_probability_value);
                                            else 
                                                t = TruthValue.truthValue_of_entity_embedding(sim, 0.5, false, setting.use_c_as_probability_value);
                                        }
                                    }
                                    else
                                        t = TruthValue.truthValue_of_entity_embedding(sim, setting.trans_entity_emb_sim_confidence, y2 == best_y2, setting.use_c_as_probability_value);
                                    if (temp_a_s_for_one_entity[fs2.id_big_to_small(y2)] == null){
                                        temp_a_s_for_one_entity[fs2.id_big_to_small(y2)] = new AlignmentSentence(y1, y2, t, 2);
                                    }
                                    else{
                                        temp_a_s_for_one_entity[fs2.id_big_to_small(y2)].add_evidence(t, 2, setting.use_c_as_probability_value);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (equalities != null&&setting.entity_clustering&&run>=setting.entity_clustering_run){
                //small ids
                int y1c, y2c, y2, y1_small;
                TruthValue t[];
                t = new TruthValue[25];
                //(𝑡𝑦𝑝𝑒 𝐼𝑋 𝑝𝑎𝑡ℎ)
                y1c = exemplars_1[fs1.id_big_to_small(y1)];
                if (y1c >= 0){
                    y1_small = fs1.id_big_to_small(y1);
                    HashMap<Integer, AlignmentSentence> alignment_sentences_of_class_y1c;
                    alignment_sentences_of_class_y1c = alignment_sentences_of_classes.get(y1c);
                    if (alignment_sentences_of_class_y1c != null){
                        for (int y2c_:alignment_sentences_of_class_y1c.keySet()){
                            y2c = y2c_;
                            if (alignment_sentences_of_class_y1c.get(y2c).truthValue.getConfidence()<0.1)
                                continue;
                            for (int y2_:exemplar_view_2.get(y2c)){
                                y2 = y2_;
                                if (y1_small <= y1c)
                                    t[1] = intensional_similarity_1.get(y1_small).get(y1c); //𝑥 → 𝑥𝑐 ⟨𝑓1, 𝑐1⟩
                                else
                                    t[1] = intensional_similarity_1.get(y1c).get(y1_small); //𝑥 → 𝑥𝑐 ⟨𝑓1, 𝑐1⟩
                                t[2] = alignment_sentences_of_class_y1c.get(y2c).truthValue;  //𝑥c ↔ 𝑦c ⟨𝑓2, 𝑐2⟩
                                t[3] = TruthFunctions.analogy(t[1], t[2]);   //𝑥 → 𝑦𝑐 ⟨𝑓3, 𝑐3⟩
                                if (y2 <= y2c)
                                    t[4] = intensional_similarity_2.get(y2).get(y2c); //y → y𝑐 ⟨𝑓1, 𝑐1⟩
                                else
                                    t[4] = intensional_similarity_2.get(y2c).get(y2); //y → y𝑐 ⟨𝑓1, 𝑐1⟩
                                t[5] = TruthFunctions.comparison(t[3], t[4]);   //𝑥 ↔ 𝑦 ⟨𝑓5, 𝑐5⟩
                                
                                if (t[5].getConfidence()<0.1)
                                    continue;
                                if (temp_a_s_for_one_entity[y2] == null){
                                    temp_a_s_for_one_entity[y2] = new AlignmentSentence(y1, factStore2.id_small_to_big(y2), t[5], 3);
                                }
                                else{
                                    temp_a_s_for_one_entity[y2].add_evidence(t[5], 3, setting.use_c_as_probability_value);
                                }
                            }
                            
                        }
                    }
                }
            }
             
			if (equalities != null)
				setEqualities(y1);
        }

		/** Run findEqualsOf on entities fetched from inputs */
		public MapperOutput findEqualsOfQueue() {
			
			//equalityProduct = new HashMap<Integer, Double>();
			//fullEqualityProduct = new HashMap<Pair<Integer, Pair<Integer, Integer>>, Pair<Pair<Double, Double>, Double>>();
            //temp_a_s_for_one_entity = new AlignmentSentence[fs2.num_proper_entities()];
			
			int done = 0;
			long start = System.currentTimeMillis();
			long last = start;
			int nManaged = 0;
			while (true) {
				Integer e1;  
				try {
					e1 = inputs.remove();
				} catch (java.util.NoSuchElementException e) {
					// someone took the last item from the queue before we did
					break;
				}
				++done;
				if (done % setting.reportInterval == 0) {
					// Announce.message("Entities done:", done, "Time per entity:", timeSum
					// / ((float) reportInterval), "ms     Facts per entity:", factSum /
					// ((float) reportInterval),
					// NumberFormatter.formatMS((long) ((double) (System.currentTimeMillis()
					// - timeStart) / (done - startAt) * (total - done))));
					long t = System.currentTimeMillis();
					double perEntity = (t - start)
							/ ((float) done);
					Announce.message("(" + id + ") Entities done:", done,
									"Time per entity:", perEntity, "ms");
					Announce.message("(" + id + ") Last time:", t - last);
					Announce.message("(" + id + ") Last entity:", fs1.entity(e1));
					last = t;
				}
				nManaged++;
				
				if (nManaged == limit) {
					break;
				}
				
				if (setting.debugEntity != null) {
					if (fs1.entity(e1).contains(setting.debugEntity)) {
						Announce.message("DEBUGENTITY");
						Announce.setLevel(Level.DEBUG);
						localDebug = true;
					}
				}

				if (localJoinLengthLimit1 == 1 && localJoinLengthLimit2 == 1 && setting.sampleEntities == 0 && setting.optimizeNoJoins){
                    findEqualsOf1(e1);
                }
				else{
                    findEqualsOf(e1);
                }
				if (setting.debugEntity != null) {
					if (fs1.entity(e1).contains(setting.debugEntity)) {
						Announce.setLevel(Level.MESSAGES);
						localDebug = false;
					}
				}
			}
			Announce.message("run", run, nManaged, "actually managed");
			return mapperOutput;
		}

		public void run() {
			target.add(findEqualsOfQueue());
		}
	}

    public static void reduce_to_1v1_range_assumption(int y1, AlignmentSentence temp_a_s_for_one_entity[], FactStore fs1, FactStore fs2) {
        //xch1 这个版本的1对一假设处理只在run==9时处理
        if (fs1.has_1v1_assumption && fs2.has_1v1_assumption){ // && run == setting.last_run
            for (int y2 = 0; y2 < fs2.num_proper_entities(); y2++) {
                if (temp_a_s_for_one_entity[y2] == null)
                    continue;
                if (fs1.belong_to_1v1_assumption(fs1.id_big_to_small(y1))) {
                    if (!fs2.belong_to_1v1_assumption(y2)) {
                        temp_a_s_for_one_entity[y2] = null;
                    }
                }
                if (!fs1.belong_to_1v1_assumption(fs1.id_big_to_small(y1))) {
                    if (fs2.belong_to_1v1_assumption(y2)) {
                        temp_a_s_for_one_entity[y2] = null;
                    }
                }
            }
        }
    }

	/** limit the mapperOutput to interesting alignments and return the relation guide */
	public static Neighborhood endSampling(int run, MapperOutput mapperOutput) {
	//the current relation normalizer and neighborhoods are the results of exploring without constraints
		Announce.message("End of the sampling phase!");
		if (setting.printNeighborhoodsSampling) {
			Announce.message("BEFORE:");
			mapperOutput.print(computed.other(mapperOutput.fs));
		}
		Neighborhood relationGuide = new HashArrayNeighborhood(mapperOutput.fs, -1, true, mapperOutput.fs.getJoinLengthLimit());
		for (int i = 0; i < mapperOutput.fs.maxJoinRelationCode(); i++) {
			if (mapperOutput.neighborhoods[i] == null)
				continue;
			boolean result;
			JoinRelation jr = mapperOutput.fs.joinRelationByCode(i);
			if (mapperOutput.relationNormalizer.getNormalizer(jr) > setting.joinThreshold) {
				result = mapperOutput.neighborhoods[i].thresholdByNormalizer(mapperOutput.relationNormalizer.getNormalizer(jr), setting.joinThreshold, jr.length() == 1);
			} else {
				mapperOutput.neighborhoods[i] = null;
				result = false;
			}
			if (result) {
				// write in relationGuide that the join relation i in the first ontology should be explored
				Neighborhood cn = relationGuide;
				for (int j = 0; j < jr.length(); j++) {
					assert(jr.get(j) <= mapperOutput.fs.maxRelationId());
					cn = cn.getChild(run, jr.get(j));
				}
				// cn is now the neighborhood representing the join relation i
				// we don't care about the value that it carries, just that it exists
			}
		}
		// relationGuide is now the tree of join relations in onto 1 which align to something in onto 2 (like the statistics module or something)
		// mapperOutput.neighborhoods[i] is now the tree of join relations in onto 2 which align to join relation i in onto 1 
		if (setting.printNeighborhoodsSampling) {
			Announce.message("AFTER:");
			mapperOutput.print(computed.other(mapperOutput.fs));
			Announce.message("GUIDE:");
			((HashArrayNeighborhood) relationGuide).print(new JoinRelation(mapperOutput.fs));
		}
		return relationGuide;
	}
	
	public static MapperOutput aggregateThreads(int run, FactStore factStore, EqualityStore equalities, EqualityStoreMultiple equalitiesMultiple, MapperOutput mapperOutput,
			Neighborhood relationGuide, ConcurrentLinkedQueue<Integer> inputs, int limit) throws InterruptedException {
		Announce.message("Spawning", setting.nThreads, "threads");
		LinkedList<Thread> threads = new LinkedList<Thread>();
		BlockingQueue<MapperOutput> results = new LinkedBlockingQueue<MapperOutput>();
	  for (int i = 0; i < setting.nThreads ; i++) {
	  	MapperOutput myMapperOutput;
	  	if (mapperOutput == null) {
	  		myMapperOutput = new MapperOutput(factStore);
	  	} else {
	  		// If we want to resume from a mapperOutput, we have to create nThreads copies of it and scale them down by this factor
	  		myMapperOutput = new MapperOutput(mapperOutput);
	  		myMapperOutput.scaleDown(setting.nThreads);
	  	}
	  	
	    Mapper mapper = new Mapper(run, i, factStore, equalities, equalitiesMultiple, myMapperOutput, relationGuide, results, inputs, limit);
	    Thread thread = new Thread(mapper);
	    threads.add(thread);
		  thread.start();
	  }
		Announce.message("waiting for thread termination...");
		// wait for termination
		for (Thread thread : threads) {
			thread.join();
			Announce.message("... one thread joined");
		}
		Announce.doing("Aggregating results...");
		// aggregate results in a blank factstore
		mapperOutput = new MapperOutput(factStore);
		for (MapperOutput p : results) {
			Announce.message("Aggregated one result...");
			mapperOutput.reduceWith(p);
		}
		return mapperOutput;
	}
	
	/** Perfom the alignment of one factStore against the other
	 *  equality (initialized by caller) is where entity alignments are stored
	 *  the relation alignment is returned as a MapperOutput */
	public static MapperOutput oneIterationOneWay(int run,
			FactStore factStore, EqualityStore equalities, EqualityStoreMultiple equalitiesMultiple) throws InterruptedException {
		
		MapperOutput mapperOutput = null;
		
		Announce.message("starting equalities at", NumberFormatter.ISOtime());
		List<Integer> entities = factStore.properEntities();
		// initialize a queue with all entities to manage
		ConcurrentLinkedQueue<Integer> inputs = new ConcurrentLinkedQueue<Integer>();
		int nAdded = 0;
		if (setting.shuffleEntities) {
			Collections.shuffle(entities);
		}
		for (int i = 0; i < entities.size(); i++) {
			int e1 = entities.get(i);
			if ((factStore.isClass(e1) && Config.ignoreClasses)
					/*|| fs1.isRelation(e1)*/)
				continue;
			inputs.add(e1);
			nAdded++;
		}
		Announce.message("run", run, nAdded, "added to queue");
	
		int limit = run >= 2 && setting.sampleEntities > 0 ? setting.sampleEntities : 0;
		int tempNThreads = 0;
		if (limit > 0 && setting.debugSampling) {
			Announce.setLevel(Level.DEBUG);
			debug = true;
			tempNThreads = setting.nThreads;
			setting.nThreads = 1;
		}
		if (setting.nThreads == 1) {
			// perform the computation directly
			Mapper mapper = new Mapper(run, -1, factStore, equalities, equalitiesMultiple, new MapperOutput(factStore), null, null, inputs, limit);
			mapperOutput = mapper.findEqualsOfQueue();
		} else {
			// spawn threads to perform the computation
			Announce.message("Will manage", nAdded, "entities");
			mapperOutput = aggregateThreads(run, factStore, equalities, equalitiesMultiple, null, null, inputs, limit);
		}
		
		if (limit > 0) {
			Announce.message("Will end sampling");
			Neighborhood relationGuide = endSampling(run, mapperOutput);
			Announce.message("Will manage the rest now that sampling is done");
			if (setting.debugSampling) {
				Announce.setLevel(Level.MESSAGES);
				debug = false;
				setting.nThreads = tempNThreads;
			}
			if (setting.nThreads == 1) {
				Mapper mapper2 = new Mapper(run, -1, factStore, equalities, equalitiesMultiple, mapperOutput, relationGuide, null, inputs, 0);
				mapperOutput = mapper2.findEqualsOfQueue();
			} else {
				mapperOutput = aggregateThreads(run, factStore, equalities, equalitiesMultiple, mapperOutput, relationGuide, inputs, 0);
			}
			
			Announce.done();
		}
		
		Announce.done();
				
		return mapperOutput;
	}

	/** Runs one whole iteration 
	 * @throws InterruptedException */
	public static void oneIteration() throws IOException, InterruptedException {
        //xch2.0 强制EntityMatchers:label间的继承关系
        //xch与paris-main不同之处3.强制EntityMatchers:label间的继承关系
        //computed.superRelationsOf1.set_entityMatchers_label();
        //computed.superRelationsOf2.set_entityMatchers_label();
        
        //xch与paris-main不同之处4.EqualityStore存放多个对齐
		//equalities1 is the pending equality and computed.equalityStore is the old one from the last Iteration
		EqualityStore equalities1 = new EqualityStore(factStore1, factStore2, setting, run);
        //if((run >= 0 && run <= setting.display_evidence_run_small)||(run >= setting.display_evidence_run_big)||(run >= setting.add_evidence_remove_duplicate_run-1 && run <= setting.add_evidence_remove_duplicate_run+1))
        if(run >= setting.display_evidence_run_big)
            display_evidence = true;
        else
            display_evidence = false;
        Stamp.set_max_evidence(setting.max_evidence);
		//EqualityStore equalities2 = new EqualityStore(factStore2, factStore1);
		EqualityStoreMultiple equalitiesMultiple = null;
		if (setting.cleverMatching) {
			Announce.doing("Performing greedy approximation of the maximum matching...");
			equalitiesMultiple = new EqualityStoreMultiple(factStore1, factStore2);
			Announce.done();
		}
        if (setting.entity_clustering) {
            if (run == 0){
                entity_clustering(factStore1, intensional_similarity_1, exemplar_view_1, exemplars_1);
                entity_clustering(factStore2, intensional_similarity_2, exemplar_view_2, exemplars_2);
            }
            if (run>=setting.entity_clustering_run)
                type_8_path();
        }
		MapperOutput mapperOutput1 = null;
		MapperOutput mapperOutput2 = null;

		/** We do the computation on the ontologies */
		mapperOutput1 = oneIterationOneWay(run, factStore1, equalities1, equalitiesMultiple);
        equalities1.temp_alignment_proper_id = new HashMap<>();
        equalities1.temp_alignment_proper_id_sentences = new HashMap<>();
		if (setting.cleverMatching)
			equalities1 = equalitiesMultiple.takeMaxMaxClever();
        if (display_evidence){
		    equalities1.dump_eqv_full(new File(setting.tsvFolder, run + "_eqv_full.tsv"), 20, false, 4000);
        }
        if (setting.matching_strategy == 0){
            equalities1.populate_right_to_left();
        }
        else if (setting.matching_strategy == 1){
            equalities1.populate_right_to_left();
            equalities1.takeMaxMaxBothWays();
        }
        else if (setting.matching_strategy == 2){
            equalities1.sparseLAPJV();
        }
        else if (setting.matching_strategy == 3){
            equalities1.lapjv();
        }
        else if (setting.matching_strategy == 4){
            if(run == setting.last_run){
                equalities1.lapjv();
            }
            else{
                equalities1.populate_right_to_left();
                equalities1.takeMaxMaxBothWays();
            }
        }
        else if (setting.matching_strategy == 5){
            equalities1.populate_right_to_left();
            equalities1.new_takeMaxMaxBothWays();
        }
		
        if (setting.matching_strategy <= 4){
            equalities1.dump_eqv_full(new File(setting.tsvFolder, run + "_eqv.tsv"), 1, false);
        }
        else{
            equalities1.dump_eqv_full(new File(setting.tsvFolder, run + "_eqv.tsv"), 1, true);
        }
        
        if (display_evidence){
            equalities1.dump_eqv_full(new File(setting.tsvFolder, run + "_eqv_reverse.tsv"), 1, false, false, -1);
        }
		
        /*for (int i : equalities1.alignment_sentences_fs1_To_fs2.keySet()) {
            LinkedList<AlignmentSentence> list = equalities1.alignment_sentences_fs1_To_fs2.get(i);
            ListIterator<AlignmentSentence> it = list.listIterator();
            while (it.hasNext()) {
                AlignmentSentence a = it.next();
                D.p(a.toString());
            }
        }
        D.p("\n\n\n\n");
        for (int i : equalities1.alignment_sentences_fs2_To_fs1.keySet()) {
            LinkedList<AlignmentSentence> list = equalities1.alignment_sentences_fs2_To_fs1.get(i);
            ListIterator<AlignmentSentence> it = list.listIterator();
            while (it.hasNext()) {
                AlignmentSentence a = it.next();
                D.p(a.toString());
            }
        }*/
        //equalities1.dump(new File(setting.tsvFolder, run + "_eqv.tsv"));
		if (setting.bothWays) {
			mapperOutput2 = oneIterationOneWay(run, factStore2, null, null);
		}
		//xch2.0  display_evidence
        if (display_evidence){
            equalities1.setTSVfile(new File(setting.tsvFolder, run + "_evidences.tsv"));
            try {
				equalities1.display_all_evidences(computed, true);
			} catch (IOException e) {}
            
        }

		computed.mapperOutput1 = mapperOutput1;
		computed.mapperOutput2 = mapperOutput2;

		computed.equalityStore = equalities1;
        if ((setting.use_entity_emb_sim || setting.use_translate_emb) && setting.adaptive_entity_emb_sim_confidence && computed.avg_sim_confidence == 0 && run == 4){
            double avg_sim_confidence = computed.compute_avg_sim_confidence();
            computed = new Result(setting, factStore1, factStore2, setting.tsvFolder, value_to_id, id_to_value);
            int divide_confidence_num;
            if (setting.no_attr)
                divide_confidence_num = 2;
            else
                divide_confidence_num = 3;
            if (setting.bootsrtap == 0)
                divide_confidence_num *= 2;
            //if (setting.table_setting == 3)
            //    divide_confidence_num *= 2;
            computed.avg_sim_confidence = TruthFunctions.divide_confidence(avg_sim_confidence, divide_confidence_num);
            run = -1;
            Announce.message("done avg_sim_confidence: ", avg_sim_confidence);
            Announce.message("divide_confidence_num: ", divide_confidence_num);
            Announce.message("done divide_confidence(): ", computed.avg_sim_confidence);
            return;
        }
        
		Announce.message("done equalities at", NumberFormatter.ISOtime());

		/** Now, we aggregate the relation alignments to use them in the next iteration */
		Announce.message("loading neighborhoods in one direction");
		computed.superRelationsOf1.loadMapperOutput(mapperOutput1, setting.use_path_3);
		Announce.message("loading neighborhoods in other direction");
		if (setting.bothWays) {
			computed.superRelationsOf2.loadMapperOutput(mapperOutput2, setting.use_path_3);
		}

        if (display_evidence ){ //|| true
            /** Write the alignments */
            if (setting.bothWays) {
                //equalities2.dump(new File(setting.tsvFolder, run + "_eqv2.tsv"));
                computed.superRelationsOf2.dump(new File(setting.tsvFolder, run
                        + "_superrelations2.tsv"));
            }
            computed.superRelationsOf1.dump(new File(setting.tsvFolder, run
                    + "_superrelations1.tsv"));
        }
		if (setting.printNeighborhoodsSampling)
			computed.printNeighborhoods();
		Announce.progressDone();
		Announce.message("done properties at", NumberFormatter.ISOtime());

        //entity_clustering();
        
	}
    public static void entity_clustering(FactStore fs_pointer, ArrayList<ArrayList<TruthValue>> intensional_similarity, HashMap<Integer,ArrayList<Integer>> exemplar_view, int[] exemplars) throws IOException {
        TruthValue t[], temp; 
        TruthValue p[]; 
        t = new TruthValue[25];
        temp = null;
        p = new TruthValue[6];
        Announce.doing("Generating intensional similarity matrix...");
        //type 5 path
        t[1] = new TruthValue(1,1);    //(∗, 𝑥1, 𝑦1) → 𝑟11 ⟨𝑓1, 𝑐1⟩
        t[2] = new TruthValue(1,0.5);    //𝑟11 ↔ 𝑟12 ⟨𝑓2, 𝑐2⟩         (1,0.7) t[6]%1.00;0.41%  (0.7,0.7)t[6] %0.70;0.33%
        t[3] = TruthFunctions.analogy(t[1], t[2]);    //(∗, 𝑥1, 𝑦1) → 𝑟12 ⟨𝑓3, 𝑐3⟩
        t[4] = new TruthValue(1,1);    //(∗, 𝑥2, 𝑦2) → 𝑟12 ⟨𝑓4, 𝑐4⟩
        t[5] = TruthFunctions.comparison(t[3], t[4]);    //(∗, 𝑥1, 𝑦1) ↔ (∗, 𝑥2, 𝑦2) ⟨𝑓5, 𝑐5⟩
        t[6] = new TruthValue(t[5]);    //𝑦1 ↔ 𝑦2 ⟨𝑓6, 𝑐6⟩    //(questionable)
        p[1] = new TruthValue(t[6]);

        //type 6 path
        t[1] = new TruthValue(1,1);    //(∗, 𝑥, 𝑦1) → 𝑟11 ⟨𝑓1, 𝑐1⟩
        t[2] = new TruthValue(1,1);    //(∗, 𝑥, 𝑦2) → 𝑟12 ⟨𝑓2, 𝑐2⟩
        t[3] = TruthFunctions.intersection(t[1], t[2]);    //(∗, 𝑥, 𝑦1) → 𝑟11 ∧ (∗, 𝑥, 𝑦2) → 𝑟12 ⟨𝑓3, 𝑐3⟩
        t[4] = new TruthValue(1,0.7); //((∗, #𝑥, #𝑦1) → 𝑟11 ∧ (∗, #𝑥, #𝑦2) → 𝑟12) ⇒ 𝑟11 ↔ 𝑟12 ⟨𝑓4, 𝑐4⟩
        t[5] = TruthFunctions.deduction(t[3], t[4]);   //𝑟11 ↔ 𝑟12 ⟨𝑓5, 𝑐5⟩
        t[6] = new TruthValue(1,0.5);    //𝑟11 ↔ 𝑟12 ⟨𝑓6, 𝑐6⟩
        t[7] = TruthFunctions.revision(t[5], t[6]);   //𝑟11 ↔ 𝑟12 ⟨𝑓7, 𝑐7⟩
        t[8] = TruthFunctions.analogy(t[1], t[7]);    //(∗, 𝑥, 𝑦1) → 𝑟12 ⟨𝑓8, 𝑐8⟩
        t[9] = new TruthValue(t[8]);   //𝑦1 → (/, 𝑟12, 𝑥, _) ⟨𝑓9, 𝑐9⟩
        t[10] = new TruthValue(t[2]);    //𝑦2 → (/, 𝑟12, 𝑥, _) ⟨𝑓10, 𝑐10⟩
        t[11] = TruthFunctions.comparison(t[9], t[10]);    //y1 ↔ 𝑦2 ⟨𝑓11, 𝑐11⟩
        p[2] = new TruthValue(t[11]);
        //inherits_same_class
        ArrayList<TruthValue> similarity1;// = new ArrayList<>(fs_pointer.num_proper_entities());
        for (int i = 0; i < fs_pointer.num_proper_entities(); i++) {
            intensional_similarity.add(i, new ArrayList<>(fs_pointer.num_proper_entities()));
            similarity1 = intensional_similarity.get(i);
            for (int j = 0; j < fs_pointer.num_proper_entities(); j++) {
                similarity1.add(j, new TruthValue(0,0));
            }
        }
        print_memory(1501);
        HashMap<Integer, ArrayList<Integer>> relation_sub_view = new HashMap<Integer,ArrayList<Integer>>();
        HashMap<Integer, ArrayList<Integer>> relation_obj_view = new HashMap<Integer,ArrayList<Integer>>();
        List<SubjectAndObject> so_list;
        int sub1,obj1,sub2,obj2,size1,k_,k2_,j_small,k_small,k2_small;
        boolean is_attribute;
        TruthValue sim,temp1;
        for (int i = 0; i < fs_pointer.numRelations(); i++) {
            is_attribute = fs_pointer.is_attribute_relation(i);
            if (fs_pointer.isInverse(i))
                continue;
            so_list = fs_pointer.facts_about_relation(i);
            if (so_list == null)
                continue;
            relation_sub_view = new HashMap<Integer,ArrayList<Integer>>();
            relation_obj_view = new HashMap<Integer,ArrayList<Integer>>();
            for (int j = 0; j < so_list.size(); j++){
                SubjectAndObject so1 = so_list.get(j);
                sub1 = so1.subject;
                obj1 = so1.object;
                relation_sub_view.putIfAbsent(sub1, new ArrayList<Integer>());
                relation_sub_view.get(sub1).add(obj1);
                if(!is_attribute){ 
                    relation_obj_view.putIfAbsent(obj1, new ArrayList<Integer>());
                    relation_obj_view.get(obj1).add(sub1);
                }
            }
            //print_memory(1526);
            if(!is_attribute){
                for (int j:relation_sub_view.keySet()){
                    j_small = fs_pointer.id_big_to_small(j);
                    for (int k:relation_sub_view.keySet()){
                        k_small = fs_pointer.id_big_to_small(k);
                        if (j_small<k_small){
                            sim = intensional_similarity.get(j_small).get(k_small);
                            intensional_similarity.get(j_small).set(k_small, TruthFunctions.revision(sim, p[1]));
                        }
                    }
                }
                for (int j:relation_obj_view.keySet()){
                    j_small = fs_pointer.id_big_to_small(j);
                    for (int k:relation_obj_view.keySet()){
                        k_small = fs_pointer.id_big_to_small(k);
                        if (j_small<k_small){
                            sim = intensional_similarity.get(j_small).get(k_small);
                            intensional_similarity.get(j_small).set(k_small, TruthFunctions.revision(sim, p[1]));
                        }
                    }
                }
                for (int j:relation_sub_view.keySet()){
                    size1 = relation_sub_view.get(j).size();
                    j_small = fs_pointer.id_big_to_small(j);
                    if (size1<=1)
                        continue;
                    for (int k = 0; k < size1; k++){
                        k_ = relation_sub_view.get(j_small).get(k);
                        k_small = fs_pointer.id_big_to_small(k_);
                        for (int k2 = k+1; k2 < size1; k2++){
                            k2_ = relation_sub_view.get(j_small).get(k2);
                            k2_small = fs_pointer.id_big_to_small(k2_);
                            sim = intensional_similarity.get(k_small).get(k2_small);
                            intensional_similarity.get(k_small).set(k2_small, TruthFunctions.revision(TruthFunctions.de_revision(sim, p[1]), p[2]));
                        }
                    }
                }
                for (int j:relation_obj_view.keySet()){
                    size1 = relation_obj_view.get(j).size();
                    j_small = fs_pointer.id_big_to_small(j);
                    if (size1<=1)
                        continue;
                        for (int k = 0; k < size1; k++){
                            k_ = relation_obj_view.get(j_small).get(k);
                            k_small = fs_pointer.id_big_to_small(k_);
                            for (int k2 = k+1; k2 < size1; k2++){
                                k2_ = relation_obj_view.get(j_small).get(k2);
                                k2_small = fs_pointer.id_big_to_small(k2_);
                                sim = intensional_similarity.get(k_small).get(k2_small);
                                intensional_similarity.get(k_small).set(k2_small, TruthFunctions.revision(TruthFunctions.de_revision(sim, p[1]), p[2]));
                            }
                        }
                }
            }
            else{ //relation i is_attribute
                for (int j:relation_sub_view.keySet()){
                    j_small = fs_pointer.id_big_to_small(j);
                    for (int k:relation_sub_view.keySet()){
                        k_small = fs_pointer.id_big_to_small(k);
                        if (j_small<k_small){
                            sim = intensional_similarity.get(j_small).get(k_small);
                            intensional_similarity.get(j_small).set(k_small, TruthFunctions.revision(sim, p[1]));
                        }
                    }
                }
            }
        }
        print_memory(1582);
        double[][] s = new double[fs_pointer.num_proper_entities()][fs_pointer.num_proper_entities()];
        double[] s_list = new double[(fs_pointer.num_proper_entities() + 1)*fs_pointer.num_proper_entities()/2];
        int k=0;
        for (int i = 0; i < fs_pointer.num_proper_entities(); i++) {
            for (int j = 0; j < fs_pointer.num_proper_entities(); j++) {
                if (i<j){
                    s[i][j] = intensional_similarity.get(i).get(j).getExpectation();
                    s_list[k++] = intensional_similarity.get(i).get(j).getExpectation();
                }
                else if (i>j)
                    s[i][j] = intensional_similarity.get(j).get(i).getExpectation();
            }
        }
        double med = FactStore.median(s_list);
        for (int i = 0; i < fs_pointer.num_proper_entities(); i++) {
            s[i][i] = med;
            //todo: to be improved
            intensional_similarity.get(i).set(i, new TruthValue(0.7, med/0.7));
        }
        Announce.done();
        DataProvider provider = new MatrixProvider(s);
        AproBuilder builder = new AproBuilder();      
        Apro apro = builder.build(provider);
        apro.run(100);
        int exemplar;
        int display = 1;
        BufferedWriter w;
        double exp;
        print_memory(1614);
        System.arraycopy(apro.getExemplars(), 0, exemplars, 0, apro.getExemplars().length);
        //exemplars = apro.getExemplars();  //cause bug
        for (int i = 0; i < fs_pointer.num_proper_entities(); i++){
            exemplar = exemplars[i]; //apro.getExemplar(i);
            if (exemplar_view.containsKey(exemplar)){
                exemplar_view.get(exemplar).add(i);
            }
            else{
                exemplar_view.put(exemplar, new ArrayList());
                exemplar_view.get(exemplar).add(i);
            }
        }
        if (run<display){
            w = new BufferedWriter(new FileWriter(new File(setting.tsvFolder, run + "_exemplars_file_fs" + fs_pointer.fs_id + "_1.tsv")));
            for (int i = 0; i < fs_pointer.num_proper_entities(); i++){
                exemplar = exemplars[i];
                if(exemplar<0){
                    w.write(fs_pointer.proper_entity_s(i) + "    null" + "\n");
                }
                else
                    w.write(fs_pointer.proper_entity_s(i) + "    " + fs_pointer.proper_entity_s(exemplar) + "\n");
            }
            w.close();
            w = new BufferedWriter(new FileWriter(new File(setting.tsvFolder, run + "_exemplars_file_fs" + fs_pointer.fs_id + "_2.tsv")));
            w.write("exemplar_view.keySet().size(): " + exemplar_view.keySet().size() + "\n");
            for (int i:exemplar_view.keySet()){
                exemplar = i;
                for (int j = 0; j < exemplar_view.get(i).size(); j++){
                    int entity = exemplar_view.get(i).get(j);
                    if(exemplar<0){
                        w.write(fs_pointer.proper_entity_s(entity) + "    null" + "\n");
                    }
                    else{
                        if (entity<=exemplar){
                            sim = intensional_similarity.get(entity).get(exemplar);
                            exp = sim.getExpectation();
                        }
                        else{
                            sim = intensional_similarity.get(exemplar).get(entity);
                            exp = sim.getExpectation();
                        }
                        w.write(fs_pointer.proper_entity_s(entity) + "    " + fs_pointer.proper_entity_s(exemplar) + sim.toString() + "   " + exp + "\n");
                    }
                }
            }
            w.close();
        }
    }

    public static void print_memory(int line){    
        Announce.message("line" + line + ": Memory (Mb):");
        Announce.message("   Java Free: " + Runtime.getRuntime().freeMemory() / 1000 / 1000);
        Announce.message("   Java Max: " + Runtime.getRuntime().maxMemory() / 1000 / 1000);
        Announce.message("   Java Total: " + Runtime.getRuntime().totalMemory() / 1000 / 1000);
    }

    public static void type_8_path() throws IOException {
        alignment_sentences_of_classes = new HashMap<Integer, HashMap<Integer, AlignmentSentence>>();
        HashMap<Integer, Pair<Integer, TruthValue>> best_alignment_map = new HashMap<Integer, Pair<Integer, TruthValue>>();
        int exemplar_xc, x, y, exemplar_yc;
        int y1, y2;
        int k=0;
        TruthValue t[], temp; 
        TruthValue p[]; 
        t = new TruthValue[25];
        temp = null;
        p = new TruthValue[6];
        for (int i : computed.equalityStore.alignment_sentences_fs1_To_fs2.keySet()) {
            k=0;
            LinkedList<AlignmentSentence> list = computed.equalityStore.alignment_sentences_fs1_To_fs2.get(i);
            ListIterator<AlignmentSentence> it = list.listIterator();
            if (it.hasNext()) {
                k++;
                AlignmentSentence a = it.next();
                y1 = a.similarityStatement.subjTermIndex;
                y2 = a.similarityStatement.predTermIndex;
                best_alignment_map.put(factStore1.id_big_to_small(y1),new Pair(factStore2.id_big_to_small(y2), a.truthValue));
            }
        }
        HashMap<Integer, AlignmentSentence> alignment_sentences_of_exemplar_xc;
        //D.p("exemplars_2:" + exemplars_2[0] + exemplars_2[1] + exemplars_2[2]);
        for (int i:exemplar_view_1.keySet()){
            exemplar_xc = i; // entity small id
            if (exemplar_xc < 0)
                continue;
            for (int j = 0; j < exemplar_view_1.get(i).size(); j++){
                x = exemplar_view_1.get(i).get(j); // entity small id
                if (best_alignment_map.get(x) == null)
                    continue;
                y = best_alignment_map.get(x).first; // entity small id
                exemplar_yc = exemplars_2[y];
                if (exemplar_yc < 0)
                    continue;
                if (x <= exemplar_xc)
                    t[1] = intensional_similarity_1.get(x).get(exemplar_xc); //𝑥 → 𝑥𝑐 ⟨𝑓1, 𝑐1⟩
                else
                    t[1] = intensional_similarity_1.get(exemplar_xc).get(x); //𝑥 → 𝑥𝑐 ⟨𝑓1, 𝑐1⟩
                t[2] = best_alignment_map.get(x).second;  //𝑥 ↔ 𝑦 ⟨𝑓2, 𝑐2⟩
                t[3] = TruthFunctions.analogy(t[1], t[2]);   //𝑦 → 𝑥𝑐 ⟨𝑓3, 𝑐3⟩
                if (y <= exemplar_yc)
                    t[4] = intensional_similarity_2.get(y).get(exemplar_yc); //y → y𝑐 ⟨𝑓4, 𝑐4⟩  bug:0 0 
                else
                    t[4] = intensional_similarity_2.get(exemplar_yc).get(y); //y → y𝑐 ⟨𝑓4, 𝑐4⟩
                t[5] = TruthFunctions.comparison(t[3], t[4]);   //𝑥𝑐 ↔ 𝑦𝑐 ⟨𝑓5, 𝑐5⟩
                //D.p("12345:" + t[1].toString() + t[2].toString() + t[3].toString() + t[4].toString() + t[5].toString());
                alignment_sentences_of_classes.putIfAbsent(exemplar_xc, new HashMap<Integer, AlignmentSentence>());
                alignment_sentences_of_exemplar_xc = alignment_sentences_of_classes.get(exemplar_xc);
                if (!alignment_sentences_of_exemplar_xc.containsKey(exemplar_yc))
                    alignment_sentences_of_exemplar_xc.putIfAbsent(exemplar_yc, new AlignmentSentence(factStore1.id_small_to_big(exemplar_xc),factStore2.id_small_to_big(exemplar_yc),t[5],3));
                else{
                    alignment_sentences_of_exemplar_xc.get(exemplar_yc).add_evidence(t[5], 3, setting.use_c_as_probability_value);
                }
            }
        }
        int exemplar;
        int display = 1;
        BufferedWriter w;
        if (run<5 && display==1){
            w = new BufferedWriter(new FileWriter(new File(setting.tsvFolder, run + "_alignment_sentences_of_classes.tsv")));
            for (int i:alignment_sentences_of_classes.keySet()){
                for (int j:alignment_sentences_of_classes.get(i).keySet()){
                    w.write(alignment_sentences_of_classes.get(i).get(j).display(factStore1, factStore2) + "\n");
                }
            }
            w.close();
        }
    }

	public static FactStore loadFactStore(int fs_id, File path,File _1v1_assumption, String prefix, String uri, ArrayList<ArrayList<Float>> entity_emb, ArrayList<ArrayList<Float>> trans_entity_emb, ArrayList<String> emb_entity_names, HashMap<Integer,String> id_to_value) throws IOException {
		FactStore fs;
		fs = new FactStore(fs_id, setting, prefix, uri,
				setting.joinLengthLimit, setting.normalizeStrings, setting.normalizeDatesToYears);
		Announce.doing("Loading facts...");
		if (path.isFile()) {
			fs.load(path);
		} else {
			fs.load(path, Pattern.compile(".*"));
		}
		fs.prepare(_1v1_assumption, entity_emb, trans_entity_emb, emb_entity_names, id_to_value);
		Announce.done();
		return fs;
	}

    public static void load_embeddings(File entity_emb_file, File trans_entity_emb_file, File emb_entity_names_file, File attribute_value_names_flie, ArrayList<ArrayList<Float>> entity_emb, ArrayList<ArrayList<Float>> trans_entity_emb, ArrayList<String> emb_entity_names) throws IOException {
        int i,j;
        if(setting.use_entity_emb_sim){
            i = 0;
            ArrayList<Float> entity_emb_i;
            for(List columns : new CSVLines(entity_emb_file)) {
                j = 0;
                entity_emb_i = new ArrayList<>(300);
                for(Object column : columns){
                    entity_emb_i.add(Float.parseFloat(column.toString()));
                    j++;
                }
                entity_emb.add(entity_emb_i);
                //if(i<5){
                //    D.p(entity_emb_i.toString());
                //}
                i++;
            }
            //估计：float实体描述45m，属性值301m ?   属性值1.30GB
            
        }
        if(setting.use_entity_emb_sim||setting.use_translate_emb){
            i = 0;
            //BufferedReader reader = new BufferedReader(new FileReader(emb_entity_names_file));
            for(String line : new FileLines(emb_entity_names_file)){
                emb_entity_names.add(Parser.compressUri(line));
                if(i<5){
                    D.p(emb_entity_names.get(i));
                }
                i++;
            }
        }
        if(setting.use_translate_emb){
            i = 0;
            ArrayList<Float> entity_emb_i;
            for(List columns : new CSVLines(trans_entity_emb_file)) {
                j = 0;
                entity_emb_i = new ArrayList<>(300);
                for(Object column : columns){
                    entity_emb_i.add(Float.parseFloat(column.toString()));
                    j++;
                }
                trans_entity_emb.add(entity_emb_i);
                //if(i<5){
                //    D.p(entity_emb_i.toString());
                //}
                i++;
            }
        }
        if(setting.use_attribute_value_emb_sim){
            i = 0;
            id_to_value = new HashMap<Integer,String>(setting.upper_attribute_value_num);
            value_to_id = new HashMap<String,Integer>(setting.upper_attribute_value_num);
            for(String line : new FileLines(attribute_value_names_flie)){
                String value = Parser.compressUri("\"" + line + "\"");
                id_to_value.put(i, value);
                value_to_id.put(value, i);
                if(i<5){
                    D.p(id_to_value.get(i));
                }
                i++;
            }
            /* 
            i = 0;
            ArrayList<Float> attribute_value_emb_i;
            attribute_value_emb = new HashMap<Integer,ArrayList<Float>>(setting.upper_attribute_value_num);
            for(List columns : new CSVLines(attribute_value_emb_flie)) {
                j = 0;
                attribute_value_emb_i = new ArrayList<>(300);
                for(Object column : columns){
                    attribute_value_emb_i.add(Float.parseFloat(column.toString()));
                    j++;
                }
                attribute_value_emb.put(i, attribute_value_emb_i);
                i++;
            }
            */
        }
    }

    public static void insert(LinkedList<Pair<Integer, Float>> list, Pair<Integer, Float> inserting_sim){
        ListIterator<Pair<Integer, Float>> it = list.listIterator();
        boolean inserted = false;
        while (it.hasNext()) {
            Pair<Integer, Float> a = it.next();
            if(inserting_sim.second > a.second){
                it.previous();
                it.add(inserting_sim);
                inserted = true;
                break;
            }
            else if (inserting_sim.second.equals(a.second) ) {
                if(inserting_sim.first > a.first){
                    it.previous();
                    it.add(inserting_sim);
                    inserted = true;
                    break;
                }
            }
        }
        if (!inserted && list.size() < setting.max_value_sim_count){
            list.addLast(inserting_sim);
            return;
        }
        if(list.size() > setting.max_value_sim_count){
            list.removeLast();
        }
    }

    public static void load_value_emb_sim() throws IOException {
        int i,j,value_id,out_of_range,object_value_id = -1;
        boolean in_fs1, in_fs2, first_inserted;
        float sim;
        LinkedList<Pair<Integer, Float>> current_list;
        //value_emb_sim : value_id --> <entity id of other fs, sim>
        factStore1.value_emb_sim = new HashMap<Integer,LinkedList<Pair<Integer, Float>>>(factStore1.numLiterals());
        factStore2.value_emb_sim = new HashMap<Integer,LinkedList<Pair<Integer, Float>>>(factStore2.numLiterals());
        for(Map.Entry<Integer, String> entry : id_to_value.entrySet()){
            in_fs1 = factStore1.value_in_factStore.get(entry.getKey());
            in_fs2 = factStore2.value_in_factStore.get(entry.getKey());
            if(in_fs1){
                factStore1.value_emb_sim.put(entry.getKey(), new LinkedList<>());
            }
            if(in_fs2){
                factStore2.value_emb_sim.put(entry.getKey(), new LinkedList<>());
            }
        }
        i = 0;
        out_of_range = 0;
        for(List columns : new CSVLines(setting.attribute_value_emb_sim_1)) {
            first_inserted = false;
            j = -1;
            value_id = Math.round(Float.parseFloat(columns.get(0).toString()));
            //if (factStore1.value_in_factStore[value_id])
            current_list = factStore1.value_emb_sim.get(value_id);
            if (current_list == null){
                D.p("value_id:",value_id,"id_to_value.get(value_id)",id_to_value.get(value_id));
                //D.p("in_fs1:",factStore1.value_in_factStore.get(value_id));
                out_of_range++;
                continue;
            }
            for(Object column : columns){
                j++;
                if(j == 0){
                    continue;
                }
                if(j%2 == 1){
                    object_value_id = Math.round(Float.parseFloat(column.toString())); 
                    continue;
                }
                if(j%2 == 0){
                    sim = Float.parseFloat(column.toString()); 
                    if(sim >= setting.value_similarity_lower_bound){
                        //make sure not to store identity similarity
                        if(value_id != object_value_id){
                            if(!factStore2.value_in_factStore.get(object_value_id))
                                continue;
                            if (first_inserted == false){
                                insert(current_list, new Pair<Integer, Float>(factStore2.entity(id_to_value.get(object_value_id)), sim));
                                first_inserted = true;
                            }
                            else if(sim >= 0.8){
                                insert(current_list, new Pair<Integer, Float>(factStore2.entity(id_to_value.get(object_value_id)), sim));
                                first_inserted = true;
                                break;
                            }
                            else{
                                break;
                            }
                        }
                    }
                    else{
                        break;
                    }
                }
            }
            i++;
        }
        i = 0;
        for(List columns : new CSVLines(setting.attribute_value_emb_sim_2)) {
            first_inserted = false;
            j = -1;
            value_id = Math.round(Float.parseFloat(columns.get(0).toString()));
            current_list = factStore2.value_emb_sim.get(value_id);
            if (current_list == null){
                D.p("value_id:",value_id,"id_to_value.get(value_id)",id_to_value.get(value_id));
                out_of_range++;
                continue;
            }
            for(Object column : columns){
                j++;
                if(j == 0){
                    continue;
                }
                if(j%2 == 1){
                    object_value_id = Math.round(Float.parseFloat(column.toString())); 
                    continue;
                }
                if(j%2 == 0){
                    sim = Float.parseFloat(column.toString()); 
                    if(sim >= setting.value_similarity_lower_bound){
                        //make sure not to store identity similarity
                        if(value_id != object_value_id){
                            if(!factStore1.value_in_factStore.get(object_value_id))
                                continue;
                            if (first_inserted == false){
                                insert(current_list, new Pair<Integer, Float>(factStore1.entity(id_to_value.get(object_value_id)), sim));
                                first_inserted = true;
                            }
                            else if(sim >= 0.8){
                                insert(current_list, new Pair<Integer, Float>(factStore1.entity(id_to_value.get(object_value_id)), sim));
                                first_inserted = true;
                                break;
                            }
                            else{
                                break;
                            }
                        }
                    }
                    else{
                        break;
                    }
                }
            }
            i++;
        }
        D.p("out_of_range:",out_of_range);
    }
    /* 
    public static void compute_value_emb_sim(){
        factStore1.value_emb_sim = new ArrayList<LinkedList<Pair<Integer, Float>>>(factStore1.numLiterals());
        factStore2.value_emb_sim = new ArrayList<LinkedList<Pair<Integer, Float>>>(factStore2.numLiterals());
        double sim;
        boolean new_list;
        int count_bigger_than_lower_bound;
        boolean in_fs1, in_fs2;
        LinkedList<Pair<Integer, Float>> list1;
        int test_num = 0;
        boolean count_count;
        int normalizer = 0;
        int[] count_bigger_than_buckets = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        double[] buckets = {0.1,0.15,0.2,0.25,0.3,0.35,0.4,0.45,0.5,0.55,0.6,0.65,0.7,0.75,0.8,0.85,0.9,0.95,0.99};
        int[] print_count = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        for (int i = 0; i < factStore1.numLiterals(); i++) {
            factStore1.value_emb_sim.add(null);
        }
        for (int i = 0; i < factStore2.numLiterals(); i++) {
            factStore2.value_emb_sim.add(null);
        }
        for(Map.Entry<Integer, String> entry : id_to_value.entrySet()){
            in_fs1 = factStore1.value_in_factStore.get(entry.getKey());
            in_fs2 = factStore2.value_in_factStore.get(entry.getKey());
            //count_bigger_than_lower_bound = 0;
            new_list = false;
            if(in_fs1){
                if(entry.getKey()%200==0){
                    count_count = true;
                    normalizer++;
                }
                else{
                    count_count = false;
                }
                if(factStore1.value_emb_sim.get(entry.getKey()) == null){
                    list1 = new LinkedList<>();
                    new_list = true;
                }
                else{
                    list1 = factStore1.value_emb_sim.get(entry.getKey());
                }
                for (int i = 0; i < factStore2.value_in_factStore.size(); i++) {
                    if(factStore2.value_in_factStore.get(i)){
                        sim = FactStore.cosine_similarity(attribute_value_emb.get(entry.getKey()), attribute_value_emb.get(i));
                        //D.p("sim:", sim);
                        if(sim >= setting.value_similarity_lower_bound){
                            insert(list1, new Pair<Integer, Float>(i, (float)sim));
                            //count_bigger_than_lower_bound++;
                        }
                        if(count_count){
                            /*if(entry.getKey()<=20000){
                                for (int b = 0; b < buckets.length; b++) {
                                    if(sim >= buckets[b]){
                                        count_bigger_than_buckets[b]++;
                                        print_count[b]++;
                                    }
                                }
                            }
                            else{
                                for (int b = 0; b < buckets.length; b++) {
                                    if(sim >= buckets[b]){
                                        count_bigger_than_buckets[b]++;
                                    }
                                }
                            }/
                            for (int b = 0; b < buckets.length; b++) {
                                if(sim >= buckets[b]){
                                    count_bigger_than_buckets[b]++;
                                }
                            }
                        }
                    }
                }
                //D.p("count_bigger_than_lower_bound:", count_bigger_than_lower_bound);
                if(new_list){
                    factStore1.value_emb_sim.set(entry.getKey(),list1);
                }
                /*
                if(count_count&&entry.getKey()<=20000){
                    D.p("\n\n\n\n entry.getKey():",entry.getKey());
                    for (int b = 0; b < buckets.length; b++) {
                        D.p("print_count[", b, "]:",print_count[b]);
                    }
                    Arrays.fill(print_count,0);
                }/
                if(entry.getKey()%20000==0){
                    D.p(" entry.getKey():",entry.getKey());
                }
            }
        }
        D.p("\n all:");
        for (int b = 0; b < buckets.length; b++) {
            D.p("count_bigger_than_buckets[", b, "]/normalizer:",count_bigger_than_buckets[b]/normalizer);
        }
    }
    */

    public static void compute_entity_emb_sim(){
        AlignmentSentence temp_a_s_for_one_entity1[];
        temp_a_s_for_one_entity1 = new AlignmentSentence[factStore2.num_proper_entities()];
        TruthValue t;
        List<Integer> entities = factStore1.properEntities(); //with big entity ids
        boolean y1_belong;
        for (int i = 0; i < entities.size(); i++) {
            int y1 = entities.get(i);
            temp_a_s_for_one_entity1 = new AlignmentSentence[factStore2.num_proper_entities()];
            y1_belong = factStore1.belong_to_1v1_assumption(factStore1.id_big_to_small(y1));
            if(setting.use_entity_emb_sim){
                if(factStore1.entity_emb.get(y1)==null){
                    continue;
                }
                for(int y2 : factStore2.entity_emb.keySet().toArray()){
                    if (y1_belong) {
                        if (!factStore2.belong_to_1v1_assumption(y2)) {
                            continue;
                        }
                    }
                    if (!y1_belong) {
                        if (factStore2.belong_to_1v1_assumption(y2)) {
                            continue;
                        }
                    }
                    if(factStore2.entity_emb.get(y2)!=null){
                        double sim = FactStore.cosine_similarity((List<Float>)factStore1.entity_emb.get(y1), (List<Float>)factStore2.entity_emb.get(y2));
                        t = TruthValue.truthValue_of_entity_embedding(sim, setting.entity_emb_sim_confidence, true, setting.use_c_as_probability_value);
                        if (temp_a_s_for_one_entity1[factStore2.id_big_to_small(y2)] == null){
                            temp_a_s_for_one_entity1[factStore2.id_big_to_small(y2)] = new AlignmentSentence(y1, y2, t, 2);
                        }
                        else{
                            temp_a_s_for_one_entity1[factStore2.id_big_to_small(y2)].add_evidence(t, 2, setting.use_c_as_probability_value);
                        }
                    }
                }
            }
            if(setting.use_translate_emb){
                if(factStore1.trans_entity_emb.get(y1)==null){
                    continue;
                }
                for(int y2 : factStore2.trans_entity_emb.keySet().toArray()){
                    if (y1_belong) {
                        if (!factStore2.belong_to_1v1_assumption(y2)) {
                            continue;
                        }
                    }
                    if (!y1_belong) {
                        if (factStore2.belong_to_1v1_assumption(y2)) {
                            continue;
                        }
                    }
                    if(factStore2.trans_entity_emb.get(y2)!=null){
                        double sim = FactStore.cosine_similarity((List<Float>)factStore1.trans_entity_emb.get(y1), (List<Float>)factStore2.trans_entity_emb.get(y2));
                        t = TruthValue.truthValue_of_entity_embedding(sim, setting.trans_entity_emb_sim_confidence, true, setting.use_c_as_probability_value);
                        if (temp_a_s_for_one_entity1[factStore2.id_big_to_small(y2)] == null){
                            temp_a_s_for_one_entity1[factStore2.id_big_to_small(y2)] = new AlignmentSentence(y1, y2, t, 2);
                        }
                        else{
                            temp_a_s_for_one_entity1[factStore2.id_big_to_small(y2)].add_evidence(t, 2, setting.use_c_as_probability_value);
                        }
                    }
                }
            }

            //reduce_to_1v1_range_assumption(y1, temp_a_s_for_one_entity1, factStore1, factStore2);
            
            for (int y2 = 0; y2 < factStore2.num_proper_entities(); y2++) {
                if (temp_a_s_for_one_entity1[y2] == null)
                    continue;
                //可优化，提前排序抽取前几
                //D.p(y1,y2);  //setting.max_alignment_sentences*4
                EqualityStore.insert_alignment_sentence_left(temp_a_s_for_one_entity1[y2], alignment_sentences_of_entity_embedding, factStore1, 5000);
            }
        }
    }
	
	/** Runs the thing */
	public static void main(String[] args) throws Exception {
        System.setProperty("file.encoding", "UTF-8");
		// Load the setting
		if (args == null || args.length < 1) {
			Announce
					.help(
							"PARIS aligns the instances, relations, and classes of two knowledge bases (KBs).\n",
							"java paris.Paris <settingFile>",
							"      You can specify a file that has no content.",
							"      PARIS will then ask for the necessary data and store it in <settingFile>.\n",
							"java paris.Paris <kb1> <kb2> <outputFolder>",
							"      Aligns <kb1> and <kb2>, puts the results into <outputFolder>.\n",
							"java paris.Paris <factstore> <dump>",
							"      Dumps all entities of <factstore> to the file <dump>\n",
							"See http://webdam.inria.fr/paris/ for further information.");
			System.exit(1);
		}
		
		if (args.length == 2) {
	    setting = new Setting("", "", "", "", "", "", null);
			dumpFactStoreEntities(new File(args[0]), args[1]);
			System.exit(0);
		}

		Announce.doing("Starting PARIS");
		if (args.length == 3) {
			Announce.message("Settings specified on command line");
	    setting = new Setting("", ".", args[0], args[1], null, args[2], null);
		} else {
			Announce.message("Settings:", args[0]);
			setting = new Setting(new File(args[0]));			
		}

		// Prepare the folders
		if (!setting.tsvFolder.exists()
				&& D.readBoolean("Do you want to create the folder "
						+ setting.tsvFolder + "?"))
			setting.tsvFolder.mkdirs();
		for (File folder : new File[] { /* setting.berkeleyFolder, */setting.tsvFolder }) {
			if (folder.list().length > 0
					&& D.readBoolean("Do you want to DELETE the files in " + folder
							+ " ?")) {
				Announce.doing("Deleting files in", folder);
				for (File f : folder.listFiles())
					f.delete();
				Announce.done();
			}
		}

		// Set output to the log folder
		Announce.done();
		File logFile = new File(setting.home, "run_" + setting.name + "_"
				+ NumberFormatter.timeStamp() + ".txt");
		Announce.message("PARIS is now running!");
		Announce
				.message("For information about the current state of affairs, look into");
		Announce.message("   ", logFile);
		if (!test)
			Announce.setWriter(new FileWriter(logFile)); /**/

		Announce.message("PARIS running at", NumberFormatter.ISOtime());
		Announce.message("@TIME", "startup", System.currentTimeMillis() / 1000L);
		Config.print();
        
		long startTime = System.currentTimeMillis();
        
		TruthValue.set_TRUTH_EPSILON(setting.TRUTH_EPSILON);
        TruthFunctions.set_setting(setting);
		Announce.doing("Loading fact stores (could take a long time...)");
		//xch1如果Setting（比如通过paris.ini文件构建的）中没有一对一假设文件路径，则认为左右图谱无一对一假设
        //xch与paris-main不同之处1.1v1_assumption
        //xch与paris-main不同之处2.initial_f/c_fs1/2
        //Float [][] entity_emb;
        ArrayList<ArrayList<Float>> entity_emb = new ArrayList<>(40000);
        ArrayList<ArrayList<Float>> trans_entity_emb = new ArrayList<>(40000);
        ArrayList<String> emb_entity_names = new ArrayList<>(40000);
        load_embeddings(setting.entity_emb, setting.trans_entity_emb, setting.emb_entity_names, setting.attribute_value_names, entity_emb, trans_entity_emb, emb_entity_names);
		factStore1 = loadFactStore(1, setting.ontology1, setting.factstore1_1v1_assumption, "", "", entity_emb, trans_entity_emb, emb_entity_names, id_to_value);
		factStore2 = loadFactStore(2, setting.ontology2, setting.factstore2_1v1_assumption, "", "", entity_emb, trans_entity_emb, emb_entity_names, id_to_value);
		if(setting.use_attribute_value_emb_sim){
            load_value_emb_sim();
        }
        if(setting.precompute_emb_sim){
            alignment_sentences_of_entity_embedding = new HashMap<Integer, LinkedList<AlignmentSentence>>((int)(factStore1.num_proper_entities()/0.75 + 1));
            for (int i = 0; i < factStore1.num_proper_entities(); i++) {
                alignment_sentences_of_entity_embedding.put(i, new LinkedList<AlignmentSentence>());
            }
        
            if(setting.use_entity_emb_sim||setting.use_translate_emb){
                compute_entity_emb_sim();
            }
        }
        Announce.done();			
        entity_emb = null;
        trans_entity_emb = null;
        emb_entity_names = null;
		Runtime.getRuntime().gc();
		Announce.message("Total memory used now that fact stores are loaded:",
				(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1000000, "megabytes");
		
		assert(factStore1.getJoinLengthLimit() > 0);
		assert(factStore2.getJoinLengthLimit() > 0);
		Announce.done();

		Announce.message("@TIME", "loaded", System.currentTimeMillis() / 1000L);
		computed = new Result(setting, factStore1, factStore2, setting.tsvFolder, value_to_id, id_to_value);
		if (debug)
			Announce.setLevel(Level.DEBUG);
		if (test) {
			runTest();
		}
		Announce.message("Factstores loaded at", NumberFormatter.ISOtime());
        intensional_similarity_1 = new ArrayList<>();
        exemplar_view_1 = new HashMap<>();
        intensional_similarity_2 = new ArrayList<>();
        exemplar_view_2 = new HashMap<>();
        exemplars_1 = new int[factStore1.num_proper_entities()];
        exemplars_2 = new int[factStore2.num_proper_entities()];
        //todo:refresh exemplar_view
		for (run = 0; run < setting.endIteration; run++) {
			Announce.message("@TIME run=", run," ", System.currentTimeMillis() / 1000L);
			// note that we don't check anymore if something has changed...

			// xch注释 主入口！！！
			oneIteration();
		}
		Announce.message("@TIME", setting.endIteration + 1, System.currentTimeMillis() / 1000L);
		computed.startIteration(setting.endIteration);
		if (Config.doComputeClasses) {
			Announce.message("computing classes at", NumberFormatter.ISOtime());
			computeClassesOneWay(factStore1, factStore2);
			computeClassesOneWay(factStore2, factStore1);
			Announce.message("computed classes at", NumberFormatter.ISOtime());
		}
		Announce.message("@TIME", "classes", System.currentTimeMillis() / 1000L);
		computed.print();
		computed.close();
		System.out.printf("PARIS terminated after %d milliseconds\n",
				System.currentTimeMillis() - startTime);
		Announce.message("@TIME", "shutdown", System.currentTimeMillis() / 1000L);
		Announce.close();
	}

	private static void dumpFactStoreEntities(File in, String out)
			throws IOException, ClassNotFoundException {
		Announce.doing("loading fact store");
		factStore1 = loadFactStore(1, in, null, "", "", null, null, null, null);
		Announce.done();
		Announce.doing("dumping entities");
		BufferedWriter w = new BufferedWriter(new FileWriter(out));
		for (int entity : factStore1.properEntities()) {
				w.write(factStore1.entity(entity).toString() + "\n");
		}
		w.close();
		Announce.done();
	}

	// ************ If you want to play around, do it in the following {} !
	public static void runTest() throws IOException {
		computed.startIteration(99);
		Announce.setLevel(Level.DEBUG);
		debug = true;
		// test code goes here
		D.p(factStore1.factsAbout("Zhao_Ziyang"));
		D.p(factStore2.factsAbout("p1357789"));
		computed.close();
		D.exit();
	}

}
