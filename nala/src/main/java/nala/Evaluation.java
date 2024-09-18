package nala;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.filehandlers.FileLines;
import javatools.filehandlers.TSVFile;
import javatools.parsers.Char;
import nala.Config;
import nala.Setting;
import nala.nal.AlignmentSentence;
import nala.nal.TruthValue;

public class Evaluation {

    public static String trim(String string, char delimiter) {
        int len = string.length();
        int st = 0;

        while ((st < len) && (string.charAt(st) == delimiter)) {
            st++;
        }
        while ((st < len) && (string.charAt(len-1) == delimiter)) {
            len--;
        }
        return ((st > 0) || (len < string.length())) ? string.substring(st, len) : string;
    }

    public static class PieceOfAlignment {
        String e1,e2;
        TruthValue truthvalue;

        public PieceOfAlignment(final String e11, final String e22) {
            e1 = e11;    
            e2 = e22;            
        }

        public PieceOfAlignment(final String e11, final String e22, TruthValue truthvalue1) {
            e1 = e11;    
            e2 = e22;            
        }

        @Override
        public boolean equals(final Object that) { 
            if (that instanceof PieceOfAlignment) {
                final PieceOfAlignment a = (PieceOfAlignment)that;
                return
                    e1.equals(a.e1) &&
                    e2.equals(a.e2);
            }
            return false;
        }

        //important Override
        @Override
        public int hashCode() {
            return Objects.hash(e1, e2);
        }
    }
	
    /** Returns: Number of entities, number of correctly assigned entities, total number of assignemnts.
     * Ties take the first assignment.*/
    public static void evaluate(File eqvFile, Setting setting, File evaluateFile, int iteration) throws IOException {
        Set<PieceOfAlignment> set_predicted = new HashSet<>();
        Set<PieceOfAlignment> set_train = new HashSet<>();
        Set<PieceOfAlignment> set_valid = new HashSet<>();
        Set<PieceOfAlignment> set_test = new HashSet<>();
        Set<PieceOfAlignment> test_union_valid = new HashSet<>();
        Set<PieceOfAlignment> predicted_outside_train = new HashSet<>();

        Set<PieceOfAlignment> correct = new HashSet<>();
        String e1,e2,ts;
        //String[] split;
        TruthValue t;
        for (String line : new FileLines(eqvFile)) {
            String[] split = line.split("\t");
            if (split.length < 3) continue;
            try {
                e1 = split[0];
                e2 = split[1];
                ts = split[2];
                //dbp_en:E000860	wiki:Q2399267	%1.00;0.61%
                //dbp_en:E922267	wiki:Q3521034	%1.00;0.94%
                ts = trim(ts, '%');
                split = ts.split(";");
                t = new TruthValue(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
                set_predicted.add(new PieceOfAlignment(e1, e2, t));
            } catch (Exception e) {
                e.printStackTrace();
            }  
        }
        for (String line : new FileLines(setting.train)) {
            String[] split = line.split("\t");
            if (split.length < 2) continue;
            try {
                e1 = split[0];
                e2 = split[1];
                set_train.add(new PieceOfAlignment(e1, e2));
            } catch (Exception e) {
            e.printStackTrace();
            }
        }
        for (String line : new FileLines(setting.valid)) {
            String[] split = line.split("\t");
            if (split.length < 2) continue;
            try {
                e1 = split[0];
                e2 = split[1];
                set_valid.add(new PieceOfAlignment(e1, e2));
            } catch (Exception e) {
              e.printStackTrace();
            }
        }
        for (String line : new FileLines(setting.test)) {
            String[] split = line.split("\t");
            if (split.length < 2) continue;
            try {
                e1 = split[0];
                e2 = split[1];
                set_test.add(new PieceOfAlignment(e1, e2));
            } catch (Exception e) {
              e.printStackTrace();
            }
        }
        if (setting.zero_seed){
            set_test.addAll(set_train);
            set_train.clear();
        }
        double precision, recall, f1;
        predicted_outside_train.addAll(set_predicted);
        predicted_outside_train.removeAll(set_train);
        int num_correct, len_predicted_outside_train;
        len_predicted_outside_train = predicted_outside_train.size();
        test_union_valid.addAll(set_test);
        test_union_valid.addAll(set_valid);
        correct.addAll(predicted_outside_train);
        correct.retainAll(test_union_valid);
        num_correct = correct.size();
        //Announce.message("len_predicted_outside_train", len_predicted_outside_train);
        //Announce.message("num_correct", num_correct);
        //Announce.message("151");
        BufferedWriter w = new BufferedWriter(new FileWriter(evaluateFile, true));
        String str = "";
        if(num_correct == 0){
            str = "iteration: " + iteration + " " + "precision: " + String.format("%.5f", 0) + " " + "recall: " + String.format("%.5f", 0) + " " + "f1: " + String.format("%.5f", 0) + " len_predicted_outside_train: " + len_predicted_outside_train + "\n";
            w.write(str);
            w.close();
            return;
        }
        precision = (double) num_correct / (double) predicted_outside_train.size();
        recall = (double) num_correct / (double) test_union_valid.size();
        f1 = 2 * precision * recall / (precision + recall);
        str = "iteration: " + iteration + " " + "precision: " + String.format("%.5f", precision) + " " + "recall: " + String.format("%.5f", recall) + " " + "f1: " + String.format("%.5f", f1) + " len_predicted_outside_train: " + len_predicted_outside_train + "\n";
        w.write(str);
        w.close();
    }


    public static void main(String[] args) throws Exception {
        String s = "%1.00;0.61%";
        String ts;
        ts = trim(s, '%');
        D.p(ts);
        String split[];
        split = ts.split(";");
        D.p(new TruthValue(Double.parseDouble(split[0]), Double.parseDouble(split[1])));
    }
}
