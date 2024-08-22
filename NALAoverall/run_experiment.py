import argparse
import os
from utils import * 
import numpy as np
import json
import copy
import pickle

#datasets/DBP15k_full_zh_en_2/721_1folds

def run_pnal_experiment(dataset, dataset_division, table_setting = 5):
    precisions = []
    recalls = []
    f1s = []
    train_times = []
    test_times = []
    print(os.path.abspath(".."))
    root_dataset = os.path.abspath("..").replace("\\", "/") + "/datasets"
    dataset_in = root_dataset + "/" + dataset + "/"
    out_folder = os.path.abspath(".").replace("\\", "/") + "/output" 
    if dataset[:3] == "D_W":
        dataset_type = "DW"
    elif dataset[:3] == "D_Y":
        dataset_type = "DY"
    else:
        dataset_type = "DBP"
    kg1_file = dataset + "kg1.nt"
    kg2_file = dataset + "kg2.nt"
    #table_setting = 5
    if table_setting == -1:
        zero_seed = False #False  #True
        no_attr = True
        _1v1 = True
    elif table_setting == 0:
        zero_seed = True #False  #True
        no_attr = False
        _1v1 = True
    elif table_setting == 1:
        zero_seed = False #False  #True
        no_attr = False
        _1v1 = True
    elif table_setting == 2:
        zero_seed = True #False  #True
        no_attr = False
        _1v1 = True
    elif table_setting == 3:
        zero_seed = True #False  #True
        no_attr = True
        _1v1 = True
    elif table_setting == 4:
        zero_seed = False #False  #True
        no_attr = False
        _1v1 = True
    elif table_setting == 5:
        zero_seed = True #False  #True
        no_attr = False
        _1v1 = True
        #if dataset_type != "DBP":
        #    _1v1 = False
    elif table_setting >= 6 and table_setting <= 11:
        zero_seed = True #False  #True
        no_attr = False
        _1v1 = True
        if table_setting == 9:# or dataset_type != "DBP"
            _1v1 = False
    if _1v1:
        kg1_1v1_assumption = dataset + "kg1_1v1_assumption.nt"
        kg2_1v1_assumption = dataset + "kg2_1v1_assumption.nt"
    else:
        kg1_1v1_assumption = None
        kg2_1v1_assumption = None
    
    only_evaluate = 0
    
    if only_evaluate == 1:
        max_run = 10
        precisions = [[] for _ in range(max_run)]
        recalls = [[] for _ in range(max_run)]
        f1s = [[] for _ in range(max_run)]
        len_res_no_trains = [[] for _ in range(max_run)]

        out_folder = "/home/2022xuch/paris/entity-matchers-master/output/DBP15k_full/数据集预处理2/xch2.0_no_1v1"
        for lang in os.listdir(out_folder):
            out_folder_lang = out_folder + "/" + lang
            print(out_folder_lang)
            #out_folders = os.listdir(out_folder_lang)
            out_folders = []
            for fold in os.listdir(out_folder_lang):
                if fold.startswith("DBP15k_full"):
                    out_folders.append(fold)
            out_folders.sort()
            #    print(fold)
            j = -1
            dataset = "DBP15k_full_" + lang + "_2"
            dataset_in = root_dataset + "/" + dataset + "/"
            for fold in os.listdir(dataset_in + dataset_division):
                j += 1 
                start_time = time.time()
                train_time = time.time() - start_time
                start_time = time.time()
                precision, recall, f1, len_res_no_train = evaluate_paris(out_folder_lang + "/" + out_folders[j], dataset_in, dataset_division, str(j+1))
                test_time = time.time() - start_time
                
                for run in range(max_run):
                    precisions[run].append(precision[run])
                    recalls[run].append(recall[run])
                    f1s[run].append(f1[run])
                    len_res_no_trains[run].append(len_res_no_train[run])
            
            with open(out_folder_lang + f"/full_result_avg.log", "w",  encoding='utf-8') as f:
                for run in range(max_run):
                    precision_array = np.array(precisions[run])
                    recall_array = np.array(recalls[run])
                    f1_array = np.array(f1s[run])
                    len_res_no_train_array = np.array(len_res_no_train[run])
                    f.write(f"run:{run}     avg. precision:{precision_array.mean():.5f}   recall:{recall_array.mean():.5f} f1:{f1_array.mean():.5f} len(res_no_train):{len_res_no_train_array.mean()}\n")
                #break
            #break
    else:
        for fold in os.listdir(dataset_in + dataset_division):
            print(f"fold: {fold}")
            start_time = time.time()
            create_nt(dataset_in, dataset_division, fold, kg1_file, kg2_file, kg1_1v1_assumption, kg2_1v1_assumption, zero_seed, no_attr)
            #paris_out_folder = out_folder + "/" + "DBP15k_full_zh_en_2_1119_183805"
            #entity_emb_sim_confidence_ = [0.8] #, 0.7,0.75,0.8,0.85,0.9,0.95
            #for entity_emb_sim_confidence in entity_emb_sim_confidence_:
            paris_out_folder, endIteration = run_paris(dataset_in, out_folder, dataset, kg1_file, kg2_file, kg1_1v1_assumption, kg2_1v1_assumption, _1v1, zero_seed, no_attr, table_setting, dataset_type)
            #endIteration = 22
            train_time = time.time() - start_time
            start_time = time.time()
            precision, recall, f1, _ = evaluate_paris(paris_out_folder, dataset_in, dataset_division, fold, endIteration, zero_seed)
            test_time = time.time() - start_time

            precisions.append(precision)
            recalls.append(recall)
            f1s.append(f1)
            train_times.append(train_time)
            test_times.append(test_time)
    os.system("rm kg1.nt")
    os.system("rm kg2.nt")
    os.system("rm kg1_1v1_assumption.nt")
    os.system("rm kg2_1v1_assumption.nt")

    precisions = np.array(precisions)
    recalls = np.array(recalls)
    f1s = np.array(f1s)
    train_times = np.array(train_times)
    test_times = np.array(test_times)

    print("precisions: ", precisions)
    print("recalls: ", recalls)
    print("f1s: ", f1s)
    print("Train times: ", train_times)
    print("Test times: ", test_times)

    print("precisions:\n\tavg: {}\n\tstd: {}".format(precisions.mean(), precisions.std()))
    print("recalls:\n\tavg: {}\n\tstd: {}".format(recalls.mean(), recalls.std()))
    print("f1s:\n\tavg: {}\n\tstd: {}".format(f1s.mean(), f1s.std()))
    print("Train times:\n\tavg: {}\n\tstd: {}".format(train_times.mean(), train_times.std()))
    print("Test times:\n\tavg: {}\n\tstd: {}".format(test_times.mean(), test_times.std()))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Run one experiment with a given dataset")
    #parser.add_argument("--root_dataset", type=str, help="Path to dataset root folder (no slash in the end)")
    parser.add_argument("--dataset", type=str, help="Dataset to use (no slash in the end)")
    parser.add_argument("--dataset_division", type=str, help="Dataset fold division (no slash in the end)")
    #parser.add_argument("--out_folder", type=str, help="Root folder for output (no slash in the end)")
    parser.add_argument("--table_setting", type=int, help="confuguration group number or ablation study number presented in the paper (no slash in the end)")

    args = parser.parse_args()
    run_pnal_experiment(args.dataset, args.dataset_division, args.table_setting)
