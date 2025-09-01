import argparse
import os
from utils import * 
import numpy as np

def run_nala_experiment(dataset, dataset_division, table_setting = 5, bootstrap = 1):
    train_times = []
    #print(os.path.abspath(".."))
    root_dataset = os.path.abspath("..").replace("\\", "/") + "/datasets"
    dataset_in = root_dataset + "/" + dataset + "/"
    out_folder = os.path.abspath(".").replace("\\", "/") + "/output" 
    if dataset[:3] == "D_W":
        dataset_type = "DW"
    elif dataset[:3] == "D_Y":
        dataset_type = "DY"
    elif dataset[:11] == "DBP15K_FULL":
        dataset_type = "DBP15K_FULL"
    else:
        dataset_type = "DBP"
    kg1_file = dataset + "kg1.nt"
    kg2_file = dataset + "kg2.nt"

    _1v1_range_assumption = True
    if table_setting == -2:
        zero_seed = False #False  #True
        no_attr = True
        _1v1_range_assumption = False
    elif table_setting == -1:
        zero_seed = False #False  #True
        no_attr = True
    elif table_setting == 0:  # table_setting == 0 represents bootstrap_1 for table_setting == 1,2,5
        zero_seed = True #False  #True
        no_attr = False
    elif table_setting == 1:
        if bootstrap == 1:
            zero_seed = True #False  #True
        else:
            zero_seed = False
        no_attr = False
    elif table_setting == 2:
        zero_seed = True #False  #True
        no_attr = False
    elif table_setting == 3:
        zero_seed = True #False  #True
        no_attr = True
    elif table_setting == 4:
        zero_seed = False #False  #True
        no_attr = False
    elif table_setting == 5:
        zero_seed = True #False  #True
        no_attr = False
        #if dataset_type != "DBP":
        #    _1v1 = False
    elif table_setting >= 6 and table_setting <= 15:
        zero_seed = True #False  #True
        no_attr = False
        if table_setting == 9:# or dataset_type != "DBP"
            _1v1_range_assumption = False
    if dataset_type == "DBP15K_FULL":
        _1v1_range_assumption = False
    if _1v1_range_assumption:
        kg1_1v1_range_assumption = dataset + "_kg1_1v1_range_assumption.nt"
        kg2_1v1_range_assumption = dataset + "_kg2_1v1_range_assumption.nt"
    else:
        kg1_1v1_range_assumption = None
        kg2_1v1_range_assumption = None
    train_path = dataset + "_train.nt"
    valid_path = dataset + "_valid.nt"
    test_path = dataset + "_test.nt"
    train_valid_test = [train_path, valid_path, test_path]
    for fold in os.listdir(dataset_in + dataset_division):
        print(f"fold: {fold}")
        start_time = time.time()
        create_nt(dataset_in, dataset_division, fold, kg1_file, kg2_file, kg1_1v1_range_assumption, kg2_1v1_range_assumption, zero_seed, no_attr, train_valid_test)
        #entity_emb_sim_confidence_ = [0.8] #, 0.7,0.75,0.8,0.85,0.9,0.95
        #for entity_emb_sim_confidence in entity_emb_sim_confidence_:
        task_name = run_nala(dataset_in, out_folder, dataset, kg1_file, kg2_file, kg1_1v1_range_assumption, kg2_1v1_range_assumption, _1v1_range_assumption, zero_seed, no_attr, table_setting, dataset_type, train_valid_test, bootstrap)

        train_time = time.time() - start_time
        start_time = time.time()
        train_times.append(train_time)
    os.system("rm " + kg1_file)
    os.system("rm " + kg2_file)
    os.system("rm " + train_path)
    os.system("rm " + valid_path)
    os.system("rm " + test_path)
    os.system("rm " + kg1_1v1_range_assumption)
    os.system("rm " + kg2_1v1_range_assumption)
    train_times = np.array(train_times)
    print("Train times: ", train_times)
    print("Train times:\n\tavg: {}\n\tstd: {}".format(train_times.mean(), train_times.std()))



if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Run one experiment with a given dataset")
    #parser.add_argument("--root_dataset", type=str, help="Path to dataset root folder (no slash in the end)")
    parser.add_argument("--dataset", type=str, help="Dataset to use (no slash in the end)")
    parser.add_argument("--dataset_division", type=str, help="Dataset fold division (no slash in the end)")
    #parser.add_argument("--out_folder", type=str, help="Root folder for output (no slash in the end)")
    parser.add_argument("--table_setting", type=int, help="setting group number or ablation study number presented in the paper (no slash in the end)")
    parser.add_argument("--bootstrap", type=int, help="bootstrap number presented in the paper (no slash in the end)")

    args = parser.parse_args()
    run_nala_experiment(args.dataset, args.dataset_division, args.table_setting, args.bootstrap)
