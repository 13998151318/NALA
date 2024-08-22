import os
from utils import * 
import functools
import random
output_path = "/home/2022xuch/PNAL/PNALoverall/output"
#lang_path = "/zh_en"
fold_path = "/DBP15k_full_zh_en_2_0210_201547"
full_fold_path = output_path + fold_path

zero_seed = True#True
filter = 0#0.1

dataset_path = "/home/2022xuch/PNAL/datasets/DBP15k_full_zh_en_2"
run = 19 #21
eqv_path = full_fold_path + "/output/" + str(run) + "_eqv.tsv"
train_path = full_fold_path + "/" + "train" + "_links"
valid_path = full_fold_path + "/" + "valid" + "_links"
test_path = full_fold_path + "/" + "test" + "_links"
inspect_path = full_fold_path + "/inspect"

attr_path = dataset_path + "/attr_triples"
attr_paths = [attr_path + "_1", attr_path + "_2"]
rel_path = dataset_path + "/rel_triples"
rel_paths = [rel_path + "_1", rel_path + "_2"]

eqv = []
eqv_2 = []
eqv_dict = dict()
with open(eqv_path, 'r', encoding='utf-8') as file:
    for line in file:
        line = line.strip('\n').split('\t')
        if len(line)!= 3:
            continue
        f = (float)(line[2].strip('%').split(';')[0])
        c = (float)(line[2].strip('%').split(';')[1])
        exp = f * c
        if exp > filter:
            eqv.append((line[0], line[1], exp, (line[2])))
            eqv_2.append((line[0], line[1]))
            eqv_dict[line[0]] = line[1]

if not os.path.exists(inspect_path):
    os.mkdir(inspect_path)


inspect_path1 = inspect_path + "/inspect_" + str(run) + "_eqv.tsv"



set_train = set()
with open(train_path, 'r', encoding='utf-8') as file:
    for line in file:
        line = line.strip('\n').split('\t')
        if len(line)!= 2:
            continue
        set_train.add((line[0], line[1]))

set_valid = set()
with open(valid_path, 'r', encoding='utf-8') as file:
    for line in file:
        line = line.strip('\n').split('\t')
        if len(line)!= 2:
            continue
        set_valid.add((line[0], line[1]))

set_test = set()
with open(test_path, 'r', encoding='utf-8') as file:
    for line in file:
        line = line.strip('\n').split('\t')
        if len(line)!= 2:
            continue
        set_test.add((line[0], line[1]))
if zero_seed:
    valid_test = set_valid | set_test | set_train
    set_train = set()
else:
    valid_test = set_valid | set_test

res_no_train = []
for align in eqv:
    if (align[0], align[1]) not in set_train:
        # Evaluate only the alignments that were not present in the training data (seed).
        res_no_train.append(align)

res_no_train_no_vt = []
for align in res_no_train:
    if (align[0], align[1]) not in valid_test:
        res_no_train_no_vt.append(align)

valid_test_no_res = []
for align in valid_test:
    if (align[0], align[1]) not in eqv_2:
        valid_test_no_res.append(align)

if not os.path.exists(inspect_path):
    os.mkdir(inspect_path)


def compute_prec_rec_f1_1(aligns, truth_links):

    aligns = set((align[0], align[1]) for align in aligns)
    truth_links = set(truth_links)
    num_correct = len(aligns.intersection(truth_links))
    if num_correct == 0 or len(aligns) == 0:
        print("Got 0, 0, 0 in evaluation!!!")
        return 0, 0, 0
    precision = num_correct / len(aligns)
    recall = num_correct / len(truth_links)
    f1 = 2 * precision * recall / (precision + recall)
    return precision, recall, f1

def average_exp():
    avg = 0
    for eq in eqv:
        avg += eq[2]
    avg = avg / len(eqv)  
    print("average_exp: ", avg)
    return avg

inspect_path1 = inspect_path + "/inspect_" + str(run) + "_eqv.tsv"
with open(inspect_path1, 'w', encoding='utf-8') as file:
    precision, recall, f1 = compute_prec_rec_f1_1(res_no_train, valid_test)
    file.write(f"run:{run}  precision:{precision:.5f}   recall:{recall:.5f} f1:{f1:.5f} len(res_no_train):{len(res_no_train)} len(res_no_train_no_vt):{len(res_no_train_no_vt)}\n")
    file.write(f"len(valid_test_no_res):{len(valid_test_no_res)}\n valid_test_no_res: \n")
    avg = average_exp()
    file.write(f"average_exp:{avg}")


average_exp()
